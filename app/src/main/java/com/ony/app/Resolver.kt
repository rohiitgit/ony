package com.ony.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Link facts that need no matching service: where a link itself opens, short-link
 * expansion, and a YouTube Music link's twin as a plain watch URL (same video id, so exact).
 * Plain YouTube links are never rewritten into YouTube Music. Every other match comes
 * from Catalog.
 */
object Resolver {

    /**
     * Follows redirects for short links (spotify.link). Returns the re-parsed
     * destination link, or the original when expansion fails.
     */
    suspend fun expandShortLink(link: MusicLink): MusicLink = withContext(Dispatchers.IO) {
        if (link.kind != "short") return@withContext link
        try {
            var current = link.originalUrl
            repeat(5) {
                val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("User-Agent", "Ony/0.1 (Android)")
                    instanceFollowRedirects = false
                }
                val code = conn.responseCode
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (code in REDIRECTS && location != null) {
                    current = URL(URL(current), location).toString()
                } else {
                    return@withContext LinkParser.parse(current) ?: link
                }
            }
            LinkParser.parse(current) ?: link
        } catch (_: Exception) { link }
    }

    /** The video itself in YouTube Music — but only for actual YouTube Music links. Null otherwise. */
    fun youtubeMusicFor(link: MusicLink): Destination? =
        if (link.provider == Provider.YOUTUBE && link.id != null && isYouTubeMusicUrl(link)) {
            Destination(MusicApp.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=${link.id}")
        } else {
            null
        }

    /** True when [link] came from music.youtube.com (a YouTube Music link, not plain YouTube). */
    fun isYouTubeMusicUrl(link: MusicLink): Boolean = "://music.youtube.com" in link.originalUrl

    /** The tapped link itself, pinned to the app it belongs to. Null for unknown links. */
    fun original(link: MusicLink): Destination? {
        val app = when (link.provider) {
            Provider.SPOTIFY -> MusicApp.SPOTIFY
            Provider.APPLE -> MusicApp.APPLE_MUSIC
            Provider.YOUTUBE ->
                if ("://music.youtube.com" in link.originalUrl) MusicApp.YOUTUBE_MUSIC else MusicApp.YOUTUBE
            Provider.SOUNDCLOUD -> MusicApp.SOUNDCLOUD
            Provider.DEEZER -> MusicApp.DEEZER
            Provider.TIDAL -> MusicApp.TIDAL
            Provider.AMAZON -> MusicApp.AMAZON_MUSIC
            Provider.UNKNOWN -> return null
        }
        return Destination(app, link.originalUrl)
    }

    private val REDIRECTS = setOf(301, 302, 303, 307, 308)
}
