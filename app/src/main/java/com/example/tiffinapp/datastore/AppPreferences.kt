package com.example.tiffinapp.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** DataStore singleton extension on [Context]. */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tiffin_prefs")

/**
 * Provides typed read/write access to persisted app preferences.
 *
 * Keys:
 * - [KEY_LAUNCH_COUNT] — number of times the app has been launched (Int).
 * - [KEY_IS_PAID]      — whether the user has an active subscription (Boolean).
 */
class AppPreferences(private val context: Context) {

    companion object {
        val KEY_LAUNCH_COUNT = intPreferencesKey("launch_count")
        val KEY_IS_PAID      = booleanPreferencesKey("is_paid")
    }

    /** Emits the current launch count, defaulting to 0. */
    val launchCountFlow: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_LAUNCH_COUNT] ?: 0 }

    /** Emits `true` when the user has completed a subscription. */
    val isPaidFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_IS_PAID] ?: false }

    /** Increments [KEY_LAUNCH_COUNT] by 1. Safe to call from any coroutine context. */
    suspend fun incrementLaunchCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_LAUNCH_COUNT] ?: 0
            prefs[KEY_LAUNCH_COUNT] = current + 1
        }
    }

    /** Persists [KEY_IS_PAID] = `true`. Once set, paywall never shows again. */
    suspend fun markAsPaid() {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_PAID] = true
        }
    }
}
