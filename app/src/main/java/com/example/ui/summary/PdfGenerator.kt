package com.example.ui.summary

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Order
import com.example.data.OrderItem
import com.example.data.SettingsManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    /**
     * Internal helper to create the PDF document file
     */
    fun createPdfFile(
        context: Context,
        reportType: String,
        orders: List<Order>,
        items: List<OrderItem>
    ): File? {
        val settingsManager = SettingsManager(context)
        val restaurantName = settingsManager.getRestaurantName()
        val restaurantPhone = settingsManager.getRestaurantPhone()
        val restaurantLocation = settingsManager.getRestaurantLocation()
        val logoPath = settingsManager.getLogoPath()

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        var yPos = 50f
        val margin = 45f
        val rightMargin = 550f

        // Draw Restaurant Logo if available
        if (logoPath.isNotBlank()) {
            val logoFile = File(logoPath)
            if (logoFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(logoPath)
                if (bitmap != null) {
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 48, 48, true)
                    canvas.drawBitmap(scaled, margin, yPos - 10f, null)
                }
            }
        }

        val textStartX = if (logoPath.isNotBlank()) margin + 58f else margin

        // Restaurant Title
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText(restaurantName, textStartX, yPos + 10f, paint)

        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        val subtitle = if (restaurantLocation.isNotBlank() && restaurantPhone.isNotBlank()) {
            "$restaurantLocation | Phone: $restaurantPhone"
        } else if (restaurantLocation.isNotBlank()) restaurantLocation else restaurantPhone
        if (subtitle.isNotBlank()) {
            canvas.drawText(subtitle, textStartX, yPos + 26f, paint)
        }
        yPos += 55f

        // Header Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, yPos, rightMargin, yPos, paint)
        yPos += 25f

        // Report Title & Date
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("$reportType Sales & Profit Report", margin, yPos, paint)

        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        paint.textSize = 11f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Generated: ${dateFormat.format(Date())}", 360f, yPos, paint)
        yPos += 30f

        // Filter orders
        val now = System.currentTimeMillis()
        val filterTime = when (reportType) {
            "Daily" -> now - 24 * 60 * 60 * 1000L
            "Weekly" -> now - 7 * 24 * 60 * 60 * 1000L
            "Monthly" -> now - 30L * 24 * 60 * 60 * 1000L
            "Yearly" -> now - 365L * 24 * 60 * 60 * 1000L
            else -> 0L
        }

        val filteredOrders = orders.filter { it.status != "Cancelled" && it.timestamp >= filterTime }
        val totalRevenue = filteredOrders.sumOf { it.totalAmount }
        val totalDiscount = filteredOrders.sumOf { it.discountAmount }
        val totalProfit = filteredOrders.sumOf {
            if (it.profitAmount > 0) it.profitAmount else (it.totalAmount * 0.4)
        }
        val cashTotal = filteredOrders.filter { it.paymentMethod == "Cash" }.sumOf { it.totalAmount }
        val onlineTotal = filteredOrders.filter { it.paymentMethod != "Cash" }.sumOf { it.totalAmount }

        // Metrics Table Box
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(margin, yPos, rightMargin, yPos + 85f, 10f, 10f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(margin, yPos, rightMargin, yPos + 85f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Row 1 metrics
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 11f
        canvas.drawText("Total Orders", margin + 15f, yPos + 25f, paint)
        canvas.drawText("Total Revenue", margin + 140f, yPos + 25f, paint)
        canvas.drawText("Net Profit", margin + 270f, yPos + 25f, paint)
        canvas.drawText("Discount Given", margin + 380f, yPos + 25f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("${filteredOrders.size}", margin + 15f, yPos + 45f, paint)
        canvas.drawText("Rs. ${"%.0f".format(totalRevenue)}", margin + 140f, yPos + 45f, paint)

        paint.color = Color.rgb(22, 163, 74) // Green
        canvas.drawText("Rs. ${"%.0f".format(totalProfit)}", margin + 270f, yPos + 45f, paint)

        paint.color = Color.rgb(220, 38, 38) // Red
        canvas.drawText("Rs. ${"%.0f".format(totalDiscount)}", margin + 380f, yPos + 45f, paint)

        // Payment Breakdown line
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Cash Collected: Rs. ${"%.0f".format(cashTotal)}    |    Online/QR Received: Rs. ${"%.0f".format(onlineTotal)}", margin + 15f, yPos + 72f, paint)

        yPos += 115f

        // Table Header: Category Sales Breakdown
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Category Sales Performance", margin, yPos, paint)
        yPos += 20f

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(margin, yPos, rightMargin, yPos + 26f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("Category", margin + 10f, yPos + 18f, paint)
        canvas.drawText("Items Sold", 320f, yPos + 18f, paint)
        canvas.drawText("Total Sales (Rs)", 440f, yPos + 18f, paint)
        yPos += 36f

        val validOrderIds = filteredOrders.map { it.id }.toSet()
        val validItems = items.filter { it.orderId in validOrderIds }

        val categorySales = validItems.groupBy { it.category.ifBlank { "General Menu" } }
            .mapValues { entry ->
                Pair(entry.value.sumOf { it.quantity }, entry.value.sumOf { it.price * it.quantity })
            }
            .toList()
            .sortedByDescending { it.second.second }

        paint.isFakeBoldText = false
        paint.textSize = 11f

        for ((category, data) in categorySales) {
            paint.color = Color.rgb(51, 65, 85)
            canvas.drawText(category, margin + 10f, yPos, paint)
            canvas.drawText("${data.first} items", 320f, yPos, paint)
            canvas.drawText("Rs. ${"%.2f".format(data.second)}", 440f, yPos, paint)

            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(margin, yPos + 8f, rightMargin, yPos + 8f, paint)
            yPos += 24f
            if (yPos > 740f) break
        }

        // Footer Note
        yPos = 790f
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 10f
        canvas.drawLine(margin, yPos - 10f, rightMargin, yPos - 10f, paint)
        canvas.drawText("Restaurant POS • Automated 12-Month Daily/Weekly/Monthly Report • Confidential", margin, yPos + 5f, paint)

        pdfDocument.finishPage(page)

        return try {
            val file = File(context.cacheDir, "SalesReport_${reportType}_${System.currentTimeMillis()}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Standard Share Intent (Chooser)
     */
    fun generateAndSharePdf(
        context: Context,
        reportType: String,
        orders: List<Order>,
        items: List<OrderItem>
    ) {
        val file = createPdfFile(context, reportType, orders, items) ?: return
        val settings = SettingsManager(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "${settings.getRestaurantName()} - $reportType Sales Report")
            putExtra(Intent.EXTRA_TEXT, "Here is the $reportType Sales and Profit Report for ${settings.getRestaurantName()}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Sales Report PDF"))
    }

    /**
     * Send Sales Report PDF directly to WhatsApp
     */
    fun sharePdfToWhatsApp(
        context: Context,
        reportType: String,
        orders: List<Order>,
        items: List<OrderItem>
    ) {
        val file = createPdfFile(context, reportType, orders, items) ?: return
        val settings = SettingsManager(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val now = System.currentTimeMillis()
        val filterTime = when (reportType) {
            "Daily" -> now - 24 * 60 * 60 * 1000L
            "Weekly" -> now - 7 * 24 * 60 * 60 * 1000L
            "Monthly" -> now - 30L * 24 * 60 * 60 * 1000L
            else -> 0L
        }
        val count = orders.count { it.status != "Cancelled" && it.timestamp >= filterTime }
        val revenue = orders.filter { it.status != "Cancelled" && it.timestamp >= filterTime }.sumOf { it.totalAmount }

        val caption = "📊 *${settings.getRestaurantName()} - $reportType Sales Report*\n" +
                "Total Orders: $count\n" +
                "Total Revenue: Rs. ${"%.0f".format(revenue)}\n" +
                "Date: ${SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault()).format(Date())}"

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
            // WhatsApp not installed or failed, fallback to chooser
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(fallbackIntent, "Send PDF to WhatsApp / Share"))
        }
    }

    /**
     * Download / Save PDF file to device public Downloads folder
     */
    fun downloadPdfToDownloads(
        context: Context,
        reportType: String,
        orders: List<Order>,
        items: List<OrderItem>
    ): File? {
        val cachedFile = createPdfFile(context, reportType, orders, items) ?: return null
        val fileName = "SalesReport_${reportType}_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.pdf"

        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)

            FileInputStream(cachedFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(context, "PDF saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

            // Open or view saved PDF
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(viewIntent)
            } catch (_: Exception) {}

            destFile
        } catch (e: Exception) {
            Toast.makeText(context, "Saved in app documents: ${cachedFile.name}", Toast.LENGTH_SHORT).show()
            cachedFile
        }
    }
}
