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
    val artworkUrl: String? = null,
)

/** An app a link can open in. Package names must also be listed in the manifest's <queries>. */
enum class MusicApp(val label: String, val packageName: String) {
    SPOTIFY("Spotify", "com.spotify.music"),
    YOUTUBE_MUSIC("YouTube Music", "com.google.android.apps.youtube.music"),
    APPLE_MUSIC("Apple Music", "com.apple.android.music"),
    YOUTUBE("YouTube", "com.google.android.youtube"),
    SOUNDCLOUD("SoundCloud", "com.soundcloud.android"),
    DEEZER("Deezer", "deezer.android.app"),
    TIDAL("Tidal", "com.aspiro.tidal"),
    AMAZON_MUSIC("Amazon Music", "com.amazon.mp3"),
}

/** The exact song (never a search page), opened only in [app]. */
data class Destination(val app: MusicApp, val url: String)

/** What one lookup found: song details (if any) and exact links in other apps. */
data class Lookup(val song: Song?, val matches: List<Destination>)
