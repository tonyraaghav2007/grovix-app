package com.example.grovix.model

import java.util.UUID

data class User(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val password: String = "Grovix@2024",
    val role: String, // owner, manager, employee, guest
    val powerLevel: Int, // 100, 75, 50, 25
    val status: String = "active", // active, inactive
    val phone: String? = null
) {
    val isOwner: Boolean get() = powerLevel >= 100
    val isManager: Boolean get() = powerLevel >= 75
    val isEmployee: Boolean get() = powerLevel >= 50
}

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val detailedNotes: String = "", // Formatted big note (bold, underline, bullets, paragraphs)
    val assignedTo: String? = null,
    val assignedToName: String? = null,
    val assignedBy: String,
    val status: String = "todo", // todo, in-progress, delayed, completed
    val priority: String = "medium", // urgent, high, medium, low
    val dueDate: String = ""
)

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: String = "info", // task_assigned, task_due, mention, system
    val targetUserId: String? = null,
    val isRead: Boolean = false,
    val timestamp: String = ""
)

data class Sale(
    val id: String = UUID.randomUUID().toString(),
    val productService: String,
    val quantity: Int = 1,
    val price: Double,
    val total: Double = quantity * price,
    val client: String = "",
    val date: String,
    val notes: String = "",
    val createdByName: String = "Staff"
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val amount: Double,
    val date: String,
    val notes: String = "",
    val createdByName: String = "Staff"
)

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val itemName: String,
    val sku: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val supplier: String = "",
    val reorderThreshold: Int = 10
) {
    val isLowStock: Boolean get() = quantity <= reorderThreshold
    val totalValue: Double get() = quantity * unitPrice
}

data class ActivityLog(
    val id: String = UUID.randomUUID().toString(),
    val userName: String,
    val userRole: String,
    val action: String,
    val details: String,
    val timestamp: String
)

data class CompanySettings(
    val companyName: String = "Grovix",
    val currency: String = "INR (₹)",
    val taxRate: Double = 18.0
)

data class SyncPayload(
    val user: User?,
    val tasks: List<TaskItem>,
    val sales: List<Sale>,
    val expenses: List<Expense>,
    val users: List<User>,
    val notifications: List<AppNotification>,
    val unreadNotifs: Int = 0,
    val currency: String = "INR (₹)"
)

