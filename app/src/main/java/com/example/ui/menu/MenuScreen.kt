package com.example.ui.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Product
import com.example.ui.dashboard.copyUriToInternalStorage
import com.example.ui.pos.PosViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(viewModel: PosViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
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

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Text("Menu Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name or category") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                singleLine = true
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
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
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Product Image / Icon
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
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
                                    Text(catEmoji, fontSize = 24.sp)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(product.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (product.stockCount >= 0) {
                                    Text("Stock: ${product.stockCount}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                                Text(
                                    "Rs. ${"%.2f".format(product.price)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { productToEdit = product }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { viewModel.deleteProduct(product) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || productToEdit != null) {
        val isEdit = productToEdit != null
        var name by remember { mutableStateOf(productToEdit?.name ?: "") }
        var price by remember { mutableStateOf(productToEdit?.price?.toString() ?: "") }
        var costPrice by remember { mutableStateOf(productToEdit?.costPrice?.takeIf { it > 0 }?.toString() ?: "") }
        var category by remember { mutableStateOf(productToEdit?.category ?: "") }
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
            onDismissRequest = { 
                showAddDialog = false
                productToEdit = null 
            },
            title = { Text(if (isEdit) "Edit Product" else "Add New Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Photo Upload Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = File(imageUrl),
                                    contentDescription = "Item Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Fastfood, contentDescription = null)
                            }
                        }
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }) {
                            Text(if (imageUrl.isNotBlank()) "Change Image" else "Upload Image / Icon")
                        }
                    }

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. Bakery, Juices)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Sale Price (Rs) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { costPrice = it },
                        label = { Text("Cost / Kharch Price (Rs) - For Profit Calculation") },
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
                    if (name.isNotBlank() && p != null && category.isNotBlank()) {
                        if (isEdit) {
                            viewModel.updateProduct(
                                productToEdit!!.copy(
                                    name = name,
                                    price = p,
                                    costPrice = cp,
                                    category = category,
                                    stockCount = q,
                                    imageUrl = imageUrl.takeIf { it.isNotBlank() }
                                )
                            )
                        } else {
                            viewModel.addProduct(
                                Product(
                                    name = name,
                                    price = p,
                                    costPrice = cp,
                                    category = category,
                                    stockCount = q,
                                    imageUrl = imageUrl.takeIf { it.isNotBlank() }
                                )
                            )
                        }
                        showAddDialog = false
                        productToEdit = null
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showAddDialog = false
                    productToEdit = null 
                }) { Text("Cancel") }
            }
        )
    }
}
