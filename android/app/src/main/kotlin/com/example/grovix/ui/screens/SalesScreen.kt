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
import com.example.grovix.model.Sale
import com.example.grovix.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen() {
    val user by GrovixRepository.currentUser.collectAsState()
    val allSales by GrovixRepository.sales.collectAsState()

    var selectedRange by remember { mutableStateOf(TimeRange.THIS_MONTH) }
    var showAddDialog by remember { mutableStateOf(false) }

    var prodName by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var priceStr by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var notesStr by remember { mutableStateOf("") }

    val canManage = (user?.powerLevel ?: 0) >= 75
    val filteredSales = allSales.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }
    val totalRevenue = filteredSales.sumOf { it.total }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrovixNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Sale")
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
                    label = "Revenue (${selectedRange.label})",
                    value = "₹${String.format(Locale.US, "%,.0f", totalRevenue)}",
                    iconColor = GrovixNavy
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ShoppingBag,
                    label = "Transactions",
                    value = "${filteredSales.size}",
                    iconColor = GrovixSuccess
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
                text = "Sales Records (${filteredSales.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GrovixTextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sales records found for ${selectedRange.label}", color = GrovixTextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filteredSales) { sale ->
                        SaleCardItem(
                            sale = sale,
                            canManage = canManage,
                            onDelete = { GrovixRepository.deleteSale(sale.id) }
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
            title = { Text("Record New Sale (INR ₹)", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Product / Service Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price (₹) *") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client / Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = { Text("Notes / Invoice Ref") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityStr.toIntOrNull() ?: 1
                        val pr = priceStr.toDoubleOrNull() ?: 0.0
                        if (prodName.isNotBlank() && pr > 0) {
                            GrovixRepository.addSale(prodName, qty, pr, clientName, dateStr, notesStr)
                            prodName = ""
                            priceStr = ""
                            clientName = ""
                            notesStr = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Save Sale", color = Color.White)
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
fun SaleCardItem(
    sale: Sale,
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
                Text(
                    text = sale.productService,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GrovixTextDark,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "₹${String.format(Locale.US, "%,.0f", sale.total)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GrovixSuccess
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (sale.client.isNotBlank()) "Client: ${sale.client}" else "Standard Sale",
                    fontSize = 12.sp,
                    color = GrovixTextMuted
                )
                Text(
                    text = "${sale.quantity}x @ ₹${String.format(Locale.US, "%,.0f", sale.price)}",
                    fontSize = 12.sp,
                    color = GrovixTextMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recorded by: ${sale.createdByName}",
                    fontSize = 11.sp,
                    color = GrovixNavy
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
            title = { Text("Delete Sale", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = { Text("Are you sure you want to remove the sale for '${sale.productService}'? This cannot be undone.", color = GrovixTextDark) },
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
