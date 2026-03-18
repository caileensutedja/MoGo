package com.fit3161.fit3162.mogo.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.AuthState
import com.fit3161.fit3162.mogo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * ViewModel for the Login screen.
 *
 * WHAT IS A VIEWMODEL:
 * A ViewModel holds and manages UI-related state that should survive
 * configuration changes (e.g. screen rotation). It sits between the UI
 * (LoginScreen composable) and the data layer (AuthRepository), and contains
 * the business logic for the login flow.
 *
 * WHAT THIS VIEWMODEL DOES:
 * - Validates the email domain before making any network call
 * - Delegates the actual login operation to [AuthRepository]
 * - Exposes the result as a [StateFlow] that the UI observes and reacts to
 * - Does NOT know anything about Supabase, Compose, or Android UI components
 *
 * @param repo The [AuthRepository] used to perform the login operation.
 */
class LoginViewModel(private val repo: AuthRepository) : ViewModel() {

    /**
     * Internal mutable state — only this ViewModel can change it.
     * Starts as [AuthState.Idle] (no operation in progress).
     */
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)

    /**
     * Public read-only state exposed to the UI.
     * The UI collects this flow and re-renders whenever the state changes.
     * [asStateFlow] prevents the UI from casting it back to MutableStateFlow.
     */
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Attempts to log the user in with the provided credentials.
     *
     * FLOW:
     * 1. Client-side domain check — if the email is not a monash address,
     *    emit an error immediately without making a network call.
     * 2. Set state to Loading — UI shows a spinner and disables the button.
     * 3. Call [AuthRepository.login] on a background coroutine.
     * 4. On success — emit [AuthState.Success], triggering navigation.
     * 5. On failure — emit [AuthState.Error] with the mapped error message.
     *
     * @param email    The email entered by the user.
     * @param password The password entered by the user.
     */
    fun login(email: String, password: String) {
        if (!isMonashEmail(email)) {
            _state.value = AuthState.Error(
                "Please use your @student.monash.edu or @monash.edu email."
            )
            return
        }
        viewModelScope.launch {
            _state.value = AuthState.Loading
            repo.login(email.trim(), password)
                .onSuccess { _state.value = AuthState.Success }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Login failed.") }
        }
    }

    /**
     * Resets the state back to [AuthState.Idle].
     * Called whenever the user edits an input field, so that previous
     * error messages are dismissed as soon as they start correcting input.
     */
    fun resetState() {
        _state.value = AuthState.Idle
    }

    /**
     * Validates that the email belongs to the University of Melbourne.
     * This is a client-side check for immediate feedback — the server-side
     * SQL trigger provides the authoritative enforcement.
     *
     * Accepted domains:
     * - @student.monash.edu (students)
     * - @monash.edu (staff)
     *
     * @param email The raw email string entered by the user.
     * @return true if the email ends with a recognised monash domain.
     */
    private fun isMonashEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@student.monash.edu.au")
                || lower.endsWith("@monash.edu.au")
    }
}

/**
 * Factory for creating [LoginViewModel] instances with constructor parameters.
 *
 * WHY THIS IS NEEDED:
 * Android's default ViewModelProvider cannot create ViewModels that have
 * constructor parameters (like our [AuthRepository] dependency). A factory
 * tells the ViewModelProvider how to instantiate the ViewModel correctly.
 * Without this, the app would crash when trying to create [LoginViewModel].
 *
 * USAGE:
 *     val viewModel: LoginViewModel = viewModel(
 *         factory = LoginViewModelFactory(authRepository)
 *     )
 *
 * @param repo The [AuthRepository] to inject into [LoginViewModel].
 */
class LoginViewModelFactory(
    private val repo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
