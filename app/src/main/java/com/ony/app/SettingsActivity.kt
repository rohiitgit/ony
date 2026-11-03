package com.ony.app

import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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

    // Window focus, not onResume: Android only lets the focused app read the clipboard.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) returns++
    }
}

// Signed out, this shows MusicLink's sign-in (with Google and a sign-up link), then the keys page.
private const val KEYS_URL = "https://musiclink.one/dashboard/keys"

private enum class Fold { APP, KEY }

@Composable
private fun SetupScreen(returns: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var installed by remember { mutableStateOf(emptySet<MusicApp>()) }
    var allowed by remember { mutableStateOf<Set<String>?>(emptySet()) }
    var ownLinks by remember { mutableStateOf(emptyMap<MusicApp, Boolean?>()) }
    var caught by remember { mutableStateOf(emptySet<LinkKind>()) }
    var myApp by remember { mutableStateOf<MusicApp?>(null) }
    var savedKey by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf(0 to 0L) }
    var lookups by remember { mutableIntStateOf(0) }
    var copiedKey by remember { mutableStateOf<String?>(null) }
    var awaitingKey by rememberSaveable { mutableStateOf(false) }
    var open by rememberSaveable { mutableStateOf<Fold?>(null) }

    LaunchedEffect(returns) {
        installed = Launch.installedApps(context)
        allowed = Launch.allowedDomains(context)
        ownLinks = installed.associateWith { Launch.opensOwnLinks(context, it.packageName) }
        caught = Prefs.caughtKinds(context)
        myApp = Prefs.myApp(context)
        savedKey = Prefs.savedApiKey(context)
        stats = Prefs.stats(context)
        lookups = Prefs.lookupsThisMonth(context)
        // Only look at the clipboard when coming back from MusicLink's site.
        if (awaitingKey) {
            copiedKey = Setup.looksLikeKey(Launch.clipboardText(context))?.takeIf { it != savedKey }
            if (copiedKey != null) open = Fold.KEY
        }
    }

    val target = Redirect.targetApp(myApp, installed)
    // Android's own answer wins; a link Ony actually received stands in when Android won't say.
    val grabbers = Setup.grabbers(installed, target).filter { kind -> ownLinks[kind.owner] ?: (kind !in caught) }
    val linksOn = Setup.linksAllowed(allowed, target)
    val hasKey = savedKey.isNotBlank() || BuildConfig.MUSICLINK_API_KEY.isNotBlank()
    val stepsLeft = listOf(target != null, linksOn, grabbers.isEmpty(), hasKey).count { !it }

    Column(
        Modifier.fillMaxSize().background(Ony.Page).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(12.dp),
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
                Stat("Lookups", lookups.toString())
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
            UnfoldingRow("Your music app", target?.label ?: "Choose", open == Fold.APP, { open = if (open == Fold.APP) null else Fold.APP }, panelPadding = 8.dp) {
                Redirect.TARGETS.forEach { app ->
                    InkChoice(app, chosen = app == target, installed = app in installed) {
                        myApp = app
                        open = null
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
            UnfoldingRow("MusicLink key", if (hasKey) "Added" else "Add", open == Fold.KEY, { open = if (open == Fold.KEY) null else Fold.KEY }) {
                KeyPanel(
                    savedKey = savedKey,
                    copiedKey = copiedKey,
                    onGetKey = { awaitingKey = true; Launch.openWeb(context, KEYS_URL) },
                    onSaved = { key -> savedKey = key; copiedKey = null; awaitingKey = false; open = null },
                )
            }
        }

        Text(
            if (grabbers.isNotEmpty()) "Apps listed above open their own links before Ony sees them. Turn off \"Open supported links\" for each."
            else "Ony uses MusicLink to find the same song in your app. Free for 300 songs a month.",
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
        Gap(28.dp)
        val mode by remember { Prefs.themeMode(context) }.collectAsState(initial = ThemeMode.AUTO)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Segmented(ThemeMode.entries.map { it.label }, ThemeMode.entries.indexOf(mode)) { i ->
                scope.launch { Prefs.setThemeMode(context, ThemeMode.entries[i]) }
            }
        }
        Gap(24.dp)
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Ony.OnBlock)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Ony.StatValue, modifier = Modifier.padding(top = 2.dp))
    }
}

/**
 * Getting a MusicLink key, on ink: a button to the keys page, a field, a save that checks
 * the key with MusicLink first. A key copied on MusicLink's site is filled in on return.
 */
@Composable
private fun KeyPanel(savedKey: String, copiedKey: String?, onGetKey: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var field by rememberSaveable(copiedKey) { mutableStateOf(copiedKey.orEmpty()) }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<KeyCheck?>(null) }

    fun save() {
        val key = field.trim()
        checking = true
        scope.launch {
            val outcome = MusicLinkApi.checkKey(key)
            result = outcome
            checking = false
            if (outcome != KeyCheck.REJECTED) {
                Prefs.setApiKey(context, key)
                onSaved(key)
            }
        }
    }

    Text(
        "Sign up free, create a key, copy it and come back. Ony fills it in.",
        style = MaterialTheme.typography.bodyMedium,
        color = Ony.BlockMuted,
    )
    Gap(14.dp)
    if (copiedKey != null) {
        Tag("Key found on your clipboard")
        Gap(10.dp)
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ony.BlockRaised).padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f).padding(vertical = 15.dp)) {
            if (field.isEmpty()) {
                Text(if (savedKey.isNotBlank()) "Replace your key" else "Paste your key", style = MaterialTheme.typography.bodyLarge, color = Ony.BlockMuted)
            }
            BasicTextField(
                value = field,
                onValueChange = { field = it; result = null },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ony.OnBlock),
                cursorBrush = SolidColor(Ony.Accent),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text(
            "PASTE",
            style = MaterialTheme.typography.labelLarge,
            color = Ony.Accent,
            modifier = Modifier.clickable { Launch.clipboardText(context)?.let { field = it.trim(); result = null } }
                .padding(horizontal = 16.dp, vertical = 15.dp),
        )
    }
    Gap(14.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton("Get a free key", onGetKey, style = PillStyle.LAVENDER, modifier = Modifier.weight(1f))
        PillButton(if (checking) "Checking" else "Save key", ::save, style = PillStyle.GHOST_ON_INK, enabled = !checking && field.isNotBlank(), modifier = Modifier.weight(1f))
    }
    result?.let {
        Gap(10.dp)
        Text(
            when (it) {
                KeyCheck.WORKS -> "Key works. Saved."
                KeyCheck.REJECTED -> "MusicLink didn't accept this key. Copy it again, all of it."
                KeyCheck.UNREACHABLE -> "Couldn't reach MusicLink to check it. Saved anyway."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (it == KeyCheck.REJECTED) Ony.Accent else Ony.BlockMuted,
        )
    }
}
