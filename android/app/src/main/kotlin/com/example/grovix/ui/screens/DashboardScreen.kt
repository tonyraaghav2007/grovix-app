package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.data.TimeRange
import com.example.grovix.ui.theme.*
import java.util.Locale

@Composable
fun DashboardScreen(
    onNavigateToTasks: () -> Unit,
    onNavigateToSales: () -> Unit,
    onNavigateToExpenses: () -> Unit
) {
    val user by GrovixRepository.currentUser.collectAsState()
    val allSales by GrovixRepository.sales.collectAsState()
    val allExpenses by GrovixRepository.expenses.collectAsState()
    val allTasks by GrovixRepository.tasks.collectAsState()
    val activityLogs by GrovixRepository.activityLogs.collectAsState()

    var selectedRange by remember { mutableStateOf(TimeRange.THIS_MONTH) }

    val isEmployeeOnly = (user?.powerLevel ?: 0) <= 50

    // Filter financial data by date range for managers/owners
    val filteredSales = allSales.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }
    val filteredExpenses = allExpenses.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }

    val totalSales = filteredSales.sumOf { it.total }
    val totalExpenses = filteredExpenses.sumOf { it.amount }
    val profit = totalSales - totalExpenses

    // Tasks for developer/employee vs company
    val myTasks = allTasks.filter { it.assignedTo == user?.id || it.assignedToName.equals(user?.name, ignoreCase = true) }
    val pendingTasks = allTasks.count { it.status != "completed" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrovixBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Welcome, ${user?.name ?: "User"}",
                        color = GrovixTextDark,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${user?.role?.uppercase()} • Power Level ${user?.powerLevel ?: 0}",
                        color = GrovixNavy,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // DEVELOPER / EMPLOYEE VIEW (ONLY tasks and what to do, NO sales, NO expenses)
        if (isEmployeeOnly) {
            item {
                Text(
                    text = "My Task Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GrovixTextDark
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Assignment,
                            label = "Assigned To Me",
                            value = "${myTasks.size}",
                            iconColor = GrovixNavy
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.PendingActions,
                            label = "In Progress",
                            value = "${myTasks.count { it.status == "in-progress" }}",
                            iconColor = GrovixInfo
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.HourglassBottom,
                            label = "Delayed",
                            value = "${myTasks.count { it.status == "delayed" }}",
                            iconColor = GrovixWarning
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircle,
                            label = "Completed",
                            value = "${myTasks.count { it.status == "completed" }}",
                            iconColor = GrovixSuccess
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onNavigateToTasks,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Icon(Icons.Default.ListAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go to My Task Workspace", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            item {
                Text(
                    text = "My Active Tasks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GrovixTextDark,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(myTasks.filter { it.status != "completed" }) { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTasks() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(task.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GrovixTextDark, modifier = Modifier.weight(1f))
                            Surface(
                                color = if (task.status == "delayed") GrovixWarning.copy(alpha = 0.2f) else GrovixNavy.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = task.status.uppercase(),
                                    color = if (task.status == "delayed") GrovixWarning else GrovixNavy,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (task.dueDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Due: ${task.dueDate}", fontSize = 11.sp, color = GrovixTextMuted)
                        }
                    }
                }
            }

        } else {
            // MANAGER & OWNER VIEW (Financials in INR ₹, Date Sorting Chips, Full Metrics)

            // Date Range Sort Filter Chips Bar
            item {
                Column {
                    Text(
                        text = "Time Range Filter:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GrovixTextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimeRange.values().forEach { range ->
                            val isSelected = selectedRange == range
                            Surface(
                                modifier = Modifier.clickable { selectedRange = range },
                                color = if (isSelected) GrovixNavy else GrovixSurface,
                                shape = RoundedCornerShape(20.dp),
                                shadowElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Text(
                                    text = range.label,
                                    color = if (isSelected) Color.White else GrovixTextDark,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Stats Grid in INR (₹)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CurrencyRupee,
                            label = "Sales (${selectedRange.label})",
                            value = "₹${String.format(Locale.US, "%,.0f", totalSales)}",
                            iconColor = GrovixNavy
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TrendingDown,
                            label = "Expenses (${selectedRange.label})",
                            value = "₹${String.format(Locale.US, "%,.0f", totalExpenses)}",
                            iconColor = GrovixError
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TrendingUp,
                            label = "Net Profit",
                            value = "₹${String.format(Locale.US, "%,.0f", profit)}",
                            iconColor = if (profit >= 0) GrovixSuccess else GrovixError
                        )
                        MetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CheckCircleOutline,
                            label = "Pending Tasks",
                            value = "$pendingTasks",
                            iconColor = GrovixWarning
                        )
                    }
                }
            }

            // Quick Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GrovixSurface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Quick Actions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = GrovixNavy
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onNavigateToSales,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Sale", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }

                            Button(
                                onClick = onNavigateToTasks,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GrovixIndigo),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Task", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }

                            Button(
                                onClick = onNavigateToExpenses,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GrovixAccent),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Expense", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }
                    }
                }
            }

            // Recent Activity Feed Header
            item {
                Text(
                    text = "Recent Operations & Activity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GrovixTextDark,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Activity Log Items (No date logged)
            items(activityLogs.take(10)) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GrovixNavy)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${log.userName} (${log.userRole})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = GrovixNavy
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${log.action}: ${log.details}",
                                fontSize = 12.sp,
                                color = GrovixTextDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GrovixSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = label, fontSize = 12.sp, color = GrovixTextMuted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GrovixTextDark)
        }
    }
}
