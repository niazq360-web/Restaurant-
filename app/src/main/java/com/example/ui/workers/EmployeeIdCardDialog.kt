package com.example.ui.workers

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.SettingsManager
import com.example.data.Worker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EmployeeIdCardDialog(
    worker: Worker,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val restaurantName = settingsManager.getRestaurantName()
    val restaurantPhone = settingsManager.getRestaurantPhone()
    val restaurantLocation = settingsManager.getRestaurantLocation()
    val logoPath = settingsManager.getLogoPath()

    val empId = "EMP-${worker.id.toString().padStart(4, '0')}"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Employee ID Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Digital ID Card Container (Styled like a modern plastic PVC badge)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Badge Header Bar (Branded Gradient)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    )
                                )
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (logoPath.isNotBlank() && File(logoPath).exists()) {
                                    AsyncImage(
                                        model = File(logoPath),
                                        contentDescription = "Logo",
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = restaurantName.uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "STAFF IDENTIFICATION CARD",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Employee Photo with stylish frame
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!worker.imagePath.isNullOrBlank() && File(worker.imagePath).exists()) {
                                AsyncImage(
                                    model = File(worker.imagePath),
                                    contentDescription = worker.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Employee Name
                        Text(
                            text = worker.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        // Designation Badge
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = (if (worker.designation.isNotBlank()) worker.designation else worker.role).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Details Grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IdCardDetailRow(label = "Employee ID", value = empId)
                            IdCardDetailRow(label = "Contact Phone", value = worker.phone.ifBlank { "Not Provided" })
                            if (worker.assignedTable.isNotBlank()) {
                                IdCardDetailRow(label = "Assigned Station", value = "Table ${worker.assignedTable}")
                            }
                            IdCardDetailRow(
                                label = "Joining Date",
                                value = worker.joiningDate.ifBlank { "Registered Staff" }
                            )
                            IdCardDetailRow(
                                label = "Status",
                                value = worker.status,
                                valueColor = if (worker.status == "Active") Color(0xFF16A34A) else Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Barcode visual styling
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .background(Color(0xFFF8FAFC)),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(28) { i ->
                                    val barWidth = if (i % 3 == 0) 3.dp else if (i % 2 == 0) 1.5.dp else 2.dp
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(barWidth)
                                            .background(Color(0xFF1E293B))
                                    )
                                }
                            }
                            Text(
                                text = "*$empId*",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Footer with Restaurant Address & Contact
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9))
                                .padding(vertical = 8.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = restaurantLocation,
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Helpdesk: $restaurantPhone • Authorized By Management",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Save PDF & Send to WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            saveEmployeeIdCardPdf(
                                context = context,
                                worker = worker,
                                restaurantName = restaurantName,
                                restaurantLocation = restaurantLocation,
                                restaurantPhone = restaurantPhone,
                                logoPath = logoPath,
                                shareViaWhatsApp = true
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            saveEmployeeIdCardPdf(
                                context = context,
                                worker = worker,
                                restaurantName = restaurantName,
                                restaurantLocation = restaurantLocation,
                                restaurantPhone = restaurantPhone,
                                logoPath = logoPath,
                                shareViaWhatsApp = false
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdCardDetailRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1E293B)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

/**
 * Generates an Employee ID Card PDF file and saves to Downloads / shares via WhatsApp.
 */
fun saveEmployeeIdCardPdf(
    context: Context,
    worker: Worker,
    restaurantName: String,
    restaurantLocation: String,
    restaurantPhone: String,
    logoPath: String,
    shareViaWhatsApp: Boolean
) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(400, 600, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = android.graphics.Paint()

        // Card Background
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, 400f, 600f, paint)

        // Header Background
        paint.color = android.graphics.Color.rgb(30, 41, 59)
        canvas.drawRect(0f, 0f, 400f, 85f, paint)

        // Logo if present
        var textStartX = 24f
        if (logoPath.isNotBlank()) {
            val logoFile = File(logoPath)
            if (logoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(logoPath)
                if (bitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, 48, 48, true)
                    canvas.drawBitmap(scaled, 24f, 18f, null)
                    textStartX = 84f
                }
            }
        }

        // Restaurant Name in Header
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(restaurantName.uppercase(), textStartX, 44f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.rgb(203, 213, 225)
        canvas.drawText("OFFICIAL EMPLOYEE IDENTIFICATION CARD", textStartX, 62f, paint)

        // Photo Frame
        var yPos = 110f
        val photoCenterX = 200f
        paint.color = android.graphics.Color.rgb(241, 245, 249)
        canvas.drawRoundRect(140f, yPos, 260f, yPos + 130f, 12f, 12f, paint)

        if (!worker.imagePath.isNullOrBlank() && File(worker.imagePath).exists()) {
            val photoBitmap = BitmapFactory.decodeFile(worker.imagePath)
            if (photoBitmap != null) {
                val scaled = Bitmap.createScaledBitmap(photoBitmap, 120, 130, true)
                canvas.drawBitmap(scaled, 140f, yPos, null)
            }
        } else {
            paint.color = android.graphics.Color.rgb(100, 116, 139)
            paint.textSize = 12f
            canvas.drawText("PHOTO", 175f, yPos + 70f, paint)
        }

        yPos += 155f

        // Name
        paint.color = android.graphics.Color.rgb(15, 23, 42)
        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.textAlign = android.graphics.Paint.Align.CENTER
        canvas.drawText(worker.name, photoCenterX, yPos, paint)

        // Designation
        yPos += 24f
        paint.textSize = 13f
        paint.color = android.graphics.Color.rgb(37, 99, 235)
        val desig = (if (worker.designation.isNotBlank()) worker.designation else worker.role).uppercase()
        canvas.drawText(desig, photoCenterX, yPos, paint)

        // Details Table
        yPos += 30f
        paint.textAlign = android.graphics.Paint.Align.LEFT
        val empId = "EMP-${worker.id.toString().padStart(4, '0')}"

        val details = listOf(
            "Employee ID" to empId,
            "Contact Phone" to worker.phone.ifBlank { "N/A" },
            "Designation" to (if (worker.designation.isNotBlank()) worker.designation else worker.role),
            "Assigned Table" to worker.assignedTable.ifBlank { "Counter/General" },
            "Joining Date" to worker.joiningDate.ifBlank { "Active Staff" },
            "Status" to worker.status
        )

        for ((lbl, v) in details) {
            paint.color = android.graphics.Color.rgb(100, 116, 139)
            paint.textSize = 11f
            paint.isFakeBoldText = false
            canvas.drawText(lbl, 45f, yPos, paint)

            paint.color = android.graphics.Color.rgb(15, 23, 42)
            paint.isFakeBoldText = true
            canvas.drawText(v, 200f, yPos, paint)

            paint.color = android.graphics.Color.rgb(241, 245, 249)
            canvas.drawLine(45f, yPos + 4f, 355f, yPos + 4f, paint)
            yPos += 22f
        }

        // Footer
        paint.color = android.graphics.Color.rgb(241, 245, 249)
        canvas.drawRect(0f, 530f, 400f, 600f, paint)

        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.color = android.graphics.Color.rgb(71, 85, 105)
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText(restaurantLocation, photoCenterX, 555f, paint)
        canvas.drawText("Phone: $restaurantPhone  •  Authorized Management Signature", photoCenterX, 575f, paint)

        pdfDocument.finishPage(page)

        val fileName = "Employee_ID_${worker.name.replace(" ", "_")}_$empId.pdf"
        val cachedFile = File(context.cacheDir, fileName)
        pdfDocument.writeTo(FileOutputStream(cachedFile))
        pdfDocument.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cachedFile)

        if (shareViaWhatsApp) {
            val caption = "🪪 *Official Employee ID Card*\n" +
                    "Name: ${worker.name}\n" +
                    "ID: $empId\n" +
                    "Designation: $desig\n" +
                    "Restaurant: $restaurantName"

            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    setPackage("com.whatsapp")
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, caption)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(fallback, "Share Employee ID Card"))
            }
        } else {
            // Save to Downloads folder
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val destFile = File(downloadsDir, fileName)

                FileInputStream(cachedFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, "ID Card downloaded to Downloads: $fileName", Toast.LENGTH_LONG).show()

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Saved ID card: ${cachedFile.name}", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to generate ID card: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
