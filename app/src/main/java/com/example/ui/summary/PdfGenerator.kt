package com.example.ui.summary

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.Order
import com.example.data.OrderItem
import com.example.data.SettingsManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateAndSharePdf(
        context: Context,
        reportType: String,
        orders: List<Order>,
        items: List<OrderItem>
    ) {
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
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 360f, yPos, paint)
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
        val filteredOrderIds = filteredOrders.map { it.id }.toSet()
        val filteredItems = items.filter { it.orderId in filteredOrderIds }

        val totalRevenue = filteredOrders.sumOf { it.totalAmount }
        val totalDiscount = filteredOrders.sumOf { it.discountAmount }
        val totalProfit = filteredOrders.sumOf { it.profitAmount.takeIf { p -> p > 0 } ?: (it.totalAmount * 0.4) }
        val totalCost = (totalRevenue - totalProfit).coerceAtLeast(0.0)

        // Summary Metric Boxes (Background Cards)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(margin, yPos, rightMargin, yPos + 75f, 10f, 10f, paint)
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(margin, yPos, rightMargin, yPos + 75f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Metric 1: Revenue
        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("TOTAL REVENUE", margin + 15f, yPos + 25f, paint)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText("Rs. ${"%.2f".format(totalRevenue)}", margin + 15f, yPos + 50f, paint)

        // Metric 2: Net Profit
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("ESTIMATED PROFIT", margin + 140f, yPos + 25f, paint)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(22, 163, 74)
        canvas.drawText("Rs. ${"%.2f".format(totalProfit)}", margin + 140f, yPos + 50f, paint)

        // Metric 3: Orders Count
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("TOTAL ORDERS", margin + 280f, yPos + 25f, paint)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText("${filteredOrders.size} Orders", margin + 280f, yPos + 50f, paint)

        // Metric 4: Discount Given
        paint.textSize = 10f
        paint.isFakeBoldText = false
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("TOTAL DISCOUNT", margin + 390f, yPos + 25f, paint)
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawText("Rs. ${"%.2f".format(totalDiscount)}", margin + 390f, yPos + 50f, paint)

        yPos += 105f

        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(margin, yPos - 15f, rightMargin, yPos + 15f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Category / Item Type", margin + 10f, yPos + 5f, paint)
        canvas.drawText("Quantity Sold", 320f, yPos + 5f, paint)
        canvas.drawText("Total Sales (Rs)", 440f, yPos + 5f, paint)
        yPos += 30f

        val categorySales = filteredItems.groupBy { it.category.ifBlank { "Uncategorized" } }
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
            if (yPos > 760f) break
        }

        // Footer Note
        yPos = 790f
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 10f
        canvas.drawLine(margin, yPos - 10f, rightMargin, yPos - 10f, paint)
        canvas.drawText("Restaurant POS • Automated 12-Month Offline Report • Confidential", margin, yPos + 5f, paint)

        pdfDocument.finishPage(page)

        try {
            val file = File(context.cacheDir, "SalesReport_${reportType}.pdf")
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "$restaurantName - $reportType Sales & Profit Report")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Download or Share Report (WhatsApp/Drive)"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
