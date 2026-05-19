package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.PresetDestinations
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

/**
 * UI state for the Book screen (rider view).
 *
 * @param showCancelSuccess Whether to show the cancel success dialog.
 */
data class BookUIState(
    val bookings: List<Booking> = emptyList(),
    val rides: List<MapsRepository.RideWithDetour> = emptyList(),
    val ongoingRide: OngoingRideDetails? = null,
    val rebookMessage: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCancelSuccess: Boolean = false
)

/**
 * ViewModel for the rider's Booked Rides screen.
 * Handles loading bookings, cancelling with reasons, and rebooking recurring rides.
 */
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

    private fun loadBookedRides() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val bookings = repo.getBookedRides(userId)
                    .sortedBy { it.rides?.departureTime }

                // Find the first upcoming booking to display as "ongoing ride"
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
                        estimatedDistanceKm = ride?.carbonEstimate?.let { it / 0.21 },
                        estimatedDurationMinutes = ride?.carbonEstimate?.let { ((it / 0.21) / 40 * 60).toInt() }
                    )
                }

                _uiState.value = _uiState.value.copy(
                    bookings = bookings,
                    ongoingRide = ongoingRide,
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

    fun loadRides(
        rider: RideUser,
        pickupLat: Double,
        pickupLng: Double,
        date: String?
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val genderPref = repo.getGenderPreference(userId)
                val alreadyBooked = repo.getBookedRideIds(userId)
                val blockedByRider = emptySet<String>()
                val blockedByDriver = emptySet<String>()

                val candidates = if (date != null)
                    repo.getFutureRidesByDate(userId, date, genderPref ?: "")
                else
                    repo.getAllFutureRides(userId, genderPref ?: "")

                val hardFiltered = candidates.filter { ride ->
                    repo.passesHardMemoryFilters(
                        ride, userId, rider,
                        alreadyBooked, blockedByRider, blockedByDriver
                    )
                }

                val softFiltered = hardFiltered.filter { ride ->
                    repo.passesSoftFilters(ride, userId, rider)
                }

                val inRadius = softFiltered.filter { ride ->
                    val oLat = ride.originLat ?: return@filter false
                    val oLng = ride.originLng ?: return@filter false
                    repo.isWithinRadiusKm(pickupLat, pickupLng, oLat, oLng, 5.0)
                }

                var withDetour = checkDetours(
                    inRadius, pickupLat, pickupLng, maxDetourKm = 5.0
                )
                if (withDetour.isEmpty()) {
                    withDetour = checkDetours(
                        inRadius, pickupLat, pickupLng, maxDetourKm = 10.0
                    )
                }

                val sorted = repo.sortRides(withDetour)
                _uiState.value = _uiState.value.copy(
                    rides = sorted,
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

    private suspend fun checkDetours(
        rides: List<Ride>,
        pickupLat: Double,
        pickupLng: Double,
        maxDetourKm: Double
    ): List<MapsRepository.RideWithDetour> {
        return rides.mapNotNull { ride ->
            val detour = mapsRepo.computeDetour(ride, pickupLat, pickupLng)
                ?: return@mapNotNull null
            if (detour.addedKm <= maxDetourKm && detour.addedMinutes <= 10) {
                MapsRepository.RideWithDetour(ride, detour.addedKm, detour.addedMinutes)
            } else null
        }
    }

    fun onRebookNextWeek(currentRide: Ride, booking: Booking) {
        val groupId = currentRide.recurringGroupId ?: return
        viewModelScope.launch {
            val nextRide = repo.getNextRecurringRide(
                recurringGroupId = groupId,
                afterDepartureTime = currentRide.departureTime
            )
            if (nextRide == null) {
                _uiState.value = _uiState.value.copy(
                    rebookMessage = "No recurring ride available next week"
                )
                return@launch
            }

            val campusLocation = PresetDestinations.byName(booking.dropoffLocation)

            val result = repo.bookRide(
                riderId = userId,
                rideId = nextRide.id,
                pickupLocation = booking.pickupLocation,
                pickupLat = booking.pickupLat ?: 0.0,
                pickupLng = booking.pickupLng ?: 0.0,
                dropoffLocation = booking.dropoffLocation,
                dropoffLat = campusLocation?.latLng?.latitude
                    ?: booking.dropoffLat ?: 0.0,
                dropoffLng = campusLocation?.latLng?.longitude
                    ?: booking.dropoffLng ?: 0.0
            )

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    rebookMessage = "Rebooked for next week!"
                )
                loadBookedRides()
            } else {
                val msg = result.exceptionOrNull()?.message ?: ""
                _uiState.value = _uiState.value.copy(
                    rebookMessage = when {
                        msg.contains("Already booked", true) ||
                                msg.contains("duplicate key", true) ||
                                msg.contains("unique_booking", true) ->
                            "You have already rebooked this ride"
                        msg.contains("No seats available", true) ->
                            "This ride is now full"
                        else ->
                            "Ride unavailable for next week"
                    }
                )
            }
        }
    }

    /**
     * Cancels a booking with a reason.
     * Restores the seat on the ride via the repository.
     * Shows a success dialog on completion.
     */
    fun cancelBooking(bookingId: String, rideId: String, reason: String) {
        _uiState.value = _uiState.value.copy(
            bookings = _uiState.value.bookings.filter { it.id != bookingId }
        )
        viewModelScope.launch {
            val result = repo.cancelBooking(bookingId, rideId, reason)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    showCancelSuccess = true
                )
            } else {
                loadBookedRides()
                _uiState.value = _uiState.value.copy(
                    error = "Failed to cancel booking"
                )
            }
        }
    }

    // Dismiss the cancel success dialog.
    fun dismissCancelSuccess() {
        _uiState.value = _uiState.value.copy(showCancelSuccess = false)
    }

    fun clearRebookMessage() {
        _uiState.value = _uiState.value.copy(rebookMessage = null)
    }

    fun refresh() = loadBookedRides()
}

// Back to 3-arg factory (no placesRepo needed)
class BookViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(
                BookRepository(client), mapsRepo, userId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}