package com.fit3161.fit3162.mogo.UIScreen.FutureRideScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.fit3161.fit3162.mogo.ui.components.AddressAutocompleteField

/**
 * Dialog that asks the rider where they want to be picked up.
 *
 * The rider has two ways to set a pickup point:
 *   1. Tap "Use my current location" inside the autocomplete dropdown — the
 *      caller is told via [onConfirm] with `useCurrentLocation = true` and
 *      should fetch device location at that moment.
 *   2. Type an address and pick a suggestion — coordinates come back resolved
 *      and the caller gets them via [onConfirm] with explicit lat/lng.
 *
 * The dialog tracks its own internal state. It commits nothing until the
 * rider taps Confirm.
 *
 * @param placesRepo used by the embedded autocomplete field.
 * @param onConfirm called with the chosen pickup. If [useCurrentLocation] is true, [pickupLat] / [pickupLng] / [pickupLabel] are
 *                  null — the caller resolves them.
 * @param onDismiss called when the rider cancels or taps outside.
 */
@Composable
fun PickupDialog(
    placesRepo: PlacesRepository,
    onConfirm: (
        useCurrentLocation: Boolean,
        pickupLat: Double?,
        pickupLng: Double?,
        pickupLabel: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    // Internal state: what has the rider chosen?
    // - useCurrentLocation = true: the dropdown's "Use my current location" was tapped.
    // - resolvedLat/Lng/label populated: an autocomplete suggestion was picked.
    // - Neither: the rider has typed something but not picked. Confirm is disabled.
    var useCurrentLocation by remember { mutableStateOf(false) }
    var resolvedLat by remember { mutableStateOf<Double?>(null) }
    var resolvedLng by remember { mutableStateOf<Double?>(null) }
    var resolvedLabel by remember { mutableStateOf<String?>(null) }
    var fieldValue by remember { mutableStateOf("") }

    // Confirm is enabled if the rider has either picked an autocomplete suggestion
    // OR explicitly chosen "Use my current location."
    val canConfirm = useCurrentLocation || (resolvedLat != null && resolvedLng != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        // Allow the dialog to grow as the autocomplete dropdown opens
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        title = {
            Text(
                text = "Where should we pick you up?",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Tap a suggestion to set the pickup, or use your current location.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                AddressAutocompleteField(
                    label = "Pickup location",
                    currentValue = fieldValue,
                    placesRepo = placesRepo,
                    onCurrentLocation = {
                        useCurrentLocation = true
                        resolvedLat = null
                        resolvedLng = null
                        resolvedLabel = null
                        fieldValue = "Current location"
                    },
                    onPlacePicked = { resolved ->
                        useCurrentLocation = false
                        resolvedLat = resolved.latLng.latitude
                        resolvedLng = resolved.latLng.longitude
                        resolvedLabel = resolved.name
                        fieldValue = resolved.name
                    },
                    setValue = { typed ->
                        // User is typing freely; clear any previous selection so
                        // we don't accidentally confirm with stale coordinates.
                        fieldValue = typed
                        if (typed != "Current location") {
                            useCurrentLocation = false
                            resolvedLat = null
                            resolvedLng = null
                            resolvedLabel = null
                        }
                    }
                )

                if (canConfirm) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (useCurrentLocation) {
                                "✓ Will use your current location"
                            } else {
                                "✓ ${resolvedLabel ?: "Pickup set"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        useCurrentLocation,
                        resolvedLat,
                        resolvedLng,
                        resolvedLabel
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFB57BFF)
                )
            ) {
                Text("Confirm pickup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
