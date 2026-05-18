package com.numisproerp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.IOSDesign
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Запис історії, незалежний від типу: дата, кількість, сума, ім'я товару.
 * Підходить і для закупівель (постачальник), і для продажів (клієнт), і для
 * будь-яких інших агрегацій по дню.
 *
 * Опціональні поля [purchaseCost] і [netProfit] заповнюються в історії
 * продажів клієнта — у DayGroupCard їх відображення активується через
 * прапор `showProfit`.
 */
data class HistoryRecord(
    val date: Long,
    val productName: String,
    val quantity: Int,
    val totalAmount: Double,
    val purchaseCost: Double = 0.0,
    val netProfit: Double = 0.0
)

/**
 * Згруповує записи історії за добою (00:00 локального часу) і повертає мапу
 * `дата (мс на початок дня) → список записів`. Дні відсортовано за спаданням
 * (найновіші зверху).
 */
fun groupHistoryByDay(records: List<HistoryRecord>): List<Pair<Long, List<HistoryRecord>>> {
    val byDay = records.groupBy { startOfDay(it.date) }
    return byDay
        .toSortedMap(compareByDescending { it })
        .map { (day, items) -> day to items.sortedByDescending { it.date } }
}

/** Повертає `timestamp` для 00:00 того ж дня у локальному часовому поясі. */
fun startOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/** Повертає `timestamp` для 23:59:59.999 того ж дня. */
fun endOfDay(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
}

/**
 * Режими виклику пікера дати (потрібно розрізняти “Від” від “До” на викликаючому боці).
 */
enum class DateFilterPickerMode { FROM, TO }

/**
 * Рядок-фільтр часового діапазону з двома кнопками (Від / До) та хрестиком
 * скидання.
 *
 * ВАЖЛИВО: цей компонент САМ НЕ відкриває діалог календаря. Раніше
 * він рендерив календар як Dialog всередині батьківського Dialog’у (Suppliers/Clients
 * detail), і state-зміни з Dialog-в-Dialog не пропагували коректно (для
 * користувача виглядало як «натиснув Готово — дата не зберігається»).
 *
 * Тепер календар (`DateChooserDialog`) піднято на рівень викликаючого екрану
 * і відкривається поруч з батьківським Dialog’ом — без вкладеності.
 * State оновлюється на рівні екрану, батьківський Dialog рекомпозується з новим
 * `startMillis`/`endMillis`.
 *
 * На кожен тап Від/До викликається `onPickerRequest(MODE)` викликаючий екран відповідає
 * за відкриття `DateChooserDialog` і виклик `onChange(…)` з результатом.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterRow(
    startMillis: Long?,
    endMillis: Long?,
    onPickerRequest: (DateFilterPickerMode) -> Unit,
    onClear: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedButton(
            onClick = { onPickerRequest(DateFilterPickerMode.FROM) },
            shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = startMillis?.let { tr("Від ", "From ") + df.format(Date(it)) }
                    ?: tr("Від", "From"),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        OutlinedButton(
            onClick = { onPickerRequest(DateFilterPickerMode.TO) },
            shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = endMillis?.let { tr("До ", "To ") + df.format(Date(it)) }
                    ?: tr("До", "To"),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (startMillis != null || endMillis != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = tr("Скинути", "Clear"),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Публічний wrapper для `DateChooserDialog` — використовується викликаючими екранами,
 * щоб відкривати календар НА СВОЄМУ РІВНІ, а не всередині батьківського Dialog’у.
 */
