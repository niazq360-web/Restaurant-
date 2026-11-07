package com.example.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.SettingsManager
import com.example.ui.dashboard.copyUriToInternalStorage
import androidx.compose.material.icons.automirrored.filled.Logout
import com.example.data.auth.UserProfile
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantProfileScreen(
    userProfile: UserProfile? = null,
    onLogout: () -> Unit = {},
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }

    var restaurantName by remember { mutableStateOf(settingsManager.getRestaurantName()) }
    var location by remember { mutableStateOf(settingsManager.getRestaurantLocation()) }
    var phone by remember { mutableStateOf(settingsManager.getRestaurantPhone()) }
    var ownerName by remember { mutableStateOf(settingsManager.getOwnerName()) }
    var ownerPhone by remember { mutableStateOf(settingsManager.getOwnerPhone()) }
    var managerPhone by remember { mutableStateOf(settingsManager.getManagerPhone()) }
    var autoSmsPayment by remember { mutableStateOf(settingsManager.isAutoSmsOnPayment()) }
    var autoSmsDelivery by remember { mutableStateOf(settingsManager.isAutoSmsOnDelivery()) }
    var receiptFooter by remember { mutableStateOf(settingsManager.getReceiptFooter()) }
    
    var logoPath by remember { mutableStateOf(settingsManager.getLogoPath()) }
    var qrPath by remember { mutableStateOf(settingsManager.getQrCodePath()) }
    var userPhotoPath by remember { mutableStateOf(settingsManager.getUserPhotoPath()) }

    var isSaved by remember { mutableStateOf(false) }

    // User / Owner Photo Picker
    val userPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = copyUriToInternalStorage(context, uri, "owner_profile_photo.jpg")
            if (saved != null) {
                settingsManager.saveUserPhotoPath(saved)
                userPhotoPath = saved
                Toast.makeText(context, "Profile Photo uploaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Logo Image Picker
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = copyUriToInternalStorage(context, uri, "restaurant_logo.jpg")
            if (saved != null) {
                settingsManager.saveLogoPath(saved)
                logoPath = saved
                Toast.makeText(context, "Logo uploaded successfully!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // QR Code Image Picker
    val qrPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val saved = copyUriToInternalStorage(context, uri, "payment_qr_code.jpg")
            if (saved != null) {
                settingsManager.saveQrCodePath(saved)
                qrPath = saved
                Toast.makeText(context, "Online Payment QR Code saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Restaurant & Owner Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Logo & Branding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Restaurant Logo & Branding", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("This logo will be printed on bills, thermal receipts, and PDF reports.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (logoPath.isNotBlank() && File(logoPath).exists()) {
                                AsyncImage(
                                    model = File(logoPath),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { logoPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (logoPath.isNotBlank()) "Change Logo" else "Upload Logo")
                            }
                            if (logoPath.isNotBlank()) {
                                TextButton(onClick = {
                                    settingsManager.saveLogoPath("")
                                    logoPath = ""
                                }) {
                                    Text("Remove Logo", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Owner & Restaurant Information
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Owner & Business Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Owner Photo Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userPhotoPath.isNotBlank() && File(userPhotoPath).exists()) {
                                AsyncImage(
                                    model = File(userPhotoPath),
                                    contentDescription = "Owner Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedButton(
                                onClick = { userPhotoPicker.launch("image/*") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (userPhotoPath.isNotBlank()) "Change Owner Photo" else "Upload Owner Photo", fontSize = 12.sp)
                            }
                            Text("Displays in app navigation & profile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Owner Fields
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Owner Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = ownerPhone,
                        onValueChange = { ownerPhone = it },
                        label = { Text("Owner WhatsApp / Mobile *") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // Restaurant Fields
                    OutlinedTextField(
                        value = restaurantName,
                        onValueChange = { restaurantName = it },
                        label = { Text("Restaurant / Brand Name *") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Address / Branch Location *") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Restaurant Landline / Order Booking Phone *") },
                        leadingIcon = { Icon(Icons.Default.Call, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section: Manager & Notification Alerts (Auto-SMS & WhatsApp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Manager Notification Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Automatic SMS and WhatsApp notifications sent directly to the Manager or Owner.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = managerPhone,
                        onValueChange = { managerPhone = it },
                        label = { Text("Manager / Alert Mobile Number") },
                        placeholder = { Text("e.g. 03001234567") },
                        leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-SMS on Payment Received", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Send SMS when cash or online payment is recorded.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSmsPayment,
                            onCheckedChange = { autoSmsPayment = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-SMS on Delivery Completed", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Send SMS when delivery boy verifies customer OTP.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoSmsDelivery,
                            onCheckedChange = { autoSmsDelivery = it }
                        )
                    }
                }
            }

            // Section 3: Online Payment QR Code
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Online Payment QR Code (JazzCash / Easypaisa / Raast)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Customers can scan this QR code directly at the checkout counter to pay.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrPath.isNotBlank() && File(qrPath).exists()) {
                                AsyncImage(
                                    model = File(qrPath),
                                    contentDescription = "Payment QR",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { qrPicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (qrPath.isNotBlank()) "Change QR" else "Upload Payment QR")
                            }
                            if (qrPath.isNotBlank()) {
                                Text("✓ QR Code Active on POS", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Section 4: Receipt Footer Note
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bill Receipt Footer Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = receiptFooter,
                        onValueChange = { receiptFooter = it },
                        label = { Text("Footer Message on Customer Receipt") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (restaurantName.isNotBlank() && location.isNotBlank()) {
                        settingsManager.saveRestaurantProfile(
                            restaurantName = restaurantName,
                            location = location,
                            phone = phone,
                            ownerName = ownerName,
                            ownerPhone = ownerPhone,
                            receiptFooter = receiptFooter
                        )
                        settingsManager.setManagerPhone(managerPhone)
                        settingsManager.setAutoSmsOnPayment(autoSmsPayment)
                        settingsManager.setAutoSmsOnDelivery(autoSmsDelivery)
                        isSaved = true
                        Toast.makeText(context, "Restaurant & Manager Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Please fill in Restaurant Name and Address.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save All Profile Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Firebase Account Information Card
            if (userProfile != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = "Firebase Authenticated Account",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text("Name: ${userProfile.fullName}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Email: ${userProfile.email}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Role: ${userProfile.getDisplayRole()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)

                        OutlinedButton(
                            onClick = onLogout,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out of Firebase Account", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
