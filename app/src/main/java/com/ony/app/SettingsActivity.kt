package com.ony.app

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * One-time setup, opened from the launcher. Re-reads system state whenever Ony regains
 * focus, because the user leaves for Android settings, MusicLink, or a test link.
 */
class SettingsActivity : ComponentActivity() {

    private var returns by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dark = rememberDarkTheme()
            // Bar icons and window follow the app's theme, which can differ from the phone's.
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                window.setBackgroundDrawable(ColorDrawable((if (dark) DarkPalette else LightPalette).page.toArgb()))
            }
            OnyTheme(dark = dark) { SetupScreen(returns) }
        }
    }

    // Re-read setup state when Ony comes back into view (from Android settings or a test link).
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) returns++
    }
}

@Composable
private fun SetupScreen(returns: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var installed by remember { mutableStateOf(emptySet<MusicApp>()) }
    var allowed by remember { mutableStateOf<Set<String>?>(emptySet()) }
    var ownLinks by remember { mutableStateOf(emptyMap<MusicApp, Boolean?>()) }
    var caught by remember { mutableStateOf(emptySet<LinkKind>()) }
    var myApp by remember { mutableStateOf<MusicApp?>(null) }
    var stats by remember { mutableStateOf(0 to 0L) }
    var appOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(returns) {
        installed = Launch.installedApps(context)
        allowed = Launch.allowedDomains(context)
        ownLinks = installed.associateWith { Launch.opensOwnLinks(context, it.packageName) }
        caught = Prefs.caughtKinds(context)
        myApp = Prefs.myApp(context)
        stats = Prefs.stats(context)
    }

    val target = Redirect.targetApp(myApp, installed)
    // Android's own answer wins; a link Ony actually received stands in when Android won't say.
    val grabbers = Setup.grabbers(installed, target).filter { kind -> ownLinks[kind.owner] ?: (kind !in caught) }
    val linksOn = Setup.linksAllowed(allowed, target)
    val stepsLeft = listOf(target != null, linksOn, grabbers.isEmpty()).count { !it }

    val mode by remember { Prefs.themeMode(context) }.collectAsState(initial = ThemeMode.AUTO)

    Box(Modifier.fillMaxSize().background(Ony.Page).windowInsetsPadding(WindowInsets.safeDrawing)) {
    Column(
        // Bottom padding keeps the end of the list clear of the bottom bar.
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = BottomBarSpace),
    ) {
        InkBlock(shape = TopBlock) {
            // Lowercase with a descender sits low in its box; lift it so the "o" centres (measured 2.5 dp).
            Text(
                "ony",
                style = MaterialTheme.typography.titleLarge,
                color = Ony.OnBlock,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().offset(y = (-2.5).dp),
            )
        }
        Joint()
        // Digits have no descenders, so 2 dp less padding below makes the space read even (measured).
        InkBlock(shape = BottomBlock, bottomPadding = 16.dp) {
            // Spread like the reference: first stat on the left inset, last ending on the right inset.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat("Songs", stats.first.toString())
                Stat("Avg. time", Setup.averageTime(stats.second, stats.first))
            }
        }

        Text(
            if (stepsLeft == 0) "Tap any song link.\nOny opens it in ${target?.label}."
            else "A few steps, then every song link\nopens in your app.",
            style = MaterialTheme.typography.bodyLarge,
            color = Ony.OnPage,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            UnfoldingRow("Your music app", target?.label ?: "Choose", appOpen, { appOpen = !appOpen }, panelPadding = 8.dp) {
                Redirect.TARGETS.forEach { app ->
                    InkChoice(app, chosen = app == target, installed = app in installed) {
                        myApp = app
                        appOpen = false
                        scope.launch { Prefs.setMyApp(context, app) }
                    }
                }
            }
            if (allowed != null) {
                OutlineRow(
                    "Open music links in Ony",
                    onClick = { Launch.openLinkSettings(context, context.packageName) },
                    value = if (linksOn) "On" else "Turn on",
                    valueColor = if (linksOn) Ony.PageMuted else Ony.OnPage,
                )
            }
            grabbers.forEach { kind ->
                OutlineRow(
                    "Stop ${kind.owner.label} taking links",
                    onClick = { Launch.openLinkSettings(context, kind.owner.packageName) },
                    value = "Turn off",
                    valueColor = Ony.OnPage,
                    icon = { AppIcon(kind.owner, 26.dp) },
                )
            }
        }

        Text(
            if (grabbers.isNotEmpty()) "Apps listed above open their own links before Ony sees them. Turn off \"Open supported links\" for each."
            else "Ony finds the same song by name, artist and album in your app's catalogue for your country.",
            style = MaterialTheme.typography.bodySmall,
            color = Ony.PageMuted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )

        if (stepsLeft == 0) {
            PillButton(
                "Play a test song",
                onClick = { Launch.fireTestLink(context, Setup.kindsToCatch(target).first().sampleUrl) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    // Bottom bar, as in the reference: stays put while the list scrolls.
    Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) {
        Segmented(
            icons = listOf(Icons.Rounded.BrightnessAuto, Icons.Rounded.LightMode, Icons.Rounded.DarkMode),
            labels = ThemeMode.entries.map { it.label },
            selected = ThemeMode.entries.indexOf(mode),
        ) { i -> scope.launch { Prefs.setThemeMode(context, ThemeMode.entries[i]) } }
    }
    }
}

// Bar height (48 dp) + its 16 dp bottom margin + 24 dp of breathing room above it.
private val BottomBarSpace = 88.dp

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ony.OnBlock)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Ony.StatValue, modifier = Modifier.padding(top = 2.dp))
    }
}
