package com.fit3161.fit3162.mogo.UIScreen.OfferScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.UIScreen.OfferScreen.ui.OfferViewModel
import com.fit3161.fit3162.mogo.data.repo.Offer

@Composable
fun OfferScreenUI(
    viewModel: OfferViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Offers",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold
        )

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        state.error?.let {
            Text("Error: $it", color = Color.Red)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.offersList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No offers available")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(state.offersList.size) { idx ->
                    val offer = state.offersList[idx]
                    OfferCardSkeleton(offer = offer)
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun OfferCardSkeleton(offer: Offer) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3E8FF), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = offer.businesses?.name ?: "MoGo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Discount: $${offer.amount ?: "N/A"}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "Expiry: ${formatDate(offer.date)}",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { /* TODO: Generate a code? */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAD7FF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Claim")
                    }

                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFB57BFF)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("T&C")
                    }
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("Terms & Conditions") },
                        text = { Text("Please refer to MoGo's terms and conditions.") },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Ok")
                            }
                        }
                    )
                }

            }

        }
    }



}

fun formatDate(dateString: String?): String {
    if (dateString == null) return "No expiry"
    return try {
        val input = java.time.OffsetDateTime.parse(dateString)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        input.format(formatter)
    } catch (e: Exception) {
        dateString.take(10)
    }
}