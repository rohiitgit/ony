package com.ony.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.YearMonth

private val Context.store by preferencesDataStore("ony")

/** Saved settings: the listener's app, their MusicLink key, and which link kinds have reached Ony. */
object Prefs {
    private val KEY_MY_APP = stringPreferencesKey("my_app")
    private val KEY_API = stringPreferencesKey("musiclink_key")
    private val KEY_CAUGHT = stringSetPreferencesKey("caught_kinds")
    private val KEY_OPENED = intPreferencesKey("songs_opened")
    private val KEY_TOTAL_MS = longPreferencesKey("total_redirect_ms")
    private val KEY_LOOKUP_MONTH = stringPreferencesKey("lookup_month")
    private val KEY_LOOKUP_COUNT = intPreferencesKey("lookup_count")

    suspend fun myApp(context: Context): MusicApp? =
        context.store.data.first()[KEY_MY_APP]?.let { name -> MusicApp.entries.firstOrNull { it.name == name } }

    suspend fun setMyApp(context: Context, app: MusicApp) {
        context.store.edit { it[KEY_MY_APP] = app.name }
    }

    /** The key the user pasted, else the one a personal build put in local.properties. */
    suspend fun apiKey(context: Context): String =
        context.store.data.first()[KEY_API]?.takeIf { it.isNotBlank() } ?: BuildConfig.MUSICLINK_API_KEY

    suspend fun savedApiKey(context: Context): String = context.store.data.first()[KEY_API].orEmpty()

    suspend fun setApiKey(context: Context, key: String) {
        context.store.edit { if (key.isBlank()) it.remove(KEY_API) else it[KEY_API] = key.trim() }
    }

    /** Link kinds Android has actually handed to Ony, proof that setup steps 2 and 3 worked. */
    suspend fun caughtKinds(context: Context): Set<LinkKind> =
        context.store.data.first()[KEY_CAUGHT].orEmpty().mapNotNull { name -> LinkKind.entries.firstOrNull { it.name == name } }.toSet()

    suspend fun markCaught(context: Context, kind: LinkKind) {
        context.store.edit { it[KEY_CAUGHT] = it[KEY_CAUGHT].orEmpty() + kind.name }
    }

    /** Songs Ony has opened, and the total time those took, for the stats panel. */
    suspend fun stats(context: Context): Pair<Int, Long> =
        context.store.data.first().let { (it[KEY_OPENED] ?: 0) to (it[KEY_TOTAL_MS] ?: 0L) }

    suspend fun recordOpen(context: Context, elapsedMs: Long) {
        context.store.edit {
            it[KEY_OPENED] = (it[KEY_OPENED] ?: 0) + 1
            it[KEY_TOTAL_MS] = (it[KEY_TOTAL_MS] ?: 0L) + elapsedMs
        }
    }

    /** MusicLink calls this phone made this calendar month. */
    suspend fun lookupsThisMonth(context: Context): Int =
        context.store.data.first().let {
            Setup.lookupsThisMonth(it[KEY_LOOKUP_MONTH], it[KEY_LOOKUP_COUNT] ?: 0, YearMonth.now().toString())
        }

    suspend fun recordLookup(context: Context) {
        val month = YearMonth.now().toString()
        context.store.edit {
            val count = Setup.lookupsThisMonth(it[KEY_LOOKUP_MONTH], it[KEY_LOOKUP_COUNT] ?: 0, month)
            it[KEY_LOOKUP_MONTH] = month
            it[KEY_LOOKUP_COUNT] = count + 1
        }
    }
}
