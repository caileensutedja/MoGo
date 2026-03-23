package com.fit3161.fit3162.mogo.UIScreen.SignInScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.AuthState
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * LoginViewModel manages LoginScreen state and business logic.
 */
class SignInViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Validates email then attempts to log in via [AuthRepository].
     */
    fun login(email: String, password: String) {
        if (!isValidEmail(email)) {
            _state.value = AuthState.Error(
                "Please use your @student.monash.edu or @monash.edu.au email."
            )
            return
        }
        viewModelScope.launch { // Login attempt happens here.
            _state.value = AuthState.Loading
            repo.login(email.trim(), password)
                .onSuccess { _state.value = AuthState.Success }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Login failed.") }
        }
    }

    /**
     * Reset state (AuthState) to Idle.
     */
    fun resetState() {
        _state.value = AuthState.Idle
    }

    /**
     * Checks if organization email is valid .
     */
    private fun isValidEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@student.monash.edu")
                || lower.endsWith("@monash.edu.au")
    }
}

/**
 *
 * Factory for LoginViewModel.
 *
 * Required because LoginViewModel has a constructor parameter (AuthRepository).
 * Android's default ViewModelProvider cannot instantiate parameterised ViewModels
 * without a factory.
 */
class SignInViewModelFactory(
    private val repo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignInViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SignInViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
