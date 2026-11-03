package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class SetupTest {

    @Test fun kindsToCatch_skipsTheListenersOwnLinks() {
        assertEquals(
            listOf(LinkKind.SPOTIFY, LinkKind.APPLE_MUSIC, LinkKind.YOUTUBE),
            Setup.kindsToCatch(MusicApp.YOUTUBE_MUSIC),
        )
    }

    @Test fun kindsToCatch_noAppYetMeansAll() {
        assertEquals(LinkKind.entries, Setup.kindsToCatch(null))
    }

    @Test fun kindOf_matchesEveryHostOfAKind() {
        assertEquals(LinkKind.YOUTUBE, Setup.kindOf("https://youtu.be/9bZkp7q19f0"))
        assertEquals(LinkKind.YOUTUBE, Setup.kindOf("https://www.youtube.com/watch?v=x"))
        assertEquals(LinkKind.YOUTUBE_MUSIC, Setup.kindOf("https://music.youtube.com/watch?v=x"))
        assertEquals(LinkKind.SPOTIFY, Setup.kindOf("https://spotify.link/abc"))
        assertNull(Setup.kindOf("https://deezer.com/track/1"))
        assertNull(Setup.kindOf(null))
    }

    @Test fun grabbers_areInstalledAppsOtherThanTheListeners() {
        val installed = setOf(MusicApp.YOUTUBE, MusicApp.YOUTUBE_MUSIC, MusicApp.SPOTIFY)
        assertEquals(listOf(LinkKind.SPOTIFY, LinkKind.YOUTUBE), Setup.grabbers(installed, MusicApp.YOUTUBE_MUSIC))
    }

    @Test fun linksAllowed_needsEveryHostOfEveryKindToCatch() {
        val allHosts = LinkKind.entries.flatMap { it.hosts }.toSet()
        assertTrue(Setup.linksAllowed(allHosts, MusicApp.YOUTUBE_MUSIC))
        assertFalse(Setup.linksAllowed(allHosts - "youtu.be", MusicApp.YOUTUBE_MUSIC))
        // The listener's own links don't need catching.
        assertTrue(Setup.linksAllowed(allHosts - "music.youtube.com", MusicApp.YOUTUBE_MUSIC))
    }

    // Below Android 12 the "Open with" chooser still appears, so nothing needs allowing.
    @Test fun linksAllowed_nullMeansNotNeeded() {
        assertTrue(Setup.linksAllowed(null, MusicApp.SPOTIFY))
    }

    // "Avg. time" on the setup screen: how long Ony takes from tap to song.
    @Test fun averageTime_noRedirectsYetIsADash() {
        assertEquals("–", Setup.averageTime(totalMs = 0, count = 0))
    }

    @Test fun averageTime_isSecondsWithOneDecimal() {
        assertEquals("1.4 s", Setup.averageTime(totalMs = 4_200, count = 3))
        assertEquals("0.3 s", Setup.averageTime(totalMs = 300, count = 1))
    }

    @Test fun averageTime_roundsHalfUp() {
        assertEquals("2.1 s", Setup.averageTime(totalMs = 2_050, count = 1))
    }

}
