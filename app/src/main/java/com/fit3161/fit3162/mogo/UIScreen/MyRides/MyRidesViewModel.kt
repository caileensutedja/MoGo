package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.RideBookingInfo
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the My Rides screen (driver view).
 *
 * @param rides             All rides posted by the driver.
 * @param rideBookings      Map of ride ID -> list of bookings for that ride.
 * @param showCancelSuccess Whether to show the cancel success dialog.
 */
data class MyRidesUiState(
    val rides: List<Ride> = emptyList(),
    val rideBookings: Map<String, List<RideBookingInfo>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCancelSuccess: Boolean = false
)

/**
 * ViewModel for the driver's "My Rides" screen.
 * Handles loading rides, starting/ending rides, and cancelling with reasons.
 */
class MyRidesViewModel(
    private val repo: BookRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRidesUiState())
    val uiState: StateFlow<MyRidesUiState> = _uiState.asStateFlow()

    init {
        loadMyRides()
    }

    // Loads all rides for the driver and fetches booking info for each.
    private fun loadMyRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val rides = repo.getMyRides(userId)

                // Fetch bookings for each ride so the driver can see who booked
                val bookingsMap = rides.associate { ride ->
                    ride.id to repo.getBookingsForRide(ride.id)
                }

                _uiState.value = _uiState.value.copy(
                    rides = rides,
                    rideBookings = bookingsMap,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // Driver taps "Start Ride" -> ride_status becomes "in_progress"
    fun startRide(rideId: String) {
        viewModelScope.launch {
            repo.startRide(rideId).fold(
                onSuccess = { loadMyRides() },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to start ride: ${it.message}"
                    )
                }
            )
        }
    }

    // Driver taps "End Ride" -> ride_status becomes "completed"
    fun endRide(rideId: String) {
        viewModelScope.launch {
            repo.completeRide(rideId).fold(
                onSuccess = { loadMyRides() },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to end ride: ${it.message}"
                    )
                }
            )
        }
    }

    /**
     * Soft-deletes the ride with a cancellation reason.
     * Also cancels all associated bookings.
     * Shows a success dialog on completion.
     */
    fun cancelRide(rideId: String, reason: String) {
        // Optimistically remove from the UI
        _uiState.value = _uiState.value.copy(
            rides = _uiState.value.rides.filter { it.id != rideId }
        )
        viewModelScope.launch {
            val result = repo.cancelRide(rideId, reason)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    showCancelSuccess = true
                )
            } else {
                // Revert on failure
                loadMyRides()
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cancel ride"
                )
            }
        }
    }

    // Dismiss the cancel success dialog.
    fun dismissCancelSuccess() {
        _uiState.value = _uiState.value.copy(showCancelSuccess = false)
    }
}

class MyRidesViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyRidesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MyRidesViewModel(BookRepository(client), userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}