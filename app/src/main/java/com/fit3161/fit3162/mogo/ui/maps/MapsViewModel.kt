package com.fit3161.fit3162.mogo.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.PresetDestination // ADDED: new import for preset destinations
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// REMOVED: FusedLocationProviderClient import — location is now handled by MapsRepository

class MapsViewModel(private val mapRepository: MapsRepository) : ViewModel() {

    // ADDED: preset destinations list — modify coordinates to match your app's locations
    val presetDestinations = listOf(
        PresetDestination(
            name = "Monash Clayton",
            latLng = LatLng(-37.91103371251901, 145.13714676692243),
            description = "Monash University Clayton Campus"
        ),
        PresetDestination(
            name = "Monash Caulfield",
            latLng = LatLng(-37.87694590809227, 145.0457298608304),
            description = "Monash University Caulfield Campus"
        ),
        PresetDestination(
            name = "Monash Peninsula",
            latLng = LatLng(-38.152447616283546, 145.1365170687726),
            description = "Monash University Peninsula Campus"
        ),
//        PresetDestination(
//            name = "Monash Parkville",
//            latLng = LatLng(-37.7838187556312, 144.95936807247296),
//            description = "Monash University Parkville Campus"
//        )
    )

    // ADDED: user location state (was not tracked before)
    private val _userLocation = MutableStateFlow<LocationState>(LocationState.Unknown)
    val userLocation: StateFlow<LocationState> = _userLocation.asStateFlow()

    // ADDED: selected destination state for chip highlighting + route info card
    private val _selectedDestination = MutableStateFlow<PresetDestination?>(null)
    val selectedDestination: StateFlow<PresetDestination?> = _selectedDestination.asStateFlow()

    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()

    // ADDED: replaces old getDeviceLocation() that took FusedLocationProviderClient as parameter
    // Now delegates to repository (proper MVVM — ViewModel doesn't touch Android framework classes)
    fun loadDeviceLocation() {
        viewModelScope.launch {
            _userLocation.value = LocationState.Loading
            _userLocation.value = mapRepository.getDeviceLocation().fold(
                onSuccess = { LocationState.Located(it) },
                onFailure = { LocationState.Error(it.message ?: "Failed to get location") }
            )
        }
    }

    // REMOVED: old getDeviceLocation(fusedLocationClient: FusedLocationProviderClient)
    // that used callback-based lastLocation

    // ADDED: called when user taps a destination chip
    // Sets selection state AND triggers route fetch in one action
    fun selectDestination(destination: PresetDestination) {
        _selectedDestination.value = destination
        fetchRoute(destination.latLng)
    }

    // CHANGED: now also clears selectedDestination (was only clearing routeState)
    fun clearRoute() {
        _selectedDestination.value = null
        _routeState.value = RouteState.Idle
    }

    // CHANGED: destination is now first parameter (required), origin is optional
    // (was: both required). If origin is null, automatically uses device location.
    fun fetchRoute(destination: LatLng, origin: LatLng? = null) {
        viewModelScope.launch {
            _routeState.value = RouteState.Loading

            // ADDED: automatic origin resolution chain
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

// ADDED: LocationState sealed class (user location was not tracked before)
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

//package com.fit3161.fit3162.mogo.ui.maps
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.fit3161.fit3162.mogo.data.model.PresetDestination
//import com.fit3161.fit3162.mogo.data.model.RouteResult
//import com.fit3161.fit3162.mogo.data.repo.MapsRepository
//import com.google.android.gms.maps.model.LatLng
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class MapsViewModel(private val mapRepository: MapsRepository) : ViewModel() {
//
//    // ── Preset destinations ──────────────────────────────────────────
//    // Modify these to match your app's actual destinations.
//    val presetDestinations = listOf(
//        PresetDestination(
//            name = "Monash Clayton",
//            latLng = LatLng(-37.9105, 145.1363),
//            description = "Monash University Clayton Campus"
//        ),
//        PresetDestination(
//            name = "Flinders St",
//            latLng = LatLng(-37.8183, 144.9671),
//            description = "Flinders Street Station"
//        ),
//        PresetDestination(
//            name = "Melbourne Airport",
//            latLng = LatLng(-37.6690, 144.8410),
//            description = "Tullamarine Airport (MEL)"
//        )
//    )
//
//    // ── User location state ──────────────────────────────────────────
//    private val _userLocation = MutableStateFlow<LocationState>(LocationState.Unknown)
//    val userLocation: StateFlow<LocationState> = _userLocation.asStateFlow()
//
//    // ── Selected destination ─────────────────────────────────────────
//    private val _selectedDestination = MutableStateFlow<PresetDestination?>(null)
//    val selectedDestination: StateFlow<PresetDestination?> = _selectedDestination.asStateFlow()
//
//    // ── Route state ──────────────────────────────────────────────────
//    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
//    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()
//
//    /**
//     * Fetches device location from the repository.
//     * Call this AFTER permission is granted from the UI layer.
//     */
//    fun loadDeviceLocation() {
//        viewModelScope.launch {
//            _userLocation.value = LocationState.Loading
//            _userLocation.value = mapRepository.getDeviceLocation().fold(
//                onSuccess = { LocationState.Located(it) },
//                onFailure = { LocationState.Error(it.message ?: "Failed to get location") }
//            )
//        }
//    }
//
//    /**
//     * Called when the user taps a preset destination chip.
//     * Sets the selected destination and immediately fetches the route
//     * from the user's current location.
//     */
//    fun selectDestination(destination: PresetDestination) {
//        _selectedDestination.value = destination
//        fetchRoute(destination.latLng)
//    }
//
//    /**
//     * Clears the current route and selected destination,
//     * returning the map to its idle state.
//     */
//    fun clearRoute() {
//        _selectedDestination.value = null
//        _routeState.value = RouteState.Idle
//    }
//
//    /**
//     * Fetches a route from origin to destination.
//     * If no origin is provided, uses the current device location.
//     */
//    fun fetchRoute(destination: LatLng, origin: LatLng? = null) {
//        viewModelScope.launch {
//            _routeState.value = RouteState.Loading
//
//            // Resolve origin: use provided value, current location, or fetch fresh
//            val resolvedOrigin = origin
//                ?: (_userLocation.value as? LocationState.Located)?.latLng
//                ?: run {
//                    val locResult = mapRepository.getDeviceLocation()
//                    locResult.getOrElse {
//                        _routeState.value = RouteState.Error(
//                            "Cannot fetch route: location unavailable. ${it.message}"
//                        )
//                        return@launch
//                    }
//                }
//
//            _routeState.value = mapRepository.getRoute(resolvedOrigin, destination).fold(
//                onSuccess = { RouteState.Success(it) },
//                onFailure = { RouteState.Error(it.message ?: "Unknown error") }
//            )
//        }
//    }
//}
//
//
//// ── Sealed classes for state ─────────────────────────────────────────
//
//sealed class LocationState {
//    object Unknown : LocationState()
//    object Loading : LocationState()
//    data class Located(val latLng: LatLng) : LocationState()
//    data class Error(val message: String) : LocationState()
//}
//
//sealed class RouteState {
//    object Idle : RouteState()
//    object Loading : RouteState()
//    data class Success(val route: RouteResult) : RouteState()
//    data class Error(val message: String) : RouteState()
//}
