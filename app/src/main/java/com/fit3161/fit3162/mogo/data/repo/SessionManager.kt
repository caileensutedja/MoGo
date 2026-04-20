package com.fit3161.fit3162.mogo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_prefs")

class SessionManager(private val context: Context) {

    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
        private const val SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN] ?: false
    }

    val loginTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[LOGIN_TIMESTAMP] ?: 0L
    }

    suspend fun saveSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs[LOGIN_TIMESTAMP] = 0L
        }
    }

    fun isSessionExpired(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp > SESSION_DURATION_MS
    }
}