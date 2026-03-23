package com.fit3161.fit3162.mogo.UIScreen.RegisterScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.AuthState
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegisterViewModel(private val repo: AuthRepository) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun register(email: String, password: String, confirmPassword: String, name: String) {
        when {
            !isValidEmail(email) ->
                _state.value = AuthState.Error(
                    "Please use your @student.monash.edu or @monash.edu email."
                )
            password.length < 8 ->
                _state.value = AuthState.Error("Password must be at least 8 characters.")
            password != confirmPassword ->
                _state.value = AuthState.Error("Passwords do not match.")
            name.length < 1 ->
                _state.value = AuthState.Error("Please enter your name")
            else -> viewModelScope.launch {
                _state.value = AuthState.Loading
                repo.register(email.trim(), password, name)
                    .onSuccess { _state.value = AuthState.AwaitingEmailConfirmation }
                    .onFailure {
                        _state.value = AuthState.Error(it.message ?: "Registration failed.")
                    }
            }
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        val lower = email.trim().lowercase()
        return lower.endsWith("@student.monash.edu")
                || lower.endsWith("@monash.edu")
    }
}

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
