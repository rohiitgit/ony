package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class LinkParserTest {

    @Test fun spotifyTrack() {
        val l = LinkParser.parse("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQp?si=abc")!!
        assertEquals(Provider.SPOTIFY, l.provider)
        assertEquals("track", l.kind)
        assertEquals("4uLU6hMCjMI75M1A2tKUQp", l.id)
    }

    @Test fun spotifyUri() {
        val l = LinkParser.parse("spotify:track:4uLU6hMCjMI75M1A2tKUQp")!!
        assertEquals(Provider.SPOTIFY, l.provider)
        assertEquals("4uLU6hMCjMI75M1A2tKUQp", l.id)
    }

    @Test fun appleWithTrackParam() {
        val l = LinkParser.parse("https://music.apple.com/us/album/never-gonna-give-you-up/1559885420?i=1559885421")!!
        assertEquals(Provider.APPLE, l.provider)
        assertEquals("1559885421", l.id)
        assertEquals("1559885420", l.albumId)
    }

    @Test fun youtubeVariants() {
        assertEquals("dQw4w9WgXcQ", LinkParser.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")!!.id)
        assertEquals("dQw4w9WgXcQ", LinkParser.parse("https://youtu.be/dQw4w9WgXcQ")!!.id)
        assertEquals("dQw4w9WgXcQ", LinkParser.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMx")!!.id)
        assertEquals("dQw4w9WgXcQ", LinkParser.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ")!!.id)
    }

    // Shared from the YouTube app: bare host plus the ?si= share param.
    @Test fun youtubeShortsShareLink() {
        val l = LinkParser.parse("https://youtube.com/shorts/nSasVaEY8aw?si=ysb2dqaXFFAdWx-G")!!
        assertEquals(Provider.YOUTUBE, l.provider)
        assertEquals("video", l.kind)
        assertEquals("nSasVaEY8aw", l.id)
    }

    @Test fun rejectsNonMusic() {
        assertNull(LinkParser.parse("https://example.com/notmusic"))
        assertNull(LinkParser.parse("just some text no link"))
        assertNull(LinkParser.parse(null))
    }

    @Test fun extractsUrlFromSurroundingText() {
        val l = LinkParser.parse("Check this https://youtu.be/dQw4w9WgXcQ out!")!!
        assertEquals(Provider.YOUTUBE, l.provider)
        assertEquals("dQw4w9WgXcQ", l.id)
    }

    // Spotify hands out localized links: open.spotify.com/intl-de/track/<id>.
    @Test fun spotifyLocalizedLinkIsATrack() {
        val l = LinkParser.parse("https://open.spotify.com/intl-de/track/0VjIjW4GlUZAMYd2vXMi3b?si=abc")!!
        assertEquals(Provider.SPOTIFY, l.provider)
        assertEquals("track", l.kind)
        assertEquals("0VjIjW4GlUZAMYd2vXMi3b", l.id)
    }
}
