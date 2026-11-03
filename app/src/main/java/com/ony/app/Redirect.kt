package com.ony.app

/**
 * Decides where a tapped link goes for someone who listens in one app.
 * Pure decisions plus one orchestrator ([resolve]); no Android types.
 */
object Redirect {

    /** Apps a song can be redirected into. */
    val TARGETS = listOf(MusicApp.SPOTIFY, MusicApp.APPLE_MUSIC, MusicApp.YOUTUBE_MUSIC, MusicApp.MELD)

    /** What to say when none of [TARGETS] is installed. */
    fun noAppMessage(): String {
        val names = TARGETS.map { it.label }
        return "Install ${names.dropLast(1).joinToString(", ")} or ${names.last()} to use Ony."
    }

    /** The listener's app: their choice if still installed, else the only installed one, else null (ask). */
    fun targetApp(chosen: MusicApp?, installed: Set<MusicApp>): MusicApp? =
        chosen?.takeIf { it in installed } ?: choices(installed).singleOrNull()

    /** The apps worth offering: installed targets only. Empty means Ony has nowhere to send songs. */
    fun choices(installed: Set<MusicApp>): List<MusicApp> = TARGETS.filter { it in installed }

    /**
     * A destination in [target] that needs no lookup: the link itself when it's [target]'s own,
     * else a YouTube video as its plain YouTube Music watch URL, for any app playing that
     * catalogue (YouTube Music, or Meld, which opens only some music.youtube.com paths).
     */
    fun instant(link: MusicLink, target: MusicApp): Destination? {
        val ownLink = Resolver.original(link)?.takeIf { it.app == target }
        val sameVideo = Resolver.youtubeMusicFor(link)
            ?.takeIf { target.catalogue == MusicApp.YOUTUBE_MUSIC }?.copy(app = target)
        return ownLink ?: sameVideo
    }

    /**
     * The song in [target]: the link itself or the same YouTube video when possible, otherwise
     * the song read from the link and found by name and artist in [target]'s catalogue for
     * [country]. Spotify has no free search, so it gets its search screen for the song.
     * Meld opens what YouTube Music's catalogue finds. [songOf] and [search] are the network
     * calls, swappable in tests.
     */
    suspend fun resolve(
        link: MusicLink,
        target: MusicApp,
        country: String,
        songOf: suspend (MusicLink) -> Song? = Catalog::songFor,
        search: suspend (Song, MusicApp, String) -> Destination? = ::searchCatalogue,
    ): Destination? {
        instant(link, target)?.let { return it }
        val expanded = Resolver.expandShortLink(link)
        instant(expanded, target)?.let { return it }

        val song = songOf(expanded) ?: return null
        return search(song, target.catalogue, country)?.copy(app = target)
    }

    private suspend fun searchCatalogue(song: Song, catalogue: MusicApp, country: String): Destination? =
        when (catalogue) {
            MusicApp.APPLE_MUSIC -> Catalog.appleMusic(song, country)
            MusicApp.YOUTUBE_MUSIC -> Catalog.youtubeMusic(song, country)
            MusicApp.SPOTIFY -> Catalog.spotifySearch(song)
            else -> null
        }
}
