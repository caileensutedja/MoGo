package com.fit3161.fit3162.mogo.ui.maps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapsViewModel(private val mapRepository: MapsRepository) : ViewModel() {

    // ── User location state ──────────────────────────────────────────
    private val _userLocation = MutableStateFlow<LocationState>(LocationState.Unknown)
    val userLocation: StateFlow<LocationState> = _userLocation.asStateFlow()

    // ── Route state ──────────────────────────────────────────────────
    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()

    /**
     * Fetches device location from the repository.
     * Call this AFTER permission is granted from the UI layer.
     */
    fun loadDeviceLocation() {
        viewModelScope.launch {
            _userLocation.value = LocationState.Loading
            _userLocation.value = mapRepository.getDeviceLocation().fold(
                onSuccess = { LocationState.Located(it) },
                onFailure = { LocationState.Error(it.message ?: "Failed to get location") }
            )
        }
    }

    /**
     * Fetches a route from origin to destination.
     * If no origin is provided, uses the current device location.
     */
    fun fetchRoute(destination: LatLng, origin: LatLng? = null) {
        viewModelScope.launch {
            _routeState.value = RouteState.Loading

            // Resolve origin: use provided value, current location, or fetch fresh
            val resolvedOrigin = origin
                ?: (_userLocation.value as? LocationState.Located)?.latLng
                ?: run {
                    // Try fetching location if we don't have one yet
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

    /** Resets route state back to idle (e.g. when user clears the route). */
    fun clearRoute() {
        _routeState.value = RouteState.Idle
    }
}


// ── Sealed classes for state ─────────────────────────────────────────

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
//import com.fit3161.fit3162.mogo.data.model.RouteResult
//import com.fit3161.fit3162.mogo.data.repo.MapsRepository
//import com.google.android.gms.maps.model.LatLng
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class MapsViewModel(private val mapRepository : MapsRepository) : ViewModel() {
//
//    private val _userLocation = MutableStateFlow<LocationState>(LocationState.Unknown)
//    val userLocation: StateFlow<LocationState> = _userLocation.asStateFlow()
//
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
////    fun getDeviceLocation(fusedLocationClient : FusedLocationProviderClient) {
////        try {
////            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
////                if (location != null) {
////                    // Update a new state: _currentUserLocation.value = LatLng(location.latitude, location.longitude)
////                }
////            }
////        } catch (e: SecurityException) {
////            // Handle exception.
////        }
////    }
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
//                    // Try fetching location if we don't have one yet
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
//
////    fun fetchRoute(origin: LatLng, destination: LatLng) {
////        viewModelScope.launch {
//////             OLD:
////            _routeState.value = RouteState.Loading
////            _routeState.value = mapRepository.getRoute(origin, destination).fold(
////                onSuccess = { RouteState.Success(it) },
////                onFailure = { RouteState.Error(it.message ?: "Unknown error") }
////            )
////        }
////    }
////}
//
//    /** Resets route state back to idle (e.g. when user clears the route). */
//    fun clearRoute() {
//        _routeState.value = RouteState.Idle
//    }
//
//}
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
//
//
