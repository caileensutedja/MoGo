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

/**
 * UI state for the Settings screen.
 *
 * Holds:
 * - List of saved emergency contacts
 * - Loading indicator for async operations
 * - Error messages for failed actions
 * - Success messages for user feedback
 */
data class SettingsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * ViewModel for the Settings screen.
 *
 * Responsibilities:
 * - Load the user's emergency contacts
 * - Add new contacts to the database
 * - Delete existing contacts
 * - Manage UI state (loading, errors, success messages)
 *
 * Uses:
 * - AuthRepository to identify the current user
 * - EmergencyContactRepository to read/write contact data
 */
class SettingsViewModel(
    private val authRepo: AuthRepository,
    private val contactRepo: EmergencyContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        loadContacts()
    }

    /**
     * Loads all emergency contacts for the current user.
     *
     * Steps:
     * - Show loading state
     * - Fetch user ID from AuthRepository
     * - Retrieve contacts from EmergencyContactRepository
     * - Update UI state with results
     */
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

    /**
     * Adds a new emergency contact for the current user.
     *
     * Behaviour:
     * - Builds an EmergencyContact object
     * - Saves it via the repository
     * - Shows a success message on success
     * - Shows an error message on failure
     * - Reloads the contact list after adding
     */
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

    /**
     * Deletes a contact by ID.
     *
     * Behaviour:
     * - Optimistically removes the contact from UI state
     * - Attempts deletion in the repository
     * - If deletion fails, reloads contacts and shows an error
     */
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

    /**
     * Clears both error and success messages from the UI state.
     *
     * Used after Snackbar messages are shown.
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    /**
     * Shows a generic "Settings saved!" confirmation message.
     *
     * Triggered when the user taps the Save button.
     */
    fun showSaved() {
        _uiState.value = _uiState.value.copy(successMessage = "Settings saved!")
    }
}

/**
 * Factory for creating SettingsViewModel instances.
 *
 * Injects:
 * - AuthRepository
 * - EmergencyContactRepository
 *
 * Required because SettingsViewModel has constructor parameters.
 */
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