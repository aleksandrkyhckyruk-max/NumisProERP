package com.numisproerp.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.numisproerp.data.dao.BundleWithSales
import com.numisproerp.data.dao.ProductInStock
import com.numisproerp.data.repository.Repository
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.AccentRed
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.viewmodel.BundleComponentDraft
import com.numisproerp.ui.viewmodel.BundleViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Екран «Моя збірка»: список вже створених збірок та кнопка створення нової.
 *
 * Кожна збірка — це готовий продукт із сумарною собівартістю компонентів та
 * статистикою продажів (скільки вже продано, на яку суму, який прибуток).
 */
@Composable
fun BundleScreen(
    navController: NavHostController,
    viewModel: BundleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bundleToDelete by remember { mutableStateOf<BundleWithSales?>(null) }
    var bundleToDisassemble by remember { mutableStateOf<BundleWithSales?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val disassemblyOk = tr(
        "Збірку розібрано — компоненти повернулися на склад.",
        "Bundle disassembled — components returned to stock."
    )
    val disassemblySold = tr(
        "Не можна розібрати: збірка вже продана.",
        "Cannot disassemble: bundle is already sold."
    )
    val disassemblyMissing = tr(
        "Збірку не знайдено.",
        "Bundle not found."
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = tr("Назад", "Back"),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = tr("Моя збірка", "My Bundle"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                textAlign = TextAlign.Center
            )

            if (uiState.bundles.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            tr("Збірок поки немає", "No bundles yet"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                        Text(
                            tr("Натисніть «+», щоб скласти збірку зі складських товарів",
                                "Tap «+» to compose a bundle from stock items"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.bundles) { b ->
                        BundleCard(
                            b = b,
                            onDelete = { bundleToDelete = b },
                            onDisassemble = { bundleToDisassemble = b }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.openCreator() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = tr("Створити збірку", "Create bundle"))
        }
    }

    if (uiState.showCreator) {
        BundleCreatorDialog(
            stockProducts = uiState.productsInStock,
            draftName = uiState.draftName,
            draftComponents = uiState.draftComponents,
            draftSuggestedPrice = uiState.draftSuggestedPrice,
            draftComment = uiState.draftComment,
            totalCost = uiState.draftTotalCost,
            errorMessage = uiState.errorMessage,
            onNameChange = viewModel::setDraftName,
            onPriceChange = viewModel::setDraftSuggestedPrice,
            onCommentChange = viewModel::setDraftComment,
            onAddComponent = viewModel::addComponent,
            onChangeQty = viewModel::setComponentQuantity,
            onRemoveComponent = viewModel::removeComponent,
            onDismiss = { viewModel.closeCreator() },
            onSave = { viewModel.saveBundle() }
        )
    }

    val pending = bundleToDelete
    if (pending != null) {
        AlertDialog(
            onDismissRequest = { bundleToDelete = null },
            title = { Text(tr("Видалити збірку?", "Delete bundle?")) },
            text = {
                Text(tr(
                    "«${pending.name}» буде видалена з історії збірок. " +
                            "Закупівля, продажі та списання компонентів залишаться без змін.",
                    "«${pending.name}» will be removed from the bundle history. " +
                            "The associated purchase, sales and component writeoffs will remain unchanged."
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteBundle(pending.bundleId)
                    bundleToDelete = null
                }) { Text(tr("Видалити", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { bundleToDelete = null }) { Text(tr("Скасувати", "Cancel")) }
            }
        )
    }

    val disassembling = bundleToDisassemble
    if (disassembling != null) {
        AlertDialog(
            onDismissRequest = { bundleToDisassemble = null },
            title = { Text(tr("Розібрати збірку?", "Disassemble bundle?")) },
            text = {
                Text(tr(
                    "«${disassembling.name}» зникне зі складу, а компоненти " +
                        "повернуться у наявність. Сама збірка видалиться.",
                    "«${disassembling.name}» will disappear from stock and " +
                        "its components will return to inventory. The bundle " +
                        "itself will be removed."
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    val bundleId = disassembling.bundleId
                    bundleToDisassemble = null
                    scope.launch {
                        val result = viewModel.disassembleBundle(bundleId)
                        val msg = when (result) {
                            is Repository.DisassembleResult.Success -> disassemblyOk
                            is Repository.DisassembleResult.AlreadySold -> disassemblySold
                            is Repository.DisassembleResult.NotFound -> disassemblyMissing
                        }
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }) { Text(tr("Розібрати", "Disassemble"), color = AccentOrange) }
            },
            dismissButton = {
                TextButton(onClick = { bundleToDisassemble = null }) {
                    Text(tr("Скасувати", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun BundleCard(
    b: BundleWithSales,
    onDelete: () -> Unit,
    onDisassemble: () -> Unit
) {
    val df = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val unitCost = b.totalCost
    val profit = b.totalRevenue - (b.soldCount * unitCost)
    // Збірку, де хоча б 1 шт. вже продана, не можна розібрати — інакше
    // повернути компоненти на склад без втрати Sale неможливо коректно.
    val canDisassemble = b.soldCount <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(b.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = tr("Складено: ", "Assembled: ") + df.format(Date(b.assembledDate)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                IconButton(
                    onClick = onDisassemble,
                    enabled = canDisassemble,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = tr("Розібрати", "Disassemble"),
                        modifier = Modifier.size(18.dp),
                        tint = if (canDisassemble) AccentOrange else AccentOrange.copy(alpha = 0.3f)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = AccentRed)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricCol(label = tr("Собівартість", "Cost"), value = formatMoney(unitCost), color = AccentOrange)
                MetricCol(label = tr("Продано", "Sold"), value = "${b.soldCount} ${tr("шт.", "pcs")}", color = AccentBlue)
                MetricCol(label = tr("Виторг", "Revenue"), value = formatMoney(b.totalRevenue), color = AccentBlue)
                MetricCol(
                    label = tr("Прибуток", "Profit"),
                    value = formatMoney(profit),
                    color = if (profit >= 0) AccentGreen else AccentRed
                )
            }
            if (b.suggestedPrice > 0) {
                Text(
                    text = tr("Пропонована ціна: ", "Suggested price: ") + formatMoney(b.suggestedPrice),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (b.comment.isNotBlank()) {
                Text(
                    text = b.comment,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BundleCreatorDialog(
    stockProducts: List<ProductInStock>,
    draftName: String,
    draftComponents: List<BundleComponentDraft>,
    draftSuggestedPrice: String,
    draftComment: String,
    totalCost: Double,
    errorMessage: String,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onCommentChange: (String) -> Unit,
    onAddComponent: (ProductInStock) -> Unit,
    onChangeQty: (String, Int) -> Unit,
    onRemoveComponent: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Нова збірка", "New bundle")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draftName,
                    onValueChange = onNameChange,
                    label = { Text(tr("Назва (наприклад «Готовий бокс ЗСУ»)", "Name (e.g. «Ready box»)")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                )

                Text(
                    text = tr("Компоненти зі складу", "Components from stock"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                if (draftComponents.isEmpty()) {
                    Text(
                        text = tr("Поки не вибрано жодного компонента",
                            "No components selected yet"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        draftComponents.forEach { c ->
                            ComponentLine(
                                c = c,
                                onMinus = { onChangeQty(c.catalogId, c.quantity - 1) },
                                onPlus = { onChangeQty(c.catalogId, c.quantity + 1) },
                                onRemove = { onRemoveComponent(c.catalogId) }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(tr("Додати компонент зі складу", "Add component from stock"))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Собівартість:", "Cost:"), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(
                        formatMoney(totalCost),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                }

                OutlinedTextField(
                    value = draftSuggestedPrice,
                    onValueChange = onPriceChange,
                    label = { Text(tr("Пропонована ціна продажу (опц.)", "Suggested sale price (optional)")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                )

                OutlinedTextField(
                    value = draftComment,
                    onValueChange = onCommentChange,
                    label = { Text(tr("Коментар (опц.)", "Comment (optional)")) },
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
            ) { Text(tr("Створити збірку", "Create bundle")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Скасувати", "Cancel")) }
        }
    )

    if (showPicker) {
        StockComponentPickerDialog(
            stock = stockProducts,
            alreadySelected = draftComponents.map { it.catalogId }.toSet(),
            onPick = {
                onAddComponent(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun ComponentLine(
    c: BundleComponentDraft,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onRemove: () -> Unit
) {
    val isOverdraw = c.quantity > c.availableInStock
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverdraw)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(c.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(
                    text = tr("На складі: ${c.availableInStock} | ціна: ", "In stock: ${c.availableInStock} | price: ") +
                            formatMoney(c.unitCost),
                    fontSize = 10.sp,
                    color = if (isOverdraw) AccentRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onMinus, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = c.quantity.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            IconButton(onClick = onPlus, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentRed)
            }
        }
    }
}

@Composable
private fun StockComponentPickerDialog(
    stock: List<ProductInStock>,
    alreadySelected: Set<String>,
    onPick: (ProductInStock) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Виберіть компонент", "Pick component")) },
        text = {
            if (stock.isEmpty()) {
                Text(
                    tr("На складі немає товарів", "No products in stock"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(stock) { p ->
                        val isAlreadyAdded = p.catalogId in alreadySelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAlreadyAdded) { onPick(p) }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Inventory2,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isAlreadyAdded) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else AccentBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.name.ifBlank { p.catalogId },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isAlreadyAdded)
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${tr("Залишок", "Stock")}: ${p.currentStock} | ${formatMoney(p.avgPurchasePrice)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            if (isAlreadyAdded) {
                                Text(
                                    tr("Додано", "Added"),
                                    fontSize = 10.sp,
                                    color = AccentGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Закрити", "Close")) }
        }
    )
}

private fun formatMoney(amount: Double): String = String.format("%,.2f ₴", amount)
