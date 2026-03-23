package com.fit3161.fit3162.mogo.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.MogoApplication
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = AuthRepository(
        (application as MogoApplication).supabase
    )

    // Variables
    private val _loginState = MutableStateFlow<Result<Unit>?>(null)
    val loginState: StateFlow<Result<Unit>?> = _loginState

    fun isLoggedIn(): Boolean = repo.isUserLogedIn()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = repo.login(email, password)
        }
    }

    fun clearState() {
        _loginState.value = null
    }
}