package com.halohome.iotresearch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.halohome.iotresearch.data.api.NetworkModule
import com.halohome.iotresearch.data.models.RegisterRequest
import com.halohome.iotresearch.telemetry.MobileTelemetryManager
import com.halohome.iotresearch.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Participant Registration Screen.
 * Registers new research participants under the privacy-preserving zero-PII model.
 * Automatically initializes a new commissioning session upon successful registration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var consentChecked by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Participant Registration", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToLogin) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateBackground)
            )
        },
        containerColor = SlateBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LightBlueBg),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                        Text("Research Ethics & Privacy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NavyPrimary)
                    }
                    Text(
                        "Your email is immediately converted into a salted SHA-256 cryptographic hash. Zero plaintext names or emails are stored in the database.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (min 4 characters)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = consentChecked,
                            onCheckedChange = { consentChecked = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "I agree to participate in this IoT usability vs security study.",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    errorMessage?.let { err ->
                        Text(
                            text = err,
                            color = DangerRed,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DangerLight, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter email and password."
                                return@Button
                            }
                            if (!consentChecked) {
                                errorMessage = "Please agree to the research consent."
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val res = NetworkModule.apiService.register(
                                        RegisterRequest(email.trim(), password.trim())
                                    )
                                    if (res.isSuccessful && res.body()?.success == true) {
                                        val body = res.body()!!
                                        NetworkModule.authToken = body.token
                                        NetworkModule.userId = body.user?.id ?: -1
                                        NetworkModule.participantId = body.user?.participantId ?: "P-001"

                                        MobileTelemetryManager.startHeartbeat()
                                        MobileTelemetryManager.logEvent("user_registered")
                                        onRegisterSuccess()
                                    } else {
                                        errorMessage = res.body()?.error ?: "Registration failed."
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Cannot connect to server (${NetworkModule.baseUrl})."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BlueAccent),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Create Participant Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            TextButton(onClick = onNavigateToLogin) {
                Text("Already registered? Sign In", color = BlueAccent)
            }
        }
    }
}
