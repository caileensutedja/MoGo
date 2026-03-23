package com.fit3161.fit3162.mogo.UIScreen.RegisterScreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fit3161.fit3162.mogo.ui.theme.MoGoTheme

@Composable
@Preview(showBackground = true)
fun RegisterScreen(modifier: Modifier = Modifier) {
    var userEmail by remember { mutableStateOf("") }
    var showOTPDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier=Modifier.height(50.dp))
        Text(
            text = "Register",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "with your Monash email",
            fontSize = 30.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier=Modifier.height(70.dp))
        //Email Input
        val isValid = userEmail.contains("@") && userEmail.endsWith(".monash.edu")
        OutlinedTextField(
            value = userEmail,
            onValueChange = { userEmail = it },
            label = { Text("Enter your Monash Email Address") },
            isError = userEmail.isNotEmpty() && !isValid,
            supportingText = {
                if (userEmail.isNotEmpty() && !isValid) {
                    Text("Please enter your Monash email (e.g. [user]@[student/staff].monash.edu)")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            // Logic to open the view model here when clicked
            onClick = {
                if (isValid) {
                    showOTPDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFCEA2FD)
            )
        ) {
            Text("Send Verification Code")
        }
        // Show the dialog logic
        if (showOTPDialog){
            OTPVIewModel(
                userEmail = userEmail,
                onDismissRequest = {showOTPDialog = false}
            )
        }

        Spacer(modifier=Modifier.height(300.dp))
        Text(
            text="Available only for Monash University students and staff.\n" +
                    "Please make sure you log in with your Monash credentials.",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.fillMaxWidth(0.85f)
        )
        Spacer(modifier=Modifier.height(20.dp))
//        Divider()
        Spacer(modifier=Modifier.height(20.dp))
        // Add Navigation Controller here
        Text(
            text = "Already have an account yet? Sign In"
        )
    }
}

@Composable
fun OTPVIewModel(
    userEmail: String,
    onDismissRequest: () -> Unit // This will handle dismissal
) {
    var OTPinput by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var userPassword by remember { mutableStateOf("") }
    var userConfirmPassword by remember { mutableStateOf("") }

    val nameValid = userName.isNotBlank()
    val passwordMatch = userPassword == userConfirmPassword && userPassword.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest, // Dismiss when clicked outside or on the back button
        confirmButton = {},
        title = {
            Text(
                text = "Complete your Registration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Message to show where the code was sent to
                Text(
                    text = "Verification code sent to $userEmail",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                // OTP input
                OutlinedTextField(
                    value = OTPinput,
                    onValueChange = { OTPinput = it },
                    label = { Text("One Time Passcode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Can change the validation logic here to !OTPinput or OTPinput == 6
                if (OTPinput.length == 0) {
                    Text(
                        text = "Please the OTP in your email inbox.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Name Input
                // Name input
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!nameValid) {
                    Text(
                        text = "Please enter your name.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Password input
                OutlinedTextField(
                    value = userPassword,
                    onValueChange = { userPassword = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Confirm Password input
                OutlinedTextField(
                    value = userConfirmPassword,
                    onValueChange = { userConfirmPassword = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!passwordMatch) {
                    Text(
                        text = "Your password does not match, please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Cancel the registration, closes the view model
                    TextButton(onClick = onDismissRequest) { // Dismiss action
                        Text("Cancel")
                    }

                    Button(
                        // Logic to save the data into the database here
                        onClick = {},
                        enabled = nameValid && passwordMatch
                    ) {
                        Text(" Complete Register")
                    }
                }

            }
        }
    )
}