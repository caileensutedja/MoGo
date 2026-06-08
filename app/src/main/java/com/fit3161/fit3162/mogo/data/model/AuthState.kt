package com.fit3161.fit3162.mogo.data.model

import com.fit3161.fit3162.mogo.data.repo.AuthRepository
/**
 * AuthState represents every possible state of an authentication process.
 *
 * Emitted by ViewModels via StateFlow and observed by Login/Register
 * screens to update the UI.
 */
sealed class AuthState {

    /**
     * Initial/default state.
     */
    object Idle : AuthState()

    /**
     * State when there is an ongoing authentication operation/process.
     */
    object Loading : AuthState()

    /**
     * State when authentication is successful.
     */
    object Success : AuthState()

    /**
     * State when auth is successful but requires respond to verification email link.
     */
    object AwaitingEmailConfirmation : AuthState()

    /**
     * Error data class. Representing a state when an authentication operation fails.
     *
     * @param message the error message displayed when encountered. Defined in more detail in [AuthRepository].
     */
    data class Error(val message: String) : AuthState()
}
