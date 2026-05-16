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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// (Optional – uncomment if you later add OkHttp and OpenRouteService)
// import okhttp3.OkHttpClient
// import okhttp3.Request
// import org.json.JSONObject
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.withContext

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
     * Haversine formula – straight‑line distance between two lat/lng points (km)
     */
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /**
     * Approximate distance (km) between two campus locations using a hard‑coded map.
     * Fallback to 10 km if route not found.
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
     * OpenRouteService API call (currently commented – uncomment when you have origin coordinates)
     * Requires adding OkHttp and originLat/originLng to UploadRideForm.
     */
    /*
    private suspend fun getRoadDistanceFromORS(
        originLat: Double, originLng: Double,
        destLat: Double, destLng: Double
    ): Double? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val url = "https://api.openrouteservice.org/v2/directions/driving-car?start=$originLng,$originLat&end=$destLng,$destLat"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: return@withContext null)
            val distanceMeters = json.getJSONArray("features")
                .getJSONObject(0)
                .getJSONObject("properties")
                .getJSONArray("segments")
                .getJSONObject(0)
                .getDouble("distance")
            distanceMeters / 1000.0
        } catch (e: Exception) {
            Log.e("ORS", "Failed to get road distance", e)
            null
        }
    }
    */

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

        // Get distance (hard‑coded map or Haversine fallback)
        var distanceKm = getApproximateDistanceKm(origin, destination)
        if (distanceKm == 10.0 && campus != null) {
            // If default fallback was used and we have destination coordinates,
            // we could compute straight‑line distance (optional – not needed now)
        }

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