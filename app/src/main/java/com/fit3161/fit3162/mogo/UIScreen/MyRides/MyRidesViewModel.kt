package com.fit3161.fit3162.mogo.UIScreen.MyRides

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class MyRidesUiState(
    val rides: List<Ride> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    // Group: null groupId = standalone, non-null = recurring series
    val groupedRides: Map<String?, List<Ride>>
        get() = rides.groupBy { it.recurringGroupId }
}

class MyRidesViewModel(
    private val repo: BookRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyRidesUiState())
    val uiState: StateFlow<MyRidesUiState> = _uiState.asStateFlow()

    init {
        loadMyRides()
    }

    private fun loadMyRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val rides = repo.getMyRides(userId)
                // Sort: scheduled first, then completed
                val sortedRides = rides.sortedBy { ride ->
                    when (ride.rideStatus) {
                        "scheduled" -> 0
                        "completed" -> 1
                        else -> 2
                    }
                }
                _uiState.value = _uiState.value.copy(rides = sortedRides, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    /**
     * Checks if the driver can cancel the ride based on departure time.
     * Rule: Cancellation allowed only if departure is at least 60 minutes from now.
     */
    private fun isWithinCancellationWindow(departureTime: String): Boolean {
        return try {
            val now = OffsetDateTime.now(ZoneOffset.UTC)
            val departure = OffsetDateTime.parse(departureTime)
            val minutesUntilDeparture = Duration.between(now, departure).toMinutes()
            minutesUntilDeparture >= 60
        } catch (e: Exception) {
            false
        }
    }

    fun cancelRide(rideId: String) {
        // 1. Find the ride from current state to check departure time
        val ride = _uiState.value.rides.find { it.id == rideId }
        if (ride == null) {
            _uiState.value = _uiState.value.copy(error = "Ride not found")
            return
        }

        // 2. Enforce cancellation window
        if (!isWithinCancellationWindow(ride.departureTime)) {
            _uiState.value = _uiState.value.copy(
                error = "Cannot cancel less than 60 minutes before departure"
            )
            return
        }

        // 3. Optimistically remove from UI
        _uiState.value = _uiState.value.copy(
            rides = _uiState.value.rides.filter { it.id != rideId }
        )

        // 4. Call repository to mark ride as cancelled (not deleted)
        viewModelScope.launch {
            val result = repo.cancelRide(rideId)   // This should set ride_status = "cancelled"
            if (result.isFailure) {
                // Rollback: reload rides to restore the cancelled ride
                loadMyRides()
                _uiState.value = _uiState.value.copy(error = "Failed to cancel ride")
            }
        }
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