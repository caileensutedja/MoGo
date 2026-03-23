package com.fit3161.fit3162.mogo.UIScreen.WelcomeScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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


/**
 * WelcomeScreen UI Composable.
 *
 * @param onNavigateToLogin TODO
 */
@Composable
@Preview(showBackground = true)
fun WelcomeScreen(onNavigateToLogin: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Spacer(modifier=Modifier.height(50.dp))
        Image(
            painter = painterResource(id = R.drawable.mogo_logo),
            contentDescription = "Dummy MoGo logo", // Change to final logo!
            modifier = Modifier.size(350.dp)
        )
        Text(
            text = buildAnnotatedString {
                append("Welcome to ")
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append("MoGo")
                }
            },
            fontSize = 37.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text="We hope you have a wonderful trip!",
            fontSize = 15.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.height(150.dp))
        Text(
            text = "Available only for Monash University students and staff. " +
                    "Please make sure you log in with your Monash credentials.",
            fontSize =  9.5.sp,
            lineHeight = 12.sp,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(vertical = 4.dp) // reduce top/bottom spacing
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onNavigateToLogin, // Go to Login Screen
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )

        ) {
            Text(text = "Start",
                fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Designed by Group 15:\nBrianna, Caileen, Jasmine, Priyana",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}
