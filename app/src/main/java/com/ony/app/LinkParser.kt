package com.ony.app

import java.net.URI
import java.net.URLDecoder

/**
 * Pure URL parser. JVM-only (java.net.URI), no Android deps, no I/O — unit-testable
 * on plain JUnit without Robolectric.
 */
object LinkParser {

    private val urlInText =
        Regex("""https?://\S+|spotify:\S+""")
    private val spotifyKinds =
        setOf("track", "album", "playlist", "artist", "episode", "show")

    fun extractUrl(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val match = urlInText.find(raw) ?: return null
        return match.value.trimEnd(')', '.', ',', ']', '\'', '"', '!')
    }

    fun parse(raw: String?): MusicLink? {
        val found = extractUrl(raw) ?: return null
        if (found.startsWith("spotify:")) return parseSpotifyUri(found)

        val normalized = if ("://" in found) found else "https://$found"
        val uri = try { URI(normalized) } catch (_: Exception) { return null }
        val host = (uri.host ?: return null).lowercase().removePrefix("www.")
        val path = uri.path.orEmpty()
        val segments = path.split("/").filter { it.isNotEmpty() }.map(::decode)
        val query = queryParams(uri)

        return when (host) {
            "open.spotify.com" -> parseSpotifyWeb(segments, normalized)
            "spotify.link" -> MusicLink(
                Provider.SPOTIFY, "short",
                path.trim { it == '/' }.takeIf { it.isNotEmpty() },
                originalUrl = normalized,
            )
            "music.apple.com", "itunes.apple.com" -> parseApple(segments, path, query, normalized)
            "music.youtube.com", "youtube.com", "youtu.be", "youtube-nocookie.com", "m.youtube.com" ->
                parseYouTube(query, segments, path, host, normalized)
            "soundcloud.com", "on.soundcloud.com" -> MusicLink(
                Provider.SOUNDCLOUD, "track",
                path.trim { it == '/' }.takeIf { it.isNotEmpty() },
                originalUrl = normalized,
            )
            "deezer.com" -> parseIdPath(path, normalized, Provider.DEEZER)
            "tidal.com", "listen.tidal.com" -> parseIdPath(path, normalized, Provider.TIDAL)
            "music.amazon.com", "music.amazon.in" -> MusicLink(
                Provider.AMAZON, "unknown", path.trim { it == '/' },
                originalUrl = normalized,
            )
            else -> null
        }
    }

    private fun parseSpotifyUri(found: String): MusicLink? {
        val parts = found.split(":")
        if (parts.size != 3 || parts[1] !in spotifyKinds) return null
        return MusicLink(Provider.SPOTIFY, parts[1], parts[2], originalUrl = found)
    }

    private fun parseSpotifyWeb(segments: List<String>, original: String): MusicLink? {
        if (segments.size < 2 || segments[0] !in spotifyKinds) return null
        val id = segments[1].split(Regex("[^A-Za-z0-9]")).firstOrNull()?.takeIf { it.isNotEmpty() }
            ?: return null
        return MusicLink(Provider.SPOTIFY, segments[0], id, originalUrl = original)
    }

    private fun parseApple(
        segments: List<String>,
        path: String,
        query: Map<String, String>,
        original: String,
    ): MusicLink? {
        val trackId = query["i"]
        val last = segments.lastOrNull() ?: ""
        val albumId = Regex("""id(\d+)""").find(last)?.groupValues?.get(1)
            ?: last.takeIf { it.all(Char::isDigit) && it.isNotEmpty() }
        val kind = when {
            "/song/" in path || trackId != null -> "song"
            "/album/" in path -> "album"
            "/playlist/" in path -> "playlist"
            else -> "unknown"
        }
        return MusicLink(Provider.APPLE, kind, trackId ?: albumId, albumId = albumId, originalUrl = original)
    }

    private fun parseYouTube(
        query: Map<String, String>,
        segments: List<String>,
        path: String,
        host: String,
        original: String,
    ): MusicLink? {
        var vid = query["v"]
        if (vid == null && host == "youtu.be") vid = segments.firstOrNull()
        if (vid == null) {
            vid = Regex("""(embed|shorts|live)/([^/?]+)""")
                .find(path.trim { it == '/' })?.groupValues?.get(2)
        }
        if (vid != null) {
            vid = vid.split(Regex("[^A-Za-z0-9_-]")).firstOrNull()?.takeIf { it.isNotEmpty() }
                ?: return null
            return MusicLink(Provider.YOUTUBE, "video", vid, originalUrl = original)
        }
        return MusicLink(Provider.YOUTUBE, "search", null, originalUrl = original)
    }

    private fun parseIdPath(path: String, original: String, provider: Provider): MusicLink? {
        val m = Regex("""/(track|album|playlist|artist)/(\d+)""").find(path) ?: return null
        return MusicLink(provider, m.groupValues[1], m.groupValues[2], originalUrl = original)
    }

    private fun queryParams(uri: URI): Map<String, String> {
        val raw = uri.rawQuery ?: return emptyMap()
        val out = LinkedHashMap<String, String>()
        for (pair in raw.split("&")) {
            val eq = pair.indexOf('=')
            val key = decode(if (eq < 0) pair else pair.substring(0, eq))
            if (key !in out) out[key] = decode(if (eq < 0) "" else pair.substring(eq + 1))
        }
        return out
    }

    private fun decode(s: String): String =
        try { URLDecoder.decode(s, "UTF-8") } catch (_: Exception) { s }

}
