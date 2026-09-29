package com.example.ui.invoices

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.Order
import com.example.data.OrderItem
import com.example.data.SettingsManager
import com.example.ui.pos.PosViewModel
import com.example.utils.BluetoothPrinterManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CustomerSummary(
    val name: String,
    val phone: String,
    val totalOrders: Int,
    val totalSpent: Double,
    val lastVisit: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(viewModel: PosViewModel, onBack: (() -> Unit)? = null) {
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var selectedCustomer by remember { mutableStateOf<CustomerSummary?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Invoices, 1: Customer History
    var searchQuery by remember { mutableStateOf("") }

    // Grouping orders by customer name / phone
    val customerSummaries = remember(orders) {
        orders.filter { it.status != "Cancelled" }
            .groupBy { 
                val name = it.customerName.ifBlank { "Walk-in" }
                val phone = it.customerPhone.trim()
                if (phone.isNotBlank()) "$name###$phone" else name
            }
            .map { (key, custOrders) ->
                val parts = key.split("###")
                val name = parts[0]
                val phone = if (parts.size > 1) parts[1] else ""
                CustomerSummary(
                    name = name,
                    phone = phone,
                    totalOrders = custOrders.size,
                    totalSpent = custOrders.sumOf { it.totalAmount },
                    lastVisit = custOrders.maxOf { it.timestamp }
                )
            }
            .sortedByDescending { it.lastVisit }
    }

    val filteredInvoices = remember(orders, searchQuery) {
        if (searchQuery.isBlank()) orders else {
            orders.filter {
                it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.customerPhone.contains(searchQuery) ||
                it.id.toString().contains(searchQuery) ||
                it.tableName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val filteredCustomers = remember(customerSummaries, searchQuery) {
        if (searchQuery.isBlank()) customerSummaries else {
            customerSummaries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Invoices & Customers", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Invoices & Bills (${orders.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Customer History (${customerSummaries.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(if (selectedTab == 0) "Search by Invoice #, Customer, or Table" else "Search Customer by Name or Mobile") },
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
            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 0) {
                // Invoices List
                if (filteredInvoices.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No invoices found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredInvoices) { order ->
                            InvoiceCard(order = order) {
                                selectedOrder = order
                            }
                        }
                    }
                }
            } else {
                // Customer History List
                if (filteredCustomers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No customer records found.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(filteredCustomers) { cust ->
                            CustomerCard(customer = cust) {
                                selectedCustomer = cust
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOrder?.let { order ->
        InvoiceDetailDialog(
            order = order,
            viewModel = viewModel,
            onDismiss = { selectedOrder = null }
        )
    }

    selectedCustomer?.let { cust ->
        CustomerHistoryDialog(
            customer = cust,
            orders = orders.filter { 
                it.customerName.equals(cust.name, ignoreCase = true) ||
                (cust.phone.isNotBlank() && it.customerPhone == cust.phone)
            },
            onDismiss = { selectedCustomer = null }
        )
    }
}

@Composable
fun CustomerCard(customer: CustomerSummary, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    customer.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (customer.phone.isNotBlank()) {
                    Text("📞 ${customer.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Total Orders: ${customer.totalOrders}  •  Last: ${dateFormat.format(Date(customer.lastVisit))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Total Spent", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${"%.0f".format(customer.totalSpent)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun CustomerHistoryDialog(
    customer: CustomerSummary,
    orders: List<Order>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Customer History: ${customer.name}", fontWeight = FontWeight.Bold)
                if (customer.phone.isNotBlank()) {
                    Text("Mobile: ${customer.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Orders", fontSize = 11.sp)
                            Text("${customer.totalOrders}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Spent", fontSize = 11.sp)
                            Text("Rs. ${"%.0f".format(customer.totalSpent)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Avg Bill", fontSize = 11.sp)
                            val avg = if (customer.totalOrders > 0) customer.totalSpent / customer.totalOrders else 0.0
                            Text("Rs. ${"%.0f".format(avg)}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Past Orders History:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(orders) { ord ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Order #${ord.id} (${ord.orderType})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Rs. ${"%.2f".format(ord.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(dateFormat.format(Date(ord.timestamp)), fontSize = 11.sp, color = Color.Gray)
                                if (ord.discountAmount > 0) {
                                    Text("Discount given: Rs. ${"%.2f".format(ord.discountAmount)}", fontSize = 11.sp, color = Color(0xFFDC2626))
                                }
                                Text("Payment: ${ord.paymentMethod} (${ord.paymentStatus})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun InvoiceCard(order: Order, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(order.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order #${order.id}", fontWeight = FontWeight.Bold)
                Text(order.status, color = if (order.status == "Completed") Color(0xFF16A34A) else Color(0xFFEA580C), fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Type: ${order.orderType}", fontSize = 13.sp)
            if (order.customerName.isNotBlank()) Text("Customer: ${order.customerName}", fontSize = 13.sp)
            if (order.customerPhone.isNotBlank()) Text("Phone: ${order.customerPhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (order.tableName.isNotBlank()) Text("Table: ${order.tableName}", fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateString, fontSize = 12.sp, color = Color.Gray)
                Column(horizontalAlignment = Alignment.End) {
                    if (order.discountAmount > 0) {
                        Text("Discount: Rs. ${"%.0f".format(order.discountAmount)}", fontSize = 11.sp, color = Color(0xFFDC2626))
                    }
                    Text("Rs. ${"%.2f".format(order.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun InvoiceDetailDialog(order: Order, viewModel: PosViewModel, onDismiss: () -> Unit) {
    val items by viewModel.getOrderItems(order.id).collectAsStateWithLifecycle(initialValue = emptyList())
    var isEditing by remember { mutableStateOf(false) }
    
    var editCustomerName by remember { mutableStateOf(order.customerName) }
    var editTable by remember { mutableStateOf(order.tableName) }
    var editStatus by remember { mutableStateOf(order.status) }
    var editTotal by remember { mutableStateOf(order.totalAmount.toString()) }
    
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val logoPath = settingsManager.getLogoPath()
    val coroutineScope = rememberCoroutineScope()
    val printerManager = remember { BluetoothPrinterManager(context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val canPrint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else {
            perms[Manifest.permission.BLUETOOTH] == true
        }
        if (canPrint) {
            coroutineScope.launch {
                val invoiceText = generateInvoiceText(order, items, settingsManager)
                printerManager.printInvoice(invoiceText)
            }
        }
    }

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
                Text("Invoice #${order.id} Details")
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isEditing) {
                    OutlinedTextField(value = editCustomerName, onValueChange = { editCustomerName = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editTable, onValueChange = { editTable = it }, label = { Text("Table Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editStatus, onValueChange = { editStatus = it }, label = { Text("Status") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editTotal, onValueChange = { editTotal = it }, label = { Text("Total Amount") }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text("Type: ${order.orderType}", fontWeight = FontWeight.SemiBold)
                    if (order.customerName.isNotBlank()) Text("Customer: ${order.customerName}")
                    if (order.customerPhone.isNotBlank()) Text("Phone: ${order.customerPhone}")
                    if (order.tableName.isNotBlank()) Text("Table: ${order.tableName}")
                    if (order.deliveryAddress.isNotBlank()) Text("Address: ${order.deliveryAddress}")
                    Text("Status: ${order.status}", color = MaterialTheme.colorScheme.primary)
                    Text("Payment: ${order.paymentStatus} via ${order.paymentMethod}")
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Items Ordered:", fontWeight = FontWeight.Bold)
                    
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(items) { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${item.quantity}x ${item.productName}", fontSize = 14.sp)
                                    if (item.category.isNotBlank()) Text(item.category, fontSize = 10.sp, color = Color.Gray)
                                }
                                Text("Rs. ${"%.2f".format(item.price * item.quantity)}", fontSize = 14.sp)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (order.discountAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 13.sp)
                            Text("Rs. ${"%.2f".format(order.subtotalAmount)}", fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount:", fontSize = 13.sp, color = Color(0xFFDC2626))
                            Text("- Rs. ${"%.2f".format(order.discountAmount)}", fontSize = 13.sp, color = Color(0xFFDC2626))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Amount", fontWeight = FontWeight.Bold)
                        Text("Rs. ${"%.2f".format(order.totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                Button(onClick = {
                    val updatedOrder = order.copy(
                        customerName = editCustomerName,
                        tableName = editTable,
                        status = editStatus,
                        totalAmount = editTotal.toDoubleOrNull() ?: order.totalAmount
                    )
                    viewModel.updateOrder(updatedOrder)
                    isEditing = false
                }) { Text("Save") }
            } else {
                Button(onClick = {
                    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                    } else {
                        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
                    }
                    val allGranted = requiredPermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
                    if (allGranted) {
                        coroutineScope.launch {
                            val invoiceText = generateInvoiceText(order, items, settingsManager)
                            printerManager.printInvoice(invoiceText)
                        }
                    } else {
                        permissionLauncher.launch(requiredPermissions)
                    }
                }) { Text("Print Receipt") }
            }
        },
        dismissButton = {
            Row {
                if (!isEditing) {
                    TextButton(onClick = { viewModel.deleteOrder(order.id); onDismiss() }) { Text("Delete", color = Color.Red) }
                    TextButton(onClick = { isEditing = true }) { Text("Edit") }
                }
                TextButton(onClick = {
                    if (isEditing) isEditing = false else onDismiss()
                }) { Text(if (isEditing) "Cancel" else "Close") }
            }
        }
    )
}

fun generateInvoiceText(order: Order, items: List<OrderItem>, settingsManager: SettingsManager): String {
    val restaurantName = settingsManager.getRestaurantName()
    val phone = settingsManager.getRestaurantPhone()
    val location = settingsManager.getRestaurantLocation()

    val sb = StringBuilder()
    sb.append("==============================\n")
    sb.append("      $restaurantName      \n")
    if (location.isNotBlank()) sb.append("   $location   \n")
    if (phone.isNotBlank()) sb.append("     Phone: $phone     \n")
    sb.append("==============================\n")
    sb.append("Invoice #: ${order.id}\n")
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    sb.append("Date: ${dateFormat.format(Date(order.timestamp))}\n")
    sb.append("Type: ${order.orderType}\n")
    if (order.tableName.isNotBlank()) sb.append("Table: ${order.tableName}\n")
    if (order.customerName.isNotBlank()) sb.append("Customer: ${order.customerName}\n")
    if (order.customerPhone.isNotBlank()) sb.append("Phone: ${order.customerPhone}\n")
    sb.append("------------------------------\n")
    sb.append(String.format("%-2s %-16s %8s\n", "Qt", "Item", "Amount"))
    sb.append("------------------------------\n")
    
    for (item in items) {
        val itemName = if (item.productName.length > 16) item.productName.substring(0, 15) else item.productName
        val amountStr = String.format(Locale.US, "%.2f", item.price * item.quantity)
        sb.append(String.format("%-2d %-16s %8s\n", item.quantity, itemName, amountStr))
    }
    
    sb.append("------------------------------\n")
    if (order.discountAmount > 0) {
        sb.append(String.format("%-19s %8s\n", "Subtotal:", String.format(Locale.US, "%.2f", order.subtotalAmount)))
        sb.append(String.format("%-19s %8s\n", "Discount:", String.format(Locale.US, "-%.2f", order.discountAmount)))
    }
    sb.append(String.format("%-19s %8s\n", "NET TOTAL:", String.format(Locale.US, "%.2f", order.totalAmount)))
    sb.append("Payment: ${order.paymentMethod} (${order.paymentStatus})\n")
    sb.append("==============================\n")
    sb.append("       Thank You!       \n")
    sb.append("       Visit Again      \n")
    sb.append("==============================\n")
    return sb.toString()
}
