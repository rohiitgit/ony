package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class MusicLinkApiTest {

    // Trimmed from a real /v2/resolve response for a YouTube link.
    private val ok = """
        {"success":true,"data":[{"type":"track","id":"h8aN_1K9",
        "title":"Never Gonna Give You Up","artist":"Rick Astley",
        "image_url":"https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg",
        "platforms":{
          "musiclink":"https://musiclink.one/song/rick-astley/never-gonna-give-you-up?i=2",
          "spotify":"https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT",
          "apple_music":"https://music.apple.com/song/1559885421",
          "deezer":"https://www.deezer.com/track/14408104",
          "youtube":"https://youtube.com/watch?v=dQw4w9WgXcQ",
          "youtube_music":"https://music.youtube.com/watch?v=dQw4w9WgXcQ"}}]}
    """.trimIndent()

    @Test fun parse_readsSong() {
        val song = MusicLinkApi.parse(ok)!!.song
        assertEquals(Song("Never Gonna Give You Up", "Rick Astley", "https://is1-ssl.mzstatic.com/image/thumb/x/600x600bb.jpg"), song)
    }

    @Test fun parse_keepsSpotifyYtMusicAndAppleMusic() {
        val matches = MusicLinkApi.parse(ok)!!.matches
        assertEquals(
            listOf(
                Destination(MusicApp.SPOTIFY, "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"),
                Destination(MusicApp.YOUTUBE_MUSIC, "https://music.youtube.com/watch?v=dQw4w9WgXcQ"),
                Destination(MusicApp.APPLE_MUSIC, "https://music.apple.com/song/1559885421"),
            ),
            matches,
        )
    }

    // MusicLink leaves a platform out when the song isn't there, so no key means no button.
    @Test fun parse_missingPlatformMeansNoButton() {
        val body = """{"success":true,"data":[{"title":"T","artist":"A","platforms":{"spotify":"https://open.spotify.com/track/x"}}]}"""
        assertEquals(listOf(MusicApp.SPOTIFY), MusicLinkApi.parse(body)!!.matches.map { it.app })
    }

    @Test fun parse_ignoresNonHttpsLinks() {
        val body = """{"success":true,"data":[{"title":"T","artist":"A","platforms":{"spotify":"javascript:alert(1)","apple_music":""}}]}"""
        assertTrue(MusicLinkApi.parse(body)!!.matches.isEmpty())
    }

    @Test fun parse_missingImageIsNull() {
        val body = """{"success":true,"data":[{"title":"T","artist":"A","platforms":{}}]}"""
        assertNull(MusicLinkApi.parse(body)!!.song!!.artworkUrl)
    }

    @Test fun parse_failureIsNull() {
        assertNull(MusicLinkApi.parse("""{"success":false,"error":"Track not found on the requested platform."}"""))
    }

    @Test fun parse_emptyDataIsNull() {
        assertNull(MusicLinkApi.parse("""{"success":true,"data":[]}"""))
    }

    @Test fun parse_garbageIsNull() {
        assertNull(MusicLinkApi.parse("<html>502</html>"))
    }

    @Test fun queryUrl_spotifyUriBecomesWebUrl() {
        val link = MusicLink(Provider.SPOTIFY, "track", "abc", originalUrl = "spotify:track:abc")
        assertEquals("https://open.spotify.com/track/abc", MusicLinkApi.queryUrl(link))
    }

    @Test fun queryUrl_youtubeBecomesCanonicalWatchUrl() {
        val link = MusicLink(Provider.YOUTUBE, "video", "dQw4w9WgXcQ", originalUrl = "https://youtu.be/dQw4w9WgXcQ?si=tracking")
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", MusicLinkApi.queryUrl(link))
    }

    @Test fun queryUrl_otherLinksPassThrough() {
        val url = "https://music.apple.com/us/album/x/1559885420?i=1559885421"
        assertEquals(url, MusicLinkApi.queryUrl(MusicLink(Provider.APPLE, "song", "1559885421", originalUrl = url)))
    }

    // A deliberately bad query: 400 means the key was accepted, and failed requests are free.
    @Test fun keyCheckFor_acceptedKey() {
        assertEquals(KeyCheck.WORKS, MusicLinkApi.keyCheckFor(400))
        assertEquals(KeyCheck.WORKS, MusicLinkApi.keyCheckFor(429))
    }

    @Test fun keyCheckFor_rejectedKey() {
        assertEquals(KeyCheck.REJECTED, MusicLinkApi.keyCheckFor(401))
        assertEquals(KeyCheck.REJECTED, MusicLinkApi.keyCheckFor(403))
    }

    @Test fun keyCheckFor_noAnswer() {
        assertEquals(KeyCheck.UNREACHABLE, MusicLinkApi.keyCheckFor(null))
        assertEquals(KeyCheck.UNREACHABLE, MusicLinkApi.keyCheckFor(502))
    }
}
