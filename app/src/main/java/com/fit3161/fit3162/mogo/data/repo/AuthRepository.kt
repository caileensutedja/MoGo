package com.fit3161.fit3162.mogo.data.repo

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

/**
 * AuthRepository class handles authentication operations using the Supabase DB client.
 *
 * Note:
 * - Viewmodel calls Repository.
 * - Repository calls DB API.
 */
class AuthRepository(private val supabase: SupabaseClient) {

    /**
     * Adds a new user to the Users table in DB.
     */
    suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Signs in an existing user from the Users table in DB.
     *
     */
    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Returns boolean if user is currently logged in or not.
     *
     *
     */
    fun isUserLogedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    /**
     * Logs the current user out of the session. Logged-out users must log in again
     * to be able to use the app.
     */
    suspend fun logout() {
        supabase.auth.signOut()
    }

    // --- Helper functions ---

    /**
     * Maps raw Supabase exception messages to user-friendly strings.
     */
    private fun mapError(e: Exception): Exception {
        val msg = when {
            e.message?.contains("Invalid login credentials") == true ->
                "Incorrect email or password."

            e.message?.contains("Email not confirmed") == true ->
                "Please confirm your email before logging in. Check your university inbox."

            e.message?.contains("Monash University students/staff only") == true ->
                "Only @student.monash.edu or @monash.edu addresses can register."

            e.message?.contains("User already registered") == true ->
                "An account with this email already exists."

            else -> e.message ?: "An unexpected error occurred."
        }
        return Exception(msg)
    }
}
