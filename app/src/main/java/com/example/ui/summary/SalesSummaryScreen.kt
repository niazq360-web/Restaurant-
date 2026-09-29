package com.example.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Order
import com.example.ui.pos.PosViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesSummaryScreen(viewModel: PosViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val allOrderItems by viewModel.allOrderItems.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf("Daily") } // "Daily", "Weekly", "Monthly", "Yearly"
    var selectedMetric by remember { mutableStateOf("Revenue") } // "Revenue", "Profit"

    val periods = listOf("Daily", "Weekly", "Monthly", "Yearly")

    val now = System.currentTimeMillis()
    val filterTime = remember(selectedPeriod, now) {
        when (selectedPeriod) {
            "Daily" -> now - 7 * 24 * 60 * 60 * 1000L
            "Weekly" -> now - 28L * 24 * 60 * 60 * 1000L
            "Monthly" -> now - 365L * 24 * 60 * 60 * 1000L
            "Yearly" -> 0L
            else -> 0L
        }
    }

    val filteredOrders = remember(orders, filterTime) {
        orders.filter { it.status != "Cancelled" && it.timestamp >= filterTime }
    }

    val totalRevenue = remember(filteredOrders) { filteredOrders.sumOf { it.totalAmount } }
    val totalDiscount = remember(filteredOrders) { filteredOrders.sumOf { it.discountAmount } }
    val totalProfit = remember(filteredOrders) {
        filteredOrders.sumOf {
            if (it.profitAmount > 0) it.profitAmount else (it.totalAmount * 0.4)
        }
    }

    // Chart data computation
    val chartData = remember(filteredOrders, selectedPeriod, selectedMetric) {
        val formatPattern = when (selectedPeriod) {
            "Daily" -> "dd MMM"
            "Weekly" -> "ww 'Wk'"
            "Monthly" -> "MMM yy"
            "Yearly" -> "yyyy"
            else -> "dd MMM"
        }
        val df = SimpleDateFormat(formatPattern, Locale.getDefault())

        val grouped = filteredOrders.groupBy { df.format(Date(it.timestamp)) }
            .mapValues { entry ->
                if (selectedMetric == "Revenue") {
                    entry.value.sumOf { it.totalAmount }
                } else {
                    entry.value.sumOf { if (it.profitAmount > 0) it.profitAmount else (it.totalAmount * 0.4) }
                }
            }
            .toList()

        if (grouped.size > 12) grouped.takeLast(12) else grouped
    }

    val categorySales = remember(filteredOrders, allOrderItems) {
        val validOrderIds = filteredOrders.map { it.id }.toSet()
        val validItems = allOrderItems.filter { it.orderId in validOrderIds }

        validItems.groupBy { it.category.ifBlank { "Uncategorized" } }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales & Profit Analytics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Share, "Share Report") }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Daily Report (PDF)") },
                            onClick = { showMenu = false; PdfGenerator.generateAndSharePdf(context, "Daily", orders, allOrderItems) }
                        )
                        DropdownMenuItem(
                            text = { Text("Weekly Report (PDF)") },
                            onClick = { showMenu = false; PdfGenerator.generateAndSharePdf(context, "Weekly", orders, allOrderItems) }
                        )
                        DropdownMenuItem(
                            text = { Text("Monthly Report (PDF)") },
                            onClick = { showMenu = false; PdfGenerator.generateAndSharePdf(context, "Monthly", orders, allOrderItems) }
                        )
                        DropdownMenuItem(
                            text = { Text("Yearly Report (PDF)") },
                            onClick = { showMenu = false; PdfGenerator.generateAndSharePdf(context, "Yearly", orders, allOrderItems) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Period Selector Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(periods) { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Revenue Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Revenue", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs. ${"%.0f".format(totalRevenue)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        Text("${filteredOrders.size} orders", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Profit Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Net Profit", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rs. ${"%.0f".format(totalProfit)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF16A34A))
                        if (totalDiscount > 0) {
                            Text("Disc: Rs. ${"%.0f".format(totalDiscount)}", fontSize = 10.sp, color = Color(0xFFDC2626))
                        } else {
                            Text("Margin: ~40%", fontSize = 10.sp, color = Color(0xFF16A34A))
                        }
                    }
                }
            }

            // Metric Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$selectedPeriod $selectedMetric Chart", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedMetric == "Revenue",
                        onClick = { selectedMetric = "Revenue" },
                        label = { Text("Revenue", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedMetric == "Profit",
                        onClick = { selectedMetric = "Profit" },
                        label = { Text("Profit", fontSize = 11.sp) }
                    )
                }
            }

            // Line Chart Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (chartData.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No sales data in this period", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LineChart(
                        data = chartData,
                        isProfit = selectedMetric == "Profit",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Category Sales Breakdown
            Text("Items Sold per Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("Quantity Sold", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    HorizontalDivider()
                    if (categorySales.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No category data found.", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn {
                            items(categorySales) { (category, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                    Text("$count", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<Pair<String, Double>>,
    isProfit: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val primaryColor = if (isProfit) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = primaryColor, fontWeight = FontWeight.Bold)

    Canvas(modifier = modifier) {
        val bottomMargin = 26.dp.toPx()
        val topMargin = 22.dp.toPx()
        val leftMargin = 24.dp.toPx()
        val rightMargin = 24.dp.toPx()

        val chartWidth = (size.width - leftMargin - rightMargin).coerceAtLeast(1f)
        val chartHeight = (size.height - topMargin - bottomMargin).coerceAtLeast(1f)

        val maxAmount = data.maxOfOrNull { it.second }?.toFloat()?.takeIf { it > 0 } ?: 1f

        // Draw background grid lines
        val steps = 3
        for (i in 0..steps) {
            val gridY = topMargin + (chartHeight * i / steps)
            drawLine(
                color = gridColor,
                start = Offset(leftMargin, gridY),
                end = Offset(size.width - rightMargin, gridY),
                strokeWidth = 1.dp.toPx()
            )
        }

        val xStep = if (data.size > 1) chartWidth / (data.size - 1) else 0f
        val path = Path()

        val points = data.mapIndexed { index, pair ->
            val px = leftMargin + index * xStep
            val normalizedVal = (pair.second.toFloat() / maxAmount).coerceIn(0f, 1f)
            val py = topMargin + chartHeight * (1f - normalizedVal)
            Offset(px, py)
        }

        points.forEachIndexed { index, pt ->
            if (index == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }

        // Draw line
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw points and labels
        data.forEachIndexed { index, pair ->
            val pt = points[index]

            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = pt
            )

            // X-axis label
            val labelLayout = textMeasurer.measure(text = pair.first, style = textStyle)
            val maxLabelX = (size.width - labelLayout.size.width).coerceAtLeast(0f)
            val labelX = (pt.x - labelLayout.size.width / 2f).coerceIn(0f, maxLabelX)
            val labelY = (size.height - bottomMargin + 4.dp.toPx()).coerceIn(0f, (size.height - labelLayout.size.height).coerceAtLeast(0f))
            drawText(
                textLayoutResult = labelLayout,
                topLeft = Offset(labelX, labelY)
            )

            // Value label above point
            val valueText = pair.second.toInt().toString()
            val valueLayout = textMeasurer.measure(text = valueText, style = valueStyle)
            val maxValueX = (size.width - valueLayout.size.width).coerceAtLeast(0f)
            val valueX = (pt.x - valueLayout.size.width / 2f).coerceIn(0f, maxValueX)
            val valueY = (pt.y - valueLayout.size.height - 4.dp.toPx()).coerceIn(0f, (size.height - valueLayout.size.height).coerceAtLeast(0f))
            drawText(
                textLayoutResult = valueLayout,
                topLeft = Offset(valueX, valueY)
            )
        }
    }
}
