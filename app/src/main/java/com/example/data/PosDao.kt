package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    // Products
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    // Orders
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    suspend fun getAllOrdersSync(): List<Order>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long

    @Update
    suspend fun updateOrder(order: Order)

    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrderById(orderId: Int)

    // Order Items
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: Int): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items")
    fun getAllOrderItems(): Flow<List<OrderItem>>

    @Query("SELECT * FROM order_items")
    suspend fun getAllOrderItemsSync(): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteOrderItemsByOrderId(orderId: Int)

    @Transaction
    suspend fun deleteOrderAndItems(orderId: Int) {
        deleteOrderById(orderId)
        deleteOrderItemsByOrderId(orderId)
    }

    // Tables
    @Query("SELECT * FROM restaurant_tables")
    fun getAllTables(): Flow<List<RestaurantTable>>

    @Query("SELECT * FROM restaurant_tables")
    suspend fun getAllTablesSync(): List<RestaurantTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: RestaurantTable)

    @Update
    suspend fun updateTable(table: RestaurantTable)

    // Workers
    @Query("SELECT * FROM workers ORDER BY name ASC")
    fun getAllWorkers(): Flow<List<Worker>>

    @Query("SELECT * FROM workers ORDER BY name ASC")
    suspend fun getAllWorkersSync(): List<Worker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: Worker): Long

    @Update
    suspend fun updateWorker(worker: Worker)

    @Delete
    suspend fun deleteWorker(worker: Worker)

    // Attendance
    @Query("SELECT * FROM worker_attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<WorkerAttendance>>

    @Query("SELECT * FROM worker_attendance ORDER BY timestamp DESC")
    suspend fun getAllAttendanceSync(): List<WorkerAttendance>

    @Query("SELECT * FROM worker_attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<WorkerAttendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: WorkerAttendance): Long

    @Update
    suspend fun updateAttendance(attendance: WorkerAttendance)
}
