package com.ony.app

/**
 * Decides where a tapped link goes for someone who listens in one app.
 * Pure decisions plus one network orchestrator ([resolve]); no Android types.
 */
object Redirect {

    /** Apps a song can be redirected into. MusicLink returns exact links for these. */
    val TARGETS = listOf(MusicApp.SPOTIFY, MusicApp.APPLE_MUSIC, MusicApp.YOUTUBE_MUSIC)

    /** The listener's app: their choice if still installed, else the only installed one, else null (ask). */
    fun targetApp(chosen: MusicApp?, installed: Set<MusicApp>): MusicApp? =
        chosen?.takeIf { it in installed } ?: TARGETS.filter { it in installed }.singleOrNull()

    /** A destination in [target] that needs no lookup: the link itself, or a YouTube video in YouTube Music. */
    fun instant(link: MusicLink, target: MusicApp): Destination? =
        listOfNotNull(Resolver.original(link), Resolver.youtubeMusicFor(link)).firstOrNull { it.app == target }

    fun pick(target: MusicApp, vararg lookups: Lookup?): Destination? =
        lookups.filterNotNull().flatMap { it.matches }.firstOrNull { it.app == target }

    /**
     * The exact song in [target], or null when it can't be found there. Exact means the link
     * itself, the same YouTube video, or MusicLink's match; never a search guess. MusicLink
     * is only called when the link isn't already from [target], to save the allowance.
     */
    suspend fun resolve(link: MusicLink, target: MusicApp, apiKey: String): Outcome {
        instant(link, target)?.let { return Outcome(it, lookedUp = false) }
        val expanded = Resolver.expandShortLink(link)
        instant(expanded, target)?.let { return Outcome(it, lookedUp = false) }

        return Outcome(pick(target, MusicLinkApi.lookup(expanded, apiKey)), lookedUp = apiKey.isNotBlank())
    }

    /** Where to go (null: not found), and whether MusicLink was asked, for the lookup count. */
    data class Outcome(val destination: Destination?, val lookedUp: Boolean)
}
