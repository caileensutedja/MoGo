package com.fit3161.fit3162.mogo.UIScreen.UploadRide

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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class UploadRideForm(
    val origin: String = "",
    val destination: String = "",
    val availableSeats: String = "3",
    val departureDate: String = "",   // YYYY-MM-DD
    val departureTime: String = "",   // HH:MM
    val isRecurring: Boolean = false,
    val recurringWeeks: Int = 1,      // how many weeks to repeat
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
    private val userId: String
) : ViewModel() {

    private val _form = MutableStateFlow(UploadRideForm())
    val form: StateFlow<UploadRideForm> = _form.asStateFlow()

    private val _status = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val status: StateFlow<UploadStatus> = _status.asStateFlow()

    // Form update methods
    fun onOriginChange(value: String) { _form.value = _form.value.copy(origin = value) }
    fun onDestinationChange(value: String) { _form.value = _form.value.copy(destination = value) }
    fun onSeatsChange(value: String) {
        if (value.all { it.isDigit() }) _form.value = _form.value.copy(availableSeats = value)
    }
    fun onDateChange(value: String) { _form.value = _form.value.copy(departureDate = value) }
    fun onTimeChange(value: String) { _form.value = _form.value.copy(departureTime = value) }
    fun onRecurringChange(value: Boolean) { _form.value = _form.value.copy(isRecurring = value) }
    fun onVehicleTypeChange(value: String) { _form.value = _form.value.copy(vehicleType = value) }
    fun onPlateNumberChange(value: String) { _form.value = _form.value.copy(plateNumber = value) }
    fun onRecurringWeeksChange(value: Int) {
        _form.value = _form.value.copy(recurringWeeks = value.coerceIn(1, 12))
    }

    /**
     * Approximate distance (km) between two campus locations.
     * Uses hard‑coded values for known Monash routes, otherwise 10 km fallback.
     */
    private fun getApproximateDistanceKm(origin: String, destination: String): Double {
        fun normalize(s: String): String {
            return s.trim().lowercase()
                .replace("campus", "")
                .replace("monash", "")
                .replace("university", "")
                .replace("melbourne", "melb")
                .replace("cbd", "city")
                .trim()
        }

        val o = normalize(origin)
        val d = normalize(destination)

        val distances = mapOf(
            "clayton|caulfield" to 12.0,
            "clayton|parkville" to 22.0,
            "clayton|city" to 19.0,
            "caulfield|parkville" to 12.0,
            "caulfield|city" to 9.5,
            "parkville|city" to 2.5,
            "clayton|richmond" to 17.0,
            "caulfield|richmond" to 6.5,
            "clayton|m-city" to 3.0,
            "caulfield|melb cbd" to 9.0,
        )

        val key = listOf(o, d).sorted().joinToString("|")
        return distances[key] ?: 10.0
    }

    /**
     * Build a single ride instance (used for each recurring week).
     */
    private fun buildRideInstance(
        origin: String,
        destination: String,
        departureDate: String,
        departureTime: String,
        weekOffset: Int,
        availableSeats: Int,
        vehicleType: String,
        plateNumber: String,
        isRecurring: Boolean,
        recurringGroupId: String?,
        recurringWeekIndex: Int
    ): Ride {
        val campus = CAMPUS_OPTIONS[destination]
        val distanceKm = getApproximateDistanceKm(origin, destination)
        val factor = when (vehicleType.lowercase()) {
            "ev" -> 0.01
            "hybrid" -> 0.12
            else -> 0.21
        }
        val carbonEstimate = distanceKm * factor

        return Ride(
            id = UUID.randomUUID().toString(),
            driverId = userId,
            vehicleId = null,
            origin = origin,
            destination = destination,
            destinationLat = campus?.latLng?.latitude,
            destinationLng = campus?.latLng?.longitude,
            rideStatus = "scheduled",
            availableSeats = availableSeats,
            departureTime = buildDepartureTime(departureDate, departureTime, weekOffset),
            isRecurring = isRecurring,
            recurringGroupId = recurringGroupId,
            recurringWeekIndex = recurringWeekIndex,
            vehicleType = vehicleType,
            plateNumber = plateNumber,
            carbonEstimate = carbonEstimate
        )
    }

    /**
     * Build ISO 8601 departure time with UTC offset, adding weekOffset weeks.
     */
    private fun buildDepartureTime(date: String, time: String, weekOffset: Int): String {
        val base = LocalDateTime.parse("${date}T${time}", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
        val shifted = base.plusWeeks(weekOffset.toLong())
        return shifted.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    /**
     * Check that departure is at least 24 hours in the future (UTC).
     */
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

    /**
     * Submit the ride(s) – either a single ride or a recurring series.
     */
    fun submitRide() {
        val data = _form.value

        when {
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

            val groupId = if (data.isRecurring) UUID.randomUUID().toString() else null
            val weeksToCreate = if (data.isRecurring) data.recurringWeeks else 1

            val rides = (0 until weeksToCreate).map { weekOffset ->
                buildRideInstance(
                    origin = data.origin,
                    destination = data.destination,
                    departureDate = data.departureDate,
                    departureTime = data.departureTime,
                    weekOffset = weekOffset,
                    availableSeats = data.availableSeats.toInt(),
                    vehicleType = data.vehicleType,
                    plateNumber = data.plateNumber,
                    isRecurring = data.isRecurring,
                    recurringGroupId = groupId,
                    recurringWeekIndex = weekOffset + 1
                )
            }

            repo.uploadRides(rides)
                .onSuccess { _status.value = UploadStatus.Success }
                .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
        }
    }

    fun resetStatus() { _status.value = UploadStatus.Idle }
}

class UploadRideViewModelFactory(
    private val client: SupabaseClient,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UploadRideViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UploadRideViewModel(BookRepository(client), userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}