package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.data.TimeRange
import com.example.grovix.ui.theme.*
import java.util.Locale

@Composable
fun ReportsScreen() {
    val allSales by GrovixRepository.sales.collectAsState()
    val allExpenses by GrovixRepository.expenses.collectAsState()

    var selectedRange by remember { mutableStateOf(TimeRange.THIS_MONTH) }

    val sales = allSales.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }
    val expenses = allExpenses.filter { GrovixRepository.isDateWithinRange(it.date, selectedRange) }

    val totalRevenue = sales.sumOf { it.total }
    val totalExpenses = expenses.sumOf { it.amount }
    val netProfit = totalRevenue - totalExpenses
    val margin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0

    // Grouping by product
    val productRevenue = sales.groupBy { it.productService }
        .mapValues { entry -> entry.value.sumOf { it.total } }
        .toList()
        .sortedByDescending { it.second }

    // Grouping by expense category
    val categoryExpenses = expenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(GrovixBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Profit & Loss Executive Summary",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GrovixTextDark
            )
        }

        // Time Range Filter Chips
        item {
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
        }

        // Summary cards in INR (₹)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CurrencyRupee,
                        label = "Gross Revenue",
                        value = "₹${String.format(Locale.US, "%,.0f", totalRevenue)}",
                        iconColor = GrovixNavy
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.TrendingDown,
                        label = "Total Outflows",
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
                        label = "Net Earnings",
                        value = "₹${String.format(Locale.US, "%,.0f", netProfit)}",
                        iconColor = if (netProfit >= 0) GrovixSuccess else GrovixError
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Percent,
                        label = "Profit Margin",
                        value = "${String.format(Locale.US, "%.1f", margin)}%",
                        iconColor = GrovixAccent
                    )
                }
            }
        }

        // Revenue by Product / Service
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Revenue by Product / Service (${selectedRange.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrovixTextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (productRevenue.isEmpty()) {
                        Text("No sales data recorded in this period", fontSize = 12.sp, color = GrovixTextMuted)
                    } else {
                        productRevenue.forEach { (prod, rev) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(prod, fontSize = 12.sp, color = GrovixTextDark, modifier = Modifier.weight(1f))
                                Text(
                                    "₹${String.format(Locale.US, "%,.0f", rev)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrovixNavy
                                )
                            }
                            HorizontalDivider(color = GrovixSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Expenses by Category
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = GrovixSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Disbursements by Category (${selectedRange.label})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GrovixTextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (categoryExpenses.isEmpty()) {
                        Text("No expense data recorded in this period", fontSize = 12.sp, color = GrovixTextMuted)
                    } else {
                        categoryExpenses.forEach { (cat, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 12.sp, color = GrovixTextDark, modifier = Modifier.weight(1f))
                                Text(
                                    "₹${String.format(Locale.US, "%,.0f", amt)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GrovixError
                                )
                            }
                            HorizontalDivider(color = GrovixSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
