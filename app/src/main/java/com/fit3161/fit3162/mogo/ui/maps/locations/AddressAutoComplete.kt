package com.fit3161.fit3162.mogo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.fit3161.fit3162.mogo.data.repo.PlacePrediction
import com.fit3161.fit3162.mogo.data.repo.PlacesRepository
import com.fit3161.fit3162.mogo.data.repo.ResolvedPlace
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.compose.runtime.rememberCoroutineScope


/**
 * The result emitted when the user picks a suggestion.
 *
 * - [ResolvedPlace] when they picked an autocomplete suggestion.
 * - "Use my current location" emits a special signal — see [AddressAutocompleteField]
 *   onPickCurrentLocation parameter.
 */

/**
 * An OutlinedTextField that shows address autocomplete predictions as the
 * user types. Always offers "Use my current location" as the first option.
 *
 * @param label Text shown as the field's label (e.g. "Starting Location").
 * @param currentValue Display text currently in the field. Caller owns the state.
 * @param placesRepo For predictions + place details.
 * @param onCurrentLocation Called when user taps "Use my current location."
 * @param onPlacePicked Called when the user picks an autocomplete suggestion AND the place's coordinates have been resolved.
 * @param setValue Called whenever the displayed text changes (typing OR picking). Lets the caller keep the field's state in sync.
 */
@OptIn(FlowPreview::class)
@Composable
fun AddressAutocompleteField(
    label: String,
    currentValue: String,
    placesRepo: PlacesRepository,
    onCurrentLocation: () -> Unit,
    onPlacePicked: (ResolvedPlace) -> Unit,
    setValue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf(currentValue) }
    var predictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var hasFocus by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val scope: CoroutineScope = rememberCoroutineScope()

    // Keep field text in sync when caller changes the value externally
    // (e.g. after device-location lookup writes "Current location").
    LaunchedEffect(currentValue) {
        if (currentValue != query) query = currentValue
    }

    // Debounced fetch: wait 300ms after the user stops typing, then query Places.
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .distinctUntilChanged()
            .debounce(300)
            .collect { q ->
                if (q.isBlank() || q.length < 2) {
                    predictions = emptyList()
                    isLoading = false
                    return@collect
                }
                isLoading = true
                try {
                    predictions = placesRepo.getPredictions(q)
                } catch (e: Exception) {
                    predictions = emptyList()
                } finally {
                    isLoading = false
                }
            }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                setValue(it)
            },
            label = { Text(label) },
            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state -> hasFocus = state.isFocused }
        )

        // Suggestion popup. Shows whenever the field has focus.
        if (hasFocus) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 4.dp
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    // First item is always "Use my current location"
                    item {
                        SuggestionRow(
                            icon = Icons.Default.MyLocation,
                            primary = "Use my current location",
                            secondary = null,
                            onClick = {
                                onCurrentLocation()
                                predictions = emptyList()
                                hasFocus = false
                            }
                        )
                        if (predictions.isNotEmpty()) {
                            HorizontalDivider()
                        }
                    }

                    items(predictions) { prediction ->
                        SuggestionRow(
                            icon = Icons.Default.LocationOn,
                            primary = prediction.primary,
                            secondary = prediction.secondary,
                            onClick = {
                                scope.launch {
                                    try {
                                        val resolved = placesRepo.resolvePrediction(prediction)
                                        setValue(resolved.name)
                                        query = resolved.name
                                        onPlacePicked(resolved)
                                        placesRepo.refreshSessionToken()
                                    } catch (_: Exception) {
                                        // Silently fail — keep typed text, user can try again.
                                    }
                                    predictions = emptyList()
                                    hasFocus = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: String,
    secondary: String?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
