package com.fit3161.fit3162.mogo.UIScreen.RegisterScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fit3161.fit3162.mogo.data.model.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateToLogin: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var showOtherInput by remember { mutableStateOf(false) }
    var otherInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = viewModel.countryOptions.filter {
        it.name.contains(searchQuery, true) ||
                it.code.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register") },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))   // reduced from 50dp because the title is now in the top bar

            Text(
                text = "Create Account",
                fontSize = 34.sp,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Register with your Monash University email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))   // adjusted spacing

            // EMAIL
            OutlinedTextField(
                value = form.email,
                onValueChange = {
                    viewModel.onEmailChange(it)
                    viewModel.resetState()
                },
                placeholder = { Text("username@student.monash.edu") },
                label = { Text("Monash Email Address") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // NAME
            OutlinedTextField(
                value = form.name,
                onValueChange = {
                    viewModel.onNameChange(it)
                    viewModel.resetState()
                },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // PHONE NUMBER
            Text(
                text = "Phone Number",
                fontSize = 13.sp,
                style = MaterialTheme.typography.bodySmall,
            )

            Row {
                OutlinedTextField(
                    value = form.countryCode,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.width(110.dp),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                OutlinedTextField(
                    value = form.phoneNumber,
                    onValueChange = {
                        viewModel.onPhoneChange(it)
                        viewModel.resetState()
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("9 digits") }
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country") },
                    modifier = Modifier.padding(8.dp)
                )

                filteredCountries.forEach { country ->
                    DropdownMenuItem(
                        text = { Text("${country.name} (${country.code})") },
                        onClick = {
                            viewModel.onCountrySelected(country.code)
                            expanded = false
                            searchQuery = ""
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GENDER
            Column {
                OutlinedTextField(
                    value = if (showOtherInput && otherInput.isNotBlank())
                        "Other: $otherInput" else form.gender,
                    onValueChange = {},
                    label = { Text("Gender") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { genderExpanded = true }) {
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    },
                    readOnly = true
                )

                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false }
                ) {
                    viewModel.genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                genderExpanded = false
                                if (option == "Other") {
                                    showOtherInput = true
                                    viewModel.onGenderChange("Other")
                                } else {
                                    showOtherInput = false
                                    otherInput = ""
                                    viewModel.onGenderChange(option)
                                }
                                viewModel.resetState()
                            }
                        )
                    }
                }

                if (showOtherInput) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otherInput,
                        onValueChange = {
                            otherInput = it
                            viewModel.onGenderChange("Other: $it")
                            viewModel.resetState()
                        },
                        label = { Text("Please specify") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // PASSWORD
            OutlinedTextField(
                value = form.password,
                onValueChange = {
                    viewModel.onPasswordChange(it)
                    viewModel.resetState()
                },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible)
                                Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // CONFIRM PASSWORD
            OutlinedTextField(
                value = form.confirmPassword,
                onValueChange = {
                    viewModel.onConfirmPasswordChange(it)
                    viewModel.resetState()
                },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = state is AuthState.Error) {
                Text(
                    text = (state as? AuthState.Error)?.message ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Available only for Monash University students and staff. " +
                        "Please make sure you log in with your Monash credentials.",
                fontSize = 9.5.sp,
                lineHeight = 12.sp,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(5.dp))

            Button(
                onClick = { viewModel.register(form) },
                enabled = state !is AuthState.Loading &&
                        state !is AuthState.AwaitingEmailConfirmation,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCEA2FD)
                )
            ) {
                if (state is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Register")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Sign in")
            }

            if (state is AuthState.AwaitingEmailConfirmation) {
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Registration Successful!") },
                    text = {
                        Text(
                            "✅ Check your Monash email and click the verification link. Then return to sign in."
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.resetState()
                                onNavigateToLogin()
                            },
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCEA2FD)
                            )
                        ) {
                            Text("Continue")
                        }
                    }
                )
            }
        }
    }
}