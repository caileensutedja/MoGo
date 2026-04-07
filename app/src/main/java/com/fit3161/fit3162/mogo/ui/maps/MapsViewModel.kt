package com.fit3161.fit3162.mogo.ui.maps

import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fit3161.fit3162.mogo.data.model.RouteResult
import com.fit3161.fit3162.mogo.data.repo.MapsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapsViewModel(private val mapRepository : MapsRepository) : ViewModel() {

    private val _routeState = MutableStateFlow<RouteState>(RouteState.Idle)
    val routeState: StateFlow<RouteState> = _routeState.asStateFlow()

    fun fetchRoute(origin: LatLng, destination: LatLng) {
        viewModelScope.launch {
            _routeState.value = RouteState.Loading
            _routeState.value = mapRepository.getRoute(origin, destination).fold(
                onSuccess = { RouteState.Success(it) },
                onFailure = { RouteState.Error(it.message ?: "Unknown error") }
            )
        }
    }
}


sealed class RouteState {
    object Idle : RouteState()
    object Loading : RouteState()
    data class Success(val route: RouteResult) : RouteState()
    data class Error(val message: String) : RouteState()
}