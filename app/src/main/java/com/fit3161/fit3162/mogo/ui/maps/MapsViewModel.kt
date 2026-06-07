package com.fit3161.fit3162.mogo.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.com.fit3161.fit3162.mogo.data.model.DriverLocation
import com.fit3161.fit3162.mogo.data.model.PresetDestination
import com.fit3161.fit3162.mogo.data.model.PresetDestinations
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


// REMOVED: FusedLocationProviderClient import — location is now handled by MapsRepository

class MapsViewModel(private val mapRepository: MapsRepository) : ViewModel() {

    // Pulled from the shared PresetDestinations object — same list the upload form uses.
    val presetDestinations: List<PresetDestination> = PresetDestinations.all

    // List of drivers nearby using dummy data.
    private val dummyDrivers = listOf(
        DriverLocation("id01", "Jack", LatLng(-37.91241, 145.13598))
    )

    // user location state (was not tracked before)
    private val _userLocation = MutableStateFlow<LocationState>(LocationState.Unknown)
    val userLocation: StateFlow<LocationState> = _userLocation.asStateFlow()

    // selected destination state for chip highlighting + route info card
    private val _selectedDestination = MutableStateFlow<PresetDestination?>(null)
    val selectedDestination: StateFlow<PresetDestination?> = _selectedDestination.asStateFlow()

    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()

    // Search radius around user.
    private val _searchRadiusMeters = MutableStateFlow(3000.0)
    val searchRadiusMeters: StateFlow<Double> = _searchRadiusMeters.asStateFlow()

    private val _nearbyDrivers = MutableStateFlow<List<DriverLocation>>(emptyList())
    val nearbyDrivers: StateFlow<List<DriverLocation>> = _nearbyDrivers.asStateFlow()


    // Replaces old getDeviceLocation() that took FusedLocationProviderClient as parameter
    // Now delegates to repository (proper MVVM — ViewModel doesn't touch Android framework classes)
    fun loadDeviceLocation() {
        viewModelScope.launch {
            _userLocation.value = LocationState.Loading
            _userLocation.value = mapRepository.getDeviceLocation().fold(
                onSuccess = { LocationState.Located(it) },
                onFailure = { LocationState.Error(it.message ?: "Failed to get location") }
            )
            // Get drivers nearby
            refreshNearbyDrivers()
        }
    }

    fun refreshNearbyDrivers() {
        val center = (_userLocation.value as? LocationState.Located)?.latLng ?: run {
            _nearbyDrivers.value = emptyList()
            return
        }
        val radius = _searchRadiusMeters.value

        _nearbyDrivers.value = dummyDrivers.filter { driver ->
            SphericalUtil.computeDistanceBetween(center, driver.latLng) <= radius
        }
    }

    fun setSearchRadius(meters: Double) {
        _searchRadiusMeters.value = meters
        refreshNearbyDrivers()
    }

    fun selectDestination(destination: PresetDestination) {
        _selectedDestination.value = destination
        fetchRoute(destination.latLng)
    }

    fun clearRoute() {
        _selectedDestination.value = null
        _routeState.value = RouteState.Idle
    }

    fun fetchRoute(destination: LatLng, origin: LatLng? = null) {
        viewModelScope.launch {
            _routeState.value = RouteState.Loading

            // Get origin cases.
            // 1. Use provided origin, OR
            // 2. Use cached user location, OR
            // 3. Fetch fresh location from repository
            val resolvedOrigin = origin
                ?: (_userLocation.value as? LocationState.Located)?.latLng
                ?: run {
                    val locResult = mapRepository.getDeviceLocation()
                    locResult.getOrElse {
                        _routeState.value = RouteState.Error(
                            "Cannot fetch route: location unavailable. ${it.message}"
                        )
                        return@launch
                    }
                }

            _routeState.value = mapRepository.getRoute(resolvedOrigin, destination).fold(
                onSuccess = { RouteState.Success(it) },
                onFailure = { RouteState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}


sealed class LocationState {
    object Unknown : LocationState()
    object Loading : LocationState()
    data class Located(val latLng: LatLng) : LocationState()
    data class Error(val message: String) : LocationState()
}


sealed class RouteState {
    object Idle : RouteState()
    object Loading : RouteState()
    data class Success(val route: RouteResult) : RouteState()
    data class Error(val message: String) : RouteState()
}
