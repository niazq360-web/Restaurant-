package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ExportImportHelper {

    private fun getPersistentBackupFile(context: Context): File {
        val dir = context.getExternalFilesDir("backup") ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "pos_offline_master_backup.json")
    }

    fun hasPersistentBackup(context: Context): Boolean {
        val file = getPersistentBackupFile(context)
        return file.exists() && file.length() > 0
    }

    suspend fun saveAutoBackup(context: Context, dao: PosDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonRoot = createFullBackupJson(dao)
            val file = getPersistentBackupFile(context)
            FileOutputStream(file).use {
                it.write(jsonRoot.toString(2).toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreAutoBackup(context: Context, dao: PosDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = getPersistentBackupFile(context)
            if (!file.exists()) return@withContext false
            val jsonString = file.readText()
            importJsonToDatabase(jsonString, dao)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportDatabaseToJson(context: Context, dao: PosDao): Intent? = withContext(Dispatchers.IO) {
        try {
            val jsonRoot = createFullBackupJson(dao)
            val jsonString = jsonRoot.toString(4)

            // Save to persistent file
            saveAutoBackup(context, dao)

            val sharedFolder = File(context.cacheDir, "shared")
            if (!sharedFolder.exists()) sharedFolder.mkdirs()
            val backupFile = File(sharedFolder, "restaurant_pos_backup_${System.currentTimeMillis()}.json")
            
            FileOutputStream(backupFile).use {
                it.write(jsonString.toByteArray())
            }
            
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Restaurant POS Complete 12-Month Offline Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareIntent
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importDatabaseFromJson(context: Context, uri: Uri, dao: PosDao): Boolean = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader().use { it?.readText() } ?: return@withContext false
            importJsonToDatabase(jsonString, dao)
            // Save as persistent backup
            val persistentFile = getPersistentBackupFile(context)
            persistentFile.writeText(jsonString)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private suspend fun createFullBackupJson(dao: PosDao): JSONObject {
        val jsonRoot = JSONObject()

        // 1. Products
        val products = dao.getAllProductsSync()
        val productsArray = JSONArray()
        products.forEach {
            val p = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("price", it.price)
                put("costPrice", it.costPrice)
                put("category", it.category)
                put("imageUrl", it.imageUrl ?: "")
                put("stockCount", it.stockCount)
            }
            productsArray.put(p)
        }
        jsonRoot.put("products", productsArray)

        // 2. Orders (12+ Months)
        val orders = dao.getAllOrdersSync()
        val ordersArray = JSONArray()
        orders.forEach {
            val o = JSONObject().apply {
                put("id", it.id)
                put("customerName", it.customerName)
                put("customerPhone", it.customerPhone)
                put("deliveryAddress", it.deliveryAddress)
                put("orderType", it.orderType)
                put("tableName", it.tableName)
                put("status", it.status)
                put("totalAmount", it.totalAmount)
                put("subtotalAmount", it.subtotalAmount)
                put("discountAmount", it.discountAmount)
                put("profitAmount", it.profitAmount)
                put("paymentMethod", it.paymentMethod)
                put("paymentStatus", it.paymentStatus)
                put("riderNumber", it.riderNumber)
                put("deliveryOtp", it.deliveryOtp)
                put("timestamp", it.timestamp)
            }
            ordersArray.put(o)
        }
        jsonRoot.put("orders", ordersArray)

        // 3. Order Items
        val orderItems = dao.getAllOrderItemsSync()
        val itemsArray = JSONArray()
        orderItems.forEach {
            val itm = JSONObject().apply {
                put("id", it.id)
                put("orderId", it.orderId)
                put("productId", it.productId)
                put("productName", it.productName)
                put("category", it.category)
                put("quantity", it.quantity)
                put("price", it.price)
            }
            itemsArray.put(itm)
        }
        jsonRoot.put("order_items", itemsArray)

        // 4. Tables
        val tables = dao.getAllTablesSync()
        val tablesArray = JSONArray()
        tables.forEach {
            val t = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("status", it.status)
            }
            tablesArray.put(t)
        }
        jsonRoot.put("tables", tablesArray)

        // 5. Workers
        val workers = dao.getAllWorkersSync()
        val workersArray = JSONArray()
        workers.forEach {
            val w = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("phone", it.phone)
                put("role", it.role)
                put("assignedTable", it.assignedTable)
                put("salary", it.salary)
                put("joiningDate", it.joiningDate)
                put("status", it.status)
                put("imagePath", it.imagePath ?: "")
                put("notes", it.notes)
            }
            workersArray.put(w)
        }
        jsonRoot.put("workers", workersArray)

        // 6. Worker Attendance
        val attendance = dao.getAllAttendanceSync()
        val attArray = JSONArray()
        attendance.forEach {
            val a = JSONObject().apply {
                put("id", it.id)
                put("workerId", it.workerId)
                put("workerName", it.workerName)
                put("date", it.date)
                put("status", it.status)
                put("timestamp", it.timestamp)
            }
            attArray.put(a)
        }
        jsonRoot.put("attendance", attArray)

        return jsonRoot
    }

    private suspend fun importJsonToDatabase(jsonString: String, dao: PosDao) {
        val jsonRoot = JSONObject(jsonString)

        // Restore Products
        if (jsonRoot.has("products")) {
            val arr = jsonRoot.getJSONArray("products")
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                dao.insertProduct(
                    Product(
                        name = p.getString("name"),
                        price = p.getDouble("price"),
                        costPrice = p.optDouble("costPrice", 0.0),
                        category = p.getString("category"),
                        imageUrl = p.optString("imageUrl").takeIf { it.isNotBlank() },
                        stockCount = p.optInt("stockCount", -1)
                    )
                )
            }
        }

        // Restore Orders
        if (jsonRoot.has("orders")) {
            val arr = jsonRoot.getJSONArray("orders")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                dao.insertOrder(
                    Order(
                        customerName = o.getString("customerName"),
                        customerPhone = o.optString("customerPhone", ""),
                        deliveryAddress = o.optString("deliveryAddress", ""),
                        orderType = o.optString("orderType", "Dine-in"),
                        tableName = o.optString("tableName", ""),
                        status = o.optString("status", "Completed"),
                        totalAmount = o.getDouble("totalAmount"),
                        subtotalAmount = o.optDouble("subtotalAmount", o.getDouble("totalAmount")),
                        discountAmount = o.optDouble("discountAmount", 0.0),
                        profitAmount = o.optDouble("profitAmount", 0.0),
                        paymentMethod = o.optString("paymentMethod", "Cash"),
                        paymentStatus = o.optString("paymentStatus", "Paid"),
                        riderNumber = o.optString("riderNumber", ""),
                        deliveryOtp = o.optString("deliveryOtp", ""),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        // Restore Order Items
        if (jsonRoot.has("order_items")) {
            val arr = jsonRoot.getJSONArray("order_items")
            val itemsList = mutableListOf<OrderItem>()
            for (i in 0 until arr.length()) {
                val itm = arr.getJSONObject(i)
                itemsList.add(
                    OrderItem(
                        orderId = itm.getInt("orderId"),
                        productId = itm.getInt("productId"),
                        productName = itm.getString("productName"),
                        category = itm.optString("category", ""),
                        quantity = itm.getInt("quantity"),
                        price = itm.getDouble("price")
                    )
                )
            }
            dao.insertOrderItems(itemsList)
        }

        // Restore Tables
        if (jsonRoot.has("tables")) {
            val arr = jsonRoot.getJSONArray("tables")
            for (i in 0 until arr.length()) {
                val t = arr.getJSONObject(i)
                dao.insertTable(
                    RestaurantTable(
                        name = t.getString("name"),
                        status = t.optString("status", "Available")
                    )
                )
            }
        }

        // Restore Workers
        if (jsonRoot.has("workers")) {
            val arr = jsonRoot.getJSONArray("workers")
            for (i in 0 until arr.length()) {
                val w = arr.getJSONObject(i)
                dao.insertWorker(
                    Worker(
                        name = w.getString("name"),
                        phone = w.optString("phone", ""),
                        role = w.optString("role", "Staff"),
                        assignedTable = w.optString("assignedTable", ""),
                        salary = w.optDouble("salary", 0.0),
                        joiningDate = w.optString("joiningDate", ""),
                        status = w.optString("status", "Active"),
                        imagePath = w.optString("imagePath").takeIf { it.isNotBlank() },
                        notes = w.optString("notes", "")
                    )
                )
            }
        }

        // Restore Attendance
        if (jsonRoot.has("attendance")) {
            val arr = jsonRoot.getJSONArray("attendance")
            for (i in 0 until arr.length()) {
                val a = arr.getJSONObject(i)
                dao.insertAttendance(
                    WorkerAttendance(
                        workerId = a.getInt("workerId"),
                        workerName = a.getString("workerName"),
                        date = a.getString("date"),
                        status = a.optString("status", "Present"),
                        timestamp = a.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }
    }
}
