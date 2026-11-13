package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.model.User
import com.example.grovix.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen() {
    val currentUser by GrovixRepository.currentUser.collectAsState()
    val users by GrovixRepository.users.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("Grovix@2024") }
    var newRole by remember { mutableStateOf(if (currentUser?.isOwner == true) "manager" else "employee") }
    var newPhone by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val isOwner = currentUser?.isOwner == true
    val isManager = currentUser?.isManager == true

    Scaffold(
        floatingActionButton = {
            if (isManager) {
                FloatingActionButton(
                    onClick = { showAddDialog = true; errorText = null },
                    containerColor = GrovixNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrovixBackground)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Group,
                    label = "Total Personnel",
                    value = "${users.size}",
                    iconColor = GrovixNavy
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Shield,
                    label = "Directors / Owners",
                    value = "${users.count { it.role == "owner" }}",
                    iconColor = RoleOwnerColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Staff Directory & Roles",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GrovixNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(users) { member ->
                    UserCardItem(
                        member = member,
                        currentUser = currentUser,
                        onToggleStatus = {
                            GrovixRepository.toggleUserStatus(member.id)
                        },
                        onRemoveUser = {
                            GrovixRepository.removeUser(member.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val allowedRoles = if (isOwner) {
            listOf("owner" to "Owner / Director (Power 100)", "manager" to "Manager & Dev (Power 75)", "employee" to "Employee / Staff (Power 50)", "guest" to "Guest Auditor (Power 25)")
        } else {
            listOf("employee" to "Employee / Staff (Power 50)", "guest" to "Guest Auditor (Power 25)")
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = GrovixSurface,
            title = { Text("Create New User Account", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (errorText != null) {
                        Text(errorText!!, color = GrovixError, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Email Address *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Temporary Password *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Role & Power Level *:", fontSize = 12.sp, color = GrovixTextMuted)
                    var roleExpanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { roleExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(allowedRoles.find { it.first == newRole }?.second ?: newRole.uppercase())
                        }
                        DropdownMenu(
                            expanded = roleExpanded,
                            onDismissRequest = { roleExpanded = false }
                        ) {
                            allowedRoles.forEach { (roleKey, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { newRole = roleKey; roleExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank() && newEmail.isNotBlank()) {
                            val res = GrovixRepository.createAccount(newName, newEmail, newPassword, newRole, newPhone)
                            if (res.isSuccess) {
                                newName = ""
                                newEmail = ""
                                newPhone = ""
                                showAddDialog = false
                            } else {
                                errorText = res.exceptionOrNull()?.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Create Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UserCardItem(
    member: User,
    currentUser: User?,
    onToggleStatus: () -> Unit,
    onRemoveUser: () -> Unit
) {
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val roleColor = when (member.role.lowercase()) {
        "owner" -> RoleOwnerColor
        "manager" -> RoleManagerColor
        "employee" -> RoleEmployeeColor
        else -> RoleGuestColor
    }

    val isFounder = member.powerLevel >= 100 || member.role.equals("owner", ignoreCase = true)
    val canModify = !isFounder && (currentUser?.isOwner == true ||
            (currentUser?.isManager == true && member.powerLevel < (currentUser.powerLevel) && member.id != currentUser.id))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GrovixSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = GrovixTextDark
                    )
                    if (member.id == currentUser?.id) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "(You)", fontSize = 11.sp, color = GrovixAccent, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = roleColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${member.role.uppercase()} • LVL ${member.powerLevel}",
                        color = roleColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = member.email, fontSize = 12.sp, color = GrovixTextDark)
            if (member.phone?.isNotBlank() == true) {
                Text(text = "Tel: ${member.phone}", fontSize = 11.sp, color = GrovixTextMuted)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (member.status == "active") GrovixSuccess.copy(alpha = 0.12f) else GrovixError.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = member.status.uppercase(),
                        color = if (member.status == "active") GrovixSuccess else GrovixError,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (isFounder) {
                    Surface(
                        color = RoleOwnerColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "👑 Founder (Protected)",
                            color = RoleOwnerColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else if (canModify && member.id != currentUser?.id) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onToggleStatus,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (member.status == "active") "Deactivate" else "Activate",
                                fontSize = 11.sp,
                                color = if (member.status == "active") GrovixWarning else GrovixSuccess,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { showRemoveConfirm = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Remove",
                                fontSize = 11.sp,
                                color = GrovixError,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Remove Team Member", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Text(
                    "Are you sure you want to permanently remove ${member.name} (${member.email}) from the company roster? This account will be completely deleted.",
                    color = GrovixTextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirm = false
                        onRemoveUser()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixError)
                ) {
                    Text("Delete Member", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Cancel", color = GrovixTextMuted)
                }
            },
            containerColor = GrovixSurface
        )
    }
}

