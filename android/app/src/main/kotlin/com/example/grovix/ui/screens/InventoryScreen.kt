package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
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
import com.example.grovix.model.InventoryItem
import com.example.grovix.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    val user by GrovixRepository.currentUser.collectAsState()
    val inventory by GrovixRepository.inventory.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }

    var itemName by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }
    var qtyStr by remember { mutableStateOf("10") }
    var priceStr by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var thresholdStr by remember { mutableStateOf("10") }

    val canManage = user?.isManager == true
    val lowStockCount = inventory.count { it.isLowStock }
    val totalValue = inventory.sumOf { it.totalValue }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrovixNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item")
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
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Inventory2,
                    label = "Stock SKUs",
                    value = "${inventory.size}",
                    iconColor = GrovixNavy
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Warning,
                    label = "Low Stock Alerts",
                    value = "$lowStockCount",
                    iconColor = if (lowStockCount > 0) GrovixError else GrovixSuccess
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Warehouse Inventory & Stock",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = GrovixNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (inventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No inventory items found", color = GrovixTextMuted)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(inventory) { item ->
                        InventoryCardItem(
                            item = item,
                            canEdit = canManage,
                            onEdit = { editingItem = item }
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
            title = { Text("Add Stock Item", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Item Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("SKU / Barcode") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qtyStr,
                            onValueChange = { qtyStr = it },
                            label = { Text("Qty In Stock") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Unit Cost ($)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = thresholdStr,
                            onValueChange = { thresholdStr = it },
                            label = { Text("Reorder Min") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = supplier,
                            onValueChange = { supplier = it },
                            label = { Text("Supplier") },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = qtyStr.toIntOrNull() ?: 0
                        val p = priceStr.toDoubleOrNull() ?: 0.0
                        val th = thresholdStr.toIntOrNull() ?: 10
                        if (itemName.isNotBlank()) {
                            GrovixRepository.addInventoryItem(itemName, sku, q, p, supplier, th)
                            itemName = ""
                            sku = ""
                            priceStr = ""
                            supplier = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Add to Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (editingItem != null) {
        var editQtyStr by remember { mutableStateOf(editingItem!!.quantity.toString()) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            containerColor = GrovixSurface,
            title = { Text("Update Quantity: ${editingItem!!.itemName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GrovixTextDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current SKU: ${editingItem!!.sku.ifBlank { "N/A" }}", fontSize = 12.sp, color = GrovixTextMuted)
                    OutlinedTextField(
                        value = editQtyStr,
                        onValueChange = { editQtyStr = it },
                        label = { Text("Available Quantity") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newQ = editQtyStr.toIntOrNull() ?: editingItem!!.quantity
                        GrovixRepository.updateInventoryItem(editingItem!!.copy(quantity = newQ))
                        editingItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Save Quantity", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("Cancel", color = GrovixTextMuted)
                }
            }
        )
    }
}

@Composable
fun InventoryCardItem(
    item: InventoryItem,
    canEdit: Boolean,
    onEdit: () -> Unit
) {
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
                    text = item.itemName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = GrovixTextDark,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    color = if (item.isLowStock) GrovixError.copy(alpha = 0.15f) else GrovixSuccess.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (item.isLowStock) "LOW STOCK (Min: ${item.reorderThreshold})" else "IN STOCK",
                        color = if (item.isLowStock) GrovixError else GrovixSuccess,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SKU: ${item.sku.ifBlank { "—" }}",
                    fontSize = 12.sp,
                    color = GrovixTextMuted
                )
                Text(
                    text = "Stock: ${item.quantity} units",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (item.isLowStock) GrovixError else GrovixTextDark
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$${String.format(Locale.US, "%.2f", item.unitPrice)}/unit • Val: $${String.format(Locale.US, "%,.2f", item.totalValue)}",
                    fontSize = 11.sp,
                    color = GrovixTextMuted
                )

                if (canEdit) {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = GrovixNavy)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Update", fontSize = 11.sp, color = GrovixNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

