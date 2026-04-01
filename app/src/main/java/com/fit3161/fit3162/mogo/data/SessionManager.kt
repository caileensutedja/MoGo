package com.fit3161.fit3162.mogo.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_session")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_LOGIN_TIMESTAMP = longPreferencesKey("login_timestamp")
    }

    // --- READ ---
    val isLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_IS_LOGGED_IN] ?: false }

    val userName: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_USER_NAME] ?: "" }

    val userEmail: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_USER_EMAIL] ?: "" }

    val loginTimestamp: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_LOGIN_TIMESTAMP] ?: 0L }

    // --- WRITE ---
    // Your colleague will also call saveSession() after their DB insert - nothing changes here
    suspend fun saveSession(name: String, email: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_LOGGED_IN] = true
            prefs[KEY_USER_NAME] = name
            prefs[KEY_USER_EMAIL] = email
            prefs[KEY_LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs -> prefs.clear() }
    }

    // --- 13-WEEK EXPIRY (your R18 requirement) ---
    fun isSessionExpired(loginTimestamp: Long): Boolean {
        val thirteenWeeks = 13L * 7 * 24 * 60 * 60 * 1000
        return System.currentTimeMillis() - loginTimestamp > thirteenWeeks
    }
}