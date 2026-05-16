package com.numisproerp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.AccentRed
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.theme.IOSIconChip
import com.numisproerp.ui.viewmodel.ReportsViewModel
import com.numisproerp.ui.viewmodel.getStartOfMonthStatic
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.numisproerp.ui.viewmodel.MonthlyStats
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavHostController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    var openTab by remember { mutableStateOf<ReportsTab?>(null) }
    var stockBreakdown by remember { mutableStateOf<List<com.numisproerp.data.dao.ProductInStock>>(emptyList()) }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = tr("Назад", "Back"),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = tr("Вибрати період", "Pick period"),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = tr("Звіти", "Reports"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        StatsGrid(
                            uiState = uiState,
                            onTabClick = { tab ->
                                if (tab == ReportsTab.STOCK) {
                                    scope.launch {
                                        stockBreakdown = viewModel.getStockBreakdown()
                                        openTab = tab
                                    }
                                } else {
                                    openTab = tab
                                }
                            }
                        )
                    }

                    item {
                        Text(
                            text = tr("Динаміка за місяцями (поточний зверху)", "Monthly dynamics (current on top)"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        MonthlyDynamicsChart(monthly = uiState.monthlyData)
                    }

                    items(uiState.monthlyData) { monthData ->
                        MonthlyStatsCard(monthData = monthData)
                    }

                    item {
                        Text(
                            text = tr("Топ товарів", "Top products"),
                            fontSize = 18.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (uiState.topProducts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = AccentOrange.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(IOSDesign.CardCornerRadius)
                            ) {
                                Text(
                                    text = tr("Немає даних про продажі", "No sales data"),
                                    modifier = Modifier.padding(16.dp),
                                    color = AccentOrange
                                )
                            }
                        }
                    } else {
                        items(uiState.topProducts) { product ->
                            TopProductCard(product = product)
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                viewModel.updateDateRange(start, end)
                showDatePicker = false
            }
        )
    }

    openTab?.let { tab ->
        ReportsDetailDialog(
            tab = tab,
            uiState = uiState,
            stockBreakdown = stockBreakdown,
            onDismiss = { openTab = null }
        )
    }
}

enum class ReportsTab {
    REVENUE,
    EXPENSES,
    PROFIT,
    STOCK
}

