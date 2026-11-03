package com.ony.app

import java.net.URI

/** A kind of music link Ony has to be handed by Android. Hosts must match the manifest. */
enum class LinkKind(val label: String, val owner: MusicApp, val hosts: List<String>, val sampleUrl: String) {
    SPOTIFY(
        "Spotify links", MusicApp.SPOTIFY, listOf("open.spotify.com", "spotify.link"),
        "https://open.spotify.com/track/0VjIjW4GlUZAMYd2vXMi3b",
    ),
    APPLE_MUSIC(
        "Apple Music links", MusicApp.APPLE_MUSIC, listOf("music.apple.com"),
        "https://music.apple.com/us/album/never-gonna-give-you-up/1559885420?i=1559885421",
    ),
    YOUTUBE_MUSIC(
        "YouTube Music links", MusicApp.YOUTUBE_MUSIC, listOf("music.youtube.com"),
        "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
    ),
    YOUTUBE(
        "YouTube links", MusicApp.YOUTUBE, listOf("youtube.com", "www.youtube.com", "youtu.be"),
        "https://www.youtube.com/watch?v=9bZkp7q19f0",
    ),
}

/** Pure answers for the setup screen: what still needs doing. */
object Setup {

    /** Every kind except the listener's own app's links, which can keep opening there. */
    fun kindsToCatch(target: MusicApp?): List<LinkKind> = LinkKind.entries.filter { it.owner != target }

    fun kindOf(url: String?): LinkKind? {
        val host = try { URI(url ?: return null).host?.lowercase() } catch (_: Exception) { null } ?: return null
        return LinkKind.entries.firstOrNull { host in it.hosts }
    }

    /** Installed apps that open their own links before Ony sees them (other than the listener's). */
    fun grabbers(installed: Set<MusicApp>, target: MusicApp?): List<LinkKind> =
        kindsToCatch(target).filter { it.owner in installed }

    /** Null [allowed] means Android below 12, where the chooser appears and nothing needs allowing. */
    fun linksAllowed(allowed: Set<String>?, target: MusicApp?): Boolean =
        allowed == null || kindsToCatch(target).all { kind -> kind.hosts.all { it in allowed } }

    /** Mean time from tap to song, "1.4 s", or a dash before the first redirect. */
    fun averageTime(totalMs: Long, count: Int): String {
        if (count <= 0) return "–"
        val tenths = (totalMs * 10 + count * 500L) / (count * 1000L)
        return "${tenths / 10}.${tenths % 10} s"
    }

}
