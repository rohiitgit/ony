package com.ony.app

import java.text.Normalizer

/**
 * Picks the same song out of another app's search results, by title and artist, with album as
 * a tie-breaker. Pure, so the
 * rules are unit-tested against real result orders (the top hit is often a remix or a cover).
 */
object Match {

    data class Candidate(val title: String, val artist: String, val url: String, val album: String? = null)

    // Words that make a recording a different version of the song.
    private val VERSION = Regex(
        """\b(remix|live|instrumental|karaoke|cover|lofi|lo fi|sped up|slowed|reverb|acoustic|nightcore|8d|parody|mashup)\b""",
    )

    // Video-title noise that isn't part of the song's name.
    private val NOISE = Regex(
        """(?i)\b(official( music| lyric)? (video|audio)|official|lyrics?( video)?|audio|video|visuali[sz]er|hd|4k|m/?v)\b""",
    )

    /**
     * Candidates with the same base title (one letter of slack for longer titles), the same
     * version and an overlapping artist; of
     * those, one from the same album if there is one, else the first (search order).
     * Album only breaks ties: names differ between stores, so it never rules a song out.
     */
    fun best(candidates: List<Candidate>, wanted: Song): Candidate? {
        val title = baseTitle(wanted.title)
        val versions = versions(wanted.title)
        val artists = artistParts(wanted.artist)
        val matching = candidates.filter { c ->
            closeEnough(baseTitle(c.title), title) &&
                versions(c.title) == versions &&
                (artists.isEmpty() || artists.any { it in normalize(c.artist) } || normalize(c.artist) in normalize(wanted.artist))
        }
        val album = wanted.album?.let(::baseAlbum)?.takeIf { it.isNotBlank() }
        return album?.let { a -> matching.firstOrNull { c -> c.album?.let { sameAlbum(baseAlbum(it), a) } == true } }
            ?: matching.firstOrNull()
    }

    /** Song from a YouTube video: "Artist - Title (Official Video)", or the channel as artist. */
    fun fromVideoTitle(videoTitle: String, channel: String): Song {
        val split = videoTitle.split(" - ", limit = 2)
        val (artist, title) = if (split.size == 2) split[0] to split[1] else channelArtist(channel) to videoTitle
        return Song(cleanVideoTitle(title), artist.trim())
    }

    /** "Title Artist" with brackets and quotes removed, for a search box. */
    fun searchTerm(song: Song): String =
        "${stripBrackets(song.title)} ${song.artist}".replace("\"", "").replace(Regex("\\s+"), " ").trim()

    private fun channelArtist(channel: String) =
        channel.removeSuffix(" - Topic").replace(Regex("(?i)vevo$|official$"), "").trim()

    private fun cleanVideoTitle(title: String): String {
        val noBrackets = Regex("""[(\[]([^)\]]*)[)\]]""").replace(title) { m ->
            val inner = m.groupValues[1]
            val nonLatin = inner.any { it.isLetter() && it.code > 0x24F }
            if (NOISE.containsMatchIn(inner) || nonLatin) " " else m.value
        }
        return NOISE.replace(noBrackets, " ").replace(Regex("\\s+"), " ").trim()
    }

    private fun stripBrackets(s: String) = s.replace(Regex("""[(\[][^)\]]*[)\]]"""), " ").replace(Regex("\\s+"), " ").trim()

    // "Angrej (Original Motion Picture Soundtrack)", "Adore - Single", "X (Deluxe Edition)" -> the name.
    private fun baseAlbum(album: String) =
        normalize(stripBrackets(album).replace(Regex("""(?i)\s-\s(single|ep)$"""), ""))
            .replace(Regex("""\b(deluxe|edition|remastered|expanded|soundtrack|original motion picture)\b"""), " ")
            .replace(Regex("\\s+"), " ").trim()

    // Equal, or one letter apart for names long enough that it isn't a different word ("judda"/"judaa").
    private fun sameAlbum(a: String, b: String) = a == b || (minOf(a.length, b.length) >= 5 && editDistance(a, b) <= 1)

    // Titles: equal, or one letter apart from 8 characters up. Stores transliterate Hindi and
    // Punjabi differently ("asi gabru punjabi" / "asi gabhru punjabi"); shorter titles stay strict.
    private fun closeEnough(a: String, b: String) = a == b || (minOf(a.length, b.length) >= 8 && editDistance(a, b) <= 1)

    private fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val cur = IntArray(b.length + 1).also { it[0] = i }
            for (j in 1..b.length) {
                cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
            }
            prev = cur
        }
        return prev[b.length]
    }

    private fun baseTitle(title: String) =
        normalize(stripBrackets(title).replace(Regex("""(?i)\s(feat\.?|ft\.?|featuring)\s.*$"""), ""))

    private fun versions(title: String) = VERSION.findAll(normalize(title)).map { it.value }.toSet()

    private fun artistParts(artist: String) =
        artist.split(Regex(""",|&| and | x |feat\.?|ft\.?""", RegexOption.IGNORE_CASE))
            .map(::normalize).filter { it.isNotBlank() }

    /** Lowercase, accents removed, punctuation to spaces. */
    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}
