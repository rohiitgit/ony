package com.ony.app

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Android-side update operations: fetch the static manifest, post an optional local
 * notification (respecting Android 13+ notification permission), and download/install the APK
 * in-app when possible (FileProvider + installer intent). Falls back to the download page.
 *
 * This never blocks core functionality; callers should run checks off the main path and ignore
 * failures.
 */
object UpdateClient {
    private const val CHANNEL_ID = "ony_updates"
    private const val NOTIFICATION_ID = 9001
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    /** How often to check (battery/network conscious). */
    const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

    /** Snooze duration when the user taps "Later" in the prompt. */
    const val SNOOZE_MS = 3L * 24 * 60 * 60 * 1000

    fun fetchManifest(url: String = UpdateEndpoints.DEFAULT_MANIFEST_URL): UpdateManifest? =
        fetchManifestInternal(url)

    private fun fetchManifestInternal(url: String): UpdateManifest? {
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                // Avoid stale caches hiding a just-published manifest, but keep it lightweight.
                useCaches = false
            }
            try {
                if (conn.responseCode !in 200..299) return null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                UpdateCheck.parseManifest(body)
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** True if we can post notifications (runtime permission on Android 13+). */
    fun canPostNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_LOW, // optional/non-intrusive
        ).apply {
            description = "Lets you know when a new version of Ony is available."
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * Posts a local notification about [manifest]. Requires notification permission on
     * Android 13+; callers should check [canPostNotifications] and skip gracefully if not.
     */
    fun notifyUpdate(context: Context, manifest: UpdateManifest) {
        if (!canPostNotifications(context)) return
        ensureChannel(context)
        val openIntent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_UPDATE_VERSION, manifest.versionCode)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, openIntent, flags)
        val text = "Ony ${manifest.versionName} is available."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Update available")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text Tap to update or get the download."))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked or restricted; ignore.
        }
    }

    const val EXTRA_UPDATE_VERSION = "update_version_code"

    /** Opens the best available destination for [manifest] (download page/release) in a browser. */
    fun openDownloadPage(context: Context, manifest: UpdateManifest?): Boolean {
        val url = manifest?.let { UpdateCheck.bestActionUrl(it) }
            ?: UpdateEndpoints.DEFAULT_DOWNLOAD_PAGE
        return openUrl(context, url)
    }

    /**
     * Opens a web fallback after an in-app download/install failed: prefer the human-readable
     * release or download page over retrying the same (broken) direct APK URL in a browser.
     */
    fun openFallbackPage(context: Context, manifest: UpdateManifest?): Boolean {
        val url = manifest?.let {
            it.releaseUrl?.takeIf { u -> u.isNotBlank() }
                ?: it.pageUrl?.takeIf { u -> u.isNotBlank() }
                ?: it.downloadUrl?.takeIf { u -> u.isNotBlank() }
        } ?: UpdateEndpoints.DEFAULT_DOWNLOAD_PAGE
        return openUrl(context, url)
    }

    fun openUrl(context: Context, url: String): Boolean = try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) {
        false
    }

    /** True if the app can request installing unknown packages (Android 8+). */
    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 26) context.packageManager.canRequestPackageInstalls() else true

    /** Opens the system screen to allow installs from this app (Android 8+). */
    fun openUnknownSourcesSettings(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) { }
        }
    }

    /**
     * Attempts to download [apkUrl] into app cache and trigger the package installer. Returns
     * true if the install intent was launched; false if download/install couldn't proceed (caller
     * should redirect to the download page). Runs blocking network I/O — call from Dispatchers.IO.
     */
    fun downloadAndInstall(context: Context, apkUrl: String, versionName: String): Boolean {
        return try {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            // Clean older APKs to avoid filling cache.
            dir.listFiles()?.forEach { if (it.name.endsWith(".apk")) it.delete() }
            val safeName = "Ony-${versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")}.apk"
            val out = File(dir, safeName)
            val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = 30_000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/vnd.android.package-archive,*/*")
            }
            try {
                if (conn.responseCode !in 200..299) return false
                conn.inputStream.use { input -> out.outputStream().use { output -> input.copyTo(output) } }
            } finally {
                conn.disconnect()
            }
            if (!out.exists() || out.length() <= 0L) return false
            installApk(context, out)
        } catch (_: Exception) {
            false
        }
    }

    private fun installApk(context: Context, file: File): Boolean = try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
