package com.numisproerp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.numisproerp.data.entities.CatalogItem
import com.numisproerp.ui.components.SortFilterRow
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.viewmodel.CatalogFilterField
import com.numisproerp.ui.viewmodel.CatalogSortField
import com.numisproerp.ui.viewmodel.CatalogViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    navController: NavHostController,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<CatalogItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadItems()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importExcelFile(context, it) { success, message ->
                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Верхній рядок
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
                text = tr("Каталог НБУ", "NBU Catalog"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row {
                IconButton(
                    onClick = { filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) }
                ) {
                    Icon(Icons.Default.Upload, contentDescription = tr("Завантажити Excel", "Upload Excel"), tint = AccentGreen)
                }
                if (uiState.isDataLoaded) {
                    IconButton(onClick = { viewModel.toggleSortDialog(true) }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = tr("Сортувати", "Sort"), tint = AccentBlue)
                    }
                    IconButton(onClick = { viewModel.toggleFilterDialog(true) }) {
                        Icon(Icons.Default.FilterList, contentDescription = tr("Фільтрувати", "Filter"), tint = AccentBlue)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 72.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Якщо дані не завантажені - показуємо кнопку завантаження
            if (!uiState.isDataLoaded && !uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tr(
                            "Каталог порожній. Завантажте Excel-файл з каталогом НБУ.",
                            "Catalog is empty. Upload an Excel file with NBU catalog."
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Text(tr("Завантажити Excel", "Upload Excel"), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            // Якщо завантажується - показуємо прогрес
            else if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            // Якщо дані є - показуємо список товарів
            else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.items) { item ->
                        CatalogItemCard(
                            item = item,
                            onClick = {
                                selectedItem = item
                                showDetailDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Діалог сортування — поле + напрямок (зростання / спадання).
    if (uiState.showSortDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleSortDialog(false) },
            title = { Text(tr("Сортувати", "Sort")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sortOptions = listOf(
                        SortOption(CatalogSortField.NAME, tr("За назвою", "By name"), Icons.Default.SortByAlpha),
                        SortOption(CatalogSortField.DATE, tr("За датою", "By date"), Icons.Default.CalendarMonth),
                        SortOption(CatalogSortField.DENOMINATION, tr("За номіналом", "By denomination"), Icons.Default.Numbers),
                        SortOption(CatalogSortField.MINTAGE, tr("За тиражем", "By mintage"), Icons.Default.Layers),
                        SortOption(CatalogSortField.MATERIAL, tr("За матеріалом", "By material"), Icons.Default.Science),
                        SortOption(CatalogSortField.SERIES, tr("За серією", "By series"), Icons.Default.Category)
                    )
                    sortOptions.forEach { opt ->
                        SortFilterRow(
                            icon = opt.icon,
                            label = opt.label,
                            selected = uiState.sortField == opt.field,
                            onClick = {
                                val ascending = if (uiState.sortField == opt.field) !uiState.sortAscending else true
                                viewModel.setSort(opt.field, ascending)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.setSort(uiState.sortField, true) },
                            shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
                            colors = if (uiState.sortAscending)
                                androidx.compose.material3.ButtonDefaults.buttonColors()
                            else
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Зростання", "Asc"))
                        }
                        Button(
                            onClick = { viewModel.setSort(uiState.sortField, false) },
                            shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius),
                            colors = if (!uiState.sortAscending)
                                androidx.compose.material3.ButtonDefaults.buttonColors()
                            else
                                androidx.compose.material3.ButtonDefaults.outlinedButtonColors()
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Спадання", "Desc"))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleSortDialog(false) }) {
                    Text(tr("Готово", "Done"))
                }
            }
        )
    }

    // Діалог фільтру — підтримує кілька осей: категорія, матеріал, серія,
    // рік, якість.
    if (uiState.showFilterDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleFilterDialog(false) },
            title = { Text(tr("Фільтри каталогу", "Catalog filters")) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterAxisSection(
                        title = tr("Категорія", "Category"),
                        icon = Icons.Default.Apps,
                        values = uiState.categories,
                        selected = uiState.selectedFilters[CatalogFilterField.CATEGORY],
                        onSelect = { value ->
                            if (value == null) viewModel.clearFilter(CatalogFilterField.CATEGORY)
                            else viewModel.setFilter(CatalogFilterField.CATEGORY, value)
                        }
                    )
                    FilterAxisSection(
                        title = tr("Матеріал", "Material"),
                        icon = Icons.Default.Science,
                        values = uiState.materials,
                        selected = uiState.selectedFilters[CatalogFilterField.MATERIAL],
                        onSelect = { value ->
                            if (value == null) viewModel.clearFilter(CatalogFilterField.MATERIAL)
                            else viewModel.setFilter(CatalogFilterField.MATERIAL, value)
                        }
                    )
                    FilterAxisSection(
                        title = tr("Серія", "Series"),
                        icon = Icons.Default.Category,
                        values = uiState.seriesList,
                        selected = uiState.selectedFilters[CatalogFilterField.SERIES],
                        onSelect = { value ->
                            if (value == null) viewModel.clearFilter(CatalogFilterField.SERIES)
                            else viewModel.setFilter(CatalogFilterField.SERIES, value)
                        }
                    )
                    FilterAxisSection(
                        title = tr("Рік", "Year"),
                        icon = Icons.Default.CalendarMonth,
                        values = uiState.years,
                        selected = uiState.selectedFilters[CatalogFilterField.YEAR],
                        onSelect = { value ->
                            if (value == null) viewModel.clearFilter(CatalogFilterField.YEAR)
                            else viewModel.setFilter(CatalogFilterField.YEAR, value)
                        }
                    )
                    FilterAxisSection(
                        title = tr("Якість", "Quality"),
                        icon = Icons.Default.Star,
                        values = uiState.qualities,
                        selected = uiState.selectedFilters[CatalogFilterField.QUALITY],
                        onSelect = { value ->
                            if (value == null) viewModel.clearFilter(CatalogFilterField.QUALITY)
                            else viewModel.setFilter(CatalogFilterField.QUALITY, value)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.toggleFilterDialog(false) }) {
                    Text(tr("Готово", "Done"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.clearAllFilters()
                }) {
                    Text(tr("Скинути", "Reset"))
                }
            }
        )
    }

    // Детальна карточка товару
    if (showDetailDialog && selectedItem != null) {
        AlertDialog(
            onDismissRequest = {
                showDetailDialog = false
                selectedItem = null
            },
            title = { Text(selectedItem!!.name) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Фото (аверс + реверс)
                    if (selectedItem!!.imageUrlFront.isNotEmpty() || selectedItem!!.imageUrlBack.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedItem!!.imageUrlFront.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedItem!!.imageUrlFront)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = tr("Аверс", "Obverse"),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = tr("Аверс", "Obverse"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                            if (selectedItem!!.imageUrlBack.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(selectedItem!!.imageUrlBack)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = tr("Реверс", "Reverse"),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = tr("Реверс", "Reverse"),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(IOSDesign.CardCornerRadius)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (selectedItem!!.series.isNotEmpty()) {
                                Row {
                                    Text("${tr("Серія", "Series")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.series, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.dateIntroduced.isNotEmpty()) {
                                Row {
                                    Text("${tr("Дата введення", "Issue date")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.dateIntroduced, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.material.isNotEmpty()) {
                                Row {
                                    Text("${tr("Матеріал", "Material")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.material, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.denomination.isNotEmpty()) {
                                Row {
                                    Text("${tr("Номінал", "Denomination")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("${selectedItem!!.denomination} ₴", fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.diameter.isNotEmpty()) {
                                Row {
                                    Text("${tr("Діаметр", "Diameter")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("${selectedItem!!.diameter} мм", fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.weight.isNotEmpty()) {
                                Row {
                                    Text("${tr("Маса", "Mass")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("${selectedItem!!.weight} г", fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.mintage.isNotEmpty()) {
                                Row {
                                    Text("${tr("Тираж", "Mintage")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.mintage, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.category.isNotEmpty()) {
                                Row {
                                    Text("${tr("Категорія", "Category")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.category, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.quality.isNotEmpty()) {
                                Row {
                                    Text("${tr("Якість карбування", "Mint quality")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.quality, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.artist.isNotEmpty()) {
                                Row {
                                    Text("${tr("Художник", "Artist")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.artist, fontSize = 13.sp)
                                }
                            }
                            if (selectedItem!!.sculptor.isNotEmpty()) {
                                Row {
                                    Text("${tr("Скульптор", "Sculptor")}: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text(selectedItem!!.sculptor, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    if (selectedItem!!.websiteUrl.isNotEmpty()) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedItem!!.websiteUrl))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null)
                            Text(tr("Переглянути на сайті НБУ", "View on NBU website"), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showDetailDialog = false
                    selectedItem = null
                }) {
                    Text(tr("Закрити", "Close"))
                }
            }
        )
    }
}

/**
 * Один варіант сортування каталогу для діалогу: поле + людська мітка + іконка.
 */
private data class SortOption(
    val field: CatalogSortField,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Секція діалогу фільтра — одна вісь (категорія / матеріал / серія / рік / якість).
 * Якщо в системі немає значень для осі — секція не показується, щоб не плодити
 * порожні розділи.
 */
@Composable
private fun FilterAxisSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    values: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        SortFilterRow(
            icon = Icons.Default.FilterList,
            label = tr("Усі", "All"),
            selected = selected == null,
            onClick = { onSelect(null) }
        )
        values.forEach { value ->
            SortFilterRow(
                icon = icon,
                label = value,
                selected = selected == value,
                onClick = { onSelect(value) }
            )
        }
    }
}

@Composable
fun CatalogItemCard(
    item: CatalogItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrlFront)
                    .crossfade(true)
                    .build(),
                contentDescription = tr("Фото", "Photo"),
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(IOSDesign.ChipCornerRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                if (item.series.isNotEmpty()) {
                    Text(
                        text = item.series,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row {
                    if (item.denomination.isNotEmpty()) {
                        Text(
                            text = "${item.denomination} ₴",
                            fontSize = 12.sp,
                            color = AccentOrange
                        )
                    }
                    if (item.dateIntroduced.isNotEmpty()) {
                        Text(
                            text = " • ${item.dateIntroduced}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
