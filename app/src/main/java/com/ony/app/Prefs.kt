package com.ony.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("ony")

/** Saved settings: the listener's app, appearance, stats, and which link kinds have reached Ony. */
object Prefs {
    private val KEY_MY_APP = stringPreferencesKey("my_app")
    private val KEY_CAUGHT = stringSetPreferencesKey("caught_kinds")
    private val KEY_OPENED = intPreferencesKey("songs_opened")
    private val KEY_TOTAL_MS = longPreferencesKey("total_redirect_ms")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    suspend fun myApp(context: Context): MusicApp? =
        context.store.data.first()[KEY_MY_APP]?.let { name -> MusicApp.entries.firstOrNull { it.name == name } }

    suspend fun setMyApp(context: Context, app: MusicApp) {
        context.store.edit { it[KEY_MY_APP] = app.name }
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

    /** The appearance choice, as a stream so the screen restyles the moment it changes. */
    fun themeMode(context: Context): Flow<ThemeMode> =
        context.store.data.map { ThemeMode.fromSaved(it[KEY_THEME]) }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.store.edit { it[KEY_THEME] = mode.name }
    }
}
