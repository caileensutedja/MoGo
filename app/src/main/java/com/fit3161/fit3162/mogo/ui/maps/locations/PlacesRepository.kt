package com.fit3161.fit3162.mogo.data.repo

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


/**
 * One row in the autocomplete dropdown.
 *
 * @param placeId    Google Place ID — opaque identifier used to fetch full details later.
 * @param primary    The big, bold text in the dropdown row (usually the place name).
 * @param secondary  The smaller text (usually the address / locality).
 */
data class PlacePrediction(
    val placeId: String,
    val primary: String,
    val secondary: String
)

/**
 * Resolved place after the user picks a prediction. Has the coordinates we
 * actually need to store on a Ride or Booking.
 */
data class ResolvedPlace(
    val placeId: String,
    val name: String,
    val address: String,
    val latLng: LatLng
)

/**
 * Wraps the Google Places SDK.
 *
 * Important context for whoever maintains this:
 *
 * - Places billing is per "session." A session is the whole flow:
 *   user types -> sees predictions -> picks one -> we fetch details.
 *   We pass a single `AutocompleteSessionToken` through both calls, then
 *   discard it. After [resolvePrediction] returns, call [refreshSessionToken]
 *   so the next interaction starts a new session.
 *
 * - Predictions are biased toward Australia (`AU`). 
 */
class PlacesRepository(
    private val placesClient: PlacesClient
) {

    companion object {
        private const val TAG = "PlacesRepository"
    }

    private var sessionToken: AutocompleteSessionToken = AutocompleteSessionToken.newInstance()

    /**
     * Fetch autocomplete predictions for [query]. Returns up to ~5 results,
     * biased to Australian addresses. Empty query returns empty list (no API call).
     */
    suspend fun getPredictions(query: String): List<PlacePrediction> {
        if (query.isBlank()) return emptyList()

        val request = FindAutocompletePredictionsRequest.builder()
            .setSessionToken(sessionToken)
            .setCountries("AU")
            .setQuery(query)
            .build()

        return suspendCancellableCoroutine { continuation ->
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    val predictions = response.autocompletePredictions.map {
                        PlacePrediction(
                            placeId = it.placeId,
                            primary = it.getPrimaryText(null).toString(),
                            secondary = it.getSecondaryText(null).toString()
                        )
                    }
                    continuation.resume(predictions)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Autocomplete failed", e)
                    continuation.resumeWithException(e)
                }
        }
    }

    /**
     * Fetch full details (name, address, lat/lng) for a picked prediction.
     * Closes the current autocomplete session — call [refreshSessionToken]
     * before starting a new search if you want a fresh billing session.
     */
    suspend fun resolvePrediction(prediction: PlacePrediction): ResolvedPlace {
        val request = FetchPlaceRequest.builder(
            prediction.placeId,
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG)
        )
            .setSessionToken(sessionToken)
            .build()

        return suspendCancellableCoroutine { continuation ->
            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val latLng = place.latLng
                    if (latLng == null) {
                        continuation.resumeWithException(
                            IllegalStateException("Place has no coordinates")
                        )
                        return@addOnSuccessListener
                    }
                    continuation.resume(
                        ResolvedPlace(
                            placeId = prediction.placeId,
                            name = place.name ?: prediction.primary,
                            address = place.address ?: prediction.secondary,
                            latLng = latLng
                        )
                    )
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Fetch place failed", e)
                    continuation.resumeWithException(e)
                }
        }
    }

    /** Start a new billing session. Call after a successful pick + resolve. */
    fun refreshSessionToken() {
        sessionToken = AutocompleteSessionToken.newInstance()
    }
}
