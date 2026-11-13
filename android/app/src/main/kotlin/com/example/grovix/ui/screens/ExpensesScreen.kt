package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.data.TimeRange
import com.example.grovix.model.Expense
import com.example.grovix.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen() {
    val user by GrovixRepository.currentUser.collectAsState()
    val allExpenses by GrovixRepository.expenses.collectAsState()

    var selectedRange by remember { mutableStateOf(TimeRange.THIS_MONTH) }
    var showAddDialog by remember { mutableStateOf(false) }

    val categories = listOf("Cloud & Hosting", "Office & Utilities", "Marketing & Advertising", "Software Subscriptions", "Hardware & Equipment", "Travel & Fuel", "Legal & Compliance", "Miscellaneous")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var amountStr by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var notesStr by remember { mutableStateOf("") }

    val canManage = (user?.powerLevel ?: 0) >= 75
    val filteredExpenses = allExpenses.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }
    val totalExpense = filteredExpenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrovixError,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Log Expense")
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
            // Header stats in INR (₹)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CurrencyRupee,
                    label = "Expenses (${selectedRange.label})",
                    value = "₹${String.format(Locale.US, "%,.0f", totalExpense)}",
                    iconColor = GrovixError
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Receipt,
                    label = "Disbursements",
                    value = "${filteredExpenses.size}",
                    iconColor = GrovixWarning
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Range Filter Chips
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
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = range.label,
                            color = if (isSelected) Color.White else GrovixTextDark,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Expense Records (${filteredExpenses.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GrovixTextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredExpenses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expense entries found for ${selectedRange.label}", color = GrovixTextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredExpenses) { exp ->
                        ExpenseCardItem(
                            exp = exp,
                            canManage = canManage,
                            onDelete = { GrovixRepository.deleteExpense(exp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = GrovixSurface,
            title = { Text("Log Corporate Expense (INR ₹)", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Category:", fontSize = 12.sp, color = GrovixTextMuted)
                    var catExpanded by remember { mutableStateOf(false) }

                    Box {
                        OutlinedButton(
                            onClick = { catExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedCategory)
                        }
                        DropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            categories.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c) },
                                    onClick = { selectedCategory = c; catExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount (₹) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = { Text("Justification / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            GrovixRepository.addExpense(selectedCategory, amt, dateStr, notesStr)
                            amountStr = ""
                            notesStr = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixError)
                ) {
                    Text("Save Expense", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = GrovixTextMuted)
                }
            }
        )
    }
}

@Composable
fun ExpenseCardItem(
    exp: Expense,
    canManage: Boolean,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

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
                Surface(
                    color = GrovixNavy.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = exp.category,
                        color = GrovixNavy,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = "₹${String.format(Locale.US, "%,.0f", exp.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GrovixError
                )
            }

            if (exp.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = exp.notes, fontSize = 12.sp, color = GrovixTextDark)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reported by: ${exp.createdByName}",
                    fontSize = 11.sp,
                    color = GrovixTextMuted
                )

                if (canManage) {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GrovixError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = GrovixSurface,
            title = { Text("Delete Expense", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = { Text("Are you sure you want to remove the expense entry for '${exp.category}'? This cannot be undone.", color = GrovixTextDark) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixError)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = GrovixTextMuted)
                }
            }
        )
    }
}
