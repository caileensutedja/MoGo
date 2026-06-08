package com.fit3161.fit3162.mogo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
/**
 * Manages user session persistence for the MoGo application.
 *
 * Stores login state and timestamp using DataStore, and enforces a 7-day session expiry.
 */
private val Context.dataStore by preferencesDataStore(name = "session_prefs")

/**
 * Manages persistent login session state using Jetpack DataStore.
 *
 * Tracks whether the user is logged in and when the session was created,
 * and provides methods to save, clear, and validate the session.
 *
 * @param context Application context used to access the DataStore.
 */
class SessionManager(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
        private const val SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }
    /** Emits true if the user is currently marked as logged in, false otherwise. */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    /** Emits true if the user is currently marked as logged in, false otherwise. */
    val loginTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LOGIN_TIMESTAMP] ?: 0L
    }

    /**
     * Persists a new login session by setting the logged-in flag and recording the current timestamp.
     */
    suspend fun saveSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Clears the current session by resetting the logged-in flag and timestamp.
     */
    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs[LOGIN_TIMESTAMP] = 0L
        }
    }

    /**
     * Checks whether a session has exceeded the 7-day expiry window.
     *
     * @param timestamp The Unix timestamp (ms) of the session to check.
     * @return True if the session has expired, false otherwise.
     */
    fun isSessionExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > SESSION_DURATION_MS
    }
}