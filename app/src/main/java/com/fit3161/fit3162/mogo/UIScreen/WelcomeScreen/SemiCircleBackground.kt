package com.fit3161.fit3162.mogo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SemiCircleBackground(
    color: Color = Color(0xFFCEA2FD),
    domeHeight: Dp = 260.dp,
    cornerRadius: Dp = 400.dp,
    backgroundColor: Color = Color.White,
    background: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Background (logo)
        Box(modifier = Modifier.fillMaxSize()) {
            background()
        }

        // Solid purple dome at bottom – fully opaque, short, wide
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(domeHeight)
                .clip(RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius))
                .background(color)   // solid, fully opaque
        )

        // Foreground content (white text, button)
        content()
    }
}