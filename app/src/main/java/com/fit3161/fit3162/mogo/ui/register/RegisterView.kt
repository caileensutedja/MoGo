package com.fit3161.fit3162.mogo.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.AuthState
import com.fit3161.fit3162.mogo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /**
     * Validates inputs and registers a new user if all checks pass.
     * Validation is performed in order — the first failing check stops
     * execution and shows its error message.
     *
     * @param email           Raw email string from the input field.
     * @param password        Raw password string from the input field.
     * @param confirmPassword Raw confirm password string from the input field.
     */
    fun register(email: String, password: String, confirmPassword: String) {
        when {
            !isMonashEmail(email) ->
                _state.value = AuthState.Error(
                    "Please use your @student.monash.edu or @monash.edu email."
                )
            password.length < 8 ->
                _state.value = AuthState.Error("Password must be at least 8 characters.")
            password != confirmPassword ->
                _state.value = AuthState.Error("Passwords do not match.")
            else -> viewModelScope.launch {
                _state.value = AuthState.Loading
                repo.register(email.trim(), password)
                    .onSuccess { _state.value = AuthState.AwaitingEmailConfirmation }
                    .onFailure {
                        _state.value = AuthState.Error(it.message ?: "Registration failed.")
                    }
            }
        }
    }

    /**
     * Resets state to Idle — called when the user edits any input field
     * so that previous error messages are cleared as they type.
     */
    fun resetState() {
        _state.value = AuthState.Idle
    }

    /**
     * Validates that the email belongs to the University of Melbourne.
     *
     * @param email The email string to validate.
     * @return true if the email ends with a known UniMelb domain.
     */
    private fun isMonashEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@student.monash.edu")
                || lower.endsWith("@monash.edu")
    }
}

/**
 * RegisterViewModelFactory
 *
 * Required because RegisterViewModel has a constructor parameter (AuthRepository).
 * Tells Android's ViewModelProvider how to instantiate RegisterViewModel correctly.
 *
 * @param repo The AuthRepository to pass into the ViewModel.
 */
class RegisterViewModelFactory(
    private val repo: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
