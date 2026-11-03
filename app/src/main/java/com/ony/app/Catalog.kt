package com.ony.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder

/**
 * Finds a song by name and artist with no account or key: reads the song from the tapped
 * link, then searches the listener's app's own catalogue for their country. Network calls
 * are thin; the parsing is pure and unit-tested.
 */
object Catalog {

    /** Title and artist of the song a link points at, or null if they can't be read. */
    suspend fun songFor(link: MusicLink): Song? = withContext(Dispatchers.IO) {
        try {
            when (link.provider) {
                Provider.SPOTIFY -> link.id?.takeIf { link.kind == "track" }?.let {
                    parseSpotifyEmbed(get("https://open.spotify.com/embed/track/$it"))
                }
                Provider.APPLE -> {
                    // Song links only (album?i=track or /song/id); an album link isn't one song.
                    val track = link.id?.takeIf { link.kind == "song" } ?: return@withContext null
                    val country = appleCountry(link.originalUrl)
                    parseItunesSong(get("https://itunes.apple.com/lookup?id=${link.albumId ?: track}&entity=song&country=$country"), track)
                }
                Provider.YOUTUBE -> link.id?.let { id ->
                    val watch = URLEncoder.encode("https://www.youtube.com/watch?v=$id", "UTF-8")
                    val o = JSONObject(get("https://www.youtube.com/oembed?url=$watch&format=json"))
                    val song = Match.fromVideoTitle(o.optString("title"), o.optString("author_name"))
                    // The album isn't in oEmbed; YouTube Music's row for this same video has it.
                    song.copy(album = albumOfVideo(searchYtMusic(song), id))
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** The song in Apple Music's catalogue for [country] (store availability differs by country). */
    suspend fun appleMusic(song: Song, country: String): Destination? = withContext(Dispatchers.IO) {
        try {
            val term = URLEncoder.encode(Match.searchTerm(song), "UTF-8")
            val results = parseItunesResults(get("https://itunes.apple.com/search?term=$term&media=music&entity=song&limit=15&country=$country"))
            Match.best(results, song)?.let { Destination(MusicApp.APPLE_MUSIC, it.url) }
        } catch (_: Exception) {
            null
        }
    }

    /** The song in YouTube Music for [country], through the search its own web player uses. */
    suspend fun youtubeMusic(song: Song, country: String): Destination? = withContext(Dispatchers.IO) {
        Match.best(searchYtMusic(song, country), song)?.let { Destination(MusicApp.YOUTUBE_MUSIC, it.url) }
    }

    private fun searchYtMusic(song: Song, country: String? = null): List<Match.Candidate> = try {
        val client = JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20241023.01.00").put("hl", "en")
        if (country != null) client.put("gl", country.uppercase())
        val body = JSONObject()
            .put("context", JSONObject().put("client", client))
            .put("query", Match.searchTerm(song))
            .put("params", SONGS_ONLY)
        parseYtMusic(post("https://music.youtube.com/youtubei/v1/search?prettyPrint=false", body.toString()))
    } catch (_: Exception) {
        emptyList()
    }

    /** The album on YouTube Music's search row for video [id], if it's among [rows]. */
    internal fun albumOfVideo(rows: List<Match.Candidate>, id: String): String? =
        rows.firstOrNull { it.url.endsWith("v=$id") }?.album

    /** Spotify has no free search, so this opens its search screen for the song. */
    fun spotifySearch(song: Song): Destination {
        val term = Match.searchTerm(song).replace('/', ' ').replace(Regex("\\s+"), " ").trim()
        return Destination(MusicApp.SPOTIFY, "spotify:search:" + URLEncoder.encode(term, "UTF-8").replace("+", "%20"))
    }

    /**
     * The listener's country for store searches: SIM first (it usually matches their Apple
     * Music or Spotify account, even when travelling), then the network, then the phone's
     * region setting, then "us". Anything that isn't a two-letter code is skipped.
     */
    fun countryOf(sim: String?, network: String?, region: String?): String =
        listOf(sim, network, region).map { it?.trim()?.lowercase().orEmpty() }
            .firstOrNull { it.length == 2 && it.all(Char::isLetter) } ?: "us"

    /** Country code in an Apple Music link (music.apple.com/in/...), "us" when absent. */
    internal fun appleCountry(url: String): String =
        try {
            URI(url).path.split("/").firstOrNull { it.length == 2 && it.all(Char::isLetter) }?.lowercase()
        } catch (_: Exception) { null } ?: "us"

    internal fun parseItunesResults(json: String): List<Match.Candidate> = try {
        JSONObject(json).optJSONArray("results").objects().mapNotNull { r ->
            val url = r.optString("trackViewUrl").ifBlank { return@mapNotNull null }
            Match.Candidate(r.optString("trackName"), r.optString("artistName"), url, r.optString("collectionName").ifBlank { null })
        }
    } catch (_: Exception) {
        emptyList()
    }

    internal fun parseItunesSong(json: String, trackId: String): Song? = try {
        val tracks = JSONObject(json).optJSONArray("results").objects().filter { it.optString("wrapperType") == "track" }
        // Only the linked track; never another song from the same album.
        tracks.firstOrNull { it.optLong("trackId").toString() == trackId }
            ?.let { Song(it.optString("trackName"), it.optString("artistName"), it.optString("collectionName").ifBlank { null }) }
    } catch (_: Exception) {
        null
    }

    internal fun parseYtMusic(json: String): List<Match.Candidate> = try {
        val out = mutableListOf<Match.Candidate>()
        fun walk(node: Any?) {
            when (node) {
                is JSONObject -> {
                    node.optJSONObject("musicResponsiveListItemRenderer")?.let { item ->
                        val id = item.optJSONObject("playlistItemData")?.optString("videoId").orEmpty()
                        val cols = item.optJSONArray("flexColumns").objects().map { col ->
                            col.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")
                                ?.optJSONArray("runs").objects().joinToString("") { it.optString("text") }
                        }
                        val title = cols.getOrNull(0).orEmpty()
                        // Second column: "Artist • Album • 3:22" for songs ("Artist • 2:04" without an album).
                        val parts = cols.getOrNull(1).orEmpty().split(" • ")
                        val album = parts.getOrNull(1)?.takeIf { parts.size >= 3 }
                        if (id.isNotBlank() && title.isNotBlank()) {
                            out += Match.Candidate(title, parts.first(), "https://music.youtube.com/watch?v=$id", album)
                        }
                    }
                    node.keys().forEach { walk(node.opt(it)) }
                }
                is JSONArray -> for (i in 0 until node.length()) walk(node.opt(i))
            }
        }
        walk(JSONObject(json))
        out.distinctBy { it.url }
    } catch (_: Exception) {
        emptyList()
    }

    internal fun parseSpotifyEmbed(html: String): Song? = try {
        val data = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            .find(html)?.groupValues?.get(1)
        val entity = data?.let { JSONObject(it) }?.optJSONObject("props")?.optJSONObject("pageProps")
            ?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
        val name = entity?.optString("name")?.ifBlank { null } ?: entity?.optString("title")?.ifBlank { null }
        val artists = entity?.optJSONArray("artists").objects().map { it.optString("name") }.filter { it.isNotBlank() }
        if (name == null) null else Song(name, artists.joinToString(", "))
    } catch (_: Exception) {
        null
    }

    // YouTube Music's "Songs" filter, so videos and podcasts don't crowd the results.
    private const val SONGS_ONLY = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun get(url: String): String = request(url, null)

    private fun post(url: String, json: String): String = request(url, json)

    private fun request(url: String, json: String?): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Ony/0.1")
            if (json != null) {
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Origin", "https://music.youtube.com")
                outputStream.use { it.write(json.toByteArray()) }
            }
        }
        try {
            if (conn.responseCode !in 200..299) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
