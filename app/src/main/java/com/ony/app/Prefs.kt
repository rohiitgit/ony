package com.ony.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("ony")

/** Saved settings: the listener's app, appearance, and which link kinds have reached Ony. */
object Prefs {
    private val KEY_MY_APP = stringPreferencesKey("my_app")
    private val KEY_CAUGHT = stringSetPreferencesKey("caught_kinds")
    private val KEY_THEME = stringPreferencesKey("theme_mode")

    // Update checks are optional and non-intrusive: we remember when we last checked,
    // which version the user dismissed/snoozed, and which version we already notified for.
    private val KEY_UPDATE_LAST_CHECK = longPreferencesKey("update_last_check_ms")
    private val KEY_UPDATE_DISMISSED_VERSION = longPreferencesKey("update_dismissed_version")
    private val KEY_UPDATE_SNOOZE_UNTIL = longPreferencesKey("update_snooze_until_ms")
    private val KEY_UPDATE_LAST_NOTIFIED = longPreferencesKey("update_last_notified_version")

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

    /** The appearance choice, as a stream so the screen restyles the moment it changes. */
    fun themeMode(context: Context): Flow<ThemeMode> =
        context.store.data.map { ThemeMode.fromSaved(it[KEY_THEME]) }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.store.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun updateLastCheckMs(context: Context): Long = context.store.data.first()[KEY_UPDATE_LAST_CHECK] ?: 0L
    suspend fun setUpdateLastCheckMs(context: Context, value: Long) {
        context.store.edit { it[KEY_UPDATE_LAST_CHECK] = value }
    }

    suspend fun updateDismissedVersion(context: Context): Long = context.store.data.first()[KEY_UPDATE_DISMISSED_VERSION] ?: 0L
    suspend fun setUpdateDismissedVersion(context: Context, versionCode: Long) {
        context.store.edit { it[KEY_UPDATE_DISMISSED_VERSION] = versionCode }
    }

    suspend fun updateSnoozeUntilMs(context: Context): Long = context.store.data.first()[KEY_UPDATE_SNOOZE_UNTIL] ?: 0L
    suspend fun setUpdateSnoozeUntilMs(context: Context, untilMs: Long) {
        context.store.edit { it[KEY_UPDATE_SNOOZE_UNTIL] = untilMs }
    }

    /** Clears snooze (e.g. when showing a newer version). */
    suspend fun clearUpdateSnooze(context: Context) {
        context.store.edit { it.remove(KEY_UPDATE_SNOOZE_UNTIL) }
    }

    suspend fun updateLastNotifiedVersion(context: Context): Long = context.store.data.first()[KEY_UPDATE_LAST_NOTIFIED] ?: 0L
    suspend fun setUpdateLastNotifiedVersion(context: Context, versionCode: Long) {
        context.store.edit { it[KEY_UPDATE_LAST_NOTIFIED] = versionCode }
    }
}
