package com.ony.app

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/** The Android side of opening things. Every open is pinned to a package, never left to Ony. */
object Launch {

    fun installedApps(context: Context): Set<MusicApp> =
        MusicApp.entries.filter { isInstalled(context, it.packageName) }.toSet()

    /** Opens [url] in [packageName] only. False when that app is missing or can't open it. */
    fun open(context: Context, url: String, packageName: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage(packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /** For a song the listener's app doesn't have: the link in its own app, else the browser. */
    fun openOriginal(context: Context, link: MusicLink): Boolean {
        Resolver.original(link)?.let { if (open(context, it.url, it.app.packageName)) return true }
        val browser = browserPackage(context) ?: return false
        return open(context, link.originalUrl, browser)
    }

    /** Android's "Open by default" page for [packageName] (Android 12+), else its app info page. */
    fun openLinkSettings(context: Context, packageName: String) {
        val uri = Uri.parse("package:$packageName")
        val action = if (Build.VERSION.SDK_INT >= 31) {
            Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS
        } else {
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        try {
            context.startActivity(Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /**
     * Link domains the user has let Ony open (Android 12+). Null below Android 12, where
     * the "Open with" chooser still appears and nothing needs turning on.
     */
    fun allowedDomains(context: Context): Set<String>? {
        if (Build.VERSION.SDK_INT < 31) return null
        val state = context.getSystemService(DomainVerificationManager::class.java)
            ?.getDomainVerificationUserState(context.packageName) ?: return emptySet()
        return state.hostToStateMap.filterValues { it != DomainVerificationUserState.DOMAIN_STATE_NONE }.keys
    }

    /** A web page in a browser tab over Ony (Custom Tab), so closing it lands back here. */
    fun openWeb(context: Context, url: String) {
        val tab = CustomTabsIntent.Builder().setShowTitle(true).build()
        try {
            tab.launchUrl(context, Uri.parse(url))
        } catch (_: ActivityNotFoundException) {
            browserPackage(context)?.let { open(context, url, it) }
        }
    }

    /** Clipboard text. Android only allows this while Ony is in front with focus. */
    fun clipboardText(context: Context): String? =
        context.getSystemService(ClipboardManager::class.java)
            ?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()

    /** Sends a link the way a tap in another app does, unpinned, to see who Android hands it to. */
    fun fireTestLink(context: Context, url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /** An installed app's launcher icon, or null if it isn't installed. */
    fun appIcon(context: Context, packageName: String): ImageBitmap? = try {
        context.packageManager.getApplicationIcon(packageName).toBitmap(144, 144).asImageBitmap()
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /**
     * Whether [packageName] still opens its own verified links (Android 12+). False means the
     * user turned off "Open supported links" for it. Null when Android won't say.
     */
    fun opensOwnLinks(context: Context, packageName: String): Boolean? {
        if (Build.VERSION.SDK_INT < 31) return null
        return try {
            context.getSystemService(DomainVerificationManager::class.java)
                ?.getDomainVerificationUserState(packageName)?.isLinkHandlingAllowed
        } catch (_: Exception) {
            null
        }
    }

    private fun isInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    // example.com is not a domain Ony handles, so whatever opens it is a browser.
    private fun browserPackage(context: Context): String? {
        val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).addCategory(Intent.CATEGORY_BROWSABLE)
        val pm = context.packageManager
        val default = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        if (default != null && default != "android" && default != context.packageName) return default
        return pm.queryIntentActivities(probe, 0).map { it.activityInfo.packageName }.firstOrNull { it != context.packageName }
    }
}
