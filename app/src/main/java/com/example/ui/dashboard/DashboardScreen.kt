package com.example.ui.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.SettingsManager
import com.example.data.auth.UserProfile
import com.example.ui.pos.PosViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DashboardScreen(
    viewModel: PosViewModel, 
    userProfile: UserProfile? = null,
    onLogout: () -> Unit = {},
    onNavigateToPos: () -> Unit = {}, 
    onNavigateToDelivery: () -> Unit = {},
    onNavigateToInvoices: () -> Unit = {},
    onNavigateToSalesSummary: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {},
    onNavigateToTables: () -> Unit = {},
    onNavigateToWorkers: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    
    val restaurantName = settingsManager.getRestaurantName()
    val restaurantLocation = settingsManager.getRestaurantLocation()
    val ownerName = settingsManager.getOwnerName()
    val logoPath = settingsManager.getLogoPath()
    val qrPath = settingsManager.getQrCodePath()
    
    var userPhone by remember { mutableStateOf(settingsManager.getUserPhone()) }
    var userName by remember { mutableStateOf(settingsManager.getUserName()) }
    var userRole by remember { mutableStateOf(settingsManager.getUserRole()) }

    var showUserAccountDialog by remember { mutableStateOf(false) }
    var showAnnualRolloverDialog by remember { mutableStateOf(false) }
    var showRoleDeniedDialog by remember { mutableStateOf<String?>(null) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showFirebaseProfileDialog by remember { mutableStateOf(false) }

    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val workers by viewModel.workers.collectAsStateWithLifecycle()
    
    val totalSales = orders.sumOf { it.totalAmount }
    val totalTransactions = orders.size
    
    val activeTables = tables.count { it.status == "Occupied" }
    val totalTables = tables.size
    val activeWorkersCount = workers.count { it.status == "Active" }
    
    val activeDeliveries = orders.count { it.orderType == "Delivery" && it.status != "Completed" && it.status != "Cancelled" }

    // 12-Month Financial Year tracker
    var cycleNum by remember { mutableStateOf(settingsManager.getCycleNumber()) }
    val cycleStartDate = remember { settingsManager.getCycleStartDate() }
    val daysElapsed = remember {
        val days = ((System.currentTimeMillis() - cycleStartDate) / (24 * 60 * 60 * 1000L)).toInt()
        days.coerceIn(1, 365)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Logo, Restaurant Name, Owner, and Profile Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .clickable { 
                        if (userProfile != null && !userProfile.isAdmin()) {
                            showRoleDeniedDialog = "Restaurant Profile & System Configuration require Administrator privileges. You are currently logged in as ${userProfile.getDisplayRole()}."
                        } else {
                            onNavigateToProfile()
                        }
                    }
            ) {
                // Restaurant Logo / Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
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
                        val initial = restaurantName.firstOrNull()?.toString()?.uppercase() ?: "R"
                        Text(initial, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Column {
                    Text(
                        text = restaurantName.ifBlank { "My Restaurant" },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Owner: ${ownerName.ifBlank { "Tap to Set" }}  •  $restaurantLocation",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Firebase User Profile & Logout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { showFirebaseProfileDialog = true },
                    label = {
                        Column {
                            Text(
                                text = userProfile?.fullName?.ifBlank { userName.ifBlank { "Staff" } } ?: userName.ifBlank { "Staff" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = userProfile?.getDisplayRole() ?: userRole.ifBlank { "Cashier" },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandSecondary
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = "User Profile", 
                            tint = BrandSecondary, 
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = AssistChipDefaults.assistChipColors(containerColor = CleanSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CleanOutline),
                    modifier = Modifier.testTag("dashboard_user_profile_chip")
                )

                IconButton(
                    onClick = { showLogoutConfirmDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CleanSurface)
                        .border(1.dp, CleanOutline, RoundedCornerShape(10.dp))
                        .testTag("dashboard_logout_icon_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Sign Out",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 12-Month Financial Year & Rollover Banner (Annual Cycle Feature)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📅", fontSize = 18.sp)
                        Column {
                            Text("12-Month Financial Cycle: Year $cycleNum", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Day $daysElapsed of 365 Days Active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(
                        onClick = { 
                            if (userProfile != null && !userProfile.isAdmin()) {
                                showRoleDeniedDialog = "12-Month Financial Ledger Rollover requires Administrator privileges. You are currently logged in as ${userProfile.getDisplayRole()}."
                            } else {
                                showAnnualRolloverDialog = true 
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("12-Mo Rollover", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                LinearProgressIndicator(
                    progress = { (daysElapsed / 365f).coerceIn(0.01f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Grid Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today's Sales
            BentoCard(
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToSalesSummary() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("TOTAL SALES & PROFITS (Daily, Weekly, Yearly)", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text("Rs. ${"%.2f".format(totalSales)}", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("$totalTransactions total bills", color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Row 2: New Bill + Tables & Invoices
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // New Bill (POS)
                BentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigateToPos() }
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧾", fontSize = 20.sp)
                        }
                        Column {
                            Text("New Bill (POS)", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Quick Billing & Discounts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                }

                // Tables & Invoices Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoCard(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigateToTables() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🪑", fontSize = 22.sp)
                            Column {
                                Text("Tables", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("$activeTables/$totalTables Occupied", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    BentoCard(
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.weight(1f).fillMaxWidth().clickable { onNavigateToInvoices() }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("📄", fontSize = 22.sp)
                            Column {
                                Text("Invoices & Customers", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("$totalTransactions History", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Row 2.5: Staff Management (Workers) & Stock (Menu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Workers / Staff
                BentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f).clickable { 
                        if (userProfile != null && userProfile.isCashier() && !userProfile.isManager()) {
                            showRoleDeniedDialog = "Staff & Employee Attendance records require Manager or Administrator privileges. You are currently logged in as Cashier."
                        } else {
                            onNavigateToWorkers()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("👥", fontSize = 24.sp)
                        Column {
                            Text("Staff & Attendance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("$activeWorkersCount Active Staff", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Menu / Stock
                BentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f).clickable { onNavigateToMenu() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📋", fontSize = 24.sp)
                        Column {
                            Text("Menu & Items", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Add Photos & Price", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Row 3: Restaurant & Owner Profile
            BentoCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToProfile() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🏢", fontSize = 24.sp)
                        Column {
                            Text("Restaurant Profile & Business Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Owner: $ownerName • Logo & Online QR Code Setup",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("Edit ›", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
            }

            // Row 4: 12-Month Offline Backup & Restore
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f).clickable { 
                        CoroutineScope(Dispatchers.Main).launch {
                            val intent = viewModel.exportData(context)
                            if (intent != null) {
                                context.startActivity(Intent.createChooser(intent, "Share / Save Complete Offline Backup"))
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("☁️", fontSize = 24.sp)
                        Column {
                            Text("Save Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Export 12-Mo Data", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                BentoCard(
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.weight(1f).clickable { 
                        CoroutineScope(Dispatchers.Main).launch {
                            val restored = viewModel.restoreAutoBackup(context)
                            if (restored) {
                                Toast.makeText(context, "All 12-month records restored successfully!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No offline backup found in device storage.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("📥", fontSize = 24.sp)
                        Column {
                            Text("Restore Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("1-Tap Restore", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Row 5: Delivery
            BentoCard(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToDelivery() }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🛵", fontSize = 24.sp)
                    Column {
                        Text("Delivery Orders", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("$activeDeliveries Active Deliveries", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // 12-Month Annual Rollover Dialog (2-minute auto update & ledger renewal)
    if (showAnnualRolloverDialog) {
        AnnualRolloverDialog(
            currentYear = cycleNum,
            onComplete = {
                settingsManager.startNext12MonthCycle()
                cycleNum = settingsManager.getCycleNumber()
                showAnnualRolloverDialog = false
                Toast.makeText(context, "Year $cycleNum Financial Cycle Activated! All records are preserved.", Toast.LENGTH_LONG).show()
            }
        )
    }

    // Offline User Account Dialog
    if (showUserAccountDialog) {
        var tempUserPhone by remember { mutableStateOf(userPhone) }
        var tempUserName by remember { mutableStateOf(userName) }
        var tempUserRole by remember { mutableStateOf(userRole) }

        AlertDialog(
            onDismissRequest = { showUserAccountDialog = false },
            title = { Text("Offline User Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Sign in or switch user with mobile number. All existing restaurant records stay safe and offline.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempUserName,
                        onValueChange = { tempUserName = it },
                        label = { Text("User / Staff Name *") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempUserPhone,
                        onValueChange = { tempUserPhone = it },
                        label = { Text("Mobile Number (e.g. 03001234567) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempUserRole,
                        onValueChange = { tempUserRole = it },
                        label = { Text("Role (Manager, Cashier, Owner)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempUserName.isNotBlank() && tempUserPhone.isNotBlank()) {
                        settingsManager.saveUserAccount(tempUserPhone, tempUserName, tempUserRole)
                        userName = tempUserName
                        userPhone = tempUserPhone
                        userRole = tempUserRole
                        showUserAccountDialog = false
                        Toast.makeText(context, "Logged in as $tempUserName ($tempUserPhone)", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Save & Login")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserAccountDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Role-Based Access Denied Dialog
    if (showRoleDeniedDialog != null) {
        AlertDialog(
            onDismissRequest = { showRoleDeniedDialog = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Access Restricted",
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            },
            text = {
                Text(
                    text = showRoleDeniedDialog ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showRoleDeniedDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Understood")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CleanSurface
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Sign Out of Firebase?",
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            },
            text = {
                Text(
                    text = "You will be signed out of this session. You will need your Firebase email & password to sign back in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_logout_button")
                ) {
                    Text("Sign Out", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CleanSurface
        )
    }

    // Firebase User Profile Detail Dialog
    if (showFirebaseProfileDialog && userProfile != null) {
        AlertDialog(
            onDismissRequest = { showFirebaseProfileDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = null,
                    tint = BrandSecondary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Authenticated Staff Profile",
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CleanSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("FULL NAME", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(userProfile.fullName.ifBlank { "Staff" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("EMAIL ADDRESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(userProfile.email.ifBlank { "N/A" }, fontSize = 13.sp, color = TextPrimary)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("ASSIGNED ROLE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (userProfile.isAdmin()) BrandSecondaryContainer else CleanOutlineVariant
                                ) {
                                    Text(
                                        text = userProfile.getDisplayRole().uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (userProfile.isAdmin()) BrandOnSecondaryContainer else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("FIREBASE UID", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Text(userProfile.uid, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
                        }
                    }

                    Text(
                        text = "🔒 Security Note: Roles are securely managed via Firebase Firestore and evaluated against Firebase Security Rules.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFirebaseProfileDialog = false
                        showLogoutConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFirebaseProfileDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CleanSurface
        )
    }
}

@Composable
fun BentoCard(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(16.dp))
                else Modifier
            )
    ) {
        content()
    }
}
