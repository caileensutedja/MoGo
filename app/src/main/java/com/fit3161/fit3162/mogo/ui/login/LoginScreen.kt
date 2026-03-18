package com.fit3161.fit3162.mogo.ui.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.data.model.AuthState

/**
 * LoginScreen.kt
 *
 * The Composable UI for the Login screen. This is a purely visual layer —
 * it displays state and forwards user interactions to the ViewModel.
 *
 * RESPONSIBILITIES:
 * - Render input fields, buttons, and error/loading states
 * - Observe AuthState from LoginViewModel and react to changes
 * - Trigger navigation callbacks when login succeeds or user taps Register
 *
 * WHAT IT DOES NOT DO:
 * - It does not contain any business logic
 * - It does not call Supabase directly
 * - It does not manage navigation itself — it calls the lambdas passed in
 *
 * @param viewModel            The LoginViewModel providing state and handling actions.
 * @param onNavigateToRegister Called when the user taps "Don't have an account? Register".
 * @param onLoginSuccess       Called when AuthState becomes Success — triggers navigation.
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    // Collects the StateFlow from the ViewModel, respecting the Compose lifecycle.
    // Re-renders only when state actually changes.
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Local UI state — these only exist within this Composable and are not persisted
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Side effect: when state becomes Success, trigger the navigation callback.
    // LaunchedEffect re-runs whenever `state` changes.
    LaunchedEffect(state) {
        if (state is AuthState.Success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in with your Monash University email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Email field — keyboard type set to Email for appropriate keyboard on device
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                viewModel.resetState() // Clear any previous error as user types
            },
            label = { Text("University Email") },
            placeholder = { Text("E.g. abc1234@student.monash.edu") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next  // "Next" moves focus to password field
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password field — obscured by default, toggle with the eye icon
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                viewModel.resetState()
            },
            label = { Text("Password") },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done  // "Done" dismisses keyboard
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible)
                            "Hide password" else "Show password"
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Error message — only visible when state is Error, animates in/out smoothly
        AnimatedVisibility(visible = state is AuthState.Error) {
            Text(
                text = (state as? AuthState.Error)?.message ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In button — disabled while loading to prevent duplicate requests
        Button(
            onClick = { viewModel.login(email, password) },
            enabled = state !is AuthState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (state is AuthState.Loading) {
                // Replace button text with a spinner during the network call
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign In")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subtle link to Register screen — less prominent than the main button
        // because most returning users won't need it
        TextButton(onClick = onNavigateToRegister) {
            Text("Don't have an account? Register")
        }
    }
}
