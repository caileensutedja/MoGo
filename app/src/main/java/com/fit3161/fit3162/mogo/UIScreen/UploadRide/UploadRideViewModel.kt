package com.fit3161.fit3162.mogo.UIScreen.UploadRide

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
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

data class UploadRideForm(
    val origin: String = "",
    val destination: String = "",
    val availableSeats: String = "3",
    val departureDate: String = "",
    val departureTime: String = "",
    val isRecurring: Boolean = false,
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

    fun submitRide() {
        val data = _form.value

        when {
            data.origin.isBlank() -> _status.value = UploadStatus.Error("Origin cannot be empty")
            data.destination.isBlank() -> _status.value = UploadStatus.Error("Destination cannot be empty")
            data.departureDate.isBlank() -> _status.value = UploadStatus.Error("Please select a date")
            data.departureTime.isBlank() -> _status.value = UploadStatus.Error("Please select a time")
            !isDepartureValid(data.departureDate, data.departureTime) ->
                _status.value = UploadStatus.Error("Departure must be at least 24 hours from now")
            else -> {
                viewModelScope.launch {
                    _status.value = UploadStatus.Loading

                    // For now, carbonEstimate is null. You can implement later with MapsRepository.
                    val distanceKm = getApproximateDistanceKm(data.origin, data.destination)
                    val factor = when (data.vehicleType.lowercase()) {
                        "ev" -> 0.01
                        "hybrid" -> 0.12
                        else -> 0.21
                    }
                    val carbonEstimate = distanceKm * factor

                    val newRide = Ride(
                        id = UUID.randomUUID().toString(),
                        driverId = userId,
                        vehicleId = null,
                        origin = data.origin,
                        destination = data.destination,
                        rideStatus = "scheduled",
                        availableSeats = data.availableSeats.toInt(),
                        departureTime = "${data.departureDate}T${data.departureTime}:00+00:00",
                        isRecurring = false,
                        vehicleType = data.vehicleType,
                        plateNumber = data.plateNumber,
                        carbonEstimate = carbonEstimate
                    )

                    repo.uploadRide(newRide)
                        .onSuccess { _status.value = UploadStatus.Success }
                        .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
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
    private fun getApproximateDistanceKm(origin: String, destination: String): Double {
        // Normalize strings: trim, lower case, remove common suffixes
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

        // Hardcoded distances (km) between known locations
        // Key format: "origin|destination" (order doesn't matter)
        val distances = mapOf(
            // Clayton ↔ Caulfield
            "clayton|caulfield" to 12.0,
            // Clayton ↔ Parkville
            "clayton|parkville" to 22.0,
            // Clayton ↔ City (Melbourne CBD)
            "clayton|city" to 19.0,
            // Caulfield ↔ Parkville
            "caulfield|parkville" to 12.0,
            // Caulfield ↔ City
            "caulfield|city" to 9.5,
            // Parkville ↔ City
            "parkville|city" to 2.5,
            // Clayton ↔ Richmond
            "clayton|richmond" to 17.0,
            // Caulfield ↔ Richmond
            "caulfield|richmond" to 6.5,
            // Clayton ↔ M-City Shopping Centre
            "clayton|m-city" to 3.0,
            // Caulfield ↔ Melbourne CBD Central Apartment Hotel
            "caulfield|melb cbd" to 9.0,
        )

        // Try both directions
        val key = listOf(o, d).sorted().joinToString("|")
        return distances[key] ?: 10.0 // default 10km if route not found
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