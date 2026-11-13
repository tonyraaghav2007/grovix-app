package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import com.example.grovix.data.CloudStatus
import com.example.grovix.data.GrovixRepository
import com.example.grovix.ui.theme.*
import kotlinx.coroutines.launch

enum class AppDestination(val title: String, val icon: ImageVector, val minPower: Int) {
    TASKS("Tasks", Icons.Default.CheckCircle, 0),
    NOTIFICATIONS("Alerts", Icons.Default.Notifications, 0),
    DASHBOARD("Dashboard", Icons.Default.Dashboard, 75),
    SALES("Sales", Icons.Default.CurrencyRupee, 75),
    EXPENSES("Expenses", Icons.AutoMirrored.Filled.TrendingDown, 75),
    INVENTORY("Inventory", Icons.Default.Inventory2, 75),
    TEAM("Team", Icons.Default.People, 75),
    REPORTS("Reports", Icons.Default.BarChart, 75),
    SETTINGS("Settings", Icons.Default.Settings, 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainerScreen(
    onLogout: () -> Unit
) {
    val currentUser by GrovixRepository.currentUser.collectAsState()
    val allNotifications by GrovixRepository.notifications.collectAsState()
    val cloudStatus by GrovixRepository.cloudStatus.collectAsState()

    val power = currentUser?.powerLevel ?: 0
    val isEmployeeOnly = power <= 50

    // Developers/employees default to TASKS; managers/owners default to DASHBOARD
    var currentDestination by remember {
        mutableStateOf(if (isEmployeeOnly) AppDestination.TASKS else AppDestination.DASHBOARD)
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allowedDestinations = AppDestination.values().filter { power >= it.minPower }
    val unreadNotifs = allNotifications.count { !it.isRead && (it.targetUserId == null || it.targetUserId == currentUser?.id) }

    val roleBadgeColor = when (currentUser?.role?.lowercase()) {
        "owner" -> RoleOwnerColor
        "manager" -> RoleManagerColor
        "employee" -> RoleEmployeeColor
        else -> RoleGuestColor
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = GrovixSurface,
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GrovixSurfaceVariant)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Grovix",
                        color = GrovixTextDark,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Business Management Platform",
                        color = GrovixTextMuted,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentUser?.name ?: "User",
                        color = GrovixTextDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Surface(
                        color = roleBadgeColor,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "${currentUser?.role?.uppercase()} • LVL $power",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation items
                allowedDestinations.forEach { dest ->
                    NavigationDrawerItem(
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (dest == AppDestination.NOTIFICATIONS && unreadNotifs > 0) {
                                        Badge(containerColor = GrovixWarning) { Text("$unreadNotifs") }
                                    }
                                }
                            ) {
                                Icon(
                                    dest.icon,
                                    contentDescription = null,
                                    tint = if (currentDestination == dest) GrovixNavy else GrovixTextMuted
                                )
                            }
                        },
                        label = {
                            Text(
                                dest.title,
                                color = if (currentDestination == dest) GrovixNavy else GrovixTextDark,
                                fontWeight = if (currentDestination == dest) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = currentDestination == dest,
                        onClick = {
                            currentDestination = dest
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = GrovixNavy.copy(alpha = 0.15f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                HorizontalDivider(color = GrovixSurfaceVariant)

                NavigationDrawerItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = GrovixError) },
                    label = { Text("Sign Out", color = GrovixError, fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        GrovixRepository.logout()
                        onLogout()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentDestination.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = GrovixTextDark
                            )
                            Text(
                                text = "${currentUser?.name} (${currentUser?.role})",
                                fontSize = 11.sp,
                                color = GrovixTextMuted
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = GrovixTextDark)
                        }
                    },
                    actions = {
                        // Central Cloud Sync Indicator & Quick Trigger
                        IconButton(onClick = {
                            GrovixRepository.syncWithCloud()
                        }) {
                            when (cloudStatus) {
                                CloudStatus.SYNCING, CloudStatus.CONNECTING -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = GrovixWarning
                                    )
                                }
                                CloudStatus.CONNECTED -> {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Cloud Connected",
                                        tint = GrovixSuccess
                                    )
                                }
                                else -> {
                                    Icon(
                                        Icons.Default.CloudOff,
                                        contentDescription = "Offline Mode (Tap to sync)",
                                        tint = GrovixTextMuted
                                    )
                                }
                            }
                        }

                        // Quick Notification Bell
                        IconButton(onClick = { currentDestination = AppDestination.NOTIFICATIONS }) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotifs > 0) {
                                        Badge(containerColor = GrovixWarning) { Text("$unreadNotifs") }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Alerts",
                                    tint = if (currentDestination == AppDestination.NOTIFICATIONS) GrovixNavy else GrovixTextDark
                                )
                            }
                        }

                        Surface(
                            color = roleBadgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "LVL $power",
                                color = roleBadgeColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = {
                            GrovixRepository.logout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = GrovixTextMuted)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GrovixSurface)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = GrovixSurface,
                    tonalElevation = 8.dp
                ) {
                    val bottomItems = if (isEmployeeOnly) {
                        // Developer / Employee: ONLY Tasks and Alerts!
                        listOf(
                            AppDestination.TASKS,
                            AppDestination.NOTIFICATIONS,
                            AppDestination.SETTINGS
                        )
                    } else {
                        // Manager / Owner: Full access
                        listOf(
                            AppDestination.DASHBOARD,
                            AppDestination.TASKS,
                            AppDestination.SALES,
                            AppDestination.EXPENSES,
                            AppDestination.NOTIFICATIONS
                        )
                    }

                    bottomItems.forEach { dest ->
                        NavigationBarItem(
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (dest == AppDestination.NOTIFICATIONS && unreadNotifs > 0) {
                                            Badge(containerColor = GrovixWarning) { Text("$unreadNotifs") }
                                        }
                                    }
                                ) {
                                    Icon(dest.icon, contentDescription = dest.title)
                                }
                            },
                            label = { Text(dest.title, fontSize = 11.sp) },
                            selected = currentDestination == dest,
                            onClick = { currentDestination = dest },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = GrovixNavy,
                                selectedTextColor = GrovixNavy,
                                indicatorColor = GrovixNavy.copy(alpha = 0.15f),
                                unselectedIconColor = GrovixTextMuted,
                                unselectedTextColor = GrovixTextMuted
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentDestination) {
                    AppDestination.TASKS -> TasksScreen()
                    AppDestination.NOTIFICATIONS -> NotificationsScreen()
                    AppDestination.DASHBOARD -> DashboardScreen(
                        onNavigateToTasks = { currentDestination = AppDestination.TASKS },
                        onNavigateToSales = { currentDestination = AppDestination.SALES },
                        onNavigateToExpenses = { currentDestination = AppDestination.EXPENSES }
                    )
                    AppDestination.SALES -> SalesScreen()
                    AppDestination.EXPENSES -> ExpensesScreen()
                    AppDestination.INVENTORY -> InventoryScreen()
                    AppDestination.TEAM -> TeamScreen()
                    AppDestination.REPORTS -> ReportsScreen()
                    AppDestination.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
