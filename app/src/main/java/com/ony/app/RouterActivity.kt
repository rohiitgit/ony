package com.ony.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Invisible link router: a tapped (or shared) music link comes in, the same song opens in
 * the listener's app, and this activity closes. Its window is transparent. Nothing is drawn
 * for instant redirects; a small card appears only if the lookup passes 350 ms, or for the
 * one-time "which app?" question.
 *
 * State: target (the listener's app, null until known), asking (no app known yet), slow.
 * Invariants: finishes after every outcome (opened, fell back, or cancelled).
 */
class RouterActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        route(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        route(intent)
    }

    private fun route(intent: Intent) {
        val link = LinkParser.parse(incomingText(intent))
        if (link == null) {
            Toast.makeText(this, "No music link found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        // A tapped link (not a share) proves Android now hands this kind of link to Ony.
        val caught = if (intent.action == Intent.ACTION_VIEW) Setup.kindOf(intent.dataString) else null
        val tappedAt = SystemClock.elapsedRealtime()
        setContent { OnyTheme(dark = rememberDarkTheme()) { RedirectCard(link, caught, tappedAt, onDone = ::finish) } }
    }

    private fun incomingText(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
        else -> intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT)
    }
}

@Composable
private fun RedirectCard(link: MusicLink, caught: LinkKind?, tappedAt: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val installed = remember { Launch.installedApps(context) }
    var target by remember(link) { mutableStateOf<MusicApp?>(null) }
    var asking by remember(link) { mutableStateOf(false) }
    // Instant redirects finish before this flips, so they never draw anything.
    var slow by remember(link) { mutableStateOf(false) }

    LaunchedEffect(link) {
        caught?.let { Prefs.markCaught(context, it) }
        target = Redirect.targetApp(Prefs.myApp(context), installed)
        asking = target == null
    }

    LaunchedEffect(link) {
        delay(350)
        slow = true
    }

    LaunchedEffect(link, target) {
        val app = target ?: return@LaunchedEffect
        val dest = Redirect.resolve(link, app, Launch.country(context))
        val opened = dest != null && Launch.open(context, dest.url, dest.app.packageName)
        if (opened) Prefs.recordOpen(context, SystemClock.elapsedRealtime() - tappedAt)
        if (!opened) {
            Toast.makeText(context, "Couldn't find this song in ${app.label}. Opening the original link.", Toast.LENGTH_LONG).show()
            if (!Launch.openOriginal(context, link)) {
                Toast.makeText(context, "No app can open this link", Toast.LENGTH_SHORT).show()
            }
        }
        onDone()
    }

    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(12.dp), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = asking || slow,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 3 },
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Ony.Block,
                contentColor = Ony.OnBlock,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (asking) {
                    AppQuestion(installed) { app ->
                        scope.launch { Prefs.setMyApp(context, app) }
                        asking = false
                        target = app
                    }
                } else {
                    Opening(target, onCancel = onDone)
                }
            }
        }
    }
}

@Composable
private fun Opening(app: MusicApp?, onCancel: () -> Unit) {
    Column {
        // Same 20 dp inset on both sides as the setup screen; equal top and bottom.
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (app != null) AppIcon(app, size = 42.dp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(app?.let { "Opening in ${it.label}" } ?: "Opening", style = MaterialTheme.typography.titleMedium, color = Ony.OnBlock)
                Text("Finding the song", style = MaterialTheme.typography.bodyMedium, color = Ony.BlockMuted)
            }
            PillButton("Cancel", onCancel, style = PillStyle.LAVENDER)
        }
        LinearProgressIndicator(
            color = Ony.Accent,
            trackColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().height(2.dp),
        )
    }
}

@Composable
private fun AppQuestion(installed: Set<MusicApp>, onPick: (MusicApp) -> Unit) {
    val choices = Redirect.TARGETS.filter { it in installed }.ifEmpty { Redirect.TARGETS }
    Column(Modifier.padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 10.dp)) {
        Text("Which app do you listen in?", style = MaterialTheme.typography.titleMedium, color = Ony.OnBlock, modifier = Modifier.padding(horizontal = 10.dp))
        Text(
            "Every song link will open there. You can change it in Ony.",
            style = MaterialTheme.typography.bodyMedium,
            color = Ony.BlockMuted,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
        )
        choices.forEach { app -> InkChoice(app, chosen = false, installed = true) { onPick(app) } }
    }
}
