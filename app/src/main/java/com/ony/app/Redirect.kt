package com.ony.app

/**
 * Decides where a tapped link goes for someone who listens in one app.
 * Pure decisions plus one orchestrator ([resolve]); no Android types.
 */
object Redirect {

    /** Apps a song can be redirected into. */
    val TARGETS = listOf(MusicApp.SPOTIFY, MusicApp.APPLE_MUSIC, MusicApp.YOUTUBE_MUSIC)

    /** The listener's app: their choice if still installed, else the only installed one, else null (ask). */
    fun targetApp(chosen: MusicApp?, installed: Set<MusicApp>): MusicApp? =
        chosen?.takeIf { it in installed } ?: TARGETS.filter { it in installed }.singleOrNull()

    /** A destination in [target] that needs no lookup: the link itself, or a YouTube video in YouTube Music. */
    fun instant(link: MusicLink, target: MusicApp): Destination? =
        listOfNotNull(Resolver.original(link), Resolver.youtubeMusicFor(link)).firstOrNull { it.app == target }

    /**
     * The song in [target]: the link itself or the same YouTube video when possible, otherwise
     * the song read from the link and found by name and artist in [target]'s catalogue for
     * [country]. Spotify has no free search, so it gets its search screen for the song.
     */
    suspend fun resolve(link: MusicLink, target: MusicApp, country: String): Destination? {
        instant(link, target)?.let { return it }
        val expanded = Resolver.expandShortLink(link)
        instant(expanded, target)?.let { return it }

        val song = Catalog.songFor(expanded) ?: return null
        return when (target) {
            MusicApp.APPLE_MUSIC -> Catalog.appleMusic(song, country)
            MusicApp.YOUTUBE_MUSIC -> Catalog.youtubeMusic(song, country)
            MusicApp.SPOTIFY -> Catalog.spotifySearch(song)
            else -> null
        }
    }
}
