package com.fit3161.fit3162.mogo.UIScreen.ActiveRide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.AuthRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.EmergencyContact
import com.fit3161.fit3162.mogo.data.repo.EmergencyContactRepository
import com.fit3161.fit3162.mogo.data.repo.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ActiveRideUiState(
    val booking: Booking? = null,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val riderName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class ActiveRideViewModel(
    private val authRepo: AuthRepository,
    private val bookRepo: BookRepository,
    private val contactRepo: EmergencyContactRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveRideUiState())
    val uiState: StateFlow<ActiveRideUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val userId = authRepo.getCurrentUserId()
            if (userId == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = "Not logged in")
                return@launch
            }
            try {
                val bookings = bookRepo.getOngoingRiderBookings(userId)
                val contacts = contactRepo.getContacts(userId)
                val profile = profileRepo.getProfile(userId)
                _uiState.value = _uiState.value.copy(
                    booking = bookings.firstOrNull(),
                    emergencyContacts = contacts,
                    riderName = profile?.user_name ?: "",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}

class ActiveRideViewModelFactory(
    private val client: SupabaseClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveRideViewModel(
                AuthRepository(client),
                BookRepository(client),
                EmergencyContactRepository(client),
                ProfileRepository(client)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}