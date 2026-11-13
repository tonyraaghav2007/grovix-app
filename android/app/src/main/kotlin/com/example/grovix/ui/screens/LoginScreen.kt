package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.CloudStatus
import com.example.grovix.data.GrovixRepository
import com.example.grovix.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val cloudStatus by GrovixRepository.cloudStatus.collectAsState()
    val serverUrl by GrovixRepository.serverUrl.collectAsState()

    var showServerDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember { mutableStateOf(serverUrl) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingServer by remember { mutableStateOf(false) }

    fun doLogin(loginEmail: String, loginPass: String) {
        if (loginEmail.isBlank() || loginPass.isBlank()) {
            errorMessage = "Please enter your email and password"
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            val result = GrovixRepository.login(loginEmail, loginPass)
            isLoading = false
            if (result.isSuccess) {
                onLoginSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check credentials or cloud connection."
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GrovixBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Grovix Brand Logo
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GrovixNavy),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "G",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Grovix",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = GrovixNavy
            )

            Text(
                text = "Central Cloud Synchronized Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = GrovixTextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GrovixTextDark
                    )
                    Text(
                        text = "Enter your Grovix company credentials",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrovixTextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (errorMessage != null) {
                        Surface(
                            color = GrovixError.copy(alpha = 0.1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = GrovixError,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.MailOutline, contentDescription = null, tint = GrovixNavy) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GrovixNavy) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = GrovixTextMuted
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { doLogin(email, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cloud Server Status & Switcher
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        tempServerUrl = serverUrl
                        testFeedback = null
                        showServerDialog = true
                    },
                color = GrovixSurfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (cloudStatus) {
                                    CloudStatus.CONNECTED -> GrovixSuccess
                                    CloudStatus.SYNCING, CloudStatus.CONNECTING -> GrovixWarning
                                    else -> GrovixTextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cloud: $serverUrl",
                        color = GrovixTextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Edit Server",
                        tint = GrovixTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    // Server Configuration Dialog
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = GrovixNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Central Cloud Server", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter the Grovix central server or public tunnel URL so all colleagues synchronize the same database:",
                        fontSize = 13.sp,
                        color = GrovixTextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = tempServerUrl,
                        onValueChange = { tempServerUrl = it; testFeedback = null },
                        label = { Text("Server Base URL") },
                        placeholder = { Text("https://grovix-cloud.loca.lt") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (testFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = testFeedback!!,
                            color = if (testFeedback!!.startsWith("🟢")) GrovixSuccess else GrovixError,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            isTestingServer = true
                            testFeedback = "Testing connection..."
                            GrovixRepository.setServerUrl(tempServerUrl)
                            GrovixRepository.checkCloudConnection { success ->
                                isTestingServer = false
                                testFeedback = if (success) "🟢 Connection successful! Connected to Grovix Cloud."
                                else "🔴 Unable to reach server. Please check the URL and ensure the server is running."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixSurfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isTestingServer
                    ) {
                        Text("Test Connection", color = GrovixTextDark, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        GrovixRepository.setServerUrl(tempServerUrl)
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel", color = GrovixTextMuted)
                }
            }
        )
    }
}
