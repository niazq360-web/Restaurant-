package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String? = null,
    val stockCount: Int = -1, // -1 means unlimited
    val costPrice: Double = 0.0 // For profit calculation
)

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val customerName: String,
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val orderType: String, // Dine-in, Takeaway, Delivery
    val tableName: String = "", // Table Number
    val status: String, // Pending, Out for Delivery, Completed, Cancelled
    val totalAmount: Double,
    val subtotalAmount: Double = totalAmount,
    val discountAmount: Double = 0.0,
    val profitAmount: Double = 0.0,
    val paymentMethod: String, // Cash, QR, Bank, Easypaisa, JazzCash
    val paymentStatus: String = "Pending", // Paid, Pending
    val riderNumber: String = "",
    val deliveryOtp: String = "",
    val deliveryPriority: Int = 0, // 0 = default, 1 = stop 1, 2 = stop 2, etc.
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_items")
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: Int,
    val productId: Int,
    val productName: String,
    val category: String = "",
    val quantity: Int,
    val price: Double
)

@Entity(tableName = "restaurant_tables")
data class RestaurantTable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val status: String = "Available" // Available, Occupied
)

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val role: String = "Staff", // Waiter, Chef, Cashier, Manager, Cleaner, Helper, Rider
    val designation: String = role, // Specific designation e.g. "Head Chef", "Delivery Boy"
    val assignedTable: String = "",
    val salary: Double = 0.0,
    val joiningDate: String = "",
    val status: String = "Active", // "Active", "On Leave", "Left", "Terminated"
    val imagePath: String? = null,
    val notes: String = ""
)

@Entity(tableName = "worker_attendance")
data class WorkerAttendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workerId: Int,
    val workerName: String,
    val date: String, // "yyyy-MM-dd"
    val status: String = "Present", // "Present", "Absent", "Half Day", "Leave"
    val timestamp: Long = System.currentTimeMillis()
)
