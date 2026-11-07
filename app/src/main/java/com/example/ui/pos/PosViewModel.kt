package com.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(val product: Product, var quantity: Int)

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allOrderItems: StateFlow<List<OrderItem>> = repository.allOrderItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val tables: StateFlow<List<RestaurantTable>> = repository.allTables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<WorkerAttendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()
    
    val deliveryOrders: StateFlow<List<Order>> = repository.allOrders
        .map { orders -> orders.filter { it.orderType == "Delivery" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomItemToCart(name: String, price: Double, quantity: Int) {
        val customId = -(System.currentTimeMillis() % 1000000).toInt() - (1..1000).random()
        val customProduct = Product(id = customId, name = name, price = price, category = "Custom Entry")
        val currentCart = _cart.value.toMutableList()
        currentCart.add(CartItem(customProduct, quantity))
        _cart.value = currentCart
    }

    fun addToCart(product: Product) {
        val currentCart = _cart.value.toMutableList()
        val existingItem = currentCart.find { it.product.id == product.id }
        if (existingItem != null) {
            existingItem.quantity++
        } else {
            currentCart.add(CartItem(product, 1))
        }
        _cart.value = currentCart
    }

    fun updateCartQuantity(product: Product, quantity: Int) {
        val currentCart = _cart.value.toMutableList()
        val existingItem = currentCart.find { it.product.id == product.id }
        if (existingItem != null) {
            if (quantity <= 0) {
                currentCart.remove(existingItem)
            } else {
                existingItem.quantity = quantity
            }
            _cart.value = currentCart
        }
    }
    
    fun clearCart() {
        _cart.value = emptyList()
    }

    fun checkout(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        orderType: String,
        tableName: String,
        paymentMethod: String,
        isPaid: Boolean,
        discount: Double = 0.0
    ) {
        val currentCart = _cart.value
        if (currentCart.isEmpty()) return

        val subtotal = currentCart.sumOf { it.product.price * it.quantity }
        val totalAmount = (subtotal - discount).coerceAtLeast(0.0)
        // Profit: price minus costPrice (or 60% default cost) minus discount
        val totalCost = currentCart.sumOf { item ->
            val cost = if (item.product.costPrice > 0) item.product.costPrice else (item.product.price * 0.6)
            cost * item.quantity
        }
        val profitAmount = (totalAmount - totalCost).coerceAtLeast(0.0)

        val status = if (orderType == "Delivery") "Pending" else "Completed"
        val paymentStatus = if (isPaid) "Paid" else "Pending"

        val order = Order(
            customerName = customerName,
            customerPhone = customerPhone,
            deliveryAddress = deliveryAddress,
            orderType = orderType,
            tableName = tableName,
            status = status,
            totalAmount = totalAmount,
            subtotalAmount = subtotal,
            discountAmount = discount,
            profitAmount = profitAmount,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus
        )

        viewModelScope.launch {
            val orderId = repository.insertOrder(order)
            val orderItems = currentCart.map {
                OrderItem(
                    orderId = orderId.toInt(),
                    productId = it.product.id,
                    productName = it.product.name,
                    category = it.product.category,
                    quantity = it.quantity,
                    price = it.product.price
                )
            }
            repository.insertOrderItems(orderItems)
            _cart.value = emptyList()
        }
    }
    
    fun dispatchDelivery(order: Order, riderNumber: String, otp: String) {
        viewModelScope.launch {
            repository.updateOrder(
                order.copy(
                    status = "Out for Delivery",
                    riderNumber = riderNumber,
                    deliveryOtp = otp
                )
            )
        }
    }

    fun completeDelivery(order: Order, finalPaymentMethod: String) {
        viewModelScope.launch {
            repository.updateOrder(
                order.copy(
                    status = "Completed",
                    paymentStatus = "Paid",
                    paymentMethod = finalPaymentMethod
                )
            )
        }
    }
    
    fun getOrderItems(orderId: Int): Flow<List<OrderItem>> {
        return repository.getOrderItems(orderId)
    }
    
    fun updateOrder(order: Order) {
        viewModelScope.launch {
            repository.updateOrder(order)
        }
    }

    fun updateDeliveryPriority(order: Order, priority: Int) {
        viewModelScope.launch {
            repository.updateOrder(order.copy(deliveryPriority = priority))
        }
    }
    
    fun deleteOrder(orderId: Int) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
        }
    }

    fun addJuiceProducts() {
        viewModelScope.launch {
            val currentProducts = repository.dao.getAllProductsSync()
            if (currentProducts.none { it.category == "Juices" }) {
                val juiceItems = listOf(
                    Product(name = "Apple Juice", price = 200.0, category = "Juices"),
                    Product(name = "Orange Juice", price = 250.0, category = "Juices"),
                    Product(name = "Mango Juice", price = 250.0, category = "Juices"),
                    Product(name = "Pineapple Juice", price = 300.0, category = "Juices"),
                    Product(name = "Watermelon Juice", price = 150.0, category = "Juices"),
                    Product(name = "Strawberry Juice", price = 300.0, category = "Juices"),
                    Product(name = "Pomegranate Juice", price = 350.0, category = "Juices"),
                    Product(name = "Grape Juice", price = 250.0, category = "Juices"),
                    Product(name = "Peach Juice", price = 200.0, category = "Juices"),
                    Product(name = "Mixed Fruit Juice", price = 350.0, category = "Juices")
                )
                juiceItems.forEach {
                    repository.insertProduct(it)
                }
            }
        }
    }

    fun addSampleProducts() {
        viewModelScope.launch {
            val currentProducts = repository.dao.getAllProductsSync()
            if (currentProducts.size < 10) {
                val sampleItems = listOf(
                    Product(name = "Plain Bread", price = 120.0, category = "Bakery"),
                    Product(name = "Bran Bread", price = 150.0, category = "Bakery"),
                    Product(name = "Croissant", price = 250.0, category = "Bakery"),
                    Product(name = "Chocolate Croissant", price = 300.0, category = "Bakery"),
                    Product(name = "Chocolate Cake (1 Lb)", price = 1200.0, category = "Bakery"),
                    Product(name = "Black Forest Cake", price = 1100.0, category = "Bakery"),
                    Product(name = "Pineapple Cake", price = 1000.0, category = "Bakery"),
                    Product(name = "Vanilla Cupcake", price = 150.0, category = "Bakery"),
                    Product(name = "Chocolate Cupcake", price = 180.0, category = "Bakery"),
                    Product(name = "Fudge Brownie", price = 200.0, category = "Bakery"),
                    Product(name = "Walnut Brownie", price = 250.0, category = "Bakery"),
                    Product(name = "Apple Pie", price = 350.0, category = "Bakery"),
                    Product(name = "Cheese Danish", price = 280.0, category = "Bakery"),
                    Product(name = "Plain Bagel", price = 100.0, category = "Bakery"),
                    Product(name = "Sesame Bagel", price = 120.0, category = "Bakery"),
                    Product(name = "Glazed Donut", price = 150.0, category = "Bakery"),
                    Product(name = "Chocolate Donut", price = 180.0, category = "Bakery"),
                    Product(name = "Fruit Tart", price = 300.0, category = "Bakery"),
                    Product(name = "Lemon Tart", price = 250.0, category = "Bakery"),
                    Product(name = "Macarons (Box of 4)", price = 600.0, category = "Bakery"),
                    Product(name = "Cinnamon Roll", price = 220.0, category = "Bakery"),
                    Product(name = "Blueberry Muffin", price = 180.0, category = "Bakery"),
                    Product(name = "Chocolate Chip Muffin", price = 200.0, category = "Bakery"),
                    Product(name = "Chocolate Eclair", price = 250.0, category = "Bakery"),
                    Product(name = "Swiss Roll", price = 200.0, category = "Bakery"),
                    Product(name = "Chicken Patties", price = 100.0, category = "Bakery"),
                    Product(name = "Vegetable Patties", price = 80.0, category = "Bakery"),
                    Product(name = "Tea Rusk (Pack)", price = 150.0, category = "Bakery"),
                    Product(name = "Almond Biscuits", price = 400.0, category = "Bakery"),
                    Product(name = "Zeera Biscuits", price = 300.0, category = "Bakery"),
                    Product(name = "Nan Khatai", price = 350.0, category = "Bakery"),
                    Product(name = "Cake Rusk", price = 250.0, category = "Bakery"),
                    Product(name = "Zinger Burger", price = 450.0, category = "Fast Food"),
                    Product(name = "Mighty Zinger", price = 650.0, category = "Fast Food"),
                    Product(name = "Beef Burger", price = 500.0, category = "Fast Food"),
                    Product(name = "Double Beef Burger", price = 700.0, category = "Fast Food"),
                    Product(name = "Chicken Wrap", price = 350.0, category = "Fast Food"),
                    Product(name = "Club Sandwich", price = 400.0, category = "Fast Food"),
                    Product(name = "BBQ Sandwich", price = 450.0, category = "Fast Food"),
                    Product(name = "French Fries (Small)", price = 150.0, category = "Fast Food"),
                    Product(name = "French Fries (Large)", price = 250.0, category = "Fast Food"),
                    Product(name = "Loaded Fries", price = 450.0, category = "Fast Food"),
                    Product(name = "Chicken Tikka Pizza (S)", price = 500.0, category = "Fast Food"),
                    Product(name = "Chicken Tikka Pizza (M)", price = 900.0, category = "Fast Food"),
                    Product(name = "Chicken Tikka Pizza (L)", price = 1300.0, category = "Fast Food"),
                    Product(name = "Fajita Pizza (S)", price = 500.0, category = "Fast Food"),
                    Product(name = "Fajita Pizza (M)", price = 900.0, category = "Fast Food"),
                    Product(name = "Fajita Pizza (L)", price = 1300.0, category = "Fast Food"),
                    Product(name = "Pepperoni Pizza (M)", price = 1000.0, category = "Fast Food"),
                    Product(name = "Chicken Nuggets (6 pcs)", price = 300.0, category = "Fast Food"),
                    Product(name = "Chicken Nuggets (10 pcs)", price = 450.0, category = "Fast Food"),
                    Product(name = "Hot Wings (6 pcs)", price = 350.0, category = "Fast Food"),
                    Product(name = "Fried Chicken (1 pc)", price = 200.0, category = "Fast Food"),
                    Product(name = "Fried Chicken (3 pcs)", price = 550.0, category = "Fast Food"),
                    Product(name = "Onion Rings", price = 200.0, category = "Fast Food"),
                    Product(name = "Hot Dog", price = 250.0, category = "Fast Food"),
                    Product(name = "Shawarma", price = 200.0, category = "Fast Food"),
                    Product(name = "Chicken Karahi (Half)", price = 1200.0, category = "Main Course"),
                    Product(name = "Chicken Karahi (Full)", price = 2200.0, category = "Main Course"),
                    Product(name = "Mutton Karahi (Half)", price = 1800.0, category = "Main Course"),
                    Product(name = "Mutton Karahi (Full)", price = 3400.0, category = "Main Course"),
                    Product(name = "Chicken Biryani", price = 350.0, category = "Main Course"),
                    Product(name = "Beef Biryani", price = 400.0, category = "Main Course"),
                    Product(name = "Mutton Pulao", price = 500.0, category = "Main Course"),
                    Product(name = "Daal Mash", price = 300.0, category = "Main Course"),
                    Product(name = "Daal Chana", price = 250.0, category = "Main Course"),
                    Product(name = "Mixed Sabzi", price = 250.0, category = "Main Course"),
                    Product(name = "Palak Paneer", price = 450.0, category = "Main Course"),
                    Product(name = "Chicken Tikka (Chest)", price = 400.0, category = "Main Course"),
                    Product(name = "Chicken Tikka (Leg)", price = 350.0, category = "Main Course"),
                    Product(name = "Seekh Kebab (Beef)", price = 450.0, category = "Main Course"),
                    Product(name = "Chicken Malai Boti", price = 550.0, category = "Main Course"),
                    Product(name = "Beef Nihari", price = 600.0, category = "Main Course"),
                    Product(name = "Chicken Haleem", price = 350.0, category = "Main Course"),
                    Product(name = "Chicken Handi", price = 1500.0, category = "Main Course"),
                    Product(name = "Mutton Handi", price = 2400.0, category = "Main Course"),
                    Product(name = "Brain Masala", price = 800.0, category = "Main Course"),
                    Product(name = "Sada Roti", price = 20.0, category = "Sides"),
                    Product(name = "Khamiri Roti", price = 30.0, category = "Sides"),
                    Product(name = "Naan", price = 40.0, category = "Sides"),
                    Product(name = "Garlic Naan", price = 60.0, category = "Sides"),
                    Product(name = "Roghni Naan", price = 70.0, category = "Sides"),
                    Product(name = "Cheese Naan", price = 150.0, category = "Sides"),
                    Product(name = "Lachha Paratha", price = 80.0, category = "Sides"),
                    Product(name = "Fresh Salad", price = 100.0, category = "Sides"),
                    Product(name = "Zeera Raita", price = 80.0, category = "Sides"),
                    Product(name = "Mint Raita", price = 80.0, category = "Sides"),
                    Product(name = "Kachumar Salad", price = 120.0, category = "Sides"),
                    Product(name = "Russian Salad", price = 250.0, category = "Sides"),
                    Product(name = "Mineral Water (Small)", price = 60.0, category = "Beverages"),
                    Product(name = "Mineral Water (Large)", price = 100.0, category = "Beverages"),
                    Product(name = "Soft Drink (Can)", price = 120.0, category = "Beverages"),
                    Product(name = "Soft Drink (1.5 Ltr)", price = 220.0, category = "Beverages"),
                    Product(name = "Fresh Lime", price = 150.0, category = "Beverages"),
                    Product(name = "Mint Margarita", price = 250.0, category = "Beverages"),
                    Product(name = "Pina Colada", price = 350.0, category = "Beverages"),
                    Product(name = "Cold Coffee", price = 300.0, category = "Beverages"),
                    Product(name = "Karak Chai", price = 80.0, category = "Beverages"),
                    Product(name = "Green Tea", price = 70.0, category = "Beverages"),
                    Product(name = "Gulab Jamun (2 pcs)", price = 150.0, category = "Desserts"),
                    Product(name = "Rasmalai (2 pcs)", price = 200.0, category = "Desserts"),
                    Product(name = "Kheer", price = 200.0, category = "Desserts"),
                    Product(name = "Ice Cream (1 Scoop)", price = 150.0, category = "Desserts"),
                    Product(name = "Gajar Halwa", price = 300.0, category = "Desserts")
                )
                sampleItems.forEach { product ->
                    repository.insertProduct(product)
                }
            }
        }
    }


    // Table Management
    fun addTable(name: String) {
        viewModelScope.launch {
            repository.insertTable(RestaurantTable(name = name))
        }
    }

    fun updateTableStatus(table: RestaurantTable, newStatus: String) {
        viewModelScope.launch {
            repository.updateTable(table.copy(status = newStatus))
        }
    }

    // Product Management
    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.insertProduct(product)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Worker Management
    fun addWorker(worker: Worker) {
        viewModelScope.launch {
            repository.insertWorker(worker)
        }
    }

    fun updateWorker(worker: Worker) {
        viewModelScope.launch {
            repository.updateWorker(worker)
        }
    }

    fun deleteWorker(worker: Worker) {
        viewModelScope.launch {
            repository.deleteWorker(worker)
        }
    }

    // Attendance Management
    fun markAttendance(workerId: Int, workerName: String, date: String, status: String) {
        viewModelScope.launch {
            repository.insertAttendance(
                WorkerAttendance(
                    workerId = workerId,
                    workerName = workerName,
                    date = date,
                    status = status
                )
            )
        }
    }

    fun hasAutoBackup(context: android.content.Context): Boolean {
        return com.example.data.ExportImportHelper.hasPersistentBackup(context)
    }

    suspend fun restoreAutoBackup(context: android.content.Context): Boolean {
        return com.example.data.ExportImportHelper.restoreAutoBackup(context, repository.dao)
    }

    fun addSampleWorkersIfEmpty() {
        viewModelScope.launch {
            val current = repository.dao.getAllWorkersSync()
            if (current.isEmpty()) {
                val sampleWorkers = listOf(
                    Worker(name = "Ali Hassan", phone = "03001234567", role = "Head Chef", assignedTable = "Kitchen", salary = 45000.0, joiningDate = "2024-01-10", status = "Active"),
                    Worker(name = "Muhammad Usman", phone = "03129876543", role = "Cashier", assignedTable = "Counter", salary = 35000.0, joiningDate = "2024-02-15", status = "Active"),
                    Worker(name = "Bilal Ahmed", phone = "03335551234", role = "Waiter", assignedTable = "T1, T2", salary = 25000.0, joiningDate = "2024-03-01", status = "Active"),
                    Worker(name = "Hamza Khan", phone = "03451112233", role = "Waiter", assignedTable = "T3, T4", salary = 25000.0, joiningDate = "2024-03-15", status = "Active"),
                    Worker(name = "Tariq Mehmood", phone = "03214443322", role = "Delivery Rider", assignedTable = "Rider Desk", salary = 28000.0, joiningDate = "2024-04-01", status = "Active")
                )
                sampleWorkers.forEach { repository.insertWorker(it) }
            }
        }
    }

    suspend fun exportData(context: android.content.Context): android.content.Intent? {
        return com.example.data.ExportImportHelper.exportDatabaseToJson(context, repository.dao)
    }

    fun importData(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            com.example.data.ExportImportHelper.importDatabaseFromJson(context, uri, repository.dao)
        }
    }
}
