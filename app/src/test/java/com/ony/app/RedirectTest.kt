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

    @Test fun instant_youtubeVideoOpensInYoutubeMusic() {
        assertEquals(
            Destination(MusicApp.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=9bZkp7q19f0"),
            Redirect.instant(ytLink, MusicApp.YOUTUBE_MUSIC),
        )
    }

    @Test fun instant_otherAppNeedsASearch() {
        assertNull(Redirect.instant(spotifyLink, MusicApp.APPLE_MUSIC))
        assertNull(Redirect.instant(ytLink, MusicApp.SPOTIFY))
    }

    // Meld plays YouTube Music's catalogue: YouTube and YouTube Music links open as the same video.
    @Test fun instant_youtubeVideoOpensInMeld() {
        assertEquals(
            Destination(MusicApp.MELD, "https://music.youtube.com/watch?v=9bZkp7q19f0"),
            Redirect.instant(ytLink, MusicApp.MELD),
        )
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
