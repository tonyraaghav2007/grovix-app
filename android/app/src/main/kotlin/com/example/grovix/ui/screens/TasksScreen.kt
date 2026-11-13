package com.example.grovix.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grovix.data.GrovixRepository
import com.example.grovix.model.TaskItem
import com.example.grovix.ui.components.FormattedNoteViewer
import com.example.grovix.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen() {
    val currentUser by GrovixRepository.currentUser.collectAsState()
    val allTasks by GrovixRepository.tasks.collectAsState()
    val users by GrovixRepository.users.collectAsState()

    // Employees (Power <= 50) ONLY see tasks assigned to them!
    val isEmployeeOnly = (currentUser?.powerLevel ?: 0) <= 50
    val tasks = if (isEmployeeOnly) {
        allTasks.filter {
            it.assignedTo == currentUser?.id ||
            it.assignedToName.equals(currentUser?.name, ignoreCase = true)
        }
    } else {
        allTasks
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("To-Do", "In Progress", "Delayed", "Completed")

    var selectedTaskForDetails by remember { mutableStateOf<TaskItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var newAssigneeId by remember { mutableStateOf<String?>(null) }
    var newPriority by remember { mutableStateOf("medium") }
    var newDueDate by remember { mutableStateOf("2026-11-15") }

    val canManage = (currentUser?.powerLevel ?: 0) >= 75

    val filteredTasks = tasks.filter { task ->
        when (selectedTab) {
            0 -> task.status.equals("todo", ignoreCase = true)
            1 -> task.status.equals("in-progress", ignoreCase = true) || task.status.equals("inprogress", ignoreCase = true)
            2 -> task.status.equals("delayed", ignoreCase = true)
            3 -> task.status.equals("completed", ignoreCase = true)
            else -> true
        }
    }

    Scaffold(
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = GrovixNavy,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GrovixBackground)
                .padding(paddingValues)
        ) {
            // Role Scope Banner for Developers/Employees
            if (isEmployeeOnly) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = GrovixNavy.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = GrovixNavy, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Assigned Tasks & Deliverables",
                            color = GrovixTextDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4 Tabs: To-Do, In Progress, Delayed, Completed
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = GrovixSurface,
                contentColor = GrovixNavy
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val count = tasks.count {
                        when (index) {
                            0 -> it.status.equals("todo", ignoreCase = true)
                            1 -> it.status.equals("in-progress", ignoreCase = true) || it.status.equals("inprogress", ignoreCase = true)
                            2 -> it.status.equals("delayed", ignoreCase = true)
                            3 -> it.status.equals("completed", ignoreCase = true)
                            else -> false
                        }
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = "$title ($count)",
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                color = if (selectedTab == index) GrovixNavy else GrovixTextMuted
                            )
                        }
                    )
                }
            }

            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, tint = GrovixTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No ${tabTitles[selectedTab]} tasks",
                            color = GrovixTextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskCardItem(
                            task = task,
                            canManage = canManage,
                            onCardClick = { selectedTaskForDetails = task },
                            onUpdateStatus = { newStatus ->
                                GrovixRepository.updateTaskStatus(task.id, newStatus)
                            },
                            onDelete = {
                                GrovixRepository.deleteTask(task.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Task Details & Big Note Dialog
    selectedTaskForDetails?.let { task ->
        TaskDetailsDialog(
            task = task,
            canManage = canManage,
            onDismiss = { selectedTaskForDetails = null },
            onUpdateStatus = { newStatus ->
                GrovixRepository.updateTaskStatus(task.id, newStatus)
                selectedTaskForDetails = GrovixRepository.tasks.value.find { it.id == task.id }
            },
            onDelete = {
                GrovixRepository.deleteTask(task.id)
                selectedTaskForDetails = null
            }
        )
    }

    // Add Task Dialog with Rich Formatting Toolbar
    if (showAddDialog) {
        val assignableUsers = users.filter { it.powerLevel <= (currentUser?.powerLevel ?: 0) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = GrovixSurface,
            title = { Text("Create & Assign Task", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Task Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Short Summary") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Big Note with Rich Formatting Toolbar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GrovixSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text("Detailed Explanatory Note (Formatted):", fontSize = 11.sp, color = GrovixNavy, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Formatting Toolbar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                modifier = Modifier.clickable { newNotes += "**bold text**" },
                                color = GrovixSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("B", fontWeight = FontWeight.Bold, color = GrovixTextDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                            Surface(
                                modifier = Modifier.clickable { newNotes += "<u>underlined</u>" },
                                color = GrovixSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("U", textDecoration = TextDecoration.Underline, color = GrovixTextDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                            Surface(
                                modifier = Modifier.clickable { newNotes += "\n• " },
                                color = GrovixSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("• Bullet", color = GrovixTextDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                            Surface(
                                modifier = Modifier.clickable { newNotes += "\n\n" },
                                color = GrovixSurfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("¶ Para", color = GrovixTextDark, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = newNotes,
                            onValueChange = { newNotes = it },
                            placeholder = { Text("Write detailed instructions, milestones, or bullets here...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            maxLines = 6
                        )
                    }

                    // Assignee dropdown
                    Text("Assign To:", fontSize = 12.sp, color = GrovixTextMuted)
                    var assigneeExpanded by remember { mutableStateOf(false) }
                    val currentAssigneeName = assignableUsers.find { it.id == newAssigneeId }?.name ?: "Select Team Member"

                    Box {
                        OutlinedButton(
                            onClick = { assigneeExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currentAssigneeName)
                        }
                        DropdownMenu(
                            expanded = assigneeExpanded,
                            onDismissRequest = { assigneeExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Unassigned") },
                                onClick = { newAssigneeId = null; assigneeExpanded = false }
                            )
                            assignableUsers.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text("${u.name} (${u.role})") },
                                    onClick = { newAssigneeId = u.id; assigneeExpanded = false }
                                )
                            }
                        }
                    }

                    // Priority dropdown
                    Text("Priority:", fontSize = 12.sp, color = GrovixTextMuted)
                    var priorityExpanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { priorityExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(newPriority.uppercase())
                        }
                        DropdownMenu(
                            expanded = priorityExpanded,
                            onDismissRequest = { priorityExpanded = false }
                        ) {
                            listOf("urgent", "high", "medium", "low").forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.uppercase()) },
                                    onClick = { newPriority = p; priorityExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newDueDate,
                        onValueChange = { newDueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            GrovixRepository.addTask(
                                title = newTitle,
                                desc = newDesc,
                                detailedNotes = newNotes,
                                assigneeId = newAssigneeId,
                                priority = newPriority,
                                dueDate = newDueDate
                            )
                            newTitle = ""
                            newDesc = ""
                            newNotes = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy)
                ) {
                    Text("Create Task", color = Color.White)
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
fun TaskCardItem(
    task: TaskItem,
    canManage: Boolean,
    onCardClick: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority.lowercase()) {
        "urgent" -> GrovixError
        "high" -> GrovixWarning
        "medium" -> GrovixInfo
        else -> GrovixSuccess
    }

    val statusColor = when (task.status.lowercase()) {
        "completed" -> GrovixSuccess
        "delayed" -> GrovixWarning
        "in-progress", "inprogress" -> GrovixNavy
        else -> GrovixTextMuted
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = GrovixSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GrovixTextDark,
                    modifier = Modifier.weight(1f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.priority.uppercase(),
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.status.uppercase(),
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = task.description, fontSize = 12.sp, color = GrovixTextMuted)
            }

            if (task.detailedNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📄 Includes detailed instructions (tap to view)",
                    fontSize = 11.sp,
                    color = GrovixNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Assigned to: ${task.assignedToName ?: "Unassigned"}",
                        fontSize = 11.sp,
                        color = GrovixTextMuted
                    )
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = "Due: ${task.dueDate}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.status == "delayed") GrovixWarning else GrovixTextDark
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (task.status != "completed") {
                        TextButton(
                            onClick = { onUpdateStatus("completed") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Done", fontSize = 11.sp, color = GrovixSuccess, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (task.status != "delayed" && task.status != "completed") {
                        TextButton(
                            onClick = { onUpdateStatus("delayed") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Delay", fontSize = 11.sp, color = GrovixWarning, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (canManage) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = GrovixError, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailsDialog(
    task: TaskItem,
    canManage: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusColor = when (task.status.lowercase()) {
        "completed" -> GrovixSuccess
        "delayed" -> GrovixWarning
        "in-progress", "inprogress" -> GrovixNavy
        else -> GrovixTextMuted
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GrovixSurface,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = GrovixTextDark,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = task.status.uppercase(),
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (task.dueDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Target Due Date: ${task.dueDate}", fontSize = 12.sp, color = GrovixWarning)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Assignee Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Assignee:", fontSize = 12.sp, color = GrovixTextMuted)
                    Text(task.assignedToName ?: "Unassigned", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrovixTextDark)
                }

                if (task.description.isNotBlank()) {
                    Column {
                        Text("Summary:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrovixNavy)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(task.description, fontSize = 13.sp, color = GrovixTextDark)
                    }
                }

                HorizontalDivider(color = GrovixSurfaceVariant)

                // Big Formatted Note View
                Column {
                    Text("Detailed Instructions & Notes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrovixNavy)
                    Spacer(modifier = Modifier.height(6.dp))
                    FormattedNoteViewer(
                        rawText = task.detailedNotes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GrovixBackground, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }

                HorizontalDivider(color = GrovixSurfaceVariant)

                // Status Update Buttons
                Text("Update Task Status:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrovixTextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onUpdateStatus("in-progress") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixNavy),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Text("In Progress", fontSize = 11.sp, color = Color.White)
                    }

                    Button(
                        onClick = { onUpdateStatus("delayed") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixWarning),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Text("Delayed", fontSize = 11.sp, color = Color.Black)
                    }

                    Button(
                        onClick = { onUpdateStatus("completed") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GrovixSuccess),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Text("Completed", fontSize = 11.sp, color = Color.White)
                    }
                }

                if (canManage) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrovixError),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(GrovixError))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Task Permanently", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = GrovixSurfaceVariant)) {
                Text("Close", color = GrovixTextDark)
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = GrovixSurface,
            title = { Text("Delete Task", fontWeight = FontWeight.Bold, color = GrovixTextDark) },
            text = { Text("Are you sure you want to delete this task? This action cannot be undone.", color = GrovixTextDark) },
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
