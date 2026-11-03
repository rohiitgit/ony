package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class CatalogTest {

    // Shape of iTunes Search (/search?entity=song) results.
    @Test fun parseItunesResults_readsTitleArtistAndLink() {
        val json = """{"resultCount":2,"results":[
            {"wrapperType":"track","trackName":"Blinding Lights (Remix)","artistName":"The Weeknd & ROSALÍA","trackViewUrl":"https://music.apple.com/in/album/x/1?i=11"},
            {"wrapperType":"track","trackName":"Blinding Lights","artistName":"The Weeknd","collectionName":"After Hours","trackViewUrl":"https://music.apple.com/in/album/y/2?i=22"}]}"""
        val c = Catalog.parseItunesResults(json)
        assertEquals(2, c.size)
        assertEquals(Match.Candidate("Blinding Lights", "The Weeknd", "https://music.apple.com/in/album/y/2?i=22", album = "After Hours"), c[1])
    }

    @Test fun parseItunesResults_skipsRowsWithoutALink() {
        assertTrue(Catalog.parseItunesResults("""{"results":[{"trackName":"A","artistName":"B"}]}""").isEmpty())
        assertTrue(Catalog.parseItunesResults("not json").isEmpty())
    }

    // iTunes lookup (/lookup?id=album&entity=song): the album row first, then its tracks.
    @Test fun parseItunesSong_picksTheLinkedTrack() {
        val json = """{"resultCount":3,"results":[
            {"wrapperType":"collection","collectionName":"Aashiqui 2"},
            {"wrapperType":"track","trackId":111,"trackName":"Sunn Raha Hai","artistName":"Ankit Tiwari"},
            {"wrapperType":"track","trackId":222,"trackName":"Tum Hi Ho","artistName":"Arijit Singh","collectionName":"Aashiqui 2"}]}"""
        assertEquals(Song("Tum Hi Ho", "Arijit Singh", album = "Aashiqui 2"), Catalog.parseItunesSong(json, trackId = "222"))
    }

    // Never guess a different track from the same album: no match means open the original link.
    @Test fun parseItunesSong_unknownTrackIsNull() {
        val json = """{"resultCount":2,"results":[
            {"wrapperType":"collection","collectionName":"Aashiqui 2"},
            {"wrapperType":"track","trackId":111,"trackName":"Sunn Raha Hai","artistName":"Ankit Tiwari"}]}"""
        assertNull(Catalog.parseItunesSong(json, trackId = "999"))
    }

    @Test fun parseItunesSong_noMatchIsNull() {
        assertNull(Catalog.parseItunesSong("""{"resultCount":0,"results":[]}""", trackId = "9"))
    }

    // YouTube Music search (songs filter): each row has a videoId and "Artist • Album • 3:22".
    @Test fun parseYtMusic_readsVideoTitleAndArtist() {
        val json = """{"contents":{"tabbedSearchResultsRenderer":{"tabs":[{"tabRenderer":{"content":{"sectionListRenderer":{"contents":[
            {"musicShelfRenderer":{"contents":[
              {"musicResponsiveListItemRenderer":{"playlistItemData":{"videoId":"J7p4bzqLvCw"},"flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Blinding Lights"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"The Weeknd"},{"text":" • "},{"text":"After Hours"},{"text":" • "},{"text":"3:22"}]}}}]}},
              {"musicResponsiveListItemRenderer":{"playlistItemData":{"videoId":"wHbMaC7RpFc"},"flexColumns":[
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Blinding Lights (Lofi)"}]}}},
                {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Cover Cat"},{"text":" • "},{"text":"2:04"}]}}}]}}
            ]}}]}}}}]}}}"""
        val c = Catalog.parseYtMusic(json)
        assertEquals(
            listOf(
                Match.Candidate("Blinding Lights", "The Weeknd", "https://music.youtube.com/watch?v=J7p4bzqLvCw", album = "After Hours"),
                Match.Candidate("Blinding Lights (Lofi)", "Cover Cat", "https://music.youtube.com/watch?v=wHbMaC7RpFc", album = null),
            ),
            c,
        )
    }

    @Test fun parseYtMusic_garbageIsEmpty() {
        assertTrue(Catalog.parseYtMusic("{}").isEmpty())
        assertTrue(Catalog.parseYtMusic("<html>").isEmpty())
    }

    // Spotify's public embed page carries the song in a __NEXT_DATA__ script.
    @Test fun parseSpotifyEmbed_readsNameAndArtists() {
        val html = """<html><script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":
            {"name":"Blinding Lights","artists":[{"name":"The Weeknd"}]}}}}}}</script></html>"""
        assertEquals(Song("Blinding Lights", "The Weeknd"), Catalog.parseSpotifyEmbed(html))
    }

    @Test fun parseSpotifyEmbed_joinsSeveralArtists() {
        val html = """<script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"state":{"data":{"entity":
            {"title":"Tum Hi Ho","artists":[{"name":"Mithoon"},{"name":"Arijit Singh"}]}}}}}}</script>"""
        assertEquals(Song("Tum Hi Ho", "Mithoon, Arijit Singh"), Catalog.parseSpotifyEmbed(html))
    }

    @Test fun parseSpotifyEmbed_missingIsNull() {
        assertNull(Catalog.parseSpotifyEmbed("<html>nothing</html>"))
    }

    @Test fun appleCountry_comesFromTheLinkPath() {
        assertEquals("in", Catalog.appleCountry("https://music.apple.com/in/album/x/1?i=2"))
        assertEquals("us", Catalog.appleCountry("https://music.apple.com/album/x/1?i=2"))
    }

    // A "/" in the query makes Spotify's search come back empty.
    @Test fun spotifySearch_isTheAppsSearchForTitleAndArtist() {
        assertEquals(
            Destination(MusicApp.SPOTIFY, "spotify:search:Back%20In%20Black%20AC%20DC"),
            Catalog.spotifySearch(Song("Back In Black", "AC/DC")),
        )
    }

    // A YouTube link's album: the YouTube Music search row for the same video.
    @Test fun albumOfVideo_findsTheRowForThatVideo() {
        val rows = listOf(
            Match.Candidate("Ki Samjiye", "Amrinder Gill", "https://music.youtube.com/watch?v=TC_u5OvL2C0", album = "Judda"),
            Match.Candidate("Asi Gabru Punjabi", "Amrinder Gill", "https://music.youtube.com/watch?v=HtM_QDwghx8", album = "Judda"),
        )
        assertEquals("Judda", Catalog.albumOfVideo(rows, "HtM_QDwghx8"))
        assertNull(Catalog.albumOfVideo(rows, "somethingElse"))
    }

    // Store searches use the listener's country: SIM first (it usually matches their store
    // account, even abroad), then the network, then the phone's region setting, then "us".
    @Test fun countryOf_prefersSimThenNetworkThenRegion() {
        assertEquals("in", Catalog.countryOf(sim = "IN", network = "gb", region = "US"))
        assertEquals("gb", Catalog.countryOf(sim = "", network = "GB", region = "US"))
        assertEquals("de", Catalog.countryOf(sim = null, network = null, region = "DE"))
        assertEquals("us", Catalog.countryOf(sim = null, network = "", region = ""))
    }

    @Test fun countryOf_ignoresValuesThatAreNotCountryCodes() {
        assertEquals("fr", Catalog.countryOf(sim = "001", network = "fr", region = "US"))
    }
}
