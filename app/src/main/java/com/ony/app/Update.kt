package com.ony.app

import org.json.JSONObject
import java.net.URI

/**
 * Version manifest published by CI (e.g. on GitHub Pages) so the app can notice updates
 * without a backend. Comparison is by [UpdateManifest.versionCode] (the build number).
 */
data class UpdateManifest(
    val versionName: String,
    val versionCode: Long,
    val tag: String? = null,
    val downloadUrl: String? = null,
    val releaseUrl: String? = null,
    val pageUrl: String? = null,
)

object UpdateCheck {

    /** Parse a static version manifest; returns null on bad/missing fields. */
    fun parseManifest(json: String): UpdateManifest? {
        return try {
            val o = JSONObject(json)
            if (!o.has("versionCode")) return null
            val versionCode = o.optLong("versionCode")
            if (versionCode <= 0) return null
            val versionName = o.optString("versionName").ifBlank { "0.1.$versionCode" }
            UpdateManifest(
                versionName = versionName,
                versionCode = versionCode,
                tag = o.optString("tag").ifBlank { null },
                downloadUrl = o.optString("downloadUrl").ifBlank { null },
                releaseUrl = o.optString("releaseUrl").ifBlank { null },
                pageUrl = o.optString("pageUrl").ifBlank { null },
            )
        } catch (_: Exception) {
            null
        }
    }

    /** True if [manifest] is newer than the currently installed build. */
    fun isUpdateAvailable(installedVersionCode: Long, manifest: UpdateManifest): Boolean =
        manifest.versionCode > installedVersionCode

    /** True if it's been at least [minIntervalMs] since the last check. */
    fun shouldCheck(lastCheckMs: Long, nowMs: Long, minIntervalMs: Long): Boolean {
        if (minIntervalMs <= 0) return true
        if (lastCheckMs <= 0) return true
        if (nowMs < lastCheckMs) return true // clock moved backwards; don't get stuck
        return nowMs - lastCheckMs >= minIntervalMs
    }

    /**
     * True if we should show an update prompt for this manifest: there's an update,
     * and the user hasn't dismissed/snoozed past [manifest.versionCode] (a dismissed
     * version code of 0 or missing means nothing dismissed).
     */
    fun shouldShowPrompt(
        installedVersionCode: Long,
        manifest: UpdateManifest,
        dismissedVersionCode: Long,
    ): Boolean = isUpdateAvailable(installedVersionCode, manifest) && manifest.versionCode > dismissedVersionCode

    /**
     * True if we should post a local notification for this manifest: there's an update,
     * we haven't already notified for this version, and (optionally) we care about not
     * spamming. [lastNotifiedVersionCode] of 0 means never notified.
     */
    fun shouldNotify(
        installedVersionCode: Long,
        manifest: UpdateManifest,
        lastNotifiedVersionCode: Long,
    ): Boolean = isUpdateAvailable(installedVersionCode, manifest) && manifest.versionCode > lastNotifiedVersionCode

    /** Prefer a direct APK download URL, else the release page, else the download page. */
    fun bestActionUrl(manifest: UpdateManifest): String? =
        manifest.downloadUrl?.takeIf { it.isNotBlank() }
            ?: manifest.releaseUrl?.takeIf { it.isNotBlank() }
            ?: manifest.pageUrl?.takeIf { it.isNotBlank() }
}

/** Where we fetch the static version manifest from (GitHub Pages). */
object UpdateEndpoints {
    /** Default static manifest URL published alongside releases. */
    const val DEFAULT_MANIFEST_URL = "https://rohiitgit.github.io/ony/version.json"

    /** Default download page (helps users on Chrome / avoids in-app browser issues). */
    const val DEFAULT_DOWNLOAD_PAGE = "https://rohiitgit.github.io/ony/download/"

    /** Default latest release page. */
    const val DEFAULT_RELEASES_PAGE = "https://github.com/rohiitgit/ony/releases/latest"
}
