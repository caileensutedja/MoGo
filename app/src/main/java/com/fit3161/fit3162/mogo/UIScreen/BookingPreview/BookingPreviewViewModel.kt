package com.fit3161.fit3162.mogo.UIScreen.BookScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Booking
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.model.LatLng
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the BookingPreview screen.
 *
 * - Loading: fetching booking + computing route
 * - Success: route is ready, three-leg trip can be rendered
 * - Error: something went wrong (booking missing, lat/lng missing, route failed)
 */
sealed class PreviewState {
    object Loading : PreviewState()
    data class Success(
        val booking: Booking,
        val driverOrigin: LatLng,
        val pickup: LatLng,
        val destination: LatLng,
        val route: RouteResult
    ) : PreviewState()
    data class Error(val message: String) : PreviewState()
}

/**
 * Loads a single booking by ID, then computes a three-leg route:
 *   driver origin -> rider pickup -> destination
 *
 * The route is fetched via Routes API (intermediates field). UI just
 * decodes the resulting polyline and drops three markers.
 */
class BookingPreviewViewModel(
    private val bookRepo: BookRepository,
    private val mapsRepo: MapsRepository,
    private val bookingId: String
) : ViewModel() {

    private val _state = MutableStateFlow<PreviewState>(PreviewState.Loading)
    val state: StateFlow<PreviewState> = _state.asStateFlow()

    init {
        loadPreview()
    }

    private fun loadPreview() {
        viewModelScope.launch {
            _state.value = PreviewState.Loading

            // 1. Fetch booking (joined with ride + driver + vehicle)
            val booking = bookRepo.getBookingById(bookingId)
            if (booking == null) {
                _state.value = PreviewState.Error("Booking not found")
                return@launch
            }

            val ride = booking.rides
            if (ride == null) {
                _state.value = PreviewState.Error("Ride details unavailable")
                return@launch
            }

            // 2. Pull all six coordinates (3 points x 2 fields). All required.
            val originLat = ride.originLat
            val originLng = ride.originLng
            val destLat = ride.destinationLat
            val destLng = ride.destinationLng
            val pickupLat = booking.pickupLat
            val pickupLng = booking.pickupLng

            if (originLat == null || originLng == null ||
                destLat == null || destLng == null ||
                pickupLat == null || pickupLng == null
            ) {
                _state.value = PreviewState.Error(
                    "Location data missing for this booking"
                )
                return@launch
            }

            val driverOrigin = LatLng(originLat, originLng)
            val pickup = LatLng(pickupLat, pickupLng)
            val destination = LatLng(destLat, destLng)

            // 3. Compute three-leg route
            mapsRepo.getRouteWithStop(driverOrigin, pickup, destination).fold(
                onSuccess = { route ->
                    _state.value = PreviewState.Success(
                        booking = booking,
                        driverOrigin = driverOrigin,
                        pickup = pickup,
                        destination = destination,
                        route = route
                    )
                },
                onFailure = {
                    _state.value = PreviewState.Error(
                        it.message ?: "Couldn't compute route"
                    )
                }
            )
        }
    }
}

class BookingPreviewViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository,
    private val bookingId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookingPreviewViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookingPreviewViewModel(
                BookRepository(client),
                mapsRepo,
                bookingId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