@Composable
fun StatsGrid(
    uiState: com.numisproerp.ui.viewmodel.ReportsUiState,
    onTabClick: (ReportsTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = tr("Дохід", "Revenue"),
            value = String.format("%,.2f", uiState.totalRevenue),
            icon = Icons.Filled.ShoppingCart,
            iconColor = AccentGreen,
            valueColor = AccentGreen,
            onClick = { onTabClick(ReportsTab.REVENUE) }
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = tr("Витрати", "Expenses"),
            value = String.format("%,.2f", uiState.totalExpenses),
            icon = Icons.Filled.Warning,
            iconColor = AccentRed,
            valueColor = AccentRed,
            onClick = { onTabClick(ReportsTab.EXPENSES) }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = tr("Чистий прибуток", "Net profit"),
            value = String.format("%,.2f", uiState.netProfit),
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconColor = if (uiState.netProfit >= 0) AccentGreen else AccentRed,
            valueColor = if (uiState.netProfit >= 0) AccentGreen else AccentRed,
            onClick = { onTabClick(ReportsTab.PROFIT) }
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = tr("Залишки на складі", "Stock balance"),
            value = String.format("%,.2f", uiState.stockValue),
            icon = Icons.Filled.Store,
            iconColor = AccentBlue,
            valueColor = AccentBlue,
            onClick = { onTabClick(ReportsTab.STOCK) }
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    valueColor: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IOSIconChip(
                    icon = icon,
                    tint = iconColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$value ₴",
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun MonthlyStatsCard(monthData: com.numisproerp.ui.viewmodel.MonthlyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = monthData.month,
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = tr("Дохід", "Revenue"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%,.2f", monthData.revenue)} ₴",
                        fontSize = 13.sp,
                        color = AccentGreen
                    )
                }
                Column {
                    Text(
                        text = tr("Витрати", "Expenses"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%,.2f", monthData.expenses)} ₴",
                        fontSize = 13.sp,
                        color = AccentRed
                    )
                }
                Column {
                    Text(
                        text = tr("Прибуток", "Profit"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "${String.format("%,.2f", monthData.profit)} ₴",
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = if (monthData.profit >= 0) AccentGreen else AccentRed
                    )
                }
            }
        }
    }
}

@Composable
fun TopProductCard(product: com.numisproerp.ui.viewmodel.TopProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IOSIconChip(
                icon = Icons.Filled.Category,
                tint = AccentOrange
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
                Text(
                    text = "${tr("Продано", "Sold")}: ${product.quantitySold} ${tr("шт.", "pcs")}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = "${String.format("%,.2f", product.totalRevenue)} ₴",
                fontSize = 14.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = AccentGreen
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (startDate: Long, endDate: Long) -> Unit
) {
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    var showStartPicker by remember { mutableStateOf(true) }

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let {
                            showStartPicker = false
                        }
                    }
                ) {
                    Text(tr("Далі", "Next"))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(tr("Скасувати", "Cancel"))
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    } else {
        DatePickerDialog(
            onDismissRequest = { onDismiss() },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = startDatePickerState.selectedDateMillis ?: getStartOfMonthStatic()
                        val end = endDatePickerState.selectedDateMillis ?: System.currentTimeMillis()
                        onConfirm(start, end)
                    }
                ) {
                    Text(tr("Підтвердити", "Confirm"))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text(tr("Скасувати", "Cancel"))
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}

@Composable
fun ReportsDetailDialog(
    tab: ReportsTab,
    uiState: com.numisproerp.ui.viewmodel.ReportsUiState,
    stockBreakdown: List<com.numisproerp.data.dao.ProductInStock>,
    onDismiss: () -> Unit
) {
    val title = when (tab) {
        ReportsTab.REVENUE -> tr("Дохід", "Revenue")
        ReportsTab.EXPENSES -> tr("Витрати", "Expenses")
        ReportsTab.PROFIT -> tr("Чистий прибуток", "Net profit")
        ReportsTab.STOCK -> tr("Залишки на складі", "Stock balance")
    }
    val accent = when (tab) {
        ReportsTab.REVENUE -> AccentGreen
        ReportsTab.EXPENSES -> AccentRed
        ReportsTab.PROFIT -> if (uiState.netProfit >= 0) AccentGreen else AccentRed
        ReportsTab.STOCK -> AccentBlue
    }
    val totalText = when (tab) {
        ReportsTab.REVENUE -> String.format("%,.2f", uiState.totalRevenue)
        ReportsTab.EXPENSES -> String.format("%,.2f", uiState.totalExpenses)
        ReportsTab.PROFIT -> String.format("%,.2f", uiState.netProfit)
        ReportsTab.STOCK -> String.format("%,.2f", uiState.stockValue)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрити", "Close")) } },
        title = {
            Column {
                Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "$totalText ₴",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (tab) {
                    ReportsTab.STOCK -> {
                        if (stockBreakdown.isEmpty()) {
                            Text(
                                text = tr("Немає товарів на складі", "No items in stock"),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            stockBreakdown.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${item.category} • ${item.material}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${item.currentStock} шт.",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentBlue
                                        )
                                        Text(
                                            text = String.format(
                                                "%,.2f ₴",
                                                item.currentStock * item.avgPurchasePrice
                                            ),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = tr("Розбивка по місяцях:", "Monthly breakdown:"),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        uiState.monthlyData.forEach { m ->
                            val (label, color) = when (tab) {
                                ReportsTab.REVENUE -> String.format("%,.2f ₴", m.revenue) to AccentGreen
                                ReportsTab.EXPENSES -> String.format("%,.2f ₴", m.expenses) to AccentRed
                                ReportsTab.PROFIT -> String.format(
                                    "%,.2f ₴", m.profit
                                ) to if (m.profit >= 0) AccentGreen else AccentRed
                                else -> "" to accent
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = m.month, fontSize = 13.sp)
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = color
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}

// ============================================================================
// MonthlyDynamicsChart – Canvas-based line chart showing Revenue / Expenses / Profit
// across the 6 most recent months. Oldest on the left, current on the right.
// ============================================================================
@Composable
fun MonthlyDynamicsChart(monthly: List<MonthlyStats>) {
    val data = remember(monthly) { monthly.asReversed() }
    val current = monthly.firstOrNull()?.month ?: ""

    val labelFormat = remember { SimpleDateFormat("LLL yyyy", Locale("uk")) }
    val now = remember { System.currentTimeMillis() }
    val shortLabels = remember(data, now) {
        val calendar = Calendar.getInstance()
        val n = data.size
        List(n) { idx ->
            val offset = (n - 1) - idx
            calendar.timeInMillis = now
            calendar.add(Calendar.MONTH, -offset)
            labelFormat.format(Date(calendar.timeInMillis))
                .trimEnd('.')
                .replaceFirstChar { it.titlecase(Locale("uk")) }
        }
    }

    val maxValueRaw = data.maxOfOrNull { max(max(it.revenue, it.expenses), it.profit) } ?: 0.0
    val minValueRaw = data.minOfOrNull { kotlin.math.min(kotlin.math.min(it.revenue, it.expenses), it.profit) } ?: 0.0
    val niceMax = niceCeil(maxValueRaw)
    val niceMin = if (minValueRaw < 0.0) -niceCeil(-minValueRaw) else 0.0
    val span = (niceMax - niceMin).coerceAtLeast(1.0)

    val gridSteps = 4
    val yLabels = remember(niceMax, niceMin) {
        List(gridSteps + 1) { i ->
            val value = niceMin + span * (gridSteps - i) / gridSteps
            formatAxisValue(value)
        }
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = onSurface.copy(alpha = 0.12f)
    val axisLabelColor = onSurface.copy(alpha = 0.55f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = current,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChartLegendDot(color = AccentGreen, label = tr("Дохід", "Revenue"))
                    ChartLegendDot(color = AccentRed, label = tr("Витрати", "Expenses"))
                    ChartLegendDot(color = AccentOrange, label = tr("Прибуток", "Profit"))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    yLabels.forEach { label ->
                        Text(text = label, fontSize = 10.sp, color = axisLabelColor)
                    }
                }

                Canvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val w = size.width
                    val h = size.height
                    val left = 4.dp.toPx()
                    val right = w - 4.dp.toPx()
                    val top = 6.dp.toPx()
                    val bottom = h - 6.dp.toPx()
                    val plotW = right - left
                    val plotH = bottom - top

                    val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    for (i in 0..gridSteps) {
                        val y = top + plotH * i / gridSteps
                        drawLine(
                            color = gridColor,
                            start = Offset(left, y),
                            end = Offset(right, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dash
                        )
                    }

                    if (niceMin < 0.0 && niceMax > 0.0) {
                        val ratio = ((niceMax - 0.0) / span).toFloat()
                        val zeroY = top + plotH * ratio
                        drawLine(
                            color = onSurface.copy(alpha = 0.25f),
                            start = Offset(left, zeroY),
                            end = Offset(right, zeroY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val n = data.size
                    if (n == 0) return@Canvas
                    val stepX = if (n > 1) plotW / (n - 1) else 0f

                    fun valueToY(value: Double): Float {
                        val ratio = ((niceMax - value) / span).toFloat()
                        return top + plotH * ratio.coerceIn(0f, 1f)
                    }

                    fun drawSeries(values: List<Double>, color: androidx.compose.ui.graphics.Color) {
                        if (values.isEmpty()) return
                        val path = Path()
                        values.forEachIndexed { idx, v ->
                            val x = left + stepX * idx
                            val y = valueToY(v)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(
                            path = path,
                            color = color,
                            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                        )
                        values.forEachIndexed { idx, v ->
                            val x = left + stepX * idx
                            val y = valueToY(v)
                            drawCircle(color = color, radius = 3.2.dp.toPx(), center = Offset(x, y))
                            drawCircle(color = androidx.compose.ui.graphics.Color.White, radius = 1.2.dp.toPx(), center = Offset(x, y))
                        }
                    }

                    drawSeries(data.map { it.revenue }, AccentGreen)
                    drawSeries(data.map { it.expenses }, AccentRed)
                    drawSeries(data.map { it.profit }, AccentOrange)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.width(56.dp))
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    shortLabels.forEach { label ->
                        Text(text = label, fontSize = 10.sp, color = axisLabelColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp)
    }
}

private fun niceCeil(value: Double): Double {
    if (value <= 0.0) return 1000.0
    val exponent = kotlin.math.floor(kotlin.math.log10(value))
    val pow = Math.pow(10.0, exponent)
    val fraction = value / pow
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5
        fraction <= 2.0 -> 2.0
        fraction <= 3.0 -> 3.0
        fraction <= 5.0 -> 5.0
        fraction <= 7.5 -> 7.5
        else -> 10.0
    }
    return niceFraction * pow
}

private fun formatAxisValue(value: Double): String {
    val abs = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        abs >= 1_000_000.0 -> "$sign${String.format("%,.1fM", abs / 1_000_000.0)} ₴"
        abs >= 1_000.0 -> "$sign${String.format("%,.0f", abs).replace(',', ' ')} ₴"
        else -> "$sign${String.format("%,.0f", abs)} ₴"
    }
}
