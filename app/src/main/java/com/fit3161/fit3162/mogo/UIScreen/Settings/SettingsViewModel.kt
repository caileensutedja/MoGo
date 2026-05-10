package com.fit3161.fit3162.mogo.UIScreen.Settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.EmergencyContact
import com.fit3161.fit3162.mogo.data.repo.EmergencyContactRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class SettingsViewModel(
    private val authRepo: AuthRepository,
    private val contactRepo: EmergencyContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadContacts()
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
                return@launch
            }
            val contacts = contactRepo.getContacts(userId)
            _uiState.value = _uiState.value.copy(contacts = contacts, isLoading = false)
        }
    }

    fun addContact(name: String, phone: String) {
        viewModelScope.launch {
            val userId = authRepo.getCurrentUserId() ?: return@launch
            val contact = EmergencyContact(
                userId = userId,
                contactName = name,
                contactPhone = phone
            )
            val result = contactRepo.addContact(contact)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "$name added as safety contact"
                )
                loadContacts()
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to add contact")
            }
        }
    }

    fun deleteContact(contactId: String) {
        _uiState.value = _uiState.value.copy(
            contacts = _uiState.value.contacts.filter { it.contactId != contactId }
        )
        viewModelScope.launch {
            val result = contactRepo.deleteContact(contactId)
            if (result.isFailure) {
                loadContacts()
                _uiState.value = _uiState.value.copy(error = "Failed to delete contact")
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

class SettingsViewModelFactory(
    private val client: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(
                AuthRepository(client),
                EmergencyContactRepository(client)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}