package com.example.data

import kotlinx.coroutines.flow.Flow

class PosRepository(val dao: PosDao) {
    val allProducts: Flow<List<Product>> = dao.getAllProducts()
    val allOrders: Flow<List<Order>> = dao.getAllOrders()
    val allOrderItems: Flow<List<OrderItem>> = dao.getAllOrderItems()
    val allTables: Flow<List<RestaurantTable>> = dao.getAllTables()
    val allWorkers: Flow<List<Worker>> = dao.getAllWorkers()
    val allAttendance: Flow<List<WorkerAttendance>> = dao.getAllAttendance()

    // Products
    suspend fun insertProduct(product: Product) = dao.insertProduct(product)
    suspend fun updateProduct(product: Product) = dao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = dao.deleteProduct(product)

    // Orders
    suspend fun insertOrder(order: Order): Long = dao.insertOrder(order)
    suspend fun updateOrder(order: Order) = dao.updateOrder(order)
    suspend fun deleteOrder(orderId: Int) = dao.deleteOrderAndItems(orderId)

    suspend fun insertOrderItems(items: List<OrderItem>) = dao.insertOrderItems(items)
    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> = dao.getOrderItems(orderId)

    // Tables
    suspend fun insertTable(table: RestaurantTable) = dao.insertTable(table)
    suspend fun updateTable(table: RestaurantTable) = dao.updateTable(table)

    // Workers
    suspend fun insertWorker(worker: Worker) = dao.insertWorker(worker)
    suspend fun updateWorker(worker: Worker) = dao.updateWorker(worker)
    suspend fun deleteWorker(worker: Worker) = dao.deleteWorker(worker)

    // Attendance
    fun getAttendanceForDate(date: String): Flow<List<WorkerAttendance>> = dao.getAttendanceForDate(date)
    suspend fun insertAttendance(attendance: WorkerAttendance) = dao.insertAttendance(attendance)
    suspend fun updateAttendance(attendance: WorkerAttendance) = dao.updateAttendance(attendance)
}
