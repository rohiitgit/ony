package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class ResolverTest {

    private val ytLink = MusicLink(
        Provider.YOUTUBE, "video", "dQw4w9WgXcQ",
        originalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
    )
    private val ytMusicLink = MusicLink(
        Provider.YOUTUBE, "video", "dQw4w9WgXcQ",
        originalUrl = "https://music.youtube.com/watch?v=dQw4w9WgXcQ",
    )
    private val spotifyLink = MusicLink(
        Provider.SPOTIFY, "track", "4cOdK2wGLETKBW3PvgPWqT",
        originalUrl = "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT",
    )

    // Ony handles open.spotify.com itself, so an unpinned intent can loop back into Ony.
    @Test fun original_isPinnedToTheSourceApp() {
        assertEquals(Destination(MusicApp.SPOTIFY, spotifyLink.originalUrl), Resolver.original(spotifyLink))
        assertEquals(MusicApp.YOUTUBE, Resolver.original(ytLink)?.app)
        assertEquals(MusicApp.YOUTUBE_MUSIC, Resolver.original(ytMusicLink)?.app)
    }

    @Test fun original_unknownProviderIsNull() {
        val unknown = MusicLink(Provider.UNKNOWN, "unknown", null, originalUrl = "https://example.com/x")
        assertNull(Resolver.original(unknown))
    }

    @Test fun youtubeMusicFor_youtubeMusicLinkIsTheSameVideoAsPlainWatchUrl() {
        assertEquals(
            Destination(MusicApp.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=dQw4w9WgXcQ"),
            Resolver.youtubeMusicFor(ytMusicLink),
        )
    }

    @Test fun youtubeMusicFor_plainYoutubeLinksHaveNone() {
        assertNull(Resolver.youtubeMusicFor(ytLink))
        assertNull(
            Resolver.youtubeMusicFor(
                MusicLink(Provider.YOUTUBE, "video", "dQw4w9WgXcQ", originalUrl = "https://www.youtube.com/shorts/dQw4w9WgXcQ")
            )
        )
        assertNull(
            Resolver.youtubeMusicFor(
                MusicLink(Provider.YOUTUBE, "video", "dQw4w9WgXcQ", originalUrl = "https://youtu.be/dQw4w9WgXcQ")
            )
        )
    }

    @Test fun youtubeMusicFor_otherSourcesHaveNone() {
        assertNull(Resolver.youtubeMusicFor(spotifyLink))
        assertNull(Resolver.youtubeMusicFor(MusicLink(Provider.YOUTUBE, "search", null, originalUrl = "https://youtube.com/results")))
    }
}
