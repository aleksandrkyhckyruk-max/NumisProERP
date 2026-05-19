package com.numisproerp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.numisproerp.data.database.AppDatabase
import com.numisproerp.di.AppDatabaseEntryPoint
import dagger.hilt.android.EntryPointAccessors
import com.numisproerp.data.entities.Purchase
import com.numisproerp.data.entities.Sale
import com.numisproerp.data.entities.Supplier
import com.numisproerp.data.entities.Client
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.theme.IOSIconChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailsScreen(
    navController: NavHostController,
    type: String,
    title: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val database: AppDatabase = remember {
        EntryPointAccessors
            .fromApplication(context, AppDatabaseEntryPoint::class.java)
            .appDatabase()
    }
    var isLoading by remember { mutableStateOf(true) }
    var purchases by remember { mutableStateOf<List<Purchase>>(emptyList()) }
    var sales by remember { mutableStateOf<List<Sale>>(emptyList()) }
    var supplierNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var clientNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var productNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    var selectedSupplierIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedClientIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilter by remember { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by remember(type) {
        mutableStateOf(
            when (type) {
                "purchases" -> DetailsSort.COUNTERPARTY_ASC
                "profit" -> DetailsSort.COUNTERPARTY_ASC
                else -> DetailsSort.DATE_DESC
            }
        )
    }

    val unknownSupplierText = tr("Невідомий постачальник", "Unknown supplier")
    val unknownClientText = tr("Невідомий клієнт", "Unknown client")
    val unknownProductText = tr("Невідомий товар", "Unknown item")
    val purchaseLabel = tr("Закупівля", "Purchase")
    val saleLabel = tr("Продаж", "Sale")
    val pcsLabel = tr("шт.", "pcs")

    val filteredPurchases by remember(purchases, selectedSupplierIds, sortMode, supplierNames) {
        derivedStateOf {
            val base = if (selectedSupplierIds.isEmpty()) purchases
            else purchases.filter { it.supplierId in selectedSupplierIds }
            base.sortedWith(purchaseComparator(sortMode, supplierNames, unknownSupplierText))
        }
    }

    val filteredSales by remember(sales, selectedClientIds, sortMode, clientNames) {
        derivedStateOf {
            val base = if (selectedClientIds.isEmpty()) sales
            else sales.filter { it.clientId in selectedClientIds }
            base.sortedWith(saleComparator(sortMode, clientNames, unknownClientText))
        }
    }

    val totalAmount by remember(type, filteredPurchases, filteredSales) {
        derivedStateOf {
            when (type) {
                "purchases" -> filteredPurchases.sumOf { it.totalAmount }
                "profit" -> filteredSales.sumOf { it.totalAmount }
                else -> filteredSales.sumOf { it.totalAmount } - filteredPurchases.sumOf { it.totalAmount }
            }
        }
    }

    LaunchedEffect(type) {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val productMap = mutableMapOf<String, String>()
            suspend fun resolveProductNames(catalogIds: List<String>) {
                catalogIds.distinct().forEach { catalogId ->
                    if (catalogId !in productMap) {
                        val product = database.productDao().getProductById(catalogId)
                        productMap[catalogId] = product?.name ?: unknownProductText
                    }
                }
            }
            when (type) {
                "purchases" -> {
                    purchases = database.purchaseDao().getAllPurchases()
                    val supplierMap = mutableMapOf<String, String>()
                    purchases.map { it.supplierId }.distinct().forEach { supplierId ->
                        val supplier = database.supplierDao().getSupplierById(supplierId)
                        supplierMap[supplierId] = supplier?.name ?: unknownSupplierText
                    }
                    supplierNames = supplierMap
                    resolveProductNames(purchases.map { it.catalogId })
                }
                "profit" -> {
                    sales = database.saleDao().getAllSales()
                    val clientMap = mutableMapOf<String, String>()
                    sales.map { it.clientId }.distinct().forEach { clientId ->
                        val client = database.clientDao().getClientById(clientId)
                        clientMap[clientId] = client?.name ?: unknownClientText
                    }
                    clientNames = clientMap
                    resolveProductNames(sales.map { it.catalogId })
                }
                else -> { // balance
                    purchases = database.purchaseDao().getAllPurchases()
                    sales = database.saleDao().getAllSales()
                    val supplierMap = mutableMapOf<String, String>()
                    purchases.map { it.supplierId }.distinct().forEach { supplierId ->
                        val supplier = database.supplierDao().getSupplierById(supplierId)
                        supplierMap[supplierId] = supplier?.name ?: unknownSupplierText
                    }
                    supplierNames = supplierMap
                    val clientMap = mutableMapOf<String, String>()
                    sales.map { it.clientId }.distinct().forEach { clientId ->
                        val client = database.clientDao().getClientById(clientId)
                        clientMap[clientId] = client?.name ?: unknownClientText
                    }
                    clientNames = clientMap
                    resolveProductNames(purchases.map { it.catalogId } + sales.map { it.catalogId })
                }
            }
            productNames = productMap
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Назад", "Back"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    if (type == "purchases" || type == "profit") {
                        Box {
                            IconButton(onClick = { sortMenuExpanded = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = tr("Сортувати", "Sort"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            DetailsSortMenu(
                                expanded = sortMenuExpanded,
                                onDismiss = { sortMenuExpanded = false },
                                isPurchase = type == "purchases",
                                current = sortMode,
                                onSelect = { newMode ->
                                    sortMode = newMode
                                    sortMenuExpanded = false
                                }
                            )
                        }
                        IconButton(onClick = { showFilter = !showFilter }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = tr("Фільтр", "Filter"),
                                tint = if (
                                    (type == "purchases" && selectedSupplierIds.isNotEmpty()) ||
                                    (type == "profit" && selectedClientIds.isNotEmpty())
                                ) AccentOrange else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(visible = showFilter && (type == "purchases" || type == "profit")) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(IOSDesign.CardCornerRadius)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (type == "purchases") tr("Фільтр по постачальнику", "Filter by supplier")
                                    else tr("Фільтр по клієнту", "Filter by client"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if ((type == "purchases" && selectedSupplierIds.isNotEmpty()) ||
                                    (type == "profit" && selectedClientIds.isNotEmpty())
                                ) {
                                    TextButton(onClick = {
                                        selectedSupplierIds = emptySet()
                                        selectedClientIds = emptySet()
                                    }) {
                                        Text(
                                            tr("Скинути", "Reset"),
                                            fontSize = 12.sp,
                                            color = AccentOrange
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (type == "purchases") {
                                supplierNames.forEach { (id, name) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = id in selectedSupplierIds,
                                            onCheckedChange = { checked ->
                                                selectedSupplierIds = if (checked) {
                                                    selectedSupplierIds + id
                                                } else {
                                                    selectedSupplierIds - id
                                                }
                                            }
                                        )
                                        Text(text = name, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                clientNames.forEach { (id, name) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = id in selectedClientIds,
                                            onCheckedChange = { checked ->
                                                selectedClientIds = if (checked) {
                                                    selectedClientIds + id
                                                } else {
                                                    selectedClientIds - id
                                                }
                                            }
                                        )
                                        Text(text = name, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(IOSDesign.CardCornerRadius)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${tr("Загальна сума", "Total amount")}:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format("%,.2f ₴", totalAmount),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (type == "purchases") AccentOrange else AccentGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (type == "purchases" && filteredPurchases.isEmpty()) {
                    Text(text = tr("Немає даних про закупівлі", "No purchase data"), modifier = Modifier.fillMaxWidth())
                } else if (type == "profit" && filteredSales.isEmpty()) {
                    Text(text = tr("Немає даних про продажі", "No sales data"), modifier = Modifier.fillMaxWidth())
                } else if (type == "balance" && purchases.isEmpty() && sales.isEmpty()) {
                    Text(text = tr("Немає даних про операції", "No transaction data"), modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (type == "purchases" || type == "balance") {
                            items(filteredPurchases) { purchase ->
                                TransactionDetailItem(
                                    date = purchase.date,
                                    description = "$purchaseLabel • ${productNames[purchase.catalogId] ?: unknownProductText}",
                                    counterparty = supplierNames[purchase.supplierId] ?: unknownSupplierText,
                                    quantity = purchase.quantity,
                                    pcsLabel = pcsLabel,
                                    amount = purchase.totalAmount,
                                    isPurchase = true
                                )
                            }
                        }
                        if (type == "profit") {
                            // Обєднуємо позиції одного чеку (один клієнт + один той же
                            // `date` як `System.currentTimeMillis()` на момент оформлення) в одну
                            // карточку. Якщо клієнт в одному чеку купив 10 позицій — буде одна
                            // відкриваювана карточка з підсумком і деталями всередині.
                            val receipts: List<Pair<String, List<Sale>>> = filteredSales
                                .groupBy { "${it.clientId}|${it.date}" }
                                .toList()
                                .sortedByDescending { it.second.maxOfOrNull { s -> s.date } ?: 0L }
                            items(
                                items = receipts,
                                key = { it.first }
                            ) { (_, salesInReceipt) ->
                                ReceiptDetailItem(
                                    sales = salesInReceipt,
                                    clientName = clientNames[salesInReceipt.first().clientId] ?: unknownClientText,
                                    productNames = productNames,
                                    unknownProductText = unknownProductText,
                                    pcsLabel = pcsLabel,
                                    saleLabel = saleLabel
                                )
                            }
                        }
                        if (type == "balance") {
                            items(filteredSales) { sale ->
                                TransactionDetailItem(
                                    date = sale.date,
                                    description = "$saleLabel • ${productNames[sale.catalogId] ?: unknownProductText}",
                                    counterparty = clientNames[sale.clientId] ?: unknownClientText,
                                    quantity = sale.quantity,
                                    pcsLabel = pcsLabel,
                                    amount = sale.totalAmount,
                                    isPurchase = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

enum class DetailsSort {
    DATE_DESC,
    DATE_ASC,
    COUNTERPARTY_ASC,
    COUNTERPARTY_DESC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

private fun purchaseComparator(
    mode: DetailsSort,
    supplierNames: Map<String, String>,
    unknownSupplierText: String
): Comparator<Purchase> = when (mode) {
    DetailsSort.DATE_DESC -> compareByDescending { it.date }
    DetailsSort.DATE_ASC -> compareBy { it.date }
    DetailsSort.COUNTERPARTY_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { supplierNames[it.supplierId] ?: unknownSupplierText }
    DetailsSort.COUNTERPARTY_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { supplierNames[it.supplierId] ?: unknownSupplierText }
    DetailsSort.AMOUNT_DESC -> compareByDescending { it.totalAmount }
    DetailsSort.AMOUNT_ASC -> compareBy { it.totalAmount }
}

private fun saleComparator(
    mode: DetailsSort,
    clientNames: Map<String, String>,
    unknownClientText: String
): Comparator<Sale> = when (mode) {
    DetailsSort.DATE_DESC -> compareByDescending { it.date }
    DetailsSort.DATE_ASC -> compareBy { it.date }
    DetailsSort.COUNTERPARTY_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { clientNames[it.clientId] ?: unknownClientText }
    DetailsSort.COUNTERPARTY_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { clientNames[it.clientId] ?: unknownClientText }
    DetailsSort.AMOUNT_DESC -> compareByDescending { it.totalAmount }
    DetailsSort.AMOUNT_ASC -> compareBy { it.totalAmount }
}

@Composable
fun DetailsSortMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isPurchase: Boolean,
    current: DetailsSort,
    onSelect: (DetailsSort) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        val counterLabelAsc = if (isPurchase) tr("Постачальник A→Я", "Supplier A→Z") else tr("Клієнт A→Я", "Client A→Z")
        val counterLabelDesc = if (isPurchase) tr("Постачальник Я→A", "Supplier Z→A") else tr("Клієнт Я→A", "Client Z→A")
        DropdownMenuItem(
            text = { Text(counterLabelAsc + if (current == DetailsSort.COUNTERPARTY_ASC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.COUNTERPARTY_ASC) }
        )
        DropdownMenuItem(
            text = { Text(counterLabelDesc + if (current == DetailsSort.COUNTERPARTY_DESC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.COUNTERPARTY_DESC) }
        )
        DropdownMenuItem(
            text = { Text(tr("Дата ↓ (нові)", "Date ↓ (new)") + if (current == DetailsSort.DATE_DESC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.DATE_DESC) }
        )
        DropdownMenuItem(
            text = { Text(tr("Дата ↑ (старі)", "Date ↑ (old)") + if (current == DetailsSort.DATE_ASC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.DATE_ASC) }
        )
        DropdownMenuItem(
            text = { Text(tr("Сума ↓", "Amount ↓") + if (current == DetailsSort.AMOUNT_DESC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.AMOUNT_DESC) }
        )
        DropdownMenuItem(
            text = { Text(tr("Сума ↑", "Amount ↑") + if (current == DetailsSort.AMOUNT_ASC) "  ✓" else "") },
            onClick = { onSelect(DetailsSort.AMOUNT_ASC) }
        )
    }
}

/**
 * Картка одного чеку у звіті «Прибуток». Об'єднує всі позиції одного продажу
 * (один клієнт + однаковий `Sale.date` = одна операція оформлення) в один елемент
 * списку — щоб 10 позицій з одного чеку не показувалися як 10 окремих рядків.
 *
 * Стиснутий вигляд: дата, ім'я клієнта, кількість позицій, загальна сума, прибуток.
 * При натисканні розгортається — показуємо назву і кількість кожної позиції з ціною.
 */
@Composable
fun ReceiptDetailItem(
    sales: List<Sale>,
    clientName: String,
    productNames: Map<String, String>,
    unknownProductText: String,
    pcsLabel: String,
    saleLabel: String
) {
    if (sales.isEmpty()) return
    var expanded by remember(sales.first().saleId) { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val firstDate = sales.first().date
    val totalAmount = sales.sumOf { it.totalAmount }
    val totalProfit = sales.sumOf { it.netProfit }
    val totalQty = sales.sumOf { it.quantity }
    val itemsCount = sales.size

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IOSIconChip(
                    icon = Icons.Outlined.ShoppingCart,
                    tint = AccentGreen
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = clientName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2
                    )
                    Text(
                        text = "${dateFormat.format(Date(firstDate))} • $itemsCount " +
                            tr("поз.", "items") +
                            " • $totalQty $pcsLabel",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${String.format("%,.2f", totalAmount)} ₴",
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                    Text(
                        text = "${tr("Прибуток", "Profit")}: ${String.format("%,.2f", totalProfit)} ₴",
                        fontSize = 11.sp,
                        color = if (totalProfit >= 0) AccentGreen else MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) tr("Згорнути", "Collapse") else tr("Розгорнути", "Expand"),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp, start = 4.dp)) {
                    sales.forEachIndexed { idx, sale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}. ${productNames[sale.catalogId] ?: unknownProductText}",
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${sale.quantity} $pcsLabel × ${String.format("%,.2f", sale.pricePerUnit)} = " +
                                    "${String.format("%,.2f", sale.totalAmount)} ₴",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionDetailItem(
    date: Long,
    description: String,
    counterparty: String,
    quantity: Int,
    pcsLabel: String,
    amount: Double,
    isPurchase: Boolean
) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

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
                icon = if (isPurchase) Icons.Outlined.LocalAtm else Icons.Outlined.ShoppingCart,
                tint = if (isPurchase) AccentOrange else AccentGreen
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = description,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    maxLines = 2
                )
                Text(
                    text = "$counterparty • $quantity $pcsLabel • ${dateFormat.format(Date(date))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = if (isPurchase) "-${String.format("%,.2f", amount)} ₴" else "+${String.format("%,.2f", amount)} ₴",
                fontWeight = FontWeight.Bold,
                color = if (isPurchase) AccentOrange else AccentGreen
            )
        }
    }
}
