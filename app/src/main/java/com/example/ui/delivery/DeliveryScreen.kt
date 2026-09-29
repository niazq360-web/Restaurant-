package com.example.ui.delivery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import com.example.data.Order
import com.example.ui.pos.PosViewModel

fun sendDirectSms(context: Context, phone: String, message: String) {
    try {
        val smsManager = context.getSystemService(android.telephony.SmsManager::class.java)
        smsManager.sendTextMessage(phone, null, message, null, null)
        Toast.makeText(context, "SMS sent to $phone", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to send SMS to $phone: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun DeliveryScreen(viewModel: PosViewModel, onBack: (() -> Unit)? = null) {
    if (onBack != null) {
        BackHandler { onBack() }
    }
    val deliveryOrders by viewModel.deliveryOrders.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Delivery Management", color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (deliveryOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No delivery orders found.", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(deliveryOrders) { order ->
                    DeliveryOrderCard(
                        order = order,
                        onDispatch = { riderNo, otp -> viewModel.dispatchDelivery(order, riderNo, otp) },
                        onComplete = { paymentMethod -> viewModel.completeDelivery(order, paymentMethod) },
                        onShareOtp = {
                            val message = "Hi ${order.customerName},\nYour order is on the way!\nRider Number: ${order.riderNumber}\nPlease provide this OTP to the rider to receive your parcel: ${order.deliveryOtp}"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share via..."))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryOrderCard(
    order: Order,
    onDispatch: (String, String) -> Unit,
    onComplete: (String) -> Unit,
    onShareOtp: () -> Unit
) {
    var riderNumber by remember { mutableStateOf(order.riderNumber) }
    var enteredOtp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun performDispatch() {
        if (riderNumber.isBlank()) return
        val otp = (1000..9999).random().toString()
        onDispatch(riderNumber, otp)
        
        val customerMsg = "Hi ${order.customerName},\nYour order is on the way!\nRider No: $riderNumber\nOTP to receive parcel: $otp"
        val riderMsg = "New Delivery for ${order.customerName}.\nAddress: ${order.deliveryAddress}\nPhone: ${order.customerPhone}\nOTP to collect: $otp"
        
        if (order.customerPhone.isNotBlank()) {
            sendDirectSms(context, order.customerPhone, customerMsg)
        }
        sendDirectSms(context, riderNumber, riderMsg)
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            performDispatch()
        } else {
            Toast.makeText(context, "SMS Permission is required to send automatic OTPs.", Toast.LENGTH_LONG).show()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order #${order.id} - ${order.customerName}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                val statusColor = when (order.status) {
                    "Pending" -> Color(0xFFEA580C)
                    "Out for Delivery" -> Color(0xFF2563EB)
                    "Completed" -> Color(0xFF16A34A)
                    else -> Color.Gray
                }
                Text(order.status, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("Phone: ${order.customerPhone}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("Address: ${order.deliveryAddress}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Total: Rs. ${"%.2f".format(order.totalAmount)} (${order.paymentStatus} via ${order.paymentMethod})", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            when (order.status) {
                "Pending" -> {
                    OutlinedTextField(
                        value = riderNumber,
                        onValueChange = { riderNumber = it },
                        label = { Text("Enter Rider Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (riderNumber.isNotBlank()) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
                                    performDispatch()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.SEND_SMS)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Dispatch Order & Send Auto-SMS")
                    }
                }
                "Out for Delivery" -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Rider: ${order.riderNumber}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        OutlinedButton(onClick = onShareOtp) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share OTP")
                        }
                    }
                    OutlinedTextField(
                        value = enteredOtp,
                        onValueChange = { 
                            enteredOtp = it
                            otpError = false
                        },
                        label = { Text("Enter OTP from Customer") },
                        isError = otpError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (otpError) {
                        Text("Invalid OTP", color = Color.Red, fontSize = 12.sp)
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Verify OTP & Complete Delivery")
                    }
                }
                "Completed" -> {
                    Text("Delivery Completed successfully.", color = Color(0xFF16A34A), fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showPaymentDialog) {
        var selectedMethod by remember { mutableStateOf("Cash") }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Payment Received") },
            text = {
                Column {
                    Text("How was the payment collected?")
                    Spacer(modifier = Modifier.height(8.dp))
                    listOf("Cash", "QR / Online").forEach { method ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedMethod == method, onClick = { selectedMethod = method })
                            Text(method, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPaymentDialog = false
                    onComplete(selectedMethod)
                }) {
                    Text("Confirm Payment & Complete")
                }
            }
        )
    }
}
