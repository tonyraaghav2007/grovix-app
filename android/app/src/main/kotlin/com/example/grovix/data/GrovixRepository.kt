package com.example.grovix.data

import android.content.Context
import com.example.grovix.api.GrovixApiClient
import com.example.grovix.model.*
import com.example.grovix.util.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

enum class TimeRange(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THREE_MONTHS("3 Months"),
    SIX_MONTHS("6 Months")
}

enum class CloudStatus(val label: String) {
    CONNECTING("Connecting to Cloud..."),
    CONNECTED("Connected to Grovix Cloud"),
    SYNCING("Synchronizing..."),
    OFFLINE("Offline Mode"),
    ERROR("Connection Error")
}

object GrovixRepository {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.US)

    var appContext: Context? = null
    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Central Cloud Synchronization State
    private val _cloudStatus = MutableStateFlow(CloudStatus.OFFLINE)
    val cloudStatus: StateFlow<CloudStatus> = _cloudStatus.asStateFlow()

    private val _cloudStatusMessage = MutableStateFlow("Ready")
    val cloudStatusMessage: StateFlow<String> = _cloudStatusMessage.asStateFlow()

    private val _serverUrl = MutableStateFlow(GrovixApiClient.defaultBaseUrl)
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String?>(null)
    val lastSyncTime: StateFlow<String?> = _lastSyncTime.asStateFlow()

    // Session State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Users
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    // Tasks
    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks.asStateFlow()

    // Sales
    private val _sales = MutableStateFlow<List<Sale>>(emptyList())
    val sales: StateFlow<List<Sale>> = _sales.asStateFlow()

    // Expenses
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    // Inventory
    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    // Activity Log
    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    // Notifications
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    // Settings (INR by default)
    private val _settings = MutableStateFlow(CompanySettings(currency = "INR (₹)"))
    val settings: StateFlow<CompanySettings> = _settings.asStateFlow()

    init {
        seedDefaults()
    }

    fun init(context: Context) {
        appContext = context
        GrovixApiClient.init(context)
        _serverUrl.value = GrovixApiClient.baseUrl
        checkCloudConnection()
    }

    fun setServerUrl(newUrl: String) {
        GrovixApiClient.setServerUrl(newUrl)
        _serverUrl.value = GrovixApiClient.baseUrl
        checkCloudConnection()
    }

    fun checkCloudConnection(onComplete: ((Boolean) -> Unit)? = null) {
        repoScope.launch {
            _cloudStatus.value = CloudStatus.CONNECTING
            _cloudStatusMessage.value = "Connecting to Grovix Cloud..."
            val healthRes = GrovixApiClient.checkHealth()
            if (healthRes.isSuccess && healthRes.getOrNull() == true) {
                _cloudStatus.value = CloudStatus.CONNECTED
                _cloudStatusMessage.value = "Connected to Grovix Cloud"
                withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                // If already logged in, trigger cloud sync
                if (_currentUser.value != null && GrovixApiClient.authToken != null) {
                    syncWithCloud()
                }
            } else {
                _cloudStatus.value = CloudStatus.OFFLINE
                val err = healthRes.exceptionOrNull()?.localizedMessage ?: "Server unreachable"
                _cloudStatusMessage.value = "Offline Mode ($err)"
                withContext(Dispatchers.Main) { onComplete?.invoke(false) }
            }
        }
    }

    fun syncWithCloud(onComplete: ((Boolean, String) -> Unit)? = null) {
        repoScope.launch {
            _cloudStatus.value = CloudStatus.SYNCING
            _cloudStatusMessage.value = "Syncing with Grovix Cloud..."
            val result = GrovixApiClient.fetchSyncData()
            result.fold(
                onSuccess = { payload ->
                    if (payload.tasks.isNotEmpty()) _tasks.value = payload.tasks
                    if (payload.sales.isNotEmpty()) _sales.value = payload.sales
                    if (payload.expenses.isNotEmpty()) _expenses.value = payload.expenses
                    if (payload.users.isNotEmpty()) _users.value = payload.users
                    if (payload.notifications.isNotEmpty()) _notifications.value = payload.notifications

                    _cloudStatus.value = CloudStatus.CONNECTED
                    val timeStr = timeFormat.format(Date())
                    _lastSyncTime.value = timeStr
                    _cloudStatusMessage.value = "Synced with Grovix Cloud at $timeStr"
                    logActivity("System", "system", "CLOUD_SYNC", "Synchronized data with Grovix Central Cloud.")
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(true, "Cloud sync successful")
                    }
                },
                onFailure = { err ->
                    _cloudStatus.value = CloudStatus.OFFLINE
                    val msg = err.localizedMessage ?: "Sync error"
                    _cloudStatusMessage.value = "Offline Cache ($msg)"
                    withContext(Dispatchers.Main) {
                        onComplete?.invoke(false, msg)
                    }
                }
            )
        }
    }

    private fun seedDefaults() {
        val defaultUsers = listOf(
            User(id = "u1", name = "Shivesh Garg", email = "shivesh@grovix.com", password = "Grovix@2024", role = "owner", powerLevel = 100, phone = "+91-98100-11111"),
            User(id = "u2", name = "Sushant Singh", email = "sushant@grovix.com", password = "Grovix@2024", role = "owner", powerLevel = 100, phone = "+91-98100-22222"),
            User(id = "u3", name = "Yuvik", email = "yuvik@grovix.com", password = "Grovix@2024", role = "owner", powerLevel = 100, phone = "+91-98100-33333"),
            User(id = "u4", name = "Operations Manager", email = "manager@grovix.com", password = "Grovix@2024", role = "manager", powerLevel = 75, phone = "+91-98100-44444"),
            User(id = "u_dev", name = "Full-Stack Developer", email = "developer@grovix.com", password = "Grovix@2024", role = "employee", powerLevel = 50, phone = "+91-98100-55555"),
            User(id = "u5", name = "Staff Associate", email = "employee@grovix.com", password = "Grovix@2024", role = "employee", powerLevel = 50, phone = "+91-98100-66666"),
            User(id = "u6", name = "Guest Auditor", email = "guest@grovix.com", password = "Grovix@2024", role = "guest", powerLevel = 25)
        )
        _users.value = defaultUsers

        val today = dateFormat.format(Date())

        val defaultTasks = listOf(
            TaskItem(
                title = "REST API Integration & Authentication",
                description = "Connect Android Compose client with backend endpoints and secure token storage",
                detailedNotes = "**Implementation Checklist:**\n• Implement HTTP client wrapper with Bearer token injection\n• Handle auto-refresh on 401 token expiration\n• <u>Validate error boundaries</u> across offline state\n\nEnsure test coverage for edge network drops.",
                assignedTo = "u_dev",
                assignedToName = "Full-Stack Developer",
                assignedBy = "u1",
                status = "in-progress",
                priority = "urgent",
                dueDate = today
            ),
            TaskItem(
                title = "Push Notification Reminders & Alerts",
                description = "Remind employees of tasks due in 1 hour, 30 minutes, 10 minutes",
                detailedNotes = "**Objectives:**\n• Create high-priority NotificationChannel\n• Handle permission checks for Android 13+\n• <u>Schedule periodic checks</u> for task due warnings\n• Notify on team mentions and task assignments.",
                assignedTo = "u_dev",
                assignedToName = "Full-Stack Developer",
                assignedBy = "u2",
                status = "todo",
                priority = "high",
                dueDate = "2026-11-10"
            ),
            TaskItem(
                title = "Database Index Optimization & Query Tuning",
                description = "Optimize queries for fast reporting and sorting across date ranges",
                detailedNotes = "**Task Scope:**\nAnalyze execution plan for date range queries.\n\n• Add compound index on `(created_at, category)`\n• Cache aggregations for 3-month and 6-month summaries.",
                assignedTo = "u_dev",
                assignedToName = "Full-Stack Developer",
                assignedBy = "u3",
                status = "delayed",
                priority = "medium",
                dueDate = "2026-11-15"
            ),
            TaskItem(
                title = "Client Onboarding Pipeline",
                description = "Set up automated onboarding flows for new enterprise accounts",
                detailedNotes = "Coordinate with sales team to finalize sign-off requirements.",
                assignedTo = "u4",
                assignedToName = "Operations Manager",
                assignedBy = "u1",
                status = "todo",
                priority = "high",
                dueDate = "2026-11-20"
            ),
            TaskItem(
                title = "Review Monthly Invoices",
                description = "Audit recent transactions and generate reconciliation report",
                detailedNotes = "Verify GST compliance and reconciliation report for accounting.",
                assignedTo = "u5",
                assignedToName = "Staff Associate",
                assignedBy = "u4",
                status = "completed",
                priority = "low",
                dueDate = "2026-11-05"
            )
        )
        _tasks.value = defaultTasks

        // Sample Sales in INR (₹)
        val defaultSales = listOf(
            Sale(productService = "Enterprise Cloud License", quantity = 2, price = 375000.0, client = "Apex Solutions Pvt Ltd", date = today, notes = "Annual corporate license", createdByName = "Operations Manager"),
            Sale(productService = "Custom Integration Service", quantity = 1, price = 265000.0, client = "Nordic Logistics India", date = today, notes = "Payment gateway & ERP link", createdByName = "Operations Manager"),
            Sale(productService = "Hardware Terminal Pro", quantity = 5, price = 65000.0, client = "Metro Retail Networks", date = "2026-11-04", notes = "POS terminal units", createdByName = "Operations Manager"),
            Sale(productService = "Maintenance AMC Q3", quantity = 1, price = 180000.0, client = "Horizon Infra", date = "2026-10-15", notes = "Quarterly support contract", createdByName = "Operations Manager"),
            Sale(productService = "Security Audit Suite", quantity = 1, price = 420000.0, client = "Nexus Fintech", date = "2026-09-10", notes = "Half-year audit", createdByName = "Operations Manager")
        )
        _sales.value = defaultSales

        // Sample Expenses in INR (₹)
        val defaultExpenses = listOf(
            Expense(category = "Cloud & Hosting", amount = 65000.0, date = today, notes = "AWS & Cloud infrastructure", createdByName = "Operations Manager"),
            Expense(category = "Office & Utilities", amount = 95000.0, date = "2026-11-01", notes = "Office rent and high-speed leased line", createdByName = "Operations Manager"),
            Expense(category = "Marketing & Advertising", amount = 35000.0, date = "2026-11-03", notes = "Digital campaigns", createdByName = "Operations Manager"),
            Expense(category = "Software Subscriptions", amount = 48000.0, date = "2026-10-12", notes = "Dev tools and JetBrains licenses", createdByName = "Operations Manager"),
            Expense(category = "Hardware & Equipment", amount = 120000.0, date = "2026-08-25", notes = "New developer workstations", createdByName = "Operations Manager")
        )
        _expenses.value = defaultExpenses

        val defaultInventory = listOf(
            InventoryItem(itemName = "POS Smart Terminal V3", sku = "SKU-TRM-001", quantity = 45, unitPrice = 24999.0, supplier = "TechEquip India", reorderThreshold = 15),
            InventoryItem(itemName = "Thermal Receipt Rolls (Box of 50)", sku = "SKU-PPR-010", quantity = 8, unitPrice = 3500.0, supplier = "PaperWorks Delhi", reorderThreshold = 12),
            InventoryItem(itemName = "Barcode Handheld Scanner 2D", sku = "SKU-SCN-042", quantity = 24, unitPrice = 7500.0, supplier = "Global Electronics", reorderThreshold = 10),
            InventoryItem(itemName = "RFID Smart Cards (Pack of 100)", sku = "SKU-CRD-100", quantity = 5, unitPrice = 9500.0, supplier = "SecureTech Bangalore", reorderThreshold = 10)
        )
        _inventory.value = defaultInventory

        // Seed initial notifications
        _notifications.value = listOf(
            AppNotification(
                title = "Welcome to Grovix",
                message = "Your workspace is ready. Check your assigned tasks.",
                type = "system",
                targetUserId = "u_dev",
                timestamp = "09:00"
            ),
            AppNotification(
                title = "⏰ Task Due Soon",
                message = "'REST API Integration & Authentication' is due today. Please update status.",
                type = "task_due",
                targetUserId = "u_dev",
                timestamp = "09:01"
            )
        )

        logActivity("System", "system", "SYSTEM_INIT", "Grovix platform initialized with INR (₹) currency.")
    }

    // Date range filter utility
    fun isDateWithinRange(dateStr: String, range: TimeRange): Boolean {
        if (dateStr.isBlank()) return true
        return try {
            val date = dateFormat.parse(dateStr) ?: return true
            val cal = Calendar.getInstance()
            val now = cal.time

            cal.time = now
            when (range) {
                TimeRange.TODAY -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    !date.before(cal.time)
                }
                TimeRange.THIS_WEEK -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    !date.before(cal.time)
                }
                TimeRange.THIS_MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    !date.before(cal.time)
                }
                TimeRange.THREE_MONTHS -> {
                    cal.add(Calendar.MONTH, -3)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    !date.before(cal.time)
                }
                TimeRange.SIX_MONTHS -> {
                    cal.add(Calendar.MONTH, -6)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    !date.before(cal.time)
                }
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun login(email: String, pass: String): Result<User> {
        val normalizedEmail = when (email.trim().lowercase()) {
            "director1@grovix.com" -> "shivesh@grovix.com"
            "director2@grovix.com" -> "sushant@grovix.com"
            "director3@grovix.com" -> "yuvik@grovix.com"
            "dev@grovix.com" -> "developer@grovix.com"
            else -> email.trim().lowercase()
        }

        // Try Online Central Cloud Login First
        val onlineRes = GrovixApiClient.login(normalizedEmail, pass)
        if (onlineRes.isSuccess) {
            val (user, _) = onlineRes.getOrThrow()
            _currentUser.value = user
            _cloudStatus.value = CloudStatus.CONNECTED
            _cloudStatusMessage.value = "Connected to Grovix Cloud"
            logActivity(user.name, user.role, "LOGIN", "Signed in via Central Cloud API.")
            syncWithCloud()
            return Result.success(user)
        }

        // If Cloud was unreachable, fallback to local cached authentication
        val localUser = _users.value.find { it.email.equals(normalizedEmail, ignoreCase = true) }
        if (localUser != null) {
            if (localUser.status != "active") {
                return Result.failure(Exception("Account is deactivated. Contact an Owner."))
            }
            if (localUser.password != pass) {
                return Result.failure(Exception("Invalid password. Check credentials."))
            }

            _currentUser.value = localUser
            _cloudStatus.value = CloudStatus.OFFLINE
            val errMsg = onlineRes.exceptionOrNull()?.message ?: "Cloud unreachable"
            _cloudStatusMessage.value = "Offline Mode ($errMsg)"
            logActivity(localUser.name, localUser.role, "LOGIN_OFFLINE", "Signed in using local offline credentials.")
            return Result.success(localUser)
        }

        val err = onlineRes.exceptionOrNull()?.message ?: "Authentication failed."
        return Result.failure(Exception(err))
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            logActivity(user.name, user.role, "LOGOUT", "User ${user.name} signed out.")
        }
        _currentUser.value = null
        GrovixApiClient.clearAuth()
    }

    fun changePassword(oldPass: String, newPass: String): Result<Unit> {
        val user = _currentUser.value ?: return Result.failure(Exception("Not signed in"))
        if (user.password != oldPass) {
            return Result.failure(Exception("Current password does not match"))
        }
        if (newPass.length < 6) {
            return Result.failure(Exception("New password must be at least 6 characters"))
        }

        val updated = user.copy(password = newPass)
        _currentUser.value = updated
        _users.value = _users.value.map { if (it.id == user.id) updated else it }
        logActivity(user.name, user.role, "PASSWORD_CHANGE", "User updated account password.")
        return Result.success(Unit)
    }

    fun createAccount(name: String, email: String, pass: String, role: String, phone: String?): Result<User> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can create accounts"))
        }

        val power = when (role.lowercase()) {
            "owner" -> 100
            "manager" -> 75
            "employee" -> 50
            "guest" -> 25
            else -> 50
        }

        if (actor.powerLevel < 100 && power >= actor.powerLevel) {
            return Result.failure(Exception("Managers can only create Employee and Guest accounts"))
        }

        if (_users.value.any { it.email.equals(email.trim(), ignoreCase = true) }) {
            return Result.failure(Exception("An account with this email already exists"))
        }

        val newUser = User(
            name = name.trim(),
            email = email.trim().lowercase(),
            password = pass,
            role = role.lowercase(),
            powerLevel = power,
            phone = phone?.takeIf { it.isNotBlank() }
        )

        _users.value = _users.value + newUser
        logActivity(actor.name, actor.role, "CREATE_USER", "Created ${newUser.role} account for ${newUser.name} (${newUser.email})")

        // Sync to cloud
        repoScope.launch {
            GrovixApiClient.createUser(name, email, pass, role, phone)
        }

        return Result.success(newUser)
    }

    fun toggleUserStatus(targetUserId: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.id == targetUserId) {
            return Result.failure(Exception("Cannot deactivate your own account"))
        }

        val target = _users.value.find { it.id == targetUserId }
            ?: return Result.failure(Exception("User not found"))

        // Founders cannot deactivate each other
        if (target.powerLevel >= 100 || target.role.equals("owner", ignoreCase = true)) {
            return Result.failure(Exception("Founders and Directors cannot be deactivated."))
        }

        if (actor.powerLevel < 100 && target.powerLevel >= actor.powerLevel) {
            return Result.failure(Exception("Cannot change status of users with equal or higher power level"))
        }

        val newStatus = if (target.status == "active") "inactive" else "active"
        val updated = target.copy(status = newStatus)
        _users.value = _users.value.map { if (it.id == targetUserId) updated else it }
        logActivity(actor.name, actor.role, "STATUS_CHANGE", "Changed status of ${target.name} to $newStatus")

        // Sync to cloud
        repoScope.launch {
            GrovixApiClient.deleteUser(targetUserId, permanent = false)
        }

        return Result.success(Unit)
    }

    fun removeUser(targetUserId: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.id == targetUserId) {
            return Result.failure(Exception("Cannot remove your own account"))
        }

        val target = _users.value.find { it.id == targetUserId }
            ?: return Result.failure(Exception("User not found"))

        // Founders cannot be removed by anyone
        if (target.powerLevel >= 100 || target.role.equals("owner", ignoreCase = true)) {
            return Result.failure(Exception("Founders and Directors cannot be removed."))
        }

        if (actor.powerLevel < 100 && target.powerLevel >= actor.powerLevel) {
            return Result.failure(Exception("Cannot remove users with equal or higher power level"))
        }

        _users.value = _users.value.filter { it.id != targetUserId }
        _tasks.value = _tasks.value.map {
            if (it.assignedTo == targetUserId) it.copy(assignedTo = null, assignedToName = "Unassigned") else it
        }
        logActivity(actor.name, actor.role, "USER_REMOVED", "Permanently removed team member: ${target.name}")

        // Sync to cloud
        repoScope.launch {
            GrovixApiClient.deleteUser(targetUserId, permanent = true)
        }

        return Result.success(Unit)
    }

    // Task Management
    fun addTask(
        title: String,
        desc: String,
        detailedNotes: String = "",
        assigneeId: String?,
        priority: String,
        dueDate: String
    ): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can assign tasks"))
        }

        val assignee = _users.value.find { it.id == assigneeId }

        val newTask = TaskItem(
            title = title.trim(),
            description = desc.trim(),
            detailedNotes = detailedNotes.trim(),
            assignedTo = assigneeId,
            assignedToName = assignee?.name ?: "Unassigned",
            assignedBy = actor.id,
            status = "todo",
            priority = priority.lowercase(),
            dueDate = dueDate
        )

        _tasks.value = listOf(newTask) + _tasks.value
        logActivity(actor.name, actor.role, "CREATE_TASK", "Created task: ${newTask.title} (Assigned to: ${newTask.assignedToName})")

        // Trigger local notification to assignee
        if (!assigneeId.isNullOrBlank()) {
            addNotification(
                title = "New Task Assigned",
                message = "You have been assigned: '$title' by ${actor.name}. Due: $dueDate",
                type = "task_assigned",
                targetUserId = assigneeId
            )
        }

        // Cloud persistence
        repoScope.launch {
            val res = GrovixApiClient.createTask(title, desc, detailedNotes, assigneeId, priority, dueDate)
            res.onSuccess { remoteTask ->
                // Update local task with remote id
                _tasks.value = _tasks.value.map { if (it.id == newTask.id) remoteTask else it }
            }
        }

        return Result.success(Unit)
    }

    fun updateTaskStatus(taskId: String, newStatus: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        val task = _tasks.value.find { it.id == taskId } ?: return Result.failure(Exception("Task not found"))

        // Employees can update their own tasks; managers/owners can update any
        if (actor.powerLevel < 75 && task.assignedTo != actor.id) {
            return Result.failure(Exception("You can only update tasks assigned to you"))
        }

        val validStatus = when (newStatus.lowercase()) {
            "todo" -> "todo"
            "in-progress", "inprogress" -> "in-progress"
            "delayed" -> "delayed"
            "completed" -> "completed"
            else -> "todo"
        }

        _tasks.value = _tasks.value.map {
            if (it.id == taskId) it.copy(status = validStatus) else it
        }
        logActivity(actor.name, actor.role, "UPDATE_TASK", "Task '${task.title}' marked as ${validStatus.uppercase()}")

        // Notify creator if different from actor
        if (task.assignedBy.isNotBlank() && task.assignedBy != actor.id) {
            addNotification(
                title = "Task Status Updated: ${task.title}",
                message = "${actor.name} updated status to ${validStatus.uppercase()}.",
                type = "status_update",
                targetUserId = task.assignedBy
            )
        }

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.updateTaskStatus(taskId, validStatus)
        }

        return Result.success(Unit)
    }

    fun deleteTask(taskId: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can remove tasks"))
        }

        val task = _tasks.value.find { it.id == taskId } ?: return Result.failure(Exception("Task not found"))
        _tasks.value = _tasks.value.filter { it.id != taskId }
        logActivity(actor.name, actor.role, "DELETE_TASK", "Removed task: ${task.title}")

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.deleteTask(taskId)
        }

        return Result.success(Unit)
    }

    // Sales Management
    fun addSale(productService: String, quantity: Int, price: Double, client: String, date: String, notes: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Managers and Owners can record sales"))
        }

        val newSale = Sale(
            productService = productService.trim(),
            quantity = quantity,
            price = price,
            total = quantity * price,
            client = client.trim(),
            date = date.ifBlank { dateFormat.format(Date()) },
            notes = notes.trim(),
            createdByName = actor.name
        )

        _sales.value = listOf(newSale) + _sales.value
        logActivity(actor.name, actor.role, "ADD_SALE", "Recorded sale: ${newSale.productService} (₹${newSale.total})")

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.createSale(productService, quantity, price, client, date, notes)
        }

        return Result.success(Unit)
    }

    fun deleteSale(saleId: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can remove sales"))
        }

        val sale = _sales.value.find { it.id == saleId } ?: return Result.failure(Exception("Sale not found"))
        _sales.value = _sales.value.filter { it.id != saleId }
        logActivity(actor.name, actor.role, "DELETE_SALE", "Removed sale: ${sale.productService}")

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.deleteSale(saleId)
        }

        return Result.success(Unit)
    }

    // Expense Management
    fun addExpense(category: String, amount: Double, date: String, notes: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Managers and Owners can record expenses"))
        }

        val newExpense = Expense(
            category = category,
            amount = amount,
            date = date.ifBlank { dateFormat.format(Date()) },
            notes = notes.trim(),
            createdByName = actor.name
        )

        _expenses.value = listOf(newExpense) + _expenses.value
        logActivity(actor.name, actor.role, "ADD_EXPENSE", "Logged expense: ${newExpense.category} (₹${newExpense.amount})")

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.createExpense(category, amount, date, notes)
        }

        return Result.success(Unit)
    }

    fun deleteExpense(expenseId: String): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can remove expenses"))
        }

        val exp = _expenses.value.find { it.id == expenseId } ?: return Result.failure(Exception("Expense not found"))
        _expenses.value = _expenses.value.filter { it.id != expenseId }
        logActivity(actor.name, actor.role, "DELETE_EXPENSE", "Removed expense: ${exp.category}")

        // Cloud persistence
        repoScope.launch {
            GrovixApiClient.deleteExpense(expenseId)
        }

        return Result.success(Unit)
    }

    // Inventory Management
    fun addInventoryItem(name: String, sku: String, quantity: Int, price: Double, supplier: String, threshold: Int): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can add inventory items"))
        }

        val newItem = InventoryItem(
            itemName = name.trim(),
            sku = sku.trim().uppercase(),
            quantity = quantity,
            unitPrice = price,
            supplier = supplier.trim(),
            reorderThreshold = threshold
        )

        _inventory.value = _inventory.value + newItem
        logActivity(actor.name, actor.role, "ADD_INVENTORY", "Added inventory: ${newItem.itemName} (${newItem.quantity} units)")
        return Result.success(Unit)
    }

    fun updateInventoryItem(item: InventoryItem): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 75) {
            return Result.failure(Exception("Only Owners and Managers can update inventory"))
        }

        _inventory.value = _inventory.value.map { if (it.id == item.id) item else it }
        logActivity(actor.name, actor.role, "UPDATE_INVENTORY", "Updated stock for ${item.itemName}: ${item.quantity} units")
        return Result.success(Unit)
    }

    fun updateSettings(settings: CompanySettings): Result<Unit> {
        val actor = _currentUser.value ?: return Result.failure(Exception("Authentication required"))
        if (actor.powerLevel < 100) {
            return Result.failure(Exception("Only Owners can modify company preferences"))
        }

        _settings.value = settings
        logActivity(actor.name, actor.role, "UPDATE_SETTINGS", "Updated company parameters: ${settings.companyName}")
        return Result.success(Unit)
    }

    // Notifications
    fun addNotification(title: String, message: String, type: String, targetUserId: String?) {
        val notif = AppNotification(
            title = title,
            message = message,
            type = type,
            targetUserId = targetUserId,
            isRead = false,
            timestamp = timeFormat.format(Date())
        )
        _notifications.value = listOf(notif) + _notifications.value

        // Trigger native Android status-bar notification
        appContext?.let { ctx ->
            NotificationHelper.sendPushNotification(ctx, title, message)
        }

        // Send to cloud if targeting user
        if (!targetUserId.isNullOrBlank()) {
            repoScope.launch {
                GrovixApiClient.sendNotification(targetUserId, message, type)
            }
        }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        repoScope.launch {
            GrovixApiClient.markNotificationRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        _notifications.value = _notifications.value.map { it.copy(isRead = true) }
        repoScope.launch {
            GrovixApiClient.markAllNotificationsRead()
        }
    }

    fun triggerDueReminders() {
        val user = _currentUser.value ?: return
        val userTasks = _tasks.value.filter { it.assignedTo == user.id && it.status != "completed" }
        val targetTask = userTasks.firstOrNull() ?: _tasks.value.firstOrNull() ?: return

        addNotification(
            title = "⏰ Task Due in 1 Hour",
            message = "Reminder: '${targetTask.title}' is due in 1 hour. Please prepare deliverables.",
            type = "task_due",
            targetUserId = user.id
        )
        addNotification(
            title = "⚠️ Task Due in 30 Minutes",
            message = "Approaching deadline: '${targetTask.title}' has 30 minutes remaining.",
            type = "task_due",
            targetUserId = user.id
        )
        addNotification(
            title = "🚨 URGENT: Due in 10 Minutes",
            message = "CRITICAL: Final 10 minutes to complete work on '${targetTask.title}'!",
            type = "task_due",
            targetUserId = user.id
        )
    }

    fun triggerMention(targetUserId: String, taskTitle: String, comment: String) {
        val actor = _currentUser.value ?: return
        addNotification(
            title = "Mentioned by ${actor.name}",
            message = "${actor.name} mentioned you on '$taskTitle': \"$comment\"",
            type = "mention",
            targetUserId = targetUserId
        )
    }

    private fun logActivity(userName: String, role: String, action: String, details: String) {
        val log = ActivityLog(
            userName = userName,
            userRole = role,
            action = action,
            details = details,
            timestamp = timeFormat.format(Date())
        )
        _activityLogs.value = listOf(log) + _activityLogs.value.take(49)
    }
}
