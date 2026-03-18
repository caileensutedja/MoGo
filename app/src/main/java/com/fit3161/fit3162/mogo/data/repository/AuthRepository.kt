package com.fit3161.fit3162.mogo.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

import com.fit3161.fit3162.mogo.MogoApplication

/**
 * Repository responsible for all authentication operations with Supabase.
 *
 * - Centralises Supabase calls (single source of truth for data domain).
 *
 * @param supabase The shared [SupabaseClient] instance in [MogoApplication] class.
 */
class AuthRepository(private val supabase: SupabaseClient) {

    /**
     * Attempts to sign in an existing user with their email and password.
     *
     * Uses [Email] provider with signInWith — the v3 SDK equivalent of
     * the v2 signInWithPassword function. Both email and password are passed
     * inside the config block.
     *
     * NOTE ON DOMAIN RESTRICTION:
     * The ViewModel performs a client-side domain check before calling this.
     * The Supabase SQL trigger performs a server-side check on the database.
     * This function itself does not validate the domain — it trusts the caller.
     *
     * @param email    The user's university email address.
     * @param password The user's password.
     * @return [Result.success] if login succeeded, else [Result.failure]
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
     * Registers a new user with their email and password.
     *
     * After a successful call, Supabase sends a verification email to the
     * provided address. The account remains INACTIVE until the user clicks
     * the link in that email. Attempting to log in before confirming will
     * return an "Email not confirmed" error.
     *
     * NOTE ON ORG RESTRICTION:
     * The Supabase SQL trigger (set up in the dashboard) will reject any
     * registration attempt from a non-university email at the database level,
     * returning an exception that is mapped to a friendly message here.
     *
     * @param email    The user's university email address.
     * @param password The password the user wants to set for their account.
     * @return [Result.success] if the registration request was accepted and
     *         the verification email was sent, else [Result.failure].
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

    /***
     * Attempts to restore a previously authenticated session.
     *
     * Supabase persists the auth session token on the device. This function
     * checks whether a valid, non-expired session exists, and refreshes it
     * if possible. Call this on app launch (e.g. in MainActivity or a
     * SplashScreen) to skip the login screen for already-authenticated users.
     *
     * @return true if a valid session was found and restored, false otherwise.
     */
    suspend fun restoreSession(): Boolean {
        return try {
            supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Signs the current user out and clears their local session.
     *
     * After calling this, [restoreSession] will return false and any
     * authenticated Supabase requests will be rejected until the user
     * logs in again.
     */
    suspend fun logout() {
        supabase.auth.signOut()
    }

//    ----- PRIVATE/HELPER FUNCTIONS BELOW -----

    /**
     * Maps raw Supabase exception messages to user-friendly strings.
     *
     * Supabase throws exceptions with technical or inconsistently worded
     * messages. This function intercepts those and replaces them with
     * clear, actionable messages suitable for display in the UI.
     *
     * WHY NOT LET THE UI HANDLE THIS:
     * The UI (Composables) should not contain business logic or knowledge
     * of Supabase internals. Keeping error mapping here means the ViewModel
     * and UI only ever see plain readable strings.
     *
     * @param e The raw exception thrown by the Supabase SDK.
     * @return A new [Exception] with a user-friendly message.
     */
    private fun mapError(e: Exception) : Exception {
        val raw = e.message ?: "An unexpected error occurred."
        val msg = when {
            raw.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect email or password."
            raw.contains("Email not confirmed", ignoreCase = true) ->
                "Please confirm your email before logging in. Check your Monash inbox."
            raw.contains("Monash University", ignoreCase = true) ->
                "Only @student.monash.edu or @monash.edu addresses can register."
            raw.contains("User already registered", ignoreCase = true) ->
                "An account with this email already exists."
            raw.contains("Password should be at least", ignoreCase = true) ->
                "Password must be at least 8 characters."
            raw.contains("Unable to validate email address", ignoreCase = true) ->
                "Please enter a valid email address."
            raw.contains("Email address is invalid", ignoreCase = true) ->
                "Please enter a valid email address."
            raw.contains("signup_disabled", ignoreCase = true) ->
                "Registration is currently disabled."
            else -> raw  // ← Show the raw message in development so you can see exactly what Supabase is returning
        }
        return Exception(msg)
    }
}
