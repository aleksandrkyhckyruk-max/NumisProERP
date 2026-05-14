package com.numisproerp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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

    var selectedSupplierIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedClientIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilter by remember { mutableStateOf(false) }

    val unknownSupplierText = tr("Невідомий постачальник", "Unknown supplier")
    val unknownClientText = tr("Невідомий клієнт", "Unknown client")
    val purchaseLabel = tr("Закупівля", "Purchase")
    val saleLabel = tr("Продаж", "Sale")

    val filteredPurchases by remember(purchases, selectedSupplierIds) {
        derivedStateOf {
            if (selectedSupplierIds.isEmpty()) purchases
            else purchases.filter { it.supplierId in selectedSupplierIds }
        }
    }

    val filteredSales by remember(sales, selectedClientIds) {
        derivedStateOf {
            if (selectedClientIds.isEmpty()) sales
            else sales.filter { it.clientId in selectedClientIds }
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
            when (type) {
                "purchases" -> {
                    purchases = database.purchaseDao().getAllPurchases()
                    val supplierMap = mutableMapOf<String, String>()
                    purchases.map { it.supplierId }.distinct().forEach { supplierId ->
                        val supplier = database.supplierDao().getSupplierById(supplierId)
                        supplierMap[supplierId] = supplier?.name ?: unknownSupplierText
                    }
                    supplierNames = supplierMap
                }
                "profit" -> {
                    sales = database.saleDao().getAllSales()
                    val clientMap = mutableMapOf<String, String>()
                    sales.map { it.clientId }.distinct().forEach { clientId ->
                        val client = database.clientDao().getClientById(clientId)
                        clientMap[clientId] = client?.name ?: unknownClientText
                    }
                    clientNames = clientMap
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
                }
            }
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
                                    description = purchaseLabel,
                                    counterparty = supplierNames[purchase.supplierId] ?: unknownSupplierText,
                                    amount = -purchase.totalAmount,
                                    isPurchase = true
                                )
                            }
                        }
                        if (type == "profit" || type == "balance") {
                            items(filteredSales) { sale ->
                                TransactionDetailItem(
                                    date = sale.date,
                                    description = saleLabel,
                                    counterparty = clientNames[sale.clientId] ?: unknownClientText,
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

@Composable
fun TransactionDetailItem(
    date: Long,
    description: String,
    counterparty: String,
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
                    fontSize = 14.sp
                )
                Text(
                    text = "$counterparty • ${dateFormat.format(Date(date))}",
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
