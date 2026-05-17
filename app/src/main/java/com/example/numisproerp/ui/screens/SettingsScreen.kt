package com.numisproerp.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.numisproerp.R
import com.numisproerp.BuildConfig
import com.numisproerp.data.database.AppDatabase
import com.numisproerp.data.repository.Repository
import com.numisproerp.data.settings.AppLanguage
import com.numisproerp.data.settings.AppTheme
import com.numisproerp.data.settings.SettingsManager
import com.numisproerp.di.AppDatabaseEntryPoint
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.utils.ExcelExporter
import com.numisproerp.utils.ExcelImporter
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val settingsManager: SettingsManager,
    private val repository: Repository
) : ViewModel() {
    val theme: AppTheme get() = settingsManager.theme
    val language: AppLanguage get() = settingsManager.language
    fun setTheme(theme: AppTheme) { settingsManager.theme = theme }
    fun setLanguage(language: AppLanguage) { settingsManager.language = language }
    fun setLowStockThreshold(value: Int) { settingsManager.lowStockThreshold = value }
    suspend fun clearAllData() { repository.clearAllData() }
    suspend fun repairZeroQuantities(): Repository.RepairResult = repository.repairZeroQuantities()
}

@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database: AppDatabase = remember {
        EntryPointAccessors
            .fromApplication(context.applicationContext, AppDatabaseEntryPoint::class.java)
            .appDatabase()
    }
    val settings = viewModel.settingsManager

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDataDialog by remember { mutableStateOf(false) }
    var showFontsDialog by remember { mutableStateOf(false) }
    var showBackgroundDialog by remember { mutableStateOf(false) }
    var showTileIconsDialog by remember { mutableStateOf(false) }
    var showEmblemDialog by remember { mutableStateOf(false) }
    var showInfoCardsDialog by remember { mutableStateOf(false) }
    // Поточна плитка, для якої запускається picker (`tileId`). Зберігаємо тут,
    // щоб після повернення з image-picker зрозуміти, куди писати файл.
    var pickerTileId by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var resetConfirmation by remember { mutableStateOf("") }
    val resetDoneText = tr("Усі дані видалено", "All data cleared")

    val repairButtonTitle = tr("Виправити кількості", "Repair quantities")
    val repairButtonSubtitle = tr("Якщо показує 0 шт. замість реальної кількості", "Fix qty=0 from old import bug")
    val repairNothingMsg = tr(
        "Нічого виправляти — всі кількості вже коректні.",
        "Nothing to fix — all quantities are correct."
    )
    val repairFixedFmt = tr(
        "Виправлено: закупівель %d, продажів %d",
        "Fixed: purchases %d, sales %d"
    )

    val importedTitle = tr("Імпорт завершено", "Import complete")
    val productsLbl = tr("Товарів", "Products")
    val clientsLbl = tr("Клієнтів", "Clients")
    val suppliersLbl = tr("Постачальників", "Suppliers")
    val purchasesLbl = tr("Закупівель", "Purchases")
    val salesLbl = tr("Продажів", "Sales")
    val expensesLbl = tr("Витрат", "Expenses")
    val writeoffsLbl = tr("Списань", "Writeoffs")
    val collectionLbl = tr("Колекція", "Collection")
    val autoIdLbl = tr("авто-ID", "auto-ID")
    val skippedLbl = tr("пропущено порожніх", "skipped empty")
    val exportDoneTitle = tr("Експорт завершено", "Export complete")

    // Builds Toast з результату. Якщо productsOnly — показуємо тільки блок товарів.
    val buildImportMessage: (ExcelImporter.ImportResult, Boolean) -> String = { result, productsOnly ->
        if (!result.success) {
            result.message
        } else if (productsOnly) {
            val parts = mutableListOf("$productsLbl: ${result.productsCount}")
            if (result.productsAutoIdCount > 0) parts += "$autoIdLbl: ${result.productsAutoIdCount}"
            if (result.productsSkippedCount > 0) parts += "$skippedLbl: ${result.productsSkippedCount}"
            "$importedTitle — ${parts.joinToString(", ")}"
        } else {
            val parts = mutableListOf(
                "$productsLbl: ${result.productsCount}",
                "$clientsLbl: ${result.clientsCount}",
                "$suppliersLbl: ${result.suppliersCount}",
                "$purchasesLbl: ${result.purchasesCount}",
                "$salesLbl: ${result.salesCount}",
                "$expensesLbl: ${result.expensesCount}",
                "$writeoffsLbl: ${result.writeoffsCount}",
                "$collectionLbl: ${result.collectionCount}"
            )
            if (result.productsAutoIdCount > 0) parts += "$autoIdLbl: ${result.productsAutoIdCount}"
            if (result.productsSkippedCount > 0) parts += "$skippedLbl: ${result.productsSkippedCount}"
            "$importedTitle — ${parts.joinToString(", ")}"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val importer = ExcelImporter(database)
                val result = importer.importFromUri(context, uri, productsOnly = false)
                Toast.makeText(context, buildImportMessage(result, false), Toast.LENGTH_LONG).show()
            }
        }
    }

    val importProductsOnlyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val importer = ExcelImporter(database)
                val result = importer.importFromUri(context, uri, productsOnly = true)
                Toast.makeText(context, buildImportMessage(result, true), Toast.LENGTH_LONG).show()
            }
        }
    }

    val exportAction: () -> Unit = {
        scope.launch {
            val exporter = ExcelExporter(database)
            val result = exporter.exportToExcelDefault(context)
            Toast.makeText(
                context,
                if (result.success) "$exportDoneTitle: ${result.filePath}" else result.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val bgSetText = tr("Фон встановлено", "Background set")
    val bgErrorText = tr("Помилка завантаження", "Load error")
    val bgPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val target = File(context.filesDir, "custom_bg.jpg")
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        stream.use { input ->
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        settings.backgroundImagePath = target.absolutePath
                        Toast.makeText(context, bgSetText, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Picker для фото плиток головного меню. Зберігаємо файл як `tile_<id>.jpg`
    // у внутрішньому сховищі додатку. Шлях йде в SettingsManager під ключем
    // `tile_photo_<id>`, звідки його зчитує `QuickAccessButton`.
    val tilePhotoSetText = tr("Фото значка встановлено", "Tile photo set")
    val tilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val tileId = pickerTileId
        if (uri != null && tileId.isNotBlank()) {
            scope.launch {
                val target = File(context.filesDir, "tile_$tileId.jpg")
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        stream.use { input ->
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        settings.setTilePhotoPath(tileId, target.absolutePath)
                        Toast.makeText(context, tilePhotoSetText, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Picker для емблеми головного екрана. Зберігаємо як `emblem.jpg`.
    val emblemSetText = tr("Емблему встановлено", "Emblem set")
    val emblemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val target = File(context.filesDir, "emblem.jpg")
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    if (stream != null) {
                        stream.use { input ->
                            FileOutputStream(target).use { output -> input.copyTo(output) }
                        }
                        settings.emblemImagePath = target.absolutePath
                        Toast.makeText(context, emblemSetText, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, bgErrorText, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
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
                text = tr("Налаштування", "Settings"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsButton(
                    icon = Icons.Default.ColorLens,
                    title = tr("Тема", "Theme"),
                    subtitle = settings.theme.name,
                    onClick = { showThemeDialog = true }
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Default.Language,
                    title = tr("Мова", "Language"),
                    subtitle = settings.language.name,
                    onClick = { showLanguageDialog = true }
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Default.Storage,
                    title = tr("Дані", "Data"),
                    subtitle = tr("Імпорт / Експорт", "Import / Export"),
                    onClick = { showDataDialog = true }
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Default.FormatSize,
                    title = tr("Шрифти", "Fonts"),
                    subtitle = tr("Розмір, тип, колір", "Size, type, color"),
                    onClick = { showFontsDialog = true }
                )
            }
            item {
                SettingsButton(
                    icon = Icons.Default.Image,
                    title = tr("Фоновий малюнок", "Background image"),
                    subtitle = if (settings.backgroundImagePath.isNotBlank())
                        tr("Встановлено", "Set") else tr("Не вибрано", "Not set"),
                    onClick = { showBackgroundDialog = true }
                )
            }
            item {
                val customPhotosCount = settings.tilePhotoPathsState.value.size
                val alphaPct = (settings.tileBackgroundAlphaState.value * 100).toInt()
                val iconSize = settings.tileIconSizeState.value
                SettingsButton(
                    icon = Icons.Default.Apps,
                    title = tr("Значки головного меню", "Main menu icons"),
                    subtitle = tr(
                        "Фото: $customPhotosCount/8 · Розмір: ${iconSize}dp · Прозорість: $alphaPct%",
                        "Photos: $customPhotosCount/8 · Size: ${iconSize}dp · Opacity: $alphaPct%"
                    ),
                    onClick = { showTileIconsDialog = true }
                )
            }
            item {
                val emblemSet = settings.emblemImagePathState.value.isNotBlank()
                val emblemSize = settings.emblemSizeState.value
                SettingsButton(
                    icon = Icons.Default.Brush,
                    title = tr("Емблема головного екрана", "Header emblem"),
                    subtitle = if (emblemSet)
                        tr("Користувацька · Розмір: ${emblemSize}dp", "Custom · Size: ${emblemSize}dp")
                    else
                        tr("Стандартна · Розмір: ${emblemSize}dp", "Default · Size: ${emblemSize}dp"),
                    onClick = { showEmblemDialog = true }
                )
            }
            item {
                val infoAlphaPct = (settings.infoCardBackgroundAlphaState.value * 100).toInt()
                val infoColorSet = settings.infoCardBackgroundColorState.value.isNotBlank()
                SettingsButton(
                    icon = Icons.Default.ColorLens,
                    title = tr("Інформаційні картки", "Information cards"),
                    subtitle = if (infoColorSet)
                        tr("Колір фону встановлено · Прозорість: $infoAlphaPct%",
                           "Background color set · Opacity: $infoAlphaPct%")
                    else
                        tr("Стандартний фон · Прозорість: $infoAlphaPct%",
                           "Default background · Opacity: $infoAlphaPct%"),
                    onClick = { showInfoCardsDialog = true }
                )
            }

            // Notifications threshold
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
                ) {
                    val currentThreshold by settings.lowStockThresholdState
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Поріг низького залишку", "Low-stock threshold"),
                                fontSize = 15.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (currentThreshold == 0) tr("вимкнено", "off")
                                else "$currentThreshold ${tr("шт.", "pcs")}",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Slider(
                            value = currentThreshold.toFloat(),
                            onValueChange = { viewModel.setLowStockThreshold(it.toInt()) },
                            valueRange = 0f..SettingsManager.MAX_LOW_STOCK_THRESHOLD.toFloat(),
                            steps = SettingsManager.MAX_LOW_STOCK_THRESHOLD - 1
                        )
                    }
                }
            }

            // About
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.size(12.dp))
                        Column {
                            Text("NumisProERP", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${tr("Версія", "Version")} ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Repair button
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsButton(
                    icon = Icons.Default.Build,
                    title = repairButtonTitle,
                    subtitle = repairButtonSubtitle,
                    onClick = {
                        scope.launch {
                            val result = viewModel.repairZeroQuantities()
                            val msg = if (result.fixedPurchases == 0 && result.fixedSales == 0) {
                                repairNothingMsg
                            } else {
                                String.format(repairFixedFmt, result.fixedPurchases, result.fixedSales)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // Danger zone
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { resetConfirmation = ""; showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Text(tr("Скинути всі дані", "Reset all data"), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    // ===== DIALOGS =====
    if (showThemeDialog) {
        ThemeDialog(
            current = settings.theme,
            onSelect = { viewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = settings.language,
            onSelect = { viewModel.setLanguage(it) },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showDataDialog) {
        DataDialog(
            onImport = {
                importLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            },
            onImportProductsOnly = {
                importProductsOnlyLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            },
            onExport = exportAction,
            onDismiss = { showDataDialog = false }
        )
    }

    if (showFontsDialog) {
        FontsDialog(
            settings = settings,
            onDismiss = { showFontsDialog = false }
        )
    }

    if (showBackgroundDialog) {
        BackgroundImageDialog(
            currentPath = settings.backgroundImagePath,
            onPickImage = { bgPickerLauncher.launch("image/*") },
            onRemove = { settings.backgroundImagePath = "" },
            onDismiss = { showBackgroundDialog = false }
        )
    }

    if (showTileIconsDialog) {
        TileIconsDialog(
            settings = settings,
            onPickPhoto = { tileId ->
                pickerTileId = tileId
                tilePickerLauncher.launch("image/*")
            },
            onRemovePhoto = { tileId -> settings.setTilePhotoPath(tileId, "") },
            onDismiss = { showTileIconsDialog = false }
        )
    }

    if (showEmblemDialog) {
        EmblemDialog(
            settings = settings,
            onPickImage = { emblemPickerLauncher.launch("image/*") },
            onRemove = { settings.emblemImagePath = "" },
            onDismiss = { showEmblemDialog = false }
        )
    }

    if (showInfoCardsDialog) {
        InfoCardsDialog(
            settings = settings,
            onDismiss = { showInfoCardsDialog = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(tr("Видалити всі дані?", "Delete all data?")) },
            text = {
                Column {
                    Text(
                        tr(
                            "Введіть RESET у поле нижче для підтвердження.",
                            "Type RESET below to confirm."
                        ), fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetConfirmation,
                        onValueChange = { resetConfirmation = it },
                        label = { Text(tr("Підтвердження", "Confirmation")) },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = resetConfirmation == "RESET",
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            viewModel.clearAllData()
                            Toast.makeText(context, resetDoneText, Toast.LENGTH_LONG).show()
                        }
                    }
                ) { Text(tr("Видалити", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text(tr("Скасувати", "Cancel")) }
            }
        )
    }
}

// ======================== Top-level settings button ========================

@Composable
private fun SettingsButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

// ======================== Theme dialog ========================

@Composable
private fun ThemeDialog(current: AppTheme, onSelect: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Тема оформлення", "Theme")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOptionRow(tr("Стандартна", "Default"), selected = current == AppTheme.DEFAULT) { onSelect(AppTheme.DEFAULT) }
                ThemeOptionRow("OlegSmile", selected = current == AppTheme.OLEG_SMILE, emblem = R.drawable.oleg_smile_emblem) { onSelect(AppTheme.OLEG_SMILE) }
                ThemeOptionRow("OlegSmile v2", selected = current == AppTheme.OLEG_SMILE_V2, emblem = R.drawable.oleg_smile_emblem) { onSelect(AppTheme.OLEG_SMILE_V2) }
                ThemeOptionRow("OlegSmile Light", selected = current == AppTheme.OLEG_SMILE_LIGHT, emblem = R.drawable.oleg_smile_emblem) { onSelect(AppTheme.OLEG_SMILE_LIGHT) }
                ThemeOptionRow(
                    tr("Преміум 3D", "Premium 3D"),
                    selected = current == AppTheme.OLEG_SMILE_PREMIUM,
                    emblem = R.drawable.tile_premium_collection
                ) { onSelect(AppTheme.OLEG_SMILE_PREMIUM) }
                ThemeOptionRow("OceanGlass", selected = current == AppTheme.OCEAN_GLASS) { onSelect(AppTheme.OCEAN_GLASS) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

@Composable
private fun ThemeOptionRow(title: String, selected: Boolean, emblem: Int? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emblem != null) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = emblem),
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

// ======================== Language dialog ========================

@Composable
private fun LanguageDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Мова інтерфейсу", "App language")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LangRow("Українська", selected = current == AppLanguage.UA) { onSelect(AppLanguage.UA) }
                LangRow("English", selected = current == AppLanguage.EN) { onSelect(AppLanguage.EN) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

@Composable
private fun LangRow(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

// ======================== Data dialog ========================

@Composable
private fun DataDialog(
    onImport: () -> Unit,
    onImportProductsOnly: () -> Unit,
    onExport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Дані", "Data")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onImport(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Імпорт з Excel (всі дані)", "Import from Excel (all data)"))
                }
                Button(onClick = { onImportProductsOnly(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.Publish, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Імпорт лише товарів", "Import products only"))
                }
                Button(onClick = { onExport(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Outlined.ImportExport, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Експорт в Excel", "Export to Excel"))
                }
                Text(
                    text = tr(
                        "Імпорт оновлює існуючі записи за їх ID і додає нові — старі дані не зносяться. Для нових рядків без CatalogID він згенерується автоматично.",
                        "Import upserts records by ID and adds new ones — existing data is preserved. Rows with empty CatalogID get an auto-generated ID."
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрити", "Close")) } }
    )
}

// ======================== Fonts dialog ========================

@Composable
private fun FontsDialog(settings: SettingsManager, onDismiss: () -> Unit) {
    var fontSize by settings.fontSizeState
    var fontFamily by settings.fontFamilyState
    var fontColor by settings.fontColorState

    val families = listOf("system", "serif", "sans-serif", "monospace")
    val colors = listOf(
        "" to tr("За замовчуванням", "Default"),
        "FFFFFF" to tr("Білий", "White"),
        "000000" to tr("Чорний", "Black"),
        "FFD700" to tr("Золотий", "Gold"),
        "007AFF" to tr("Синій", "Blue"),
        "34C759" to tr("Зелений", "Green")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Шрифти", "Fonts")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Size
                Text(tr("Розмір шрифту: ${fontSize}sp", "Font size: ${fontSize}sp"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { settings.fontSize = it.toInt() },
                    valueRange = SettingsManager.MIN_FONT_SIZE.toFloat()..SettingsManager.MAX_FONT_SIZE.toFloat(),
                    steps = (SettingsManager.MAX_FONT_SIZE - SettingsManager.MIN_FONT_SIZE) - 1
                )
                Text(
                    tr("Рекомендовано 14–16sp для зручного читання", "Recommended 14–16sp for comfortable reading"),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                // Family
                Text(tr("Тип шрифту", "Font type"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                families.forEach { family ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { settings.fontFamily = family }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(family, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        RadioButton(
                            selected = fontFamily == family,
                            onClick = { settings.fontFamily = family },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
                Text(
                    tr("Рекомендація: system — найбільш читабельний на вашому пристрої",
                        "Recommendation: system — most readable on your device"),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(4.dp))
                // Color
                Text(tr("Колір тексту", "Text color"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                colors.forEach { (hex, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { settings.fontColor = hex }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hex.isNotBlank()) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor("#$hex")))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(label, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        RadioButton(
                            selected = fontColor == hex,
                            onClick = { settings.fontColor = hex },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

// ======================== Tile icons dialog ========================

/**
 * Діалог для керування значками головного меню (плитками швидкого доступу).
 * - Для кожного з 8 `tileId` дозволяє вибрати власне фото (через переданий
 *   `onPickPhoto`) і прибрати фото (через `onRemovePhoto`).
 * - Один глобальний повзунок керує прозорістю фону всіх плиток
 *   (записує в `settings.tileBackgroundAlpha`).
 */
@Composable
private fun TileIconsDialog(
    settings: SettingsManager,
    onPickPhoto: (tileId: String) -> Unit,
    onRemovePhoto: (tileId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val photos by settings.tilePhotoPathsState
    var alpha by settings.tileBackgroundAlphaState
    var iconSize by settings.tileIconSizeState
    var bgColor by settings.tileBackgroundColorState

    // Назви + accent кольори для відображення в діалозі. Mapping tileId -> (Ua, En, accent).
    data class TileLabel(val id: String, val ua: String, val en: String)
    val labels = remember {
        listOf(
            TileLabel("purchase", "Закупівля", "Purchase"),
            TileLabel("sale", "Продаж", "Sale"),
            TileLabel("stock", "Склад", "Stock"),
            TileLabel("clients", "Клієнти", "Clients"),
            TileLabel("reports", "Звіти", "Reports"),
            TileLabel("suppliers", "Постачальники", "Suppliers"),
            TileLabel("expenses", "Витрати", "Expenses"),
            TileLabel("collection", "Моя колекція", "Collection")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Значки головного меню", "Main menu icons")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Розмір значка (масштаб для збільшення власних фото).
                Text(
                    tr("Розмір значка: ${iconSize}dp", "Icon size: ${iconSize}dp"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = iconSize.toFloat(),
                    onValueChange = { settings.tileIconSize = it.toInt() },
                    valueRange = SettingsManager.MIN_TILE_ICON_SIZE.toFloat()
                            ..SettingsManager.MAX_TILE_ICON_SIZE.toFloat(),
                    steps = (SettingsManager.MAX_TILE_ICON_SIZE - SettingsManager.MIN_TILE_ICON_SIZE) / 4 - 1
                )
                Text(
                    tr(
                        "Стандарт — 68dp. Збільшіть, щоб ваші фото на плитках виглядали більшими.",
                        "Default — 68dp. Increase if your tile photos look too small."
                    ),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Глобальний повзунок прозорості фону плитки.
                val alphaPct = (alpha * 100).toInt()
                Text(
                    tr("Прозорість фону значка: $alphaPct%", "Tile background opacity: $alphaPct%"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = alpha,
                    onValueChange = { settings.tileBackgroundAlpha = it },
                    valueRange = 0f..1f
                )
                Text(
                    tr(
                        "0% — повністю прозорий фон, 100% — як у системних іконках.",
                        "0% — fully transparent background, 100% — like system icons."
                    ),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Колір фону плиток.
                Text(
                    tr("Колір фону значків", "Tile background color"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                ColorPickerRow(
                    currentHex = bgColor,
                    onSelect = { settings.tileBackgroundColor = it }
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    tr("Фото для значків", "Tile photos"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )

                labels.forEach { tile ->
                    val hasPhoto = photos[tile.id].orEmpty().isNotBlank()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tr(tile.ua, tile.en),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (hasPhoto) tr("Фото встановлено", "Photo set")
                                else tr("Стандартний значок", "Default icon"),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        TextButton(onClick = { onPickPhoto(tile.id) }) {
                            Text(
                                if (hasPhoto) tr("Замінити", "Replace")
                                else tr("Вибрати", "Choose"),
                                fontSize = 13.sp
                            )
                        }
                        if (hasPhoto) {
                            TextButton(onClick = { onRemovePhoto(tile.id) }) {
                                Text(
                                    tr("Видалити", "Remove"),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

// ======================== Background image dialog ========================

@Composable
private fun BackgroundImageDialog(currentPath: String, onPickImage: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Фоновий малюнок", "Background image")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (currentPath.isNotBlank()) {
                    Text(
                        tr("Поточний: встановлено", "Current: set"),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        tr("Фон не вибрано", "No background set"),
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Button(onClick = { onPickImage(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Вибрати зображення…", "Choose image…"))
                }
                if (currentPath.isNotBlank()) {
                    OutlinedButton(onClick = { onRemove(); onDismiss() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Text(tr("Видалити фон", "Remove background"))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Закрити", "Close")) } }
    )
}

// ======================== Emblem dialog ========================

/**
 * Діалог керування емблемою, що показується у шапці головного екрана
 * над інформаційними картками. Дозволяє підмінити стандартну емблему
 * власним фото та задати її розмір.
 */
@Composable
private fun EmblemDialog(
    settings: SettingsManager,
    onPickImage: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val emblemPath by settings.emblemImagePathState
    var emblemSize by settings.emblemSizeState

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Емблема головного екрана", "Header emblem")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (emblemPath.isNotBlank())
                        tr("Поточна: користувацька", "Current: custom")
                    else
                        tr("Поточна: стандартна", "Current: default"),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Button(
                    onClick = { onPickImage(); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Вибрати зображення…", "Choose image…"))
                }
                if (emblemPath.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onRemove() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(tr("Скинути до стандартної", "Reset to default"))
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    tr("Розмір емблеми: ${emblemSize}dp", "Emblem size: ${emblemSize}dp"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = emblemSize.toFloat(),
                    onValueChange = { settings.emblemSize = it.toInt() },
                    valueRange = SettingsManager.MIN_EMBLEM_SIZE.toFloat()
                            ..SettingsManager.MAX_EMBLEM_SIZE.toFloat(),
                    steps = (SettingsManager.MAX_EMBLEM_SIZE - SettingsManager.MIN_EMBLEM_SIZE) / 4 - 1
                )
                Text(
                    tr(
                        "Стандарт — 72dp. Емблема відображається круглою у шапці.",
                        "Default — 72dp. The emblem appears rounded in the header."
                    ),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

// ======================== Info cards dialog ========================

/**
 * Діалог керування фоновим виглядом інформаційних карток на головному екрані
 * (баланс, місячні стати, останні операції): колір фону + прозорість.
 */
@Composable
private fun InfoCardsDialog(
    settings: SettingsManager,
    onDismiss: () -> Unit
) {
    var bgColor by settings.infoCardBackgroundColorState
    var alpha by settings.infoCardBackgroundAlphaState

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Інформаційні картки", "Information cards")) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    tr("Колір фону карток", "Card background color"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                ColorPickerRow(
                    currentHex = bgColor,
                    onSelect = { settings.infoCardBackgroundColor = it }
                )

                Spacer(modifier = Modifier.height(4.dp))
                val alphaPct = (alpha * 100).toInt()
                Text(
                    tr("Прозорість фону: $alphaPct%", "Background opacity: $alphaPct%"),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = alpha,
                    onValueChange = { settings.infoCardBackgroundAlpha = it },
                    valueRange = 0f..1f
                )
                Text(
                    tr(
                        "0% — повністю прозорий фон карток, 100% — суцільний колір.",
                        "0% — fully transparent card background, 100% — solid color."
                    ),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Готово", "Done")) } }
    )
}

// ======================== Color picker row ========================

/**
 * Горизонтальний рядок з палітрою стандартних кольорів + опцією "за замовчуванням".
 * Кожен колір зберігається як hex-рядок без `#`, порожній рядок = використати
 * стандартний колір теми у викликачів.
 */
@Composable
private fun ColorPickerRow(currentHex: String, onSelect: (String) -> Unit) {
    // Палітра кольорів для фону (узгоджена з палітрою кольорів шрифтів).
    val swatches = remember {
        listOf(
            "" to "default",
            "FFFFFF" to "white",
            "000000" to "black",
            "FFD700" to "gold",
            "F5E6C8" to "cream",
            "FFA500" to "orange",
            "FF3B30" to "red",
            "FF2D55" to "pink",
            "AF52DE" to "purple",
            "5856D6" to "indigo",
            "007AFF" to "blue",
            "34C759" to "green",
            "8E8E93" to "gray"
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        swatches.forEach { (hex, _) ->
            val selected = currentHex.equals(hex, ignoreCase = true)
            val border = if (selected) 3.dp else 1.dp
            val borderColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (hex.isBlank()) MaterialTheme.colorScheme.surface
                        else Color(android.graphics.Color.parseColor("#$hex"))
                    )
                    .border(border, borderColor, CircleShape)
                    .clickable { onSelect(hex) },
                contentAlignment = Alignment.Center
            ) {
                if (hex.isBlank()) {
                    Text("∅", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}
