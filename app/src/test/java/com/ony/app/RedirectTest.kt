package com.ony.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RedirectTest {

    private val spotifyLink = MusicLink(
        Provider.SPOTIFY, "track", "0VjIjW4GlUZAMYd2vXMi3b",
        originalUrl = "https://open.spotify.com/track/0VjIjW4GlUZAMYd2vXMi3b",
    )
    private val ytLink = MusicLink(
        Provider.YOUTUBE, "video", "9bZkp7q19f0",
        originalUrl = "https://www.youtube.com/watch?v=9bZkp7q19f0",
    )

    // -- targetApp: whose app do we open? --

    @Test fun targetApp_choiceWinsWhenInstalled() {
        val installed = setOf(MusicApp.SPOTIFY, MusicApp.APPLE_MUSIC)
        assertEquals(MusicApp.APPLE_MUSIC, Redirect.targetApp(MusicApp.APPLE_MUSIC, installed))
    }

    @Test fun targetApp_onlyInstalledMusicAppNeedsNoQuestion() {
        val installed = setOf(MusicApp.YOUTUBE_MUSIC, MusicApp.YOUTUBE)
        assertEquals(MusicApp.YOUTUBE_MUSIC, Redirect.targetApp(null, installed))
    }

    @Test fun targetApp_severalInstalledAndNoChoiceMeansAsk() {
        assertNull(Redirect.targetApp(null, setOf(MusicApp.SPOTIFY, MusicApp.APPLE_MUSIC)))
    }

    @Test fun targetApp_uninstalledChoiceFallsBackToTheOnlyInstalledApp() {
        assertEquals(MusicApp.SPOTIFY, Redirect.targetApp(MusicApp.APPLE_MUSIC, setOf(MusicApp.SPOTIFY)))
    }

    @Test fun targetApp_noMusicAppInstalledMeansAsk() {
        assertNull(Redirect.targetApp(null, setOf(MusicApp.YOUTUBE)))
    }

    // -- instant: no lookup needed --

    @Test fun instant_linkFromTheTargetAppOpensAsIs() {
        assertEquals(
            Destination(MusicApp.SPOTIFY, spotifyLink.originalUrl),
            Redirect.instant(spotifyLink, MusicApp.SPOTIFY),
        )
    }

    // Plain YouTube videos stay out of YouTube Music: only music.youtube.com links open there.
    @Test fun instant_plainYoutubeVideoIsNotForYoutubeMusic() {
        assertNull(Redirect.instant(ytLink, MusicApp.YOUTUBE_MUSIC))
    }

    // Repro for the reported issue: a Shorts link must not be pulled into YouTube Music —
    // only actual YouTube Music links open there.
    @Test fun instant_shortsLinkDoesNotOpenInYoutubeMusic() {
        val shorts = MusicLink(
            Provider.YOUTUBE, "video", "dQw4w9WgXcQ",
            originalUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ",
        )
        assertNull(Redirect.instant(shorts, MusicApp.YOUTUBE_MUSIC))
        assertNull(Redirect.instant(shorts, MusicApp.MELD))
    }

    @Test fun instant_otherAppNeedsASearch() {
        assertNull(Redirect.instant(spotifyLink, MusicApp.APPLE_MUSIC))
        assertNull(Redirect.instant(ytLink, MusicApp.SPOTIFY))
    }

    // Meld plays YouTube Music's catalogue: YouTube Music links open as the same video.
    // Plain YouTube links stay out: they open in YouTube instead.
    @Test fun instant_plainYoutubeVideoIsNotForMeld() {
        assertNull(Redirect.instant(ytLink, MusicApp.MELD))
    }

    // Meld opens only some music.youtube.com paths, so it always gets the plain watch URL.
    @Test fun instant_youtubeMusicLinkOpensInMeldAsTheWatchUrl() {
        val ytmLink = ytLink.copy(originalUrl = "https://music.youtube.com/watch?v=9bZkp7q19f0&si=abc")
        assertEquals(
            Destination(MusicApp.MELD, "https://music.youtube.com/watch?v=9bZkp7q19f0"),
            Redirect.instant(ytmLink, MusicApp.MELD),
        )
    }

    // A YouTube Music page that isn't a song (podcast, explore, home) isn't sent to Meld.
    @Test fun instant_youtubeMusicPageThatIsNotASongIsNotForMeld() {
        val page = MusicLink(Provider.YOUTUBE, "search", null, originalUrl = "https://music.youtube.com/podcast/abc")
        assertNull(Redirect.instant(page, MusicApp.MELD))
    }

    // The YouTube Music app itself still gets its own links untouched.
    @Test fun instant_youtubeMusicLinkOpensInYoutubeMusicAsIs() {
        val ytmLink = ytLink.copy(originalUrl = "https://music.youtube.com/watch?v=9bZkp7q19f0&si=abc")
        assertEquals(
            Destination(MusicApp.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=9bZkp7q19f0&si=abc"),
            Redirect.instant(ytmLink, MusicApp.YOUTUBE_MUSIC),
        )
    }

    @Test fun instant_spotifyLinkNeedsASearchForMeld() {
        assertNull(Redirect.instant(spotifyLink, MusicApp.MELD))
    }

    // Each app is searched in the catalogue it plays; Meld's is YouTube Music's.
    @Test fun catalogue_ofMeldIsYoutubeMusic() {
        assertEquals(MusicApp.YOUTUBE_MUSIC, MusicApp.MELD.catalogue)
        assertEquals(MusicApp.APPLE_MUSIC, MusicApp.APPLE_MUSIC.catalogue)
    }

    // A Spotify link for a Meld listener: searched in YouTube Music for their country, opened in Meld.
    @Test fun resolve_meldOpensWhatYoutubeMusicSearchFinds() = runTest {
        var searched: Pair<MusicApp, String>? = null
        val found = Redirect.resolve(
            spotifyLink, MusicApp.MELD, "in",
            songOf = { Song("Blinding Lights", "The Weeknd") },
            search = { _, catalogue, country ->
                searched = catalogue to country
                Destination(catalogue, "https://music.youtube.com/watch?v=4NRXx6U8ABQ")
            },
        )
        assertEquals(MusicApp.YOUTUBE_MUSIC to "in", searched)
        assertEquals(Destination(MusicApp.MELD, "https://music.youtube.com/watch?v=4NRXx6U8ABQ"), found)
    }

    @Test fun resolve_songNotFoundIsNull() = runTest {
        val found = Redirect.resolve(spotifyLink, MusicApp.MELD, "in", songOf = { Song("X", "Y") }, search = { _, _, _ -> null })
        assertNull(found)
    }

    // Plain YouTube links (watch, Shorts, youtu.be) open in YouTube for YouTube Music
    // listeners — even when a catalogue search could match them. No lookup is attempted.
    @Test fun resolve_plainYoutubeOpensInYoutubeForYoutubeMusic() = runTest {
        val urls = listOf(
            "https://www.youtube.com/watch?v=9bZkp7q19f0",
            "https://www.youtube.com/shorts/9bZkp7q19f0",
            "https://youtu.be/9bZkp7q19f0",
        )
        for (url in urls) {
            val link = ytLink.copy(originalUrl = url)
            val found = Redirect.resolve(
                link, MusicApp.YOUTUBE_MUSIC, "in",
                songOf = { error("must not look up a plain YouTube link") },
                search = { _, _, _ -> error("must not search for a plain YouTube link") },
            )
            assertEquals(Destination(MusicApp.YOUTUBE, url), found)
        }
    }

    @Test fun resolve_plainYoutubeOpensInYoutubeForMeld() = runTest {
        val shorts = ytLink.copy(originalUrl = "https://www.youtube.com/shorts/9bZkp7q19f0")
        val found = Redirect.resolve(
            shorts, MusicApp.MELD, "in",
            songOf = { error("must not look up a plain YouTube link") },
            search = { _, _, _ -> error("must not search for a plain YouTube link") },
        )
        assertEquals(Destination(MusicApp.YOUTUBE, shorts.originalUrl), found)
    }

    // The exact share-link shape from the YouTube app: bare host plus ?si= param.
    @Test fun resolve_shortsShareLinkOpensInYoutube() = runTest {
        val shared = MusicLink(
            Provider.YOUTUBE, "video", "nSasVaEY8aw",
            originalUrl = "https://youtube.com/shorts/nSasVaEY8aw?si=ysb2dqaXFFAdWx-G",
        )
        val found = Redirect.resolve(
            shared, MusicApp.YOUTUBE_MUSIC, "in",
            songOf = { error("must not look up a plain YouTube link") },
            search = { _, _, _ -> error("must not search for a plain YouTube link") },
        )
        assertEquals(Destination(MusicApp.YOUTUBE, shared.originalUrl), found)
    }

    // A YouTube Music link still opens in YouTube Music (and in Meld as the plain watch URL).
    @Test fun resolve_youtubeMusicLinkStaysInYoutubeMusic() = runTest {
        val ytm = ytLink.copy(originalUrl = "https://music.youtube.com/watch?v=9bZkp7q19f0&si=abc")
        val found = Redirect.resolve(
            ytm, MusicApp.YOUTUBE_MUSIC, "in",
            songOf = { error("must not look up an own link") },
            search = { _, _, _ -> error("must not search for an own link") },
        )
        assertEquals(Destination(MusicApp.YOUTUBE_MUSIC, ytm.originalUrl), found)
        val forMeld = Redirect.resolve(
            ytm, MusicApp.MELD, "in",
            songOf = { error("must not look up an own link") },
            search = { _, _, _ -> error("must not search for an own link") },
        )
        assertEquals(Destination(MusicApp.MELD, "https://music.youtube.com/watch?v=9bZkp7q19f0"), forMeld)
    }

    // Other listeners are unaffected: a plain YouTube video is still read and searched.
    @Test fun resolve_plainYoutubeStillSearchedForSpotify() = runTest {
        var searched: Pair<MusicApp, String>? = null
        val found = Redirect.resolve(
            ytLink, MusicApp.SPOTIFY, "us",
            songOf = { Song("Gangnam Style", "PSY") },
            search = { _, catalogue, country ->
                searched = catalogue to country
                Destination(catalogue, "https://open.spotify.com/search/x")
            },
        )
        assertEquals(MusicApp.SPOTIFY to "us", searched)
        assertEquals(Destination(MusicApp.SPOTIFY, "https://open.spotify.com/search/x"), found)
    }

    @Test fun noAppMessage_namesEveryTarget() {
        assertEquals("Install Spotify, Apple Music, YouTube Music or Meld to use Ony.", Redirect.noAppMessage())
    }

    @Test fun choices_includeMeldWhenInstalled() {
        assertEquals(listOf(MusicApp.SPOTIFY, MusicApp.MELD), Redirect.choices(setOf(MusicApp.MELD, MusicApp.SPOTIFY)))
    }

    // Only installed apps are offered; none installed means there's nothing to ask.
    @Test fun choices_areTheInstalledTargets() {
        assertEquals(listOf(MusicApp.APPLE_MUSIC), Redirect.choices(setOf(MusicApp.APPLE_MUSIC, MusicApp.YOUTUBE)))
        assertTrue(Redirect.choices(setOf(MusicApp.YOUTUBE)).isEmpty())
    }
}
