package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.RideUser
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneOffset

data class OngoingRideDetails(
    val driverName: String? = null,
    val origin: String,
    val destination: String,
    val departureTime: String,
    val estimatedDistanceKm: Double? = null,
    val estimatedDurationMinutes: Int? = null
)

data class BookUIState(
    val bookings: List<Booking> = emptyList(),
    val rides: List<MapsRepository.RideWithDetour> = emptyList(),
    val ongoingRide: OngoingRideDetails? = null,   // <-- ADD THIS
    val isLoading: Boolean = false,
    val error: String? = null,
    val rebookMessage: String? = null
)

class BookViewModel(
    private val repo: BookRepository,
    private val mapsRepo: MapsRepository,
    private val userId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookUIState())
    val uiState: StateFlow<BookUIState> = _uiState.asStateFlow()

    init {
        loadBookedRides()
    }
    fun onRebookNextWeek(ride: Ride, oldBooking: Booking) {
        viewModelScope.launch {
            // Calculate new departure time for next week
            val newDepartureTime = try {
                val oldTime = java.time.OffsetDateTime.parse(ride.departureTime)
                oldTime.plusWeeks(1).format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(rebookMessage = "Invalid departure time")
                return@launch
            }

            // Create a new ride instance for next week
            val newRide = ride.copy(
                id = java.util.UUID.randomUUID().toString(),
                departureTime = newDepartureTime,
                isRecurring = true,
                recurringGroupId = ride.recurringGroupId ?: ride.id
            )

            repo.uploadRide(newRide).onSuccess {
                _uiState.value = _uiState.value.copy(rebookMessage = "✅ Rebooked for next week!")
                loadBookedRides() // refresh list
            }.onFailure {
                _uiState.value = _uiState.value.copy(rebookMessage = "❌ Failed to rebook: ${it.message}")
            }
        }
    }

    fun cancelBooking(bookingId: String, rideId: String) {
        viewModelScope.launch {
            val result = repo.cancelBooking(bookingId, userId, "rider")
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(rebookMessage = "✅ Booking cancelled")
                loadBookedRides()
            } else {
                _uiState.value = _uiState.value.copy(rebookMessage = "❌ Failed to cancel: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private fun loadBookedRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bookings = repo.getBookedRides(userId)
                // Compute ongoing ride (first confirmed booking with future departure)
                val ongoingRide = bookings.firstOrNull { booking ->
                    val departure = try {
                        OffsetDateTime.parse(booking.rides?.departureTime)
                    } catch (e: Exception) { null }
                    departure != null && departure.isAfter(OffsetDateTime.now(ZoneOffset.UTC))
                }?.let { booking ->
                    val ride = booking.rides
                    OngoingRideDetails(
                        driverName = ride?.users?.userName,
                        origin = ride?.origin ?: "",
                        destination = ride?.destination ?: "",
                        departureTime = ride?.departureTime ?: "",
                        estimatedDistanceKm = ride?.carbonEstimate?.let { carbon -> carbon / 0.21 },
                        estimatedDurationMinutes = ride?.carbonEstimate?.let { carbon -> ((carbon / 0.21) / 40 * 60).toInt() }
                    )
                }
                _uiState.value = _uiState.value.copy(
                    bookings = bookings,
                    ongoingRide = ongoingRide,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Add your other existing functions: loadRides(), checkDetours(), onRebookNextWeek(), cancelBooking(), etc.
    // (Keep them unchanged from your previous version - they are not shown here but must be present.)
}

class BookViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(BookRepository(client), mapsRepo, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}