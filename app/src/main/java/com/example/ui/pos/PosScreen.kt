package com.example.ui.pos

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Product
import com.example.data.SettingsManager
import com.example.ui.dashboard.copyUriToInternalStorage
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { product ->
            val matchesCategory = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                product.name.contains(searchQuery, ignoreCase = true) ||
                product.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left side: Products Grid
        Column(modifier = Modifier.weight(2f).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Menu Items", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Button(onClick = { showCustomItemDialog = true }) {
                    Text("+ Add Custom Item")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search items by name or category") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Category filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProducts) { product ->
                    ProductCard(
                        product = product,
                        onClick = { viewModel.addToCart(product) },
                        onEdit = { productToEdit = product }
                    )
                }
            }
        }

        // Right side: Cart
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Order", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (cart.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (cart.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cart is empty.\nTap items on the left to add.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(cart) { item ->
                        CartItemRow(
                            item = item,
                            onAdd = { viewModel.addToCart(item.product) },
                            onRemove = { viewModel.updateCartQuantity(item.product, item.quantity - 1) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            val total = cart.sumOf { it.product.price * it.quantity }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtotal:", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Rs. ${"%.2f".format(total)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showCheckoutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = cart.isNotEmpty()
            ) {
                Text("Checkout & Bill", modifier = Modifier.padding(8.dp))
            }
        }
    }

    if (showCustomItemDialog) {
        CustomItemDialog(
            onDismiss = { showCustomItemDialog = false },
            onAdd = { name, price, qty, imgPath ->
                viewModel.addCustomItemToCart(name, price, qty)
                showCustomItemDialog = false
            }
        )
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            total = cart.sumOf { it.product.price * it.quantity },
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { customer, phone, address, type, table, payment, isPaid, discount ->
                viewModel.checkout(customer, phone, address, type, table, payment, isPaid, discount)
                showCheckoutDialog = false
            }
        )
    }

    if (productToEdit != null) {
        var name by remember { mutableStateOf(productToEdit?.name ?: "") }
        var price by remember { mutableStateOf(productToEdit?.price?.toString() ?: "") }
        var costPrice by remember { mutableStateOf(productToEdit?.costPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var quantity by remember { mutableStateOf(productToEdit?.stockCount?.takeIf { it >= 0 }?.toString() ?: "") }
        var imageUrl by remember { mutableStateOf(productToEdit?.imageUrl ?: "") }

        val imagePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val saved = copyUriToInternalStorage(context, uri, "product_${System.currentTimeMillis()}.jpg")
                if (saved != null) {
                    imageUrl = saved
                }
            }
        }

        AlertDialog(
            onDismissRequest = { productToEdit = null },
            title = { Text("Edit Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = File(imageUrl),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Fastfood, contentDescription = null)
                            }
                        }
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                            Text(if (imageUrl.isNotBlank()) "Change Image" else "Upload Image")
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost Price (Rs) - For Profit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Stock Quantity (Leave blank for unlimited)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val p = price.toDoubleOrNull()
                    val cp = costPrice.toDoubleOrNull() ?: 0.0
                    val q = quantity.toIntOrNull() ?: -1
                    if (name.isNotBlank() && p != null) {
                        viewModel.updateProduct(
                            productToEdit!!.copy(
                                name = name,
                                price = p,
                                costPrice = cp,
                                stockCount = q,
                                imageUrl = imageUrl.takeIf { it.isNotBlank() }
                            )
                        )
                        productToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { productToEdit = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CustomItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, Int, String?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var imageUrl by remember { mutableStateOf("") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = copyUriToInternalStorage(context, uri, "custom_${System.currentTimeMillis()}.jpg")
            if (saved != null) {
                imageUrl = saved
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Quick custom item entry for instant billing.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = File(imageUrl),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Fastfood, contentDescription = null)
                        }
                    }
                    OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                        Text(if (imageUrl.isNotBlank()) "Change Image" else "Upload Image")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val p = price.toDoubleOrNull()
                val q = quantity.toIntOrNull()
                if (name.isNotBlank() && p != null && q != null && q > 0) {
                    onAdd(name, p, q, imageUrl.takeIf { it.isNotBlank() })
                }
            }) {
                Text("Add to Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Product Image Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!product.imageUrl.isNullOrBlank()) {
                    val imgFile = File(product.imageUrl)
                    if (imgFile.exists()) {
                        AsyncImage(
                            model = imgFile,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        AsyncImage(
                            model = product.imageUrl,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    val catEmoji = when {
                        product.category.contains("Bakery", true) -> "🥐"
                        product.category.contains("Juice", true) -> "🧃"
                        product.category.contains("Fast", true) || product.category.contains("Burger", true) -> "🍔"
                        product.category.contains("Pizza", true) -> "🍕"
                        product.category.contains("Main", true) || product.category.contains("Karahi", true) -> "🍲"
                        product.category.contains("Side", true) || product.category.contains("Roti", true) -> "🥗"
                        product.category.contains("Beverage", true) || product.category.contains("Tea", true) -> "🥤"
                        product.category.contains("Dessert", true) -> "🍨"
                        else -> "🍽️"
                    }
                    Text(catEmoji, fontSize = 36.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(22.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(15.dp))
                }
            }
            Text(
                product.category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (product.stockCount >= 0) {
                Text("Stock: ${product.stockCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Rs. ${"%.2f".format(product.price)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.product.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Rs. ${"%.2f".format(item.product.price)} × ${item.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = "Remove", modifier = Modifier.size(16.dp))
            }
            Text("${item.quantity}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp))
            IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutDialog(
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, type: String, table: String, payment: String, isPaid: Boolean, discount: Double) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val qrPath = settingsManager.getQrCodePath()
    val logoPath = settingsManager.getLogoPath()

    var customerName by remember { mutableStateOf("Walk-in") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var tableName by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("") }
    
    val discount = discountInput.toDoubleOrNull() ?: 0.0
    val payableTotal = (total - discount).coerceAtLeast(0.0)

    val orderTypes = listOf("Dine-in", "Takeaway", "Delivery")
    var orderType by remember { mutableStateOf("Dine-in") }
    
    val paymentMethods = listOf("Cash", "QR / Online")
    var paymentMethod by remember { mutableStateOf("Cash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (logoPath.isNotBlank()) {
                    AsyncImage(
                        model = File(logoPath),
                        contentDescription = "Logo",
                        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                    )
                }
                Text("Checkout & Final Bill")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bill Summary Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 13.sp)
                            Text("Rs. ${"%.2f".format(total)}", fontSize = 13.sp)
                        }
                        if (discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount:", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                Text("- Rs. ${"%.2f".format(discount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Payable:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Rs. ${"%.2f".format(payableTotal)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Order Type Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    orderTypes.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = orderType == type,
                            onClick = { orderType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = orderTypes.size)
                        ) {
                            Text(type, fontSize = 12.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Customer Mobile Number (For History)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Discount field
                OutlinedTextField(
                    value = discountInput,
                    onValueChange = { discountInput = it },
                    label = { Text("Discount Amount (Rs)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (orderType == "Dine-in") {
                    OutlinedTextField(
                        value = tableName,
                        onValueChange = { tableName = it },
                        label = { Text("Table Number (e.g. T1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (orderType == "Delivery") {
                    OutlinedTextField(
                        value = deliveryAddress,
                        onValueChange = { deliveryAddress = it },
                        label = { Text("Delivery Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Payment Method Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    paymentMethods.forEachIndexed { index, method ->
                        SegmentedButton(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = paymentMethods.size)
                        ) {
                            Text(method, fontSize = 12.sp)
                        }
                    }
                }
                
                if (paymentMethod == "QR / Online") {
                    if (qrPath.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeFile(qrPath)
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.size(160.dp).align(Alignment.CenterHorizontally).padding(top = 4.dp)
                            )
                            Text("Please scan the QR code to pay.", modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 12.sp)
                        } else {
                            Text("QR code file not found. Please re-upload in settings.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    } else {
                        Text("No QR Code configured. Setup in Dashboard.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                val isPaid = (paymentMethod != "Cash")
                onConfirm(customerName, customerPhone, deliveryAddress, orderType, tableName, paymentMethod, isPaid, discount) 
            }) {
                Text("Complete & Print Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