@Composable
fun DateChooserSheet(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    DateChooserDialog(
        initialMillis = initialMillis,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

/**
 * Заголовок дня з агрегованими сумами: дата + загальна кількість + загальна
 * сума. Під ним — деталі записів за цей день.
 *
 * @param currencyFormat функція форматування грошової суми (для локалізації).
 */
@Composable
fun DayGroupCard(
    day: Long,
    records: List<HistoryRecord>,
    summaryColor: androidx.compose.ui.graphics.Color = AccentBlue,
    currencyFormat: (Double) -> String = { String.format("%,.2f \u20b4", it) },
    showProfit: Boolean = false
) {
    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val totalQty = records.sumOf { it.quantity }
    val totalAmount = records.sumOf { it.totalAmount }
    val totalCost = records.sumOf { it.purchaseCost }
    val totalProfit = records.sumOf { it.netProfit }
    val profitColor = if (totalProfit >= 0) AccentBlue else Color(0xFFD32F2F)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = summaryColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = df.format(Date(day)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${tr("Шт.", "Pcs")}: $totalQty",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${tr("Сума", "Sum")}: ${currencyFormat(totalAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = summaryColor
                    )
                    if (showProfit) {
                        Text(
                            text = "${tr("Закупка", "Cost")}: ${currencyFormat(totalCost)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${tr("Прибуток", "Profit")}: ${currencyFormat(totalProfit)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = profitColor
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            records.forEach { rec ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rec.productName,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${rec.quantity} ${tr("шт.", "pcs")}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currencyFormat(rec.totalAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = summaryColor
                        )
                    }
                    if (showProfit) {
                        val recProfitColor = if (rec.netProfit >= 0) AccentBlue else Color(0xFFD32F2F)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${tr("Закупка", "Cost")}: ${currencyFormat(rec.purchaseCost)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${tr("Прибуток", "Profit")}: ${currencyFormat(rec.netProfit)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = recProfitColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Повноекранний діалог-календар з місячною сіткою. Дозволяє вибрати будь-який
 * день в діапазоні 1900..2100. Має:
 *   • заголовок з ◄ ► для переходу по місяцях,
 *   • тап на «Місяць Рік» відкриває скролл-список років (для далеких років),
 *   • місячну сітку 6×7 з тапом по дню,
 *   • кнопку «Сьогодні» для швидкого повернення на сьогодні,
 *   • Готово / Скасувати знизу.
 *
 * Реалізовано власноруч (без Material3 DatePicker), щоб уникнути конфліктів
 * з батьківськими scrollable-контейнерами і Dialog-у-Dialog.
 *
 * Повертає мс локального часового поясу (00:00 вибраного дня).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateChooserDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val today = remember { Calendar.getInstance() }
    val initialCal = remember(initialMillis) {
        Calendar.getInstance().apply {
            if (initialMillis != null) timeInMillis = initialMillis
        }
    }
    var year by remember(initialMillis) { mutableStateOf(initialCal.get(Calendar.YEAR)) }
    var month by remember(initialMillis) { mutableStateOf(initialCal.get(Calendar.MONTH)) }
    var day by remember(initialMillis) { mutableStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var showYearList by remember { mutableStateOf(false) }

    val monthNames = listOf(
        tr("Січень", "January"), tr("Лютий", "February"), tr("Березень", "March"),
        tr("Квітень", "April"), tr("Травень", "May"), tr("Червень", "June"),
        tr("Липень", "July"), tr("Серпень", "August"), tr("Вересень", "September"),
        tr("Жовтень", "October"), tr("Листопад", "November"), tr("Грудень", "December")
    )
    val weekdayNames = listOf(
        tr("Пн", "Mon"), tr("Вт", "Tue"), tr("Ср", "Wed"),
        tr("Чт", "Thu"), tr("Пт", "Fri"), tr("Сб", "Sat"), tr("Нд", "Sun")
    )

    // Геометрія місяця: який день тижня — 1-е число, скільки днів у місяці.
    val monthCal = remember(year, month) {
        Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    // Calendar.DAY_OF_WEEK: 1=Sun..7=Sat. Конвертуємо так, щоб 0=Mon..6=Sun.
    val firstDow = (monthCal.get(Calendar.DAY_OF_WEEK) + 5) % 7
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Якщо після зміни місяця обраний день виходить за межі — обрізаємо.
    if (day > daysInMonth) day = daysInMonth

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.82f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // ── Header ──────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Виберіть дату", "Pick a date"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = tr("Закрити", "Close"))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // ── Month/Year header з кнопками-стрілками ──────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            if (month == 0) {
                                if (year > 1900) {
                                    year -= 1
                                    month = 11
                                }
                            } else {
                                month -= 1
                            }
                        },
                        enabled = !(year == 1900 && month == 0)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = tr("Попередній місяць", "Previous month"))
                    }

                    TextButton(
                        onClick = { showYearList = !showYearList },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${monthNames[month]} $year",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            if (month == 11) {
                                if (year < 2100) {
                                    year += 1
                                    month = 0
                                }
                            } else {
                                month += 1
                            }
                        },
                        enabled = !(year == 2100 && month == 11)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = tr("Наступний місяць", "Next month"))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // ── Тіло: або сітка місяця, або скролл-список років ──────
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (showYearList) {
                        val years = remember { (1900..2100).toList() }
                        val initialIndex = (year - 1900 - 3).coerceAtLeast(0)
                        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(years) { y ->
                                val isCurrent = y == year
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            year = y
                                            showYearList = false
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = y.toString(),
                                        fontSize = if (isCurrent) 22.sp else 18.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Заголовок днів тижня
                            Row(modifier = Modifier.fillMaxWidth()) {
                                weekdayNames.forEach { dn ->
                                    Text(
                                        text = dn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                                    )
                                }
                            }

                            // Місячна сітка 6 рядків × 7 стовпців
                            for (week in 0 until 6) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (dow in 0 until 7) {
                                        val cellIndex = week * 7 + dow
                                        val dayNum = cellIndex - firstDow + 1
                                        val isValid = dayNum in 1..daysInMonth
                                        val isSelected = isValid && dayNum == day
                                        val isToday = isValid &&
                                            year == today.get(Calendar.YEAR) &&
                                            month == today.get(Calendar.MONTH) &&
                                            dayNum == today.get(Calendar.DAY_OF_MONTH)

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else Color.Transparent
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                    color = if (isToday && !isSelected) MaterialTheme.colorScheme.primary
                                                            else Color.Transparent,
                                                    shape = CircleShape
                                                )
                                                .clickable(enabled = isValid) {
                                                    if (isValid) day = dayNum
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isValid) {
                                                Text(
                                                    text = dayNum.toString(),
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                                        isToday -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Bottom row: Сьогодні + Скасувати + Готово ───────────
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = {
                        year = today.get(Calendar.YEAR)
                        month = today.get(Calendar.MONTH)
                        day = today.get(Calendar.DAY_OF_MONTH)
                        showYearList = false
                    }) {
                        Text(tr("Сьогодні", "Today"))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text(tr("Скасувати", "Cancel"))
                    }
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply {
                            clear()
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, day)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(cal.timeInMillis)
                    }) {
                        Text(
                            tr("Готово", "OK"),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

