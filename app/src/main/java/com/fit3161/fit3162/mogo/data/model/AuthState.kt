package com.fit3161.fit3162.mogo.data.model

import com.fit3161.fit3162.mogo.data.repository.AuthRepository

/**
 * Represents all possible states of an authentication operation.
 *
 * NOTE:
 * This is a sealed class, meaning every possible state is defined here —
 * no other state can exist. This makes it exhaustive and safe to use in
 * when() expressions without an else branch.
 *
 * - Using sealed class instead of Boolean allows type-safe state and carrying data alongside state itself.
 */
sealed class AuthState {
    /**
     * Default state (no operation is in progress).
     */
    object Idle: AuthState()

    /**
     * State representing an auth operation is currently in progress (network request is running).
     * UI Should disable input fields, show loading indicator (prevent duplicate submission).
     */
    object Loading : AuthState()

    /**
     * State representing successful and completed auth operation.
     */
    object Success : AuthState()

    /**
     * State representing waiting for email confirmation.
     * This is typically only called during after Registration process, in which
     * the user still needs to confirm their identity (usually via verification email link).
     */
    object AwaitingEmailConfirmation : AuthState()

    /**
     * State representing a failed auth operation.
     *
     * The [message] property contains a user-friendly
     * description of what went wrong (e.g. "Incorrect email or password.").
     *
     * @property message A human-readable error message suitable for display in the UI.
     *                   This is mapped from raw Supabase exceptions in [AuthRepository].
     */
    data class Error(val message: String): AuthState()
}
