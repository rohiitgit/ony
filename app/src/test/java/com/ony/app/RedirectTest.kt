package com.ony.app

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

    // Only installed apps are offered; none installed means there's nothing to ask.
    @Test fun choices_areTheInstalledTargets() {
        assertEquals(listOf(MusicApp.APPLE_MUSIC), Redirect.choices(setOf(MusicApp.APPLE_MUSIC, MusicApp.YOUTUBE)))
        assertTrue(Redirect.choices(setOf(MusicApp.YOUTUBE)).isEmpty())
    }
}
