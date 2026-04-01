package com.fit3161.fit3162.mogo.UIScreen.SettingsScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val driverPreference: String = "",
    val carPreference: String = "",
    val role: String = "",
    val placeholder: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onDriverPreferenceChange(value: String) {
        _uiState.update { it.copy(driverPreference = value) }
    }

    fun onCarPreferenceChange(value: String) {
        _uiState.update { it.copy(carPreference = value) }
    }

    fun onRoleChange(value: String) {
        _uiState.update { it.copy(role = value) }
    }

    fun onPlaceholderChange(value: String) {
        _uiState.update { it.copy(placeholder = value) }
    }

    fun saveSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveSuccess = false, errorMessage = null) }
            try {
                // TODO: call your repository here, e.g.:
                // repository.saveUserSettings(uiState.value)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun clearSaveResult() {
        _uiState.update { it.copy(saveSuccess = false, errorMessage = null) }
    }
}
