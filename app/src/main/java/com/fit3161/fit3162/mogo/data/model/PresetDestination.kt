package com.fit3161.fit3162.mogo.data.model

import com.google.android.gms.maps.model.LatLng

/**
 * Represents a preset destination the user can select.
 *
 * @param name          Display name shown in the UI chip/button.
 * @param latLng        Coordinates of the destination.
 * @param description   Optional short description (e.g. address or landmark info).
 */
data class PresetDestination(
    val name: String,
    val latLng: LatLng,
    val description: String = ""
)
