package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.CloudStatus
import com.example.grovix.data.GrovixRepository
import com.example.grovix.ui.theme.*

@Composable
fun SettingsScreen() {
    val currentUser by GrovixRepository.currentUser.collectAsState()
    val settings by GrovixRepository.settings.collectAsState()
    val activityLogs by GrovixRepository.activityLogs.collectAsState()

    val cloudStatus by GrovixRepository.cloudStatus.collectAsState()
    val cloudStatusMessage by GrovixRepository.cloudStatusMessage.collectAsState()
    val serverUrl by GrovixRepository.serverUrl.collectAsState()
    val lastSyncTime by GrovixRepository.lastSyncTime.collectAsState()

    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var passFeedback by remember { mutableStateOf<String?>(null) }
    var passSuccess by remember { mutableStateOf(false) }

    var companyName by remember { mutableStateOf(settings.companyName) }
    var currency by remember { mutableStateOf(settings.currency) }
    var taxRateStr by remember { mutableStateOf(settings.taxRate.toString()) }
    var settingsFeedback by remember { mutableStateOf<String?>(null) }

    var isSyncing by remember { mutableStateOf(false) }
    var syncFeedback by remember { mutableStateOf<String?>(null) }

    var showServerDialog by remember { mutableStateOf(false) }
    var tempServerUrl by remember { mutableStateOf(serverUrl) }
    var testFeedback by remember { mutableStateOf<String?>(null) }
    var isTestingServer by remember { mutableStateOf(false) }

    val isOwner = currentUser?.isOwner == true

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrovixBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Administration & Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GrovixTextDark
            )
        }

        // CENTRAL CLOUD SYNC CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = GrovixNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Central Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrovixTextDark)
                        }

                        // Status Badge
                        Surface(
                            color = when (cloudStatus) {
                                CloudStatus.CONNECTED -> GrovixSuccess.copy(alpha = 0.15f)
                                CloudStatus.SYNCING, CloudStatus.CONNECTING -> GrovixWarning.copy(alpha = 0.15f)
                                else -> GrovixTextMuted.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (cloudStatus) {
                                                CloudStatus.CONNECTED -> GrovixSuccess
                                                CloudStatus.SYNCING, CloudStatus.CONNECTING -> GrovixWarning
                                                else -> GrovixTextMuted
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (cloudStatus) {
                                        CloudStatus.CONNECTED -> "Cloud Active"
                                        CloudStatus.SYNCING -> "Syncing..."
                                        CloudStatus.CONNECTING -> "Connecting..."
                                        else -> "Offline Mode"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (cloudStatus) {
                                        CloudStatus.CONNECTED -> GrovixSuccess
                                        CloudStatus.SYNCING, CloudStatus.CONNECTING -> GrovixWarning
                                        else -> GrovixTextMuted
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Synchronize live tasks, sales, expenses, and team data across all colleagues' Android devices.",
                        fontSize = 12.sp,
                        color = GrovixTextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        color = GrovixSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Connected Server Endpoint:",
                                fontSize = 11.sp,
                                color = GrovixTextMuted
                            )
                            Text(
                                text = serverUrl,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GrovixNavy
                            )
                            if (lastSyncTime != null) {
                                Text(
                                    text = "Last Synced: Today at $lastSyncTime",
                                    fontSize = 10.sp,
                                    color = GrovixSuccess
                                )
                            }
                        }
                    }

                    if (syncFeedback != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncFeedback!!,
                            color = if (syncFeedback!!.startsWith("🟢")) GrovixSuccess else GrovixWarning,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Sync Now Button
                        Button(
                            onClick = {
                                isSyncing = true
                                syncFeedback = "Synchronizing with Grovix Cloud..."
                                GrovixRepository.syncWithCloud { success, msg ->
                                    isSyncing = false
                                    syncFeedback = if (success) "🟢 $msg" else "⚠️ $msg"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isSyncing
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Syncing...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sync Now", fontSize = 12.sp)
                            }
                        }

                        // Configure Server Button
                        OutlinedButton(
                            onClick = {
                                tempServerUrl = serverUrl
                                testFeedback = null
                                showServerDialog = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = GrovixTextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit URL", fontSize = 12.sp, color = GrovixTextDark)
                        }
                    }
                }
            }
        }

        // Account Security & Password
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GrovixNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Account Security", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrovixTextDark)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (passFeedback != null) {
                        Text(
                            text = passFeedback!!,
                            color = if (passSuccess) GrovixSuccess else GrovixError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = oldPass,
                        onValueChange = { oldPass = it; passFeedback = null },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; passFeedback = null },
                        label = { Text("New Password (min 6 characters)") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; passFeedback = null },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (newPass != confirmPass) {
                                passFeedback = "Passwords do not match."
                                passSuccess = false
                                return@Button
                            }
                            val res = GrovixRepository.changePassword(oldPass, newPass)
                            if (res.isSuccess) {
                                passFeedback = "Password successfully changed!"
                                passSuccess = true
                                oldPass = ""
                                newPass = ""
                                confirmPass = ""
                            } else {
                                passFeedback = res.exceptionOrNull()?.message ?: "Failed to change password."
                                passSuccess = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Update Password", color = Color.White)
                    }
                }
            }
        }

        // Company Settings (Owners only)
        if (isOwner) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Business, contentDescription = null, tint = GrovixNavy)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Company Parameters (Owner Authority)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrovixTextDark)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (settingsFeedback != null) {
                            Text(
                                text = settingsFeedback!!,
                                color = GrovixSuccess,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it; settingsFeedback = null },
                            label = { Text("Company Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it; settingsFeedback = null },
                            label = { Text("Operating Currency") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = taxRateStr,
                            onValueChange = { taxRateStr = it; settingsFeedback = null },
                            label = { Text("Default Tax Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val rate = taxRateStr.toDoubleOrNull() ?: 18.0
                                GrovixRepository.updateSettings(settings.copy(companyName = companyName, currency = currency, taxRate = rate))
                                settingsFeedback = "Company preferences updated!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Company Settings", color = Color.White)
                        }
                    }
                }
            }
        }

        // Full Activity Audit Log
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = GrovixNavy)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Activity Audit Trail", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GrovixTextDark)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    activityLogs.take(20).forEach { log ->
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Text(
                                text = "${log.userName} (${log.userRole})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GrovixNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${log.action}: ${log.details}",
                                fontSize = 12.sp,
                                color = GrovixTextDark
                            )
                        }
                        HorizontalDivider(color = GrovixSurfaceVariant)
                    }
                }
            }
        }
    }

    // Change Server URL Dialog
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
                        text = "Enter the Central Server or Public Tunnel URL used by your Grovix team:",
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
