package com.ony.app

import java.net.URI

/** A kind of music link Ony has to be handed by Android. Hosts must match the manifest. */
enum class LinkKind(val label: String, val owner: MusicApp, val hosts: List<String>) {
    SPOTIFY("Spotify links", MusicApp.SPOTIFY, listOf("open.spotify.com", "spotify.link")),
    APPLE_MUSIC("Apple Music links", MusicApp.APPLE_MUSIC, listOf("music.apple.com")),
    YOUTUBE_MUSIC("YouTube Music links", MusicApp.YOUTUBE_MUSIC, listOf("music.youtube.com")),
    YOUTUBE("YouTube links", MusicApp.YOUTUBE, listOf("youtube.com", "www.youtube.com", "youtu.be")),
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

    /** How many of the link addresses Ony needs are switched on, out of how many: (on, total). */
    fun linksOn(allowed: Set<String>, target: MusicApp?): Pair<Int, Int> {
        val needed = kindsToCatch(target).flatMap { it.hosts }
        return needed.count { it in allowed } to needed.size
    }

    /** Null [allowed] means Android below 12, where the chooser appears and nothing needs allowing. */
    fun linksAllowed(allowed: Set<String>?, target: MusicApp?): Boolean =
        allowed == null || kindsToCatch(target).all { kind -> kind.hosts.all { it in allowed } }

}
