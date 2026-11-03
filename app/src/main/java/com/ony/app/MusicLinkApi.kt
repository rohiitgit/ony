package com.ony.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * MusicLink (musiclink.one) turns one song link into exact links on other services.
 * Free plan: 300 lookups a month, 1 per second. A platform is only in the response when
 * the song was found there, so every link it returns is exact.
 */
object MusicLinkApi {

    private const val ENDPOINT = "https://api.musiclink.one/v2/resolve"

    // Response key -> app. Only the apps Ony offers as targets.
    private val TARGETS = listOf(
        "spotify" to MusicApp.SPOTIFY,
        "youtube_music" to MusicApp.YOUTUBE_MUSIC,
        "apple_music" to MusicApp.APPLE_MUSIC,
    )

    /** Null when there is no key, nothing matched, the quota is used up, or the network fails. */
    suspend fun lookup(link: MusicLink, apiKey: String): Lookup? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val url = "$ENDPOINT?q=" + URLEncoder.encode(queryUrl(link), "UTF-8")
        try {
            request(url, apiKey, retriesLeft = 1)?.let(::parse)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks a key without spending the allowance: a deliberately bad query answers 400 for
     * an accepted key and 401 for a wrong one, and failed requests are free.
     */
    suspend fun checkKey(apiKey: String): KeyCheck = withContext(Dispatchers.IO) {
        val code = try {
            val conn = (URL("$ENDPOINT?q=ony-key-check").openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                setRequestProperty("User-Agent", "Ony/0.1 (Android)")
            }
            try { conn.responseCode } finally { conn.disconnect() }
        } catch (_: Exception) {
            null
        }
        keyCheckFor(code)
    }

    internal fun keyCheckFor(code: Int?): KeyCheck = when (code) {
        200, 400, 404, 429 -> KeyCheck.WORKS
        401, 403 -> KeyCheck.REJECTED
        else -> KeyCheck.UNREACHABLE
    }

    /** Canonical form of the link, so share-tracking params and URI forms don't cause misses. */
    internal fun queryUrl(link: MusicLink): String = when {
        link.provider == Provider.SPOTIFY && link.id != null && link.kind != "short" ->
            "https://open.spotify.com/${link.kind}/${link.id}"
        link.provider == Provider.YOUTUBE && link.id != null ->
            "https://www.youtube.com/watch?v=${link.id}"
        else -> link.originalUrl
    }

    internal fun parse(body: String): Lookup? = try {
        val root = JSONObject(body)
        val item = root.takeIf { it.optBoolean("success") }
            ?.optJSONArray("data")?.optJSONObject(0)
        val title = item?.optString("title").orEmpty()
        if (item == null || title.isBlank()) {
            null
        } else {
            val platforms = item.optJSONObject("platforms")
            Lookup(
                song = Song(title, item.optString("artist"), item.optString("image_url").ifBlank { null }),
                matches = TARGETS.mapNotNull { (key, app) ->
                    platforms?.optString(key)?.takeIf { it.startsWith("https://") }?.let { Destination(app, it) }
                },
            )
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Body of a 200, else null. A per-second limit (429 with a short Retry-After) is retried
     * once; a used-up monthly quota (429 without Retry-After) is not.
     */
    private fun request(url: String, apiKey: String, retriesLeft: Int): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            // The first lookup of a song searches every service and can take 10-15 s.
            readTimeout = 30_000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("User-Agent", "Ony/0.1 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code == 200) return conn.inputStream.bufferedReader().use { it.readText() }
            val retryAfter = conn.getHeaderField("Retry-After")?.toLongOrNull()
            if (code == 429 && retriesLeft > 0 && retryAfter != null && retryAfter in 0..5) {
                Thread.sleep(retryAfter * 1000 + 200)
                return request(url, apiKey, retriesLeft - 1)
            }
            return null
        } finally {
            conn.disconnect()
        }
    }
}

enum class KeyCheck { WORKS, REJECTED, UNREACHABLE }
