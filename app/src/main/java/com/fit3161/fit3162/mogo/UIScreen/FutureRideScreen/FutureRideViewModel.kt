package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.CAMPUS_OPTIONS
import com.fit3161.fit3162.mogo.data.repo.Ride
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FutureRideUiState(
    val selectedDate: String = "",
    val rides: List<Ride> = emptyList(),
    val hiddenRideIds: Set<String> = emptySet(),
    val selectedCampus: String? = null,
    val genderPreference: String? = null, // Filters gender preference
    val isLoading: Boolean = false,
    val bookingMessage: String? = null,
    val error: String? = null
) {
    val visibleRides: List<Ride>
        get() = rides
            .filter { it.id !in hiddenRideIds }
            .filter { selectedCampus == null || it.destination == selectedCampus }  // ADD THIS

    val hiddenRides: List<Ride>
        get() = rides
            .filter { it.id in hiddenRideIds }
            .filter { selectedCampus == null || it.destination == selectedCampus }  // ADD THIS
}


class FutureRideViewModel (
    private val repo: BookRepository,
    private val userId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(FutureRideUiState())
    val uiState: StateFlow<FutureRideUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val pref = repo.getGenderPreference(userId)
                val hiddenIds = repo.getHiddenRideIds(userId)
                _uiState.value = _uiState.value.copy(
                    genderPreference = pref,
                    hiddenRideIds = hiddenIds
                )
                loadAllFutureRides()
            } catch (e: Exception) {
                Log.e("CRASH", "Init failed. Full Error: ${e.stackTraceToString()}") // This gives the full story
                _uiState.value = _uiState.value.copy(error = "Connection Failed: ${e.message}")
            }
        }
    }

    fun onDateSelected(date: String) {
        _uiState.value = _uiState.value.copy(
            selectedDate = date,
            isLoading = true,
            error = null
        )
        loadRidesByDate(date)
    }

    fun onDateCleared() {
        _uiState.value = _uiState.value.copy(
            selectedDate = "",
            isLoading = true,
            error = null
        )
        loadAllFutureRides()
    }

    fun onCampusSelected(campus: String?) {
        _uiState.value = _uiState.value.copy(selectedCampus = campus)
    }

    private fun loadAllFutureRides() {
        viewModelScope.launch {
            try {
                val rides = repo.getAllFutureRides(
                    userId = userId,  // ← userId FIRST
                    genderPreference = _uiState.value.genderPreference  // ← SECOND (optional)
                )
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun loadRidesByDate(date: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val rides = repo.getFutureRidesByDate(
                    userId,
                    date,
                    genderPreference = _uiState.value.genderPreference)
                _uiState.value = _uiState.value.copy(rides = rides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun hideRide(rideId: String) {
        // Optimistically update UI immediately
        _uiState.value = _uiState.value.copy(
            hiddenRideIds = _uiState.value.hiddenRideIds + rideId
        )
        viewModelScope.launch {
            try {
                repo.hideRide(userId, rideId)
                Log.d("HIDE", "Success to hide ride: $rideId")
            } catch (e: Exception) {
                // Revert if DB call fails
                _uiState.value = _uiState.value.copy(
                    hiddenRideIds = _uiState.value.hiddenRideIds - rideId
                )
                Log.d("HIDE", "Failed to hide ride: ${e.message}")
            }
        }
    }

    fun unhideRide(rideId: String) {
        // Optimistically update UI immediately
        _uiState.value = _uiState.value.copy(
            hiddenRideIds = _uiState.value.hiddenRideIds - rideId
        )
        viewModelScope.launch {
            try {
                repo.unhideRide(userId, rideId)
            } catch (e: Exception) {
                // Revert if DB call fails
                _uiState.value = _uiState.value.copy(
                    hiddenRideIds = _uiState.value.hiddenRideIds + rideId
                )
                Log.d("HIDE", "Failed to unhide ride: ${e.message}")
            }
        }
    }

    fun bookRide(ride: Ride, pickupName: String, pickupLat: Double, pickupLng: Double) {
        val campusLocation = CAMPUS_OPTIONS[ride.destination]
        val dropoffLat = campusLocation?.latLng?.latitude ?: ride.destinationLat ?: 0.0
        val dropoffLng = campusLocation?.latLng?.longitude ?: ride.destinationLng ?: 0.0

        viewModelScope.launch {
            val result = repo.bookRide(
                riderId = userId,
                rideId = ride.id,
                pickupLocation = pickupName,
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                dropoffLocation = ride.destination,
                dropoffLat = dropoffLat,
                dropoffLng = dropoffLng,
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(bookingMessage = "Ride booked successfully!")
                // Reload to reflect updated seat count and remove this ride from list
                if (_uiState.value.selectedDate.isNotEmpty())
                    loadRidesByDate(_uiState.value.selectedDate)
                else
                    loadAllFutureRides()
            } else {
                _uiState.value = _uiState.value.copy(
                    bookingMessage = result.exceptionOrNull()?.message ?: "Booking failed"
                )
            }
        }
    }

    fun clearBookingMessage() {
        _uiState.value = _uiState.value.copy(bookingMessage = null)
    }

}

class FutureRideViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FutureRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FutureRideViewModel(BookRepository(client), userId) as T  // ← pass it
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}