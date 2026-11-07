package com.ony.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * One-time setup, opened from the launcher. Re-reads system state every time Ony resumes,
 * because the user leaves for Android settings and comes back.
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

    // onResume, not window focus: with the links prompt open, focus returns to the sheet's own
    // window, but the activity still resumes, so the ticks and count stay current.
    override fun onResume() {
        super.onResume()
        returns++
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
    var appOpen by rememberSaveable { mutableStateOf(false) }
    // Setup state isn't known for a moment after opening; wait for it so the prompt never flashes.
    var loaded by remember { mutableStateOf(false) }
    var promptDismissed by rememberSaveable { mutableStateOf(false) }

    var updateManifest by remember { mutableStateOf<UpdateManifest?>(null) }
    var updatePromptVisible by rememberSaveable { mutableStateOf(false) }
    var updateBusy by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* If granted later, next check/notification cycle can use it; prompt remains in-app. */ }

    LaunchedEffect(returns) {
        installed = Launch.installedApps(context)
        allowed = Launch.allowedDomains(context)
        ownLinks = installed.associateWith { Launch.opensOwnLinks(context, it.packageName) }
        caught = Prefs.caughtKinds(context)
        myApp = Prefs.myApp(context)
        loaded = true
    }

    // Optional, non-blocking update check on launch/resume (throttled). Never interferes with setup.
    LaunchedEffect(returns) {
        val installedCode = try { BuildConfig.VERSION_CODE.toLong() } catch (_: Exception) { 0L }
        if (installedCode <= 0L) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val last = Prefs.updateLastCheckMs(context)
        if (!UpdateCheck.shouldCheck(last, now, UpdateClient.CHECK_INTERVAL_MS)) return@LaunchedEffect
        val manifest = withContext(Dispatchers.IO) { UpdateClient.fetchManifest() } ?: run {
            // Record an attempt time only on success-like handling? We avoid spamming by still
            // not writing on hard failure except we could; better throttle attempts by writing.
            Prefs.setUpdateLastCheckMs(context, now)
            return@LaunchedEffect
        }
        Prefs.setUpdateLastCheckMs(context, now)
        if (!UpdateCheck.isUpdateAvailable(installedCode, manifest)) return@LaunchedEffect

        val snoozeUntil = Prefs.updateSnoozeUntilMs(context)
        val dismissed = Prefs.updateDismissedVersion(context)
        val show = manifest.versionCode > dismissed && now >= snoozeUntil &&
            UpdateCheck.shouldShowPrompt(installedCode, manifest, dismissed)
        if (show) {
            Prefs.clearUpdateSnooze(context)
            updateManifest = manifest
            updatePromptVisible = true
        }

        // Local notification is optional: only if we haven't notified for this version and we can.
        val lastNotified = Prefs.updateLastNotifiedVersion(context)
        if (UpdateCheck.shouldNotify(installedCode, manifest, lastNotified)) {
            if (UpdateClient.canPostNotifications(context)) {
                UpdateClient.notifyUpdate(context, manifest)
                Prefs.setUpdateLastNotifiedVersion(context, manifest.versionCode)
            } else if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Don't force a disruptive prompt here; in-app prompt covers it. Permission can be
                // requested from the update prompt context if desired, but we keep it optional.
            }
        }
    }

    val target = Redirect.targetApp(myApp, installed)
    // Android's own answer wins; a link Ony actually received stands in when Android won't say.
    val grabbers = Setup.grabbers(installed, target).filter { kind -> ownLinks[kind.owner] ?: (kind !in caught) }
    val linksOn = Setup.linksAllowed(allowed, target)
    // Legacy cleanup: older Ony had users switch YouTube's links off while Ony intercepted
    // YouTube taps. Ony no longer does, so that leftover state sends Shorts to the browser
    // until the user switches YouTube's handling back on (only they can).
    val youtubeBroken = Setup.youtubeHandoffBroken(MusicApp.YOUTUBE in installed, ownLinks[MusicApp.YOUTUBE])
    val stepsLeft = listOf(target != null, linksOn, grabbers.isEmpty(), !youtubeBroken).count { !it }

    val mode by remember { Prefs.themeMode(context) }.collectAsState(initial = ThemeMode.AUTO)

    Box(Modifier.fillMaxSize().background(Ony.Page).windowInsetsPadding(WindowInsets.safeDrawing)) {
    Column(
        // Bottom padding keeps the end of the list clear of the bottom bar.
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = BottomBarSpace),
    ) {
        Text(
            "ony",
            style = MaterialTheme.typography.displaySmall,
            color = Ony.OnPage,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
        )
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
            allowed?.let { domains ->
                val (on, total) = Setup.linksOn(domains, target)
                OutlineRow(
                    "Open music links in Ony",
                    onClick = { Launch.openLinkSettings(context, context.packageName) },
                    value = if (linksOn) "On" else "$on of $total on",
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
            if (youtubeBroken) {
                OutlineRow(
                    "Let YouTube open YouTube links",
                    onClick = { Launch.openLinkSettings(context, MusicApp.YOUTUBE.packageName) },
                    value = "Turn on",
                    valueColor = Ony.OnPage,
                    icon = { AppIcon(MusicApp.YOUTUBE, 26.dp) },
                )
            }
        }

        Text(
            when {
                youtubeBroken -> "Ony no longer opens YouTube links when tapped, but YouTube's own handling is still switched off, so Shorts open in the browser. Turn it back on for YouTube."
                grabbers.isNotEmpty() -> "Apps listed above open their own links before Ony sees them. Turn off \"Open supported links\" for each."
                else -> "Ony finds the same song by name, artist and album in your app's catalogue for your country."
            },
            style = MaterialTheme.typography.bodySmall,
            color = Ony.PageMuted,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        )
    }

    // Until Ony may open music links, say so up front, every time Ony opens.
    val domains = allowed
    val linksPromptShowing = loaded && domains != null && !linksOn && !promptDismissed
    if (linksPromptShowing) {
        LinksPrompt(
            hosts = Setup.kindsToCatch(target).flatMap { it.hosts },
            on = domains,
            onOpenSettings = { Launch.openLinkSettings(context, context.packageName) },
            onLater = { promptDismissed = true },
        )
    }

    // Optional update prompt (non-blocking). Suppressed while the setup links prompt is showing.
    val manifest = updateManifest
    if (!linksPromptShowing && updatePromptVisible && manifest != null) {
        UpdatePrompt(
            manifest = manifest,
            busy = updateBusy,
            onUpdate = {
                scope.launch {
                    updateBusy = true
                    try {
                        val apkUrl = manifest.downloadUrl
                        val installed = if (!apkUrl.isNullOrBlank() && apkUrl.endsWith(".apk", ignoreCase = true)) {
                            // Android 8+: need install permission; guide user if missing.
                            if (Build.VERSION.SDK_INT >= 26 && !UpdateClient.canInstallPackages(context)) {
                                Toast.makeText(
                                    context,
                                    "Allow Ony to install updates, then try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                UpdateClient.openUnknownSourcesSettings(context)
                                false
                            } else {
                                withContext(Dispatchers.IO) {
                                    UpdateClient.downloadAndInstall(context, apkUrl, manifest.versionName)
                                }
                            }
                        } else false
                        if (!installed) {
                            // Fallback: open the release/download page (not the same broken APK
                            // URL) — helps on Chrome / in-app browser cases.
                            if (!UpdateClient.openFallbackPage(context, manifest)) {
                                Toast.makeText(context, "Couldn't open the download page.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } finally {
                        updateBusy = false
                    }
                }
            },
            onLater = {
                updatePromptVisible = false
                scope.launch { Prefs.setUpdateSnoozeUntilMs(context, System.currentTimeMillis() + UpdateClient.SNOOZE_MS) }
            },
            onSkip = {
                updatePromptVisible = false
                scope.launch { Prefs.setUpdateDismissedVersion(context, manifest.versionCode) }
            },
        )
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

/**
 * The one setup step Android won't let an app do itself: approving Ony for other companies'
 * links. A sheet with the addresses to switch on, each ticked once it is, and a button to
 * the right Settings page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinksPrompt(hosts: List<String>, on: Set<String>, onOpenSettings: () -> Unit, onLater: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ony.Block,
        contentColor = Ony.OnBlock,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("Let Ony open music links", style = MaterialTheme.typography.headlineSmall, color = Ony.OnBlock)
            Text(
                "Android asks you to do this once. Tap Open settings, then switch on these addresses.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ony.BlockMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            hosts.forEach { host ->
                val done = host in on
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (done) "On" else "Off",
                        tint = if (done) Ony.Accent else Ony.BlockMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(host, style = MaterialTheme.typography.bodyLarge, color = Ony.OnBlock, modifier = Modifier.padding(start = 12.dp))
                }
            }
            Row(Modifier.padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton("Open settings", onOpenSettings, modifier = Modifier.weight(1f))
                TextButton(onClick = onLater, modifier = Modifier.padding(start = 8.dp)) {
                    Text("LATER", style = MaterialTheme.typography.labelLarge, color = Ony.OnBlock)
                }
            }
        }
    }
}

/** Optional, non-intrusive update prompt. Matches the existing ink-block visual style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePrompt(
    manifest: UpdateManifest,
    busy: Boolean,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onSkip: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onLater,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ony.Block,
        contentColor = Ony.OnBlock,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Text("Update available", style = MaterialTheme.typography.headlineSmall, color = Ony.OnBlock)
            Text(
                "Ony ${manifest.versionName} is ready. Updating is optional and keeps your settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ony.BlockMuted,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            Text(
                "If the download doesn't finish in an in-app browser, open the download page in Chrome.",
                style = MaterialTheme.typography.bodySmall,
                color = Ony.BlockMuted,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Ony.Accent, strokeWidth = 2.dp)
                    Text("Downloading…", style = MaterialTheme.typography.bodyMedium, color = Ony.OnBlock, modifier = Modifier.padding(start = 10.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PillButton(if (busy) "Downloading" else "Update", onUpdate, modifier = Modifier.weight(1f))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onLater, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("LATER", style = MaterialTheme.typography.labelLarge, color = Ony.OnBlock)
                }
                TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text("SKIP THIS VERSION", style = MaterialTheme.typography.labelLarge, color = Ony.BlockMuted)
                }
            }
        }
    }
}
