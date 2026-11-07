package com.example.ui.delivery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Order
import com.example.data.SettingsManager
import com.example.ui.pos.PosViewModel
import com.example.utils.NotificationHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryScreen(viewModel: PosViewModel, onBack: (() -> Unit)? = null) {
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val deliveryOrders by viewModel.deliveryOrders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Active Deliveries, 1: Rider Route & Google Map, 2: Completed
    var selectedOrderForMap by remember { mutableStateOf<Order?>(null) }
    var deliveryCompletionNotification by remember { mutableStateOf<Pair<Order, String>?>(null) }

    val activeDeliveries = remember(deliveryOrders) {
        deliveryOrders.filter { it.status == "Pending" || it.status == "Out for Delivery" }
            .sortedWith(compareByDescending<Order> { it.deliveryPriority }.thenBy { it.timestamp })
    }

    val completedDeliveries = remember(deliveryOrders) {
        deliveryOrders.filter { it.status == "Completed" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text("Delivery & Rider Route", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "${activeDeliveries.size} active delivery orders",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            actions = {
                // Quick Call Manager button
                IconButton(
                    onClick = {
                        val mgrPhone = settingsManager.getManagerPhone()
                        if (mgrPhone.isNotBlank()) {
                            NotificationHelper.dialPhoneNumber(context, mgrPhone)
                        } else {
                            Toast.makeText(context, "Manager phone not set in Profile.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = "Call Manager", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        // Segmented Tabs
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SegmentedButton(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) {
                Text("Active (${activeDeliveries.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) {
                Text("Map & Route", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) {
                Text("History (${completedDeliveries.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        when (selectedTab) {
            0 -> {
                // Active Deliveries List
                if (activeDeliveries.isEmpty()) {
                    EmptyDeliveriesPlaceholder("No active delivery orders pending.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(activeDeliveries) { order ->
                            DeliveryOrderCard(
                                order = order,
                                onDispatch = { riderNo, otp ->
                                    viewModel.dispatchDelivery(order, riderNo, otp)
                                },
                                onComplete = { paymentMethod ->
                                    viewModel.completeDelivery(order, paymentMethod)
                                    // Send auto SMS to Manager/Owner
                                    val managerPhone = settingsManager.getManagerPhone()
                                    val autoSms = settingsManager.isAutoSmsOnDelivery()
                                    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                    val msg = "✅ DELIVERY CONFIRMED!\n" +
                                            "Order #${order.id} delivered to ${order.customerName}.\n" +
                                            "Address: ${order.deliveryAddress}\n" +
                                            "Amount: Rs. ${"%.0f".format(order.totalAmount)} ($paymentMethod)\n" +
                                            "OTP Verified: ${order.deliveryOtp}\n" +
                                            "Time: $timeStr"

                                    if (autoSms && managerPhone.isNotBlank()) {
                                        NotificationHelper.sendAutoSms(context, managerPhone, msg)
                                    }
                                    deliveryCompletionNotification = Pair(order, msg)
                                },
                                onPrioritize = { newPriority ->
                                    viewModel.updateDeliveryPriority(order, newPriority)
                                    Toast.makeText(context, "Delivery priority updated!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Rider Map & Route Planner
                RiderMapRouteSection(
                    activeOrders = activeDeliveries,
                    onSelectOrder = { order -> selectedOrderForMap = order },
                    onPrioritize = { order, prio ->
                        viewModel.updateDeliveryPriority(order, prio)
                    }
                )
            }
            2 -> {
                // Completed Deliveries
                if (completedDeliveries.isEmpty()) {
                    EmptyDeliveriesPlaceholder("No completed deliveries yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(completedDeliveries) { order ->
                            CompletedDeliveryCard(order = order)
                        }
                    }
                }
            }
        }
    }

    // Delivery Completed Alert Dialog with WhatsApp Manager action
    if (deliveryCompletionNotification != null) {
        val (completedOrder, messageText) = deliveryCompletionNotification!!
        val managerPhone = settingsManager.getManagerPhone()

        AlertDialog(
            onDismissRequest = { deliveryCompletionNotification = null },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(44.dp)
                )
            },
            title = {
                Text("Delivery Successfully Verified!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Order #${completedOrder.id} has been received by ${completedOrder.customerName}.",
                        fontSize = 13.sp
                    )
                    Text(
                        "Payment: Rs. ${"%.2f".format(completedOrder.totalAmount)} (${completedOrder.paymentMethod})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    if (managerPhone.isNotBlank()) {
                        Text(
                            "Manager ($managerPhone) was notified via automatic SMS.",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        deliveryCompletionNotification = null
                        if (managerPhone.isNotBlank()) {
                            NotificationHelper.sendWhatsAppMessage(context, managerPhone, messageText)
                        } else {
                            NotificationHelper.sendWhatsAppMessage(context, "", messageText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("Notify on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deliveryCompletionNotification = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun EmptyDeliveriesPlaceholder(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DeliveryDining, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text, color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun DeliveryOrderCard(
    order: Order,
    onDispatch: (String, String) -> Unit,
    onComplete: (String) -> Unit,
    onPrioritize: (Int) -> Unit
) {
    var riderNumber by remember { mutableStateOf(order.riderNumber) }
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isPriorityStop = order.deliveryPriority > 0

    fun performDispatch() {
        if (riderNumber.isBlank()) return
        val otp = (1000..9999).random().toString()
        onDispatch(riderNumber, otp)

        val customerMsg = "Hi ${order.customerName},\nYour order #${order.id} is on the way!\nRider No: $riderNumber\nOTP to receive parcel: $otp\nTotal: Rs. ${order.totalAmount}"
        val riderMsg = "New Delivery for ${order.customerName}.\nAddress: ${order.deliveryAddress}\nPhone: ${order.customerPhone}\nOTP to collect from customer: $otp"

        if (order.customerPhone.isNotBlank()) {
            NotificationHelper.sendAutoSms(context, order.customerPhone, customerMsg)
        }
        NotificationHelper.sendAutoSms(context, riderNumber, riderMsg)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            performDispatch()
        } else {
            Toast.makeText(context, "SMS Permission is recommended to send automated OTPs.", Toast.LENGTH_LONG).show()
            performDispatch()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isPriorityStop) 2.dp else 1.dp,
                color = if (isPriorityStop) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(if (isPriorityStop) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Order #, Customer & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Order #${order.id}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPriorityStop) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "STOP #${order.deliveryPriority} (FIRST)",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                val statusColor = when (order.status) {
                    "Pending" -> Color(0xFFEA580C)
                    "Out for Delivery" -> Color(0xFF2563EB)
                    "Completed" -> Color(0xFF16A34A)
                    else -> Color.Gray
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        order.status.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Customer Name & Contact
            Text(
                text = order.customerName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Address Card with Google Maps button
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFEA4335), // Google Maps Red
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = order.deliveryAddress.ifBlank { "Address not specified" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Navigation & Contact Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Open in Google Maps
                        Button(
                            onClick = {
                                NotificationHelper.openGoogleMapsNavigation(context, order.deliveryAddress)
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)) // Google Maps Blue
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("Google Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Call Customer
                        if (order.customerPhone.isNotBlank()) {
                            OutlinedButton(
                                onClick = { NotificationHelper.dialPhoneNumber(context, order.customerPhone) },
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call", fontSize = 11.sp)
                            }

                            // WhatsApp Customer
                            IconButton(
                                onClick = {
                                    NotificationHelper.sendWhatsAppMessage(
                                        context,
                                        order.customerPhone,
                                        "Salam ${order.customerName}, your delivery parcel from Restaurant is on its way!"
                                    )
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                            }
                        }
                    }
                }
            }

            // Bill Total & Priority Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Total: Rs. ${"%.2f".format(order.totalAmount)} (${order.paymentMethod})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Select Delivery Sequence / Priority
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = {
                            val nextPrio = if (order.deliveryPriority == 1) 0 else 1
                            onPrioritize(nextPrio)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = if (isPriorityStop) Icons.Default.Check else Icons.Default.LowPriority,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isPriorityStop) "Stop #1 Active" else "Deliver First",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider()

            // Order Status Actions
            when (order.status) {
                "Pending" -> {
                    OutlinedTextField(
                        value = riderNumber,
                        onValueChange = { riderNumber = it },
                        label = { Text("Assign Rider Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (riderNumber.isNotBlank()) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                    performDispatch()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            } else {
                                Toast.makeText(context, "Please enter Rider Phone Number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Dispatch & Send Auto-SMS with OTP")
                    }
                }
                "Out for Delivery" -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rider: ${order.riderNumber}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        TextButton(
                            onClick = {
                                val message = "Hi ${order.customerName},\nYour order #${order.id} is on the way!\nRider Number: ${order.riderNumber}\nProvide this OTP to rider: ${order.deliveryOtp}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share OTP via..."))
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reshare OTP", fontSize = 12.sp)
                        }
                    }

                    // OTP Input from Customer
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = {
                            enteredOtp = it
                            otpError = false
                        },
                        label = { Text("Enter 4-Digit OTP from Customer") },
                        placeholder = { Text("Ask customer for OTP code") },
                        isError = otpError,
                        trailingIcon = {
                            if (enteredOtp == order.deliveryOtp && enteredOtp.isNotBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = Color(0xFF16A34A))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (otpError) {
                        Text(
                            "Invalid OTP code. Please verify code with customer.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (enteredOtp == order.deliveryOtp) {
                                if (order.paymentStatus == "Pending") {
                                    showPaymentDialog = true
                                } else {
                                    onComplete(order.paymentMethod)
                                }
                            } else {
                                otpError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Verify Customer OTP & Finish Delivery")
                    }
                }
            }
        }
    }

    if (showPaymentDialog) {
        var selectedMethod by remember { mutableStateOf(order.paymentMethod.ifBlank { "Cash" }) }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Confirm Cash/Online Collection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Total Amount to Collect: Rs. ${"%.2f".format(order.totalAmount)}", fontWeight = FontWeight.Bold)
                    Text("Select payment mode received:")
                    listOf("Cash", "Online / QR (JazzCash/Easypaisa)").forEach { method ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedMethod.startsWith(method.take(4)),
                                onClick = { selectedMethod = method }
                            )
                            Text(method, modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPaymentDialog = false
                    onComplete(selectedMethod)
                }) {
                    Text("Payment Received & Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CompletedDeliveryCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Order #${order.id} - ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Address: ${order.deliveryAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Rs. ${"%.2f".format(order.totalAmount)} (${order.paymentMethod}) • OTP Verified: ${order.deliveryOtp}",
                    fontSize = 11.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RiderMapRouteSection(
    activeOrders: List<Order>,
    onSelectOrder: (Order) -> Unit,
    onPrioritize: (Order, Int) -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val restaurantName = settingsManager.getRestaurantName()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Visual Interactive Delivery Route Map Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Deep Map Theme
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerX = w / 2f
                    val centerY = h / 2f

                    // Grid lines for map feel
                    val gridPaint = Color(0xFF1E293B)
                    for (x in 0..w.toInt() step 60) {
                        drawLine(gridPaint, Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), 1f)
                    }
                    for (y in 0..h.toInt() step 60) {
                        drawLine(gridPaint, Offset(0f, y.toFloat()), Offset(w, y.toFloat()), 1f)
                    }

                    // Restaurant Hub (Center point)
                    drawCircle(Color(0xFFEA580C), radius = 16f, center = Offset(centerX, centerY))
                    drawCircle(Color.White, radius = 6f, center = Offset(centerX, centerY))

                    // Draw delivery pins and route path
                    if (activeOrders.isNotEmpty()) {
                        val angleStep = (2 * Math.PI / activeOrders.size.coerceAtLeast(1)).toFloat()
                        var previousPoint = Offset(centerX, centerY)

                        activeOrders.forEachIndexed { index, order ->
                            val angle = index * angleStep - (Math.PI / 2).toFloat()
                            val radius = (minOf(w, h) * 0.36f)
                            val px = centerX + (radius * Math.cos(angle.toDouble())).toFloat()
                            val py = centerY + (radius * Math.sin(angle.toDouble())).toFloat()
                            val customerPoint = Offset(px, py)

                            // Connecting dashed route line
                            drawLine(
                                color = if (order.deliveryPriority > 0) Color(0xFF38BDF8) else Color(0xFF64748B),
                                start = previousPoint,
                                end = customerPoint,
                                strokeWidth = 3f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            // Customer stop marker
                            drawCircle(
                                color = if (order.deliveryPriority > 0) Color(0xFF22C55E) else Color(0xFF38BDF8),
                                radius = 14f,
                                center = customerPoint
                            )
                            drawCircle(Color.White, radius = 4f, center = customerPoint)

                            previousPoint = customerPoint
                        }
                    }
                }

                // Overlay Map Legends & Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "📍 $restaurantName (Kitchen)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any stop below to navigate via Google Maps",
                        color = Color.LightGray,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Text(
            text = "Delivery Route Sequence (Select Next Stop):",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        if (activeOrders.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("No active delivery destinations to plot.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(activeOrders) { order ->
                    val isFirst = order.deliveryPriority > 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectOrder(order) }
                            .border(
                                width = if (isFirst) 1.5.dp else 1.dp,
                                color = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isFirst) "1" else "#",
                                    color = if (isFirst) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Order #${order.id} • ${order.customerName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    order.deliveryAddress.ifBlank { "No address given" },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Collect: Rs. ${"%.0f".format(order.totalAmount)} (${order.paymentMethod})",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Google Maps Button
                            IconButton(
                                onClick = {
                                    NotificationHelper.openGoogleMapsNavigation(context, order.deliveryAddress)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Navigation,
                                    contentDescription = "Navigate",
                                    tint = Color(0xFF1A73E8)
                                )
                            }

                            // Priority Toggle Button
                            Button(
                                onClick = {
                                    onPrioritize(order, if (isFirst) 0 else 1)
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isFirst) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (isFirst) "Selected #1" else "Deliver Next",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
