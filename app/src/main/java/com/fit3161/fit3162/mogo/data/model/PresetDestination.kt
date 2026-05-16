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


/**
 * Shared list of preset destinations (Monash campuses) used across the app.
 *
 * Both the rider's map screen and the driver's upload form pull from this
 * single source of truth so destinations stay in sync. When you eventually
 * replace these with Places Autocomplete, this object goes away.
 */
object PresetDestinations {

    val all: List<PresetDestination> = listOf(
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
        )
    )

    /** Look up a preset by name (used when persisting/retrieving destination as a string). */
    fun byName(name: String): PresetDestination? =
        all.firstOrNull { it.name == name }
}
data class Location(
    val name: String,
    val latLng: LatLng
)