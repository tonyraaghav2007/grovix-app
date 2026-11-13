package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.model.AppNotification
import com.example.grovix.ui.theme.*

@Composable
fun NotificationsScreen() {
    val currentUser by GrovixRepository.currentUser.collectAsState()
    val allNotifications by GrovixRepository.notifications.collectAsState()

    // Filter notifications for this user or broadcast
    val userNotifications = allNotifications.filter {
        it.targetUserId == null || it.targetUserId == currentUser?.id || currentUser?.isOwner == true
    }

    val unreadCount = userNotifications.count { !it.isRead }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrovixBackground)
            .padding(16.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Notifications & Reminders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = GrovixTextDark
                )
                Text(
                    text = if (unreadCount > 0) "$unreadCount unread alerts" else "All caught up",
                    fontSize = 12.sp,
                    color = if (unreadCount > 0) GrovixWarning else GrovixTextMuted
                )
            }

            if (unreadCount > 0) {
                TextButton(onClick = { GrovixRepository.markAllNotificationsAsRead() }) {
                    Text("Mark Read", color = GrovixNavy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Push Notification Simulation Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = GrovixSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Test Push Notifications (Real-Time Triggers)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = GrovixNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { GrovixRepository.triggerDueReminders() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Due Reminders", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val user = currentUser ?: return@Button
                            GrovixRepository.triggerMention(user.id, "REST API Integration", "Please review PR #12 when ready!")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixIndigo),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Team Mention", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (userNotifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No notifications at this time", color = GrovixTextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(userNotifications) { notif ->
                    NotificationCardItem(
                        notification = notif,
                        onMarkRead = { GrovixRepository.markNotificationAsRead(notif.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    notification: AppNotification,
    onMarkRead: () -> Unit
) {
    val icon = when (notification.type) {
        "task_due" -> Icons.Default.Timer
        "task_assigned" -> Icons.Default.Assignment
        "mention" -> Icons.Default.AlternateEmail
        "status_update" -> Icons.Default.CheckCircle
        else -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        "task_due" -> if (notification.title.contains("10 Minutes")) GrovixError else GrovixWarning
        "task_assigned" -> GrovixNavy
        "mention" -> GrovixInfo
        "status_update" -> GrovixSuccess
        else -> GrovixNavy
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMarkRead() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (notification.isRead) GrovixSurface else GrovixSurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = GrovixTextDark
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GrovixWarning)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = if (notification.isRead) GrovixTextMuted else GrovixTextDark,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

