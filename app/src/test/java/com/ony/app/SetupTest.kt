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

    // No link kind belongs to Meld (its YouTube filters can't be verified), so a Meld listener
    // needs every kind caught, YouTube Music's too.
    @Test fun kindsToCatch_meldListenerNeedsEveryKind() {
        assertEquals(LinkKind.entries, Setup.kindsToCatch(MusicApp.MELD))
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


    // The setup row shows progress, e.g. "3 of 6 on", counting only links Ony must catch.
    @Test fun linksOn_countsAllowedHostsAmongThoseNeeded() {
        val needed = Setup.kindsToCatch(MusicApp.YOUTUBE_MUSIC).flatMap { it.hosts }
        assertEquals(0 to needed.size, Setup.linksOn(emptySet(), MusicApp.YOUTUBE_MUSIC))
        assertEquals(2 to needed.size, Setup.linksOn(setOf("open.spotify.com", "music.apple.com", "music.youtube.com"), MusicApp.YOUTUBE_MUSIC))
        assertEquals(needed.size to needed.size, Setup.linksOn(needed.toSet(), MusicApp.YOUTUBE_MUSIC))
    }
}
