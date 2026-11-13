package com.example.grovix.api

import android.content.Context
import android.content.SharedPreferences
import com.example.grovix.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object GrovixApiClient {
    private const val PREFS_NAME = "grovix_cloud_prefs"
    private const val KEY_SERVER_URL = "cloud_server_url"
    private const val KEY_AUTH_TOKEN = "cloud_auth_token"

    // Default server address: Render 24/7 Cloud Backend
    // Colleagues automatically connect to this live cloud server out of the box
    var defaultBaseUrl = "https://grovix-app.onrender.com"

    private var prefs: SharedPreferences? = null

    var baseUrl: String = defaultBaseUrl
        private set

    var authToken: String? = null
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        baseUrl = prefs?.getString(KEY_SERVER_URL, defaultBaseUrl) ?: defaultBaseUrl
        authToken = prefs?.getString(KEY_AUTH_TOKEN, null)
    }

    fun setServerUrl(newUrl: String) {
        var clean = newUrl.trim()
        if (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length - 1)
        }
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            clean = "http://$clean"
        }
        baseUrl = clean
        prefs?.edit()?.putString(KEY_SERVER_URL, clean)?.apply()
    }

    fun setToken(token: String?) {
        authToken = token
        prefs?.edit()?.putString(KEY_AUTH_TOKEN, token)?.apply()
    }

    fun clearAuth() {
        authToken = null
        prefs?.edit()?.remove(KEY_AUTH_TOKEN)?.apply()
    }

    // Health check ping
    suspend fun checkHealth(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = executeRequest("GET", "/api/health", null, null)
            val json = JSONObject(response)
            Result.success(json.optString("status") == "ok")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login via Central Cloud API
    suspend fun login(email: String, pass: String): Result<Pair<User, String>> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }
            val response = executeRequest("POST", "/api/auth/login", body.toString(), null)
            val json = JSONObject(response)
            val token = json.getString("token")
            val userObj = json.getJSONObject("user")

            val user = User(
                id = userObj.optString("id"),
                name = userObj.optString("name"),
                email = userObj.optString("email"),
                role = userObj.optString("role"),
                powerLevel = userObj.optInt("power_level", 50),
                status = userObj.optString("status", "active"),
                phone = if (userObj.has("phone") && !userObj.isNull("phone")) userObj.getString("phone") else null
            )

            setToken(token)
            Result.success(Pair(user, token))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch Full Cloud Synchronization State
    suspend fun fetchSyncData(): Result<SyncPayload> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val response = executeRequest("GET", "/api/sync", null, token)
            val json = JSONObject(response)

            // Parse User
            val user = if (json.has("user")) {
                val u = json.getJSONObject("user")
                User(
                    id = u.optString("id"),
                    name = u.optString("name"),
                    email = u.optString("email"),
                    role = u.optString("role"),
                    powerLevel = u.optInt("power_level", 50),
                    status = u.optString("status", "active"),
                    phone = if (u.has("phone") && !u.isNull("phone")) u.getString("phone") else null
                )
            } else null

            // Parse Tasks
            val tasksList = mutableListOf<TaskItem>()
            val tasksArr = json.optJSONArray("tasks") ?: JSONArray()
            for (i in 0 until tasksArr.length()) {
                val t = tasksArr.getJSONObject(i)
                tasksList.add(
                    TaskItem(
                        id = t.optString("id"),
                        title = t.optString("title"),
                        description = t.optString("description", ""),
                        detailedNotes = t.optString("detailed_notes", t.optString("detailedNotes", "")),
                        assignedTo = if (t.has("assigned_to") && !t.isNull("assigned_to")) t.getString("assigned_to") else null,
                        assignedToName = if (t.has("assigned_to_name") && !t.isNull("assigned_to_name")) t.getString("assigned_to_name") else "Unassigned",
                        assignedBy = t.optString("assigned_by", ""),
                        status = t.optString("status", "todo"),
                        priority = t.optString("priority", "medium"),
                        dueDate = t.optString("due_date", "")
                    )
                )
            }

            // Parse Sales
            val salesList = mutableListOf<Sale>()
            val salesArr = json.optJSONArray("sales") ?: JSONArray()
            for (i in 0 until salesArr.length()) {
                val s = salesArr.getJSONObject(i)
                salesList.add(
                    Sale(
                        id = s.optString("id"),
                        productService = s.optString("product_service"),
                        quantity = s.optInt("quantity", 1),
                        price = s.optDouble("price", 0.0),
                        total = s.optDouble("total", 0.0),
                        client = s.optString("client", ""),
                        date = s.optString("date", ""),
                        notes = s.optString("notes", ""),
                        createdByName = s.optString("created_by_name", "Staff")
                    )
                )
            }

            // Parse Expenses
            val expensesList = mutableListOf<Expense>()
            val expensesArr = json.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expensesArr.length()) {
                val e = expensesArr.getJSONObject(i)
                expensesList.add(
                    Expense(
                        id = e.optString("id"),
                        category = e.optString("category"),
                        amount = e.optDouble("amount", 0.0),
                        date = e.optString("date", ""),
                        notes = e.optString("notes", ""),
                        createdByName = e.optString("created_by_name", "Staff")
                    )
                )
            }

            // Parse Users
            val usersList = mutableListOf<User>()
            val usersArr = json.optJSONArray("users") ?: JSONArray()
            for (i in 0 until usersArr.length()) {
                val u = usersArr.getJSONObject(i)
                usersList.add(
                    User(
                        id = u.optString("id"),
                        name = u.optString("name"),
                        email = u.optString("email"),
                        role = u.optString("role"),
                        powerLevel = u.optInt("power_level", 50),
                        status = u.optString("status", "active"),
                        phone = if (u.has("phone") && !u.isNull("phone")) u.getString("phone") else null
                    )
                )
            }

            // Parse Notifications
            val notifsList = mutableListOf<AppNotification>()
            val notifsArr = json.optJSONArray("notifications") ?: JSONArray()
            for (i in 0 until notifsArr.length()) {
                val n = notifsArr.getJSONObject(i)
                notifsList.add(
                    AppNotification(
                        id = n.optString("id"),
                        title = if (n.optString("type") == "task") "Task Alert" else "Notification",
                        message = n.optString("message", ""),
                        type = n.optString("type", "info"),
                        targetUserId = n.optString("user_id"),
                        isRead = n.optInt("read", 0) == 1,
                        timestamp = n.optString("created_at", "")
                    )
                )
            }

            Result.success(
                SyncPayload(
                    user = user,
                    tasks = tasksList,
                    sales = salesList,
                    expenses = expensesList,
                    users = usersList,
                    notifications = notifsList,
                    unreadNotifs = json.optInt("unreadNotifs", 0),
                    currency = json.optString("currency", "INR (₹)")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Task Actions
    suspend fun createTask(
        title: String,
        description: String,
        detailedNotes: String,
        assignedTo: String?,
        priority: String,
        dueDate: String
    ): Result<TaskItem> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("title", title)
                put("description", description)
                put("detailed_notes", detailedNotes)
                if (assignedTo != null) put("assigned_to", assignedTo)
                put("priority", priority.lowercase())
                put("due_date", dueDate)
            }
            val res = executeRequest("POST", "/api/tasks", body.toString(), token)
            val json = JSONObject(res)
            val t = json.getJSONObject("task")
            Result.success(
                TaskItem(
                    id = t.optString("id"),
                    title = t.optString("title"),
                    description = t.optString("description", ""),
                    detailedNotes = t.optString("detailed_notes", detailedNotes),
                    assignedTo = if (t.has("assigned_to") && !t.isNull("assigned_to")) t.getString("assigned_to") else null,
                    assignedToName = if (t.has("assigned_to_name") && !t.isNull("assigned_to_name")) t.getString("assigned_to_name") else "Unassigned",
                    assignedBy = t.optString("assigned_by", ""),
                    status = t.optString("status", "todo"),
                    priority = t.optString("priority", priority),
                    dueDate = t.optString("due_date", dueDate)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("status", newStatus.lowercase())
            }
            executeRequest("PUT", "/api/tasks/$taskId", body.toString(), token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            executeRequest("DELETE", "/api/tasks/$taskId", null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sales Actions
    suspend fun createSale(
        productService: String,
        quantity: Int,
        price: Double,
        client: String,
        date: String,
        notes: String
    ): Result<Sale> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("product_service", productService)
                put("quantity", quantity)
                put("price", price)
                put("client", client)
                put("date", date)
                put("notes", notes)
            }
            val res = executeRequest("POST", "/api/sales", body.toString(), token)
            val json = JSONObject(res)
            val s = json.getJSONObject("sale")
            Result.success(
                Sale(
                    id = s.optString("id"),
                    productService = s.optString("product_service"),
                    quantity = s.optInt("quantity", quantity),
                    price = s.optDouble("price", price),
                    total = s.optDouble("total", quantity * price),
                    client = s.optString("client", client),
                    date = s.optString("date", date),
                    notes = s.optString("notes", notes),
                    createdByName = s.optString("created_by_name", "Staff")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSale(saleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            executeRequest("DELETE", "/api/sales/$saleId", null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Expense Actions
    suspend fun createExpense(
        category: String,
        amount: Double,
        date: String,
        notes: String
    ): Result<Expense> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("category", category)
                put("amount", amount)
                put("date", date)
                put("notes", notes)
            }
            val res = executeRequest("POST", "/api/expenses", body.toString(), token)
            val json = JSONObject(res)
            val exp = json.getJSONObject("expense")
            Result.success(
                Expense(
                    id = exp.optString("id"),
                    category = exp.optString("category"),
                    amount = exp.optDouble("amount", amount),
                    date = exp.optString("date", date),
                    notes = exp.optString("notes", notes),
                    createdByName = exp.optString("created_by_name", "Staff")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            executeRequest("DELETE", "/api/expenses/$expenseId", null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Team Actions
    suspend fun createUser(
        name: String,
        email: String,
        pass: String,
        role: String,
        phone: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("name", name)
                put("email", email)
                put("password", pass)
                put("role", role.lowercase())
                if (phone != null) put("phone", phone)
            }
            val res = executeRequest("POST", "/api/users", body.toString(), token)
            val json = JSONObject(res)
            val u = json.getJSONObject("user")
            Result.success(
                User(
                    id = u.optString("id"),
                    name = u.optString("name"),
                    email = u.optString("email"),
                    role = u.optString("role"),
                    powerLevel = u.optInt("power_level", 50),
                    status = u.optString("status", "active"),
                    phone = if (u.has("phone") && !u.isNull("phone")) u.getString("phone") else null
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(userId: String, permanent: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val url = if (permanent) "/api/users/$userId?permanent=true" else "/api/users/$userId"
            executeRequest("DELETE", url, null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notification Actions
    suspend fun sendNotification(
        userId: String,
        message: String,
        type: String = "info"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            val body = JSONObject().apply {
                put("user_id", userId)
                put("message", message)
                put("type", type)
            }
            executeRequest("POST", "/api/notifications", body.toString(), token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            executeRequest("PUT", "/api/notifications/$id/read", null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val token = authToken ?: return@withContext Result.failure(Exception("Not authenticated"))
            executeRequest("PUT", "/api/notifications/read-all", null, token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Low-level HTTP Executor
    private fun executeRequest(
        method: String,
        endpoint: String,
        jsonBody: String?,
        token: String?
    ): String {
        val fullUrl = "$baseUrl$endpoint"
        val url = URL(fullUrl)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 7000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            if (token != null) {
                setRequestProperty("Authorization", "Bearer $token")
            }
            // For localtunnel / cloud headers
            setRequestProperty("Bypass-Tunnel-Reminder", "true")
            setRequestProperty("User-Agent", "GrovixAndroid/1.0")

            if (jsonBody != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val writer = OutputStreamWriter(outputStream, "UTF-8")
                writer.write(jsonBody)
                writer.flush()
                writer.close()
            }
        }

        val responseCode = conn.responseCode
        val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream

        val reader = BufferedReader(InputStreamReader(stream ?: conn.inputStream, "UTF-8"))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        conn.disconnect()

        val respStr = response.toString()

        if (responseCode !in 200..299) {
            val errMsg = try {
                val j = JSONObject(respStr)
                j.optString("error", "Server returned HTTP $responseCode")
            } catch (e: Exception) {
                "Server HTTP $responseCode: $respStr"
            }
            throw Exception(errMsg)
        }

        return respStr
    }
}
