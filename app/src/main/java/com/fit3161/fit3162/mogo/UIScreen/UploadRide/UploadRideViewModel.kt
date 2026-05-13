package com.fit3161.fit3162.mogo.UIScreen.UploadRide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.PresetDestination
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.CAMPUS_OPTIONS
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Form state for the upload-ride screen.
 *
 * Coordinates are tracked separately from the display string because the
 * origin can come from either Places Autocomplete (resolved coordinates)
 * or "Use my current location" (device coordinates with a friendly label).
 */
data class UploadRideForm(
    val origin: String = "",
    val originLat: Double? = null,
    val originLng: Double? = null,
    val destination: PresetDestination? = null,
    val availableSeats: String = "3",
    val departureDate: String = "", // YYYY-MM-DD
    val departureTime: String = "", // HH:MM
    val isRecurring: Boolean = false,
    val recurringWeeks: Int = 1,
    val vehicleType: String = "",
    val plateNumber: String = ""
)

sealed class UploadStatus {
    object Idle : UploadStatus()
    object Loading : UploadStatus()
    object Success : UploadStatus()
    data class Error(val message: String) : UploadStatus()
}

class UploadRideViewModel(
    private val repo: BookRepository,
    private val mapsRepo: MapsRepository,
    val placesRepo: PlacesRepository,
    private val userId: String
) : ViewModel() {

    private val _form = MutableStateFlow(UploadRideForm())
    val form: StateFlow<UploadRideForm> = _form.asStateFlow()

    private val _status = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val status: StateFlow<UploadStatus> = _status.asStateFlow()

//    fun onOriginChange(value: String) { _form.value = _form.value.copy(origin = value) }
//    fun onDestinationChange(value: String) { _form.value = _form.value.copy(destination = value) }
    // Form Update Methods
    fun onOriginChange(value: String) {
        // When the user is typing freely, clear any previously-resolved coordinates
        // so we don't accidentally submit stale lat/lng with new typed text.
        _form.value = _form.value.copy(origin = value, originLat = null, originLng = null)
    }

    /** Called when the user picks an autocomplete suggestion. */
    fun onOriginPlacePicked(name: String, lat: Double, lng: Double) {
        _form.value = _form.value.copy(origin = name, originLat = lat, originLng = lng)
    }

    /**
     * Called when the user taps "Use my current location" in the autocomplete dropdown.
     * Fetches device location and stores both a friendly label and the coordinates.
     */
    fun useCurrentLocationForOrigin() {
        viewModelScope.launch {
            mapsRepo.getDeviceLocation().fold(
                onSuccess = { latLng ->
                    _form.value = _form.value.copy(
                        origin = "Current location",
                        originLat = latLng.latitude,
                        originLng = latLng.longitude
                    )
                },
                onFailure = {
                    _status.value = UploadStatus.Error(
                        "Couldn't get your location: ${it.message}"
                    )
                }
            )
        }
    }

    fun onDestinationChange(value: PresetDestination) {
        _form.value = _form.value.copy(destination = value)
    }

    fun onSeatsChange(value: String) {
        if (value.all { it.isDigit() }) _form.value = _form.value.copy(availableSeats = value)
    }
    fun onDateChange(value: String) { _form.value = _form.value.copy(departureDate = value) }
    fun onTimeChange(value: String) { _form.value = _form.value.copy(departureTime = value) }
    fun onRecurringChange(value: Boolean) { _form.value = _form.value.copy(isRecurring = value) }
    fun onVehicleTypeChange(value: String) { _form.value = _form.value.copy(vehicleType = value) }
    fun onPlateNumberChange(value: String) { _form.value = _form.value.copy(plateNumber = value) }

//    fun submitRide() {
//        val data = _form.value
//
//        // Validation logic
//        when {
//            data.origin.isBlank() -> _status.value = UploadStatus.Error("Origin cannot be empty")
//            data.destination.isBlank() -> _status.value = UploadStatus.Error("Destination cannot be empty")
//            data.departureDate.isBlank() -> _status.value = UploadStatus.Error("Please select a date")
//            data.departureTime.isBlank() -> _status.value = UploadStatus.Error("Please select a time")
//            !isDepartureValid(data.departureDate, data.departureTime) ->
//                _status.value = UploadStatus.Error("Departure must be at least 24 hours from now")
//            else -> {
//                viewModelScope.launch {
//                    _status.value = UploadStatus.Loading
//
//                    val newRide = Ride(
//                        id = UUID.randomUUID().toString(),
//                        driverId = userId,
//                        vehicleId = null, // or omit if you removed the column
//                        origin = data.origin,
//                        destination = data.destination,
//                        rideStatus = "scheduled",
//                        availableSeats = data.availableSeats.toInt(),
//                        departureTime = "${data.departureDate}T${data.departureTime}:00+00:00",
//                        isRecurring = false,
//                        vehicleType = data.vehicleType,     // NEW
//                        plateNumber = data.plateNumber,     // NEW
//                    )
//
//                    repo.uploadRide(newRide)
//                        .onSuccess { _status.value = UploadStatus.Success }
//                        .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
//                }
//            }
//        }
//        viewModelScope.launch {
//            _status.value = UploadStatus.Loading
//
//            val campus = CAMPUS_OPTIONS[data.destination]
//            val groupId = if (data.isRecurring) UUID.randomUUID().toString() else null
//            val weeksToCreate = if (data.isRecurring) data.recurringWeeks else 1
//
//            val rides = (0 until weeksToCreate).map { weekOffset ->
//                Ride(
//                    id = UUID.randomUUID().toString(),
//                    driverId = userId,
//                    vehicleId = null,
//                    origin = data.origin,
//                    destination = data.destination,
//                    destinationLat = campus?.latLng?.latitude,
//                    destinationLng = campus?.latLng?.longitude,
//                    rideStatus = "scheduled",
//                    availableSeats = data.availableSeats.toInt(),
//                    departureTime = buildDepartureTime(data.departureDate, data.departureTime, weekOffset),
//                    isRecurring = data.isRecurring,
//                    recurringGroupId = groupId,           // new field
//                    recurringWeekIndex = weekOffset + 1,  // new field
//                    vehicleType = data.vehicleType,
//                    plateNumber = data.plateNumber,
//                )
//            }
//
//            // Upload all at once
//            repo.uploadRides(rides)
//                .onSuccess { _status.value = UploadStatus.Success }
//                .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
//        }
//    }
    fun submitRide() {
        val data = _form.value
        val destination = data.destination

        // Validation logic
        when {
            data.origin.isBlank() ->
                _status.value = UploadStatus.Error("Please set a starting location")
            data.originLat == null || data.originLng == null ->
                _status.value = UploadStatus.Error(
                    "Please pick a starting location from the suggestions, or tap 'Use my current location'"
                )
            destination == null ->
                _status.value = UploadStatus.Error("Please select a destination")
            data.departureDate.isBlank() ->
                _status.value = UploadStatus.Error("Please select a date")
            data.departureTime.isBlank() ->
                _status.value = UploadStatus.Error("Please select a time")
            !isDepartureValid(data.departureDate, data.departureTime) ->
            data.origin.isBlank() -> {
                _status.value = UploadStatus.Error("Origin cannot be empty")
                return
            }
            data.destination.isBlank() -> {
                _status.value = UploadStatus.Error("Destination cannot be empty")
                return
            }
            data.departureDate.isBlank() -> {
                _status.value = UploadStatus.Error("Please select a date")
                return
            }
            data.departureTime.isBlank() -> {
                _status.value = UploadStatus.Error("Please select a time")
                return
            }
            data.isRecurring && data.recurringWeeks < 1 -> {
                _status.value = UploadStatus.Error("Please select at least 1 week for recurring rides")
                return
            }
            !isDepartureValid(data.departureDate, data.departureTime) -> {
                _status.value = UploadStatus.Error("Departure must be at least 24 hours from now")
                return
            }
        }

        viewModelScope.launch {
            _status.value = UploadStatus.Loading

            val campus = CAMPUS_OPTIONS[data.destination]
            val groupId = if (data.isRecurring) UUID.randomUUID().toString() else null
            val weeksToCreate = if (data.isRecurring) data.recurringWeeks else 1

            val rides = (0 until weeksToCreate).map { weekOffset ->
                Ride(
                    id = UUID.randomUUID().toString(),
                    driverId = userId,
                    vehicleId = null,
                    origin = data.origin,
                    destination = data.destination,
                    destinationLat = campus?.latLng?.latitude,
                    destinationLng = campus?.latLng?.longitude,
                    rideStatus = "scheduled",
                    availableSeats = data.availableSeats.toInt(),
                    departureTime = buildDepartureTime(data.departureDate, data.departureTime, weekOffset),
                    isRecurring = data.isRecurring,
                    recurringGroupId = groupId,
                    recurringWeekIndex = weekOffset + 1,
                    vehicleType = data.vehicleType,
                    plateNumber = data.plateNumber,
                )
            }
                    val newRide = Ride(
                        id = UUID.randomUUID().toString(),
                        driverId = userId,
                        vehicleId = null,
                        origin = data.origin,
                        destination = destination.name,
                        originLat = data.originLat,
                        originLng = data.originLng,
                        destinationLat = destination.latLng.latitude,
                        destinationLng = destination.latLng.longitude,
                        rideStatus = "scheduled",
                        availableSeats = data.availableSeats.toInt(),
                        departureTime = "${data.departureDate}T${data.departureTime}:00+00:00",
                        isRecurring = false,
                        vehicleType = data.vehicleType,
                        plateNumber = data.plateNumber,
                    )

            repo.uploadRides(rides)
                .onSuccess { _status.value = UploadStatus.Success }
                .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
        }
    }

    fun onRecurringWeeksChange(value: Int) {
        _form.value = _form.value.copy(recurringWeeks = value.coerceIn(1, 12))
    }

    private fun buildDepartureTime(date: String, time: String, weekOffset: Int): String {
        val base = LocalDateTime.parse("${date}T${time}", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        val shifted = base.plusWeeks(weekOffset.toLong())
        return shifted.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    repo.uploadRide(newRide)
                        .onSuccess { _status.value = UploadStatus.Success }
                        .onFailure {
                            _status.value = UploadStatus.Error(
                                it.message ?: "Failed to post ride"
                            )
                        }
                }
            }
        }
    }

    private fun isDepartureValid(date: String, time: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
            val departure = LocalDateTime.parse("${date}T${time}", formatter)
            val earliest = LocalDateTime.now(ZoneOffset.UTC).plusHours(24)
            !departure.isBefore(earliest)
        } catch (e: Exception) {
            false
        }
    }

    fun resetStatus() { _status.value = UploadStatus.Idle }
}


class UploadRideViewModelFactory(
    private val client: SupabaseClient,
    private val mapsRepo: MapsRepository,
    private val placesRepo: PlacesRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UploadRideViewModel(
                BookRepository(client),
                mapsRepo,
                placesRepo,
                userId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}