package com.fit3161.fit3162.mogo.UIScreen.UploadRide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.repo.BookRepository
import com.fit3161.fit3162.mogo.data.repo.Ride
import com.fit3161.fit3162.mogo.data.repo.Vehicle
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UploadRideForm(
    val origin: String = "",
    val destination: String = "",
    val availableSeats: String = "3",
    val departureDate: String = "", // YYYY-MM-DD
    val departureTime: String = "", // HH:MM
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

//    fun loadVehicles() {
//        viewModelScope.launch {
//            val vehicles = repo.getUserVehicles(userId)
//            _form.value = _form.value.copy(availableVehicles = vehicles)
//            // Auto-select the first one if available
//            if (vehicles.isNotEmpty()) {
//                _form.value = _form.value.copy(selectedVehicle = vehicles.first())
//            }
//        }
//    }

    // Form Update Methods
//    fun onVehicleSelected(vehicle: Vehicle) {
//        _form.value = _form.value.copy(selectedVehicle = vehicle)
//    }
    fun onOriginChange(value: String) { _form.value = _form.value.copy(origin = value) }
    fun onDestinationChange(value: String) { _form.value = _form.value.copy(destination = value) }
    fun onSeatsChange(value: String) {
        if (value.all { it.isDigit() }) _form.value = _form.value.copy(availableSeats = value)
    }
    fun onDateChange(value: String) { _form.value = _form.value.copy(departureDate = value) }
    fun onTimeChange(value: String) { _form.value = _form.value.copy(departureTime = value) }
    fun onRecurringChange(value: Boolean) {_form.value = _form.value.copy(isRecurring = value) }
    fun onVehicleTypeChange(value: String) {_form.value = _form.value.copy(vehicleType = value) }
    fun onPlateNumberChange(value: String) {_form.value = _form.value.copy(plateNumber = value) }

    fun submitRide() {
        val data = _form.value

        // Validation logic
        when {
            data.origin.isBlank() -> _status.value = UploadStatus.Error("Origin cannot be empty")
            data.destination.isBlank() -> _status.value = UploadStatus.Error("Destination cannot be empty")
            data.departureDate.isBlank() -> _status.value = UploadStatus.Error("Please select a date")
            data.departureTime.isBlank() -> _status.value = UploadStatus.Error("Please select a time")
            else -> {
                viewModelScope.launch {
                    _status.value = UploadStatus.Loading

//                    val newRide = Ride(
//                        id = UUID.randomUUID().toString(),
//                        driverId = userId,
//                        vehicleId = "v1", // Note: Usually fetched from driver profile
//                        origin = data.origin,
//                        destination = data.destination,
//                        rideStatus = "scheduled",
//                        availableSeats = data.availableSeats.toIntOrNull() ?: 1,
//                        departureTime = "${data.departureDate}T${data.departureTime}:00Z",
//                        isRecurring = data.isRecurring
//                    )

                    val newRide = Ride(
                        id = UUID.randomUUID().toString(),
                        driverId = userId,
                        vehicleId = null, // or omit if you removed the column
                        origin = data.origin,
                        destination = data.destination,
                        rideStatus = "scheduled",
                        availableSeats = data.availableSeats.toInt(),
                        departureTime = "${data.departureDate}T${data.departureTime}:00+00:00",
                        isRecurring = false,
                        vehicleType = data.vehicleType,     // NEW
                        plateNumber = data.plateNumber,     // NEW
                    )

                    repo.uploadRide(newRide)
                        .onSuccess { _status.value = UploadStatus.Success }
                        .onFailure { _status.value = UploadStatus.Error(it.message ?: "Failed to post ride") }
                }
            }
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