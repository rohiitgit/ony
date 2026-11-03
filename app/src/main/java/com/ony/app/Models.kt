package com.ony.app

/** Canonical state for one routed link. Everything else is derived. */
enum class Provider { SPOTIFY, APPLE, YOUTUBE, SOUNDCLOUD, DEEZER, TIDAL, AMAZON, UNKNOWN }

data class MusicLink(
    val provider: Provider,
    val kind: String,
    val id: String?,
    val albumId: String? = null,
    val originalUrl: String,
)

data class Song(
    val title: String,
    val artist: String,
    val album: String? = null,
)

/**
 * An app a link can open in. Package names must also be listed in the manifest's <queries>.
 * [plays] names another app whose catalogue this one plays, when it has none of its own.
 */
enum class MusicApp(val label: String, val packageName: String, private val plays: MusicApp? = null) {
    SPOTIFY("Spotify", "com.spotify.music"),
    YOUTUBE_MUSIC("YouTube Music", "com.google.android.apps.youtube.music"),
    APPLE_MUSIC("Apple Music", "com.apple.android.music"),
    MELD("Meld", "com.meld.app", plays = YOUTUBE_MUSIC),
    YOUTUBE("YouTube", "com.google.android.youtube"),
    SOUNDCLOUD("SoundCloud", "com.soundcloud.android"),
    DEEZER("Deezer", "deezer.android.app"),
    TIDAL("Tidal", "com.aspiro.tidal"),
    AMAZON_MUSIC("Amazon Music", "com.amazon.mp3"),
    ;

    /** The catalogue this app plays, where its songs are searched for. */
    val catalogue: MusicApp get() = plays ?: this
}

/** The exact song (never a search page), opened only in [app]. */
data class Destination(val app: MusicApp, val url: String)
