package com.fit3161.fit3162.mogo.UIScreen.SignInScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

class SignInScreen : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoGoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SignInScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SignInScreen(modifier: Modifier = Modifier) {
    var userEmail by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier=Modifier.height(50.dp))
        Text(
            text = "Sign In",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "with your Monash email",
            fontSize = 30.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier=Modifier.height(50.dp))
        //Email Input
        val isValid = userEmail.contains("@") && userEmail.endsWith(".monash.edu")
        OutlinedTextField(
            value = userEmail,
            onValueChange = { userEmail = it },
            label = { Text("Monash Email Address") },
            isError = userEmail.isNotEmpty() && !isValid,
            supportingText = {
                if (userEmail.isNotEmpty() && !isValid) {
                    Text("Please enter your Monash email (e.g. [user]@[student/staff].monash.edu)")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        //Password Input
        OutlinedTextField(
            value = userPassword,
            onValueChange = { userPassword = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // Implement this!!!
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = "Forgot Password? Re-register with the same email!",
                fontSize = 12.sp
                // Add navigation to Register screen here!
            )
        }
        // Confirm Login here!


        Spacer(modifier=Modifier.height(200.dp))
        Text(
            text="Available only for Monash University students and staff.\n" +
                    "Please make sure you log in with your Monash credentials.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier=Modifier.height(20.dp))

        Button(
            // Click logic here
            onClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )
        ) {
            Text("Sign In")
        }
        Spacer(modifier=Modifier.height(20.dp))
        // Add Navigation Controller here
        Text(
            text = "Don't have an account yet? Register"
        )
    }
}
