package com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.R
import com.fit3161.fit3162.mogo.ui.components.SemiCircleBackground

@Composable
fun WelcomeScreen(onNavigateToLogin: () -> Unit = {}) {
    SemiCircleBackground(
        color = Color(0xFFCEA2FD),
        domeHeight = 550.dp,
        cornerRadius = 5000.dp,
        backgroundColor = Color(0xFF85BBE1),
        background = {
            Image(
                painter = painterResource(id = R.drawable.new_welcomescreen_image),
                contentDescription = "MoGo Logo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1200.dp)
                    .offset(y = (-200).dp),
                contentScale = ContentScale.FillWidth
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            // Spacer to push everything further down
            Spacer(modifier = Modifier.weight(1f))

            // Title
            Text(
                text = buildAnnotatedString {
                    append("Welcome to ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                        append("MoGo")
                    }
                },
                fontSize = 36.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            // More space between title and subtitle
            Spacer(modifier = Modifier.height(32.dp))

            // Subtitle
            Text(
                text = "We hope you have a wonderful trip!",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Info text
            Text(
                text = "Available only for Monash University students and staff.\nPlease make sure you log in with your Monash credentials.",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.fillMaxWidth(0.85f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Button
            Button(
                onClick = onNavigateToLogin,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text(
                    text = "Start",
                    fontSize = 18.sp,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Text(
                text = "Designed by Group 15:\nBrianna, Caileen, Jasmine, Pia",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWelcomeScreen() {
    WelcomeScreen()
}