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

    suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            } // TODO: There is no signInWithPassword(email, password)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Registers user.
     */
    suspend fun register(email: String, password: String): Result<Unit> {
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

    /***
     * Attempt to restore the current session.
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
     * Logout of the account.
     */
    suspend fun logout() {
        supabase.auth.signOut()
    }

//    ----- PRIVATE FUNCTIONS BELOW -----

    private fun mapError(e: Exception) : Exception {
        val msg = when {
            e.message?.contains("Invalid login credentials") == true -> "Incorrect email or password."
            e.message?.contains("Email unconfirmed") == true -> "Please confirm your email before logging in. Check your university inbox."
            e.message?.contains("Monash University") == true -> "Only @student.monash.edu or @monash.edu addresses can register."
            e.message?.contains("User already registered") == true -> "An account with this email already exists."

            else -> e.message ?: "An unexpected error has occurred."
        }
        return Exception(msg)
    }
}