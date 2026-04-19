package com.fit3161.fit3162.mogo.UIScreen.HomeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import com.fit3161.fit3162.mogo.data.repo.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val profile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val authRepo: AuthRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
                return@launch
            }
            try {
                val profile = profileRepo.getProfile(userId)
                _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}