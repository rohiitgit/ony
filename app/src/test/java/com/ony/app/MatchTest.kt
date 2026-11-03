package com.ony.app

import org.junit.Assert.*
import org.junit.Test

class MatchTest {

    private fun c(title: String, artist: String) = Match.Candidate(title, artist, "https://x/$title/$artist")

    // Real iTunes order for "Blinding Lights The Weeknd": the remix and live versions come first.
    @Test fun best_skipsRemixAndLiveForThePlainSong() {
        val results = listOf(
            c("Blinding Lights (Remix)", "The Weeknd & ROSALÍA"),
            c("Blinding Lights (Live)", "The Weeknd"),
            c("Blinding Lights", "The Weeknd"),
        )
        assertEquals("Blinding Lights", Match.best(results, Song("Blinding Lights", "The Weeknd"))?.title)
    }

    @Test fun best_keepsTheVersionTheLinkAskedFor() {
        val results = listOf(c("Blinding Lights", "The Weeknd"), c("Blinding Lights (Remix)", "The Weeknd & ROSALÍA"))
        assertEquals("Blinding Lights (Remix)", Match.best(results, Song("Blinding Lights (Remix)", "The Weeknd"))?.title)
    }

    // Real YouTube Music order: lo-fi and instrumental covers sit right below the song.
    @Test fun best_skipsCoversByOtherArtists() {
        val results = listOf(
            c("Blinding Lights (Lofi)", "Cover Cat"),
            c("Blinding Lights (Instrumental)", "Vibe2Vibe"),
            c("Blinding Lights", "The Weeknd"),
        )
        assertEquals("The Weeknd", Match.best(results, Song("Blinding Lights", "The Weeknd"))?.artist)
    }

    @Test fun best_ignoresSoundtrackTagsInTheTitle() {
        val results = listOf(c("Tum Hi Ho (From \"Aashiqui 2\")", "Arijit Singh"))
        assertNotNull(Match.best(results, Song("Tum Hi Ho", "Arijit Singh")))
    }

    @Test fun best_acceptsCoCreditedArtists() {
        val results = listOf(c("Tum Hi Ho", "Mithoon & Arijit Singh"))
        assertNotNull(Match.best(results, Song("Tum Hi Ho", "Arijit Singh")))
    }

    @Test fun best_rejectsADifferentSong() {
        val results = listOf(c("Not Gonna Give It Up", "Peter Tosh"), c("Never Give You Up", "Jerry Butler"))
        assertNull(Match.best(results, Song("Never Gonna Give You Up", "Rick Astley")))
    }

    @Test fun best_rejectsParodies() {
        val results = listOf(c("Orange Nya-Nya Style (Psy Gangnam Style Parody)", "Annoying Orange"))
        assertNull(Match.best(results, Song("Gangnam Style", "PSY")))
    }

    @Test fun best_isCaseAndAccentInsensitive() {
        assertNotNull(Match.best(listOf(c("GANGNAM STYLE", "Psy")), Song("Gangnam Style", "PSY")))
        assertNotNull(Match.best(listOf(c("Despacito", "Luis Fonsi")), Song("Despacíto", "Luis Fonsi")))
    }

    // YouTube video titles: "Artist - Title (Official Video)" and friends.
    @Test fun fromVideoTitle_splitsArtistAndCleansNoise() {
        assertEquals(Song("GANGNAM STYLE", "PSY"), Match.fromVideoTitle("PSY - GANGNAM STYLE(강남스타일) M/V", "officialpsy"))
        assertEquals(
            Song("Never Gonna Give You Up", "Rick Astley"),
            Match.fromVideoTitle("Rick Astley - Never Gonna Give You Up (Official Music Video)", "Rick Astley"),
        )
    }

    @Test fun fromVideoTitle_usesTheChannelWhenTitleHasNoArtist() {
        assertEquals(Song("Never Gonna Give You Up", "Rick Astley"), Match.fromVideoTitle("Never Gonna Give You Up", "Rick Astley - Topic"))
        assertEquals(Song("Blinding Lights", "The Weeknd"), Match.fromVideoTitle("Blinding Lights [Official Audio]", "TheWeekndVEVO".let { "The Weeknd" }))
    }

    @Test fun searchTerm_isTitleAndArtistWithoutBrackets() {
        assertEquals("Tum Hi Ho Arijit Singh", Match.searchTerm(Song("Tum Hi Ho (From \"Aashiqui 2\")", "Arijit Singh")))
    }

    // Album is a tie-breaker: among matching results, the one from the same album wins.
    @Test fun best_prefersTheSameAlbum() {
        val results = listOf(
            Match.Candidate("Tum Hi Ho", "Arijit Singh", "https://x/compilation", album = "Romantic Hits 2014"),
            Match.Candidate("Tum Hi Ho", "Arijit Singh", "https://x/original", album = "Aashiqui 2"),
        )
        assertEquals("https://x/original", Match.best(results, Song("Tum Hi Ho", "Arijit Singh", album = "Aashiqui 2"))?.url)
    }

    // Album names differ between stores, so no album match must not mean no song.
    @Test fun best_albumIsOnlyAPreference() {
        val results = listOf(Match.Candidate("Tum Hi Ho", "Arijit Singh", "https://x/only", album = "Greatest Hits"))
        assertEquals("https://x/only", Match.best(results, Song("Tum Hi Ho", "Arijit Singh", album = "Aashiqui 2"))?.url)
    }

    @Test fun best_albumIgnoresEditionSuffixes() {
        val results = listOf(
            Match.Candidate("Kurta Suha", "Amrinder Gill", "https://x/other", album = "Punjabi Hits"),
            Match.Candidate("Kurta Suha", "Amrinder Gill", "https://x/film", album = "Angrej (Original Motion Picture Soundtrack)"),
        )
        assertEquals("https://x/film", Match.best(results, Song("Kurta Suha", "Amrinder Gill", album = "Angrej"))?.url)
        val single = listOf(
            Match.Candidate("Adore", "Amrinder Gill", "https://x/comp", album = "Love Songs"),
            Match.Candidate("Adore", "Amrinder Gill", "https://x/single", album = "Adore - Single"),
        )
        assertEquals("https://x/single", Match.best(single, Song("Adore", "Amrinder Gill", album = "Adore"))?.url)
    }

    // Real case: India's Apple catalogue says "Judda", the US one "Judaa".
    @Test fun best_albumToleratesAOneLetterSpellingDifference() {
        val results = listOf(
            Match.Candidate("Yaarian", "Amrinder Gill", "https://x/comp", album = "Best of Amrinder Gill"),
            Match.Candidate("Yaarian", "Amrinder Gill", "https://x/judda", album = "Judda"),
        )
        assertEquals("https://x/judda", Match.best(results, Song("Yaarian", "Amrinder Gill", album = "Judaa"))?.url)
    }

    // Real case: India's store has "Asi Gabru Punjabi", the UK/US/German stores "Asi Gabhru Punjabi".
    @Test fun best_toleratesAOneLetterTransliterationInLongerTitles() {
        val uk = listOf(c("Asi Gabhru Punjabi", "Amrinder Gill & Dr Zeus"))
        assertNotNull(Match.best(uk, Song("Asi Gabru Punjabi", "Amrinder Gill")))
    }

    // Short titles stay strict: one letter can make a different song there.
    @Test fun best_keepsShortTitlesStrict() {
        assertNull(Match.best(listOf(c("Yaara", "Amrinder Gill")), Song("Yaar", "Amrinder Gill")))
    }

    // The tolerance never overrides the artist check.
    @Test fun best_oneLetterTitleNeedsTheSameArtist() {
        assertNull(Match.best(listOf(c("Asi Gabhru Punjabi", "Someone Else")), Song("Asi Gabru Punjabi", "Amrinder Gill")))
    }

    // Noise words are only noise in brackets or at the end; "Video Games" is a song title.
    @Test fun fromVideoTitle_keepsNoiseWordsThatArePartOfTheTitle() {
        assertEquals(
            Song("Video Games", "Lana Del Rey"),
            Match.fromVideoTitle("Lana Del Rey - Video Games (Official Music Video)", "LanaDelRey"),
        )
        assertEquals(Song("Radio Ga Ga", "Queen"), Match.fromVideoTitle("Queen - Radio Ga Ga (Official Video)", "Queen Official"))
    }

    @Test fun fromVideoTitle_dropsTrailingNoiseOutsideBrackets() {
        assertEquals(Song("Tum Hi Ho", "Arijit Singh"), Match.fromVideoTitle("Arijit Singh - Tum Hi Ho | Official Video", "T-Series"))
    }

    // "ft" inside a name (Daft Punk, Fifth Harmony) is not a featuring credit.
    @Test fun best_doesNotSplitArtistNamesContainingFt() {
        assertNull(Match.best(listOf(c("Get Lucky", "Adam Young")), Song("Get Lucky", "Daft Punk")))
        assertNotNull(Match.best(listOf(c("Get Lucky", "Daft Punk feat. Pharrell Williams")), Song("Get Lucky", "Daft Punk")))
    }

    @Test fun best_artistPartsMatchWholeWords() {
        // "Sofia" must not count as containing "fi".
        assertNull(Match.best(listOf(c("Work from Home", "Sofia Carson")), Song("Work from Home", "Fifth Harmony ft. Ty Dolla Sign")))
        assertNotNull(Match.best(listOf(c("Work from Home", "Fifth Harmony")), Song("Work from Home", "Fifth Harmony ft. Ty Dolla Sign")))
    }
}
