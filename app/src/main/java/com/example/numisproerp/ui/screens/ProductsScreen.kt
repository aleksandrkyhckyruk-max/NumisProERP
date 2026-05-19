package com.numisproerp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.numisproerp.data.entities.Product
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.theme.IOSIconChip
import com.numisproerp.ui.viewmodel.ProductsViewModel
import com.numisproerp.utils.ImageStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    navController: NavHostController,
    viewModel: ProductsViewModel = hiltViewModel(),
    /**
     * Якщо `true` — діалог ручного додавання товару відкривається одразу
     * після першої композиції. Використовується кнопкою "Додати товар" у
     * боковому меню — переходимо в "Товари" і відразу даємо заповнити форму.
     */
    openAddOnStart: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showAddDialog by remember { mutableStateOf(openAddOnStart) }
    val context = LocalContext.current
    val addedText = tr("Товар додано в каталог", "Product added to catalog")

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

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(tr("Додати товар", "Add product")) },
            containerColor = AccentBlue,
            contentColor = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = tr("Товари", "Products"),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(tr("Пошук за назвою, серією або ID...", "Search by name, series or ID...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Outlined.Clear, contentDescription = tr("Очистити", "Clear"))
                        }
                    }
                },
                shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Помітна кнопка ручного додавання: користувачі не завжди помічають
            // FAB у нижньому правому куті, тому дублюємо її на видному місці
            // одразу під пошуком. Обидві відкривають той самий діалог.
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(IOSDesign.ButtonCornerRadius)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tr("Додати товар вручну", "Add product manually"),
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (uiState.categories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = uiState.selectedCategory.isBlank(),
                            onClick = { viewModel.updateSelectedCategory("") },
                            label = { Text(tr("Усі", "All")) }
                        )
                    }
                    items(uiState.categories) { category ->
                        FilterChip(
                            selected = uiState.selectedCategory == category,
                            onClick = { viewModel.updateSelectedCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val visibleProducts = viewModel.filteredProducts()

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                visibleProducts.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = tr("Немає товарів у базі.\nІмпортуйте каталог через 'Каталог НБУ'", "No products in database.\nImport via 'NBU Catalog'"),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    Text(
                        text = "${tr("Знайдено", "Found")}: ${visibleProducts.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(visibleProducts) { product ->
                            ProductRowCard(
                                product = product,
                                imageUrl = viewModel.getProductImageUrl(product),
                                onClick = { selectedProduct = product }
                            )
                        }
                    }
                }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    selectedProduct?.let { product ->
        val imageUrls = viewModel.getProductImageUrls(product)
        val addedCollectionText = tr("Додано до колекції", "Added to collection")
        val alreadyText = tr("Вже в колекції", "Already in collection")
        ProductDetailDialog(
            product = product,
            imageUrlFront = imageUrls.first,
            imageUrlBack = imageUrls.second,
            onDismiss = { selectedProduct = null },
            onAddToCollection = {
                coroutineScope.launch {
                    val added = viewModel.addProductToCollection(product)
                    Toast.makeText(
                        context,
                        if (added) addedCollectionText else alreadyText,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                selectedProduct = null
            }
        )
    }

    if (showAddDialog) {
        AddManualProductDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newProduct ->
                viewModel.addManualProduct(newProduct) {
                    Toast.makeText(context, addedText, Toast.LENGTH_SHORT).show()
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ProductRowCard(
    product: Product,
    imageUrl: String,
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(photoPath = imageUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                if (product.series.isNotBlank()) {
                    Text(
                        text = product.series,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (product.category.isNotBlank()) {
                        TagChip(text = product.category, color = AccentBlue)
                    }
                    if (product.material.isNotBlank()) {
                        TagChip(text = product.material, color = AccentOrange)
                    }
                }
            }
            Text(
                text = product.catalogId,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun TagChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun ProductThumbnail(photoPath: String) {
    val context = LocalContext.current
    if (photoPath.isBlank()) {
        IOSIconChip(
            icon = Icons.Outlined.PhotoCamera,
            tint = MaterialTheme.colorScheme.primary,
            chipSize = IOSDesign.IconChipLarge,
            iconSize = IOSDesign.IconSizeLarge,
            cornerRadius = IOSDesign.CardCornerRadiusSmall,
            contentDescription = tr("Фото", "Photo")
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context).data(photoPath).build(),
            contentDescription = tr("Фото товару", "Product photo"),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(IOSDesign.IconChipLarge)
                .clip(RoundedCornerShape(IOSDesign.CardCornerRadiusSmall))
        )
    }
}

@Composable
fun ProductDetailDialog(
    product: Product,
    imageUrlFront: String = product.photoPath,
    imageUrlBack: String = product.photoPathBack,
    onDismiss: () -> Unit,
    onAddToCollection: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onAddToCollection != null) {
                    TextButton(onClick = onAddToCollection) {
                        Text(tr("В колекцію", "To collection"), color = AccentGreen)
                    }
                }
                TextButton(onClick = onDismiss) { Text(tr("Закрити", "Close")) }
            }
        },
        title = {
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (imageUrlFront.isNotBlank() || imageUrlBack.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (imageUrlFront.isNotBlank()) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProductDetailPhoto(photoPath = imageUrlFront)
                                if (imageUrlBack.isNotBlank()) {
                                    Text(
                                        text = tr("Аверс", "Obverse"),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                        if (imageUrlBack.isNotBlank()) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ProductDetailPhoto(photoPath = imageUrlBack)
                                Text(
                                    text = tr("Реверс", "Reverse"),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                DetailRow("ID каталогу", product.catalogId)
                DetailRow(tr("Серія", "Series"), product.series)
                DetailRow(tr("Категорія", "Category"), product.category)
                DetailRow(tr("Матеріал", "Material"), product.material)
                DetailRow(tr("Номінал", "Nominal"), product.nominal)
                DetailRow(tr("Якість", "Quality"), product.quality)
                DetailRow(tr("Діаметр", "Diameter"), product.diameter)
                DetailRow(tr("Вага", "Weight"), product.weight)
                DetailRow(tr("Тираж (заявлено)", "Mintage (announced)"), product.mintageAnnounced)
                DetailRow(tr("Тираж (фактично)", "Mintage (actual)"), product.mintageActual)
                DetailRow(tr("Дата випуску", "Issue date"), product.issueDate)
                DetailRow(tr("Художник", "Artist"), product.artist)
                DetailRow(tr("Скульптор", "Sculptor"), product.sculptor)
                if (product.estimatedValue > 0.0) {
                    DetailRow(
                        tr("Орієнтовна вартість", "Estimated value"),
                        String.format("%,.2f \u20b4", product.estimatedValue)
                    )
                }
                DetailRow(tr("Опис", "Description"), product.description)
            }
        }
    )
}

@Composable
private fun ProductDetailPhoto(photoPath: String) {
    val context = LocalContext.current
    if (photoPath.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(IOSDesign.CardCornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
        }
    } else {
        AsyncImage(
            model = ImageRequest.Builder(context).data(photoPath).build(),
            contentDescription = tr("Фото", "Photo"),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(IOSDesign.CardCornerRadius))
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.5f)
        )
    }
}

/**
 * Діалог ручного додавання товару в каталог (без прив'язки до постачальника
 * і без імпорту Excel). Підтримує до 2 фото (аверс/реверс) та усі поля
 * Product, які заповнюються в авто-імпортованих товарах.
 *
 * Кількість не вводиться — товар з'являється в каталозі з нульовим залишком,
 * закупка/продаж відстежуються через існуючі механізми покупок та продажів.
 */
@Composable
private fun AddManualProductDialog(
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var series by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var material by remember { mutableStateOf("") }
    var nominal by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf("") }
    var diameter by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var mintageAnnounced by remember { mutableStateOf("") }
    var mintageActual by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var sculptor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var estimatedValueStr by remember { mutableStateOf("") }
    var photoFront by remember { mutableStateOf("") }
    var photoBack by remember { mutableStateOf("") }

    val frontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            ImageStorage.copyUriToInternalStorage(context, uri)?.let { photoFront = it }
        }
    }
    val backLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            ImageStorage.copyUriToInternalStorage(context, uri)?.let { photoBack = it }
        }
    }

    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                text = tr("Новий товар у каталог", "New catalog product"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr(
                        "Заповніть інформацію про товар. Кількість не вказується — змінюється закупками та продажами.",
                        "Fill product details. Quantity is not specified — it is tracked via purchases and sales."
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ManualProductPhotoBox(
                        photoPath = photoFront,
                        label = tr("Аверс", "Obverse"),
                        onPick = {
                            frontLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClear = { photoFront = "" }
                    )
                    ManualProductPhotoBox(
                        photoPath = photoBack,
                        label = tr("Реверс", "Reverse"),
                        onPick = {
                            backLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onClear = { photoBack = "" }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(tr("Назва *", "Name *")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = series,
                    onValueChange = { series = it },
                    label = { Text(tr("Серія", "Series")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(tr("Категорія", "Category")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = material,
                    onValueChange = { material = it },
                    label = { Text(tr("Матеріал", "Material")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = nominal,
                    onValueChange = { nominal = it },
                    label = { Text(tr("Номінал", "Nominal")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = quality,
                    onValueChange = { quality = it },
                    label = { Text(tr("Якість", "Quality")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = diameter,
                    onValueChange = { diameter = it },
                    label = { Text(tr("Діаметр", "Diameter")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text(tr("Вага", "Weight")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mintageAnnounced,
                    onValueChange = { mintageAnnounced = it },
                    label = { Text(tr("Тираж (заявлено)", "Mintage (announced)")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mintageActual,
                    onValueChange = { mintageActual = it },
                    label = { Text(tr("Тираж (фактично)", "Mintage (actual)")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = issueDate,
                    onValueChange = { issueDate = it },
                    label = { Text(tr("Дата випуску", "Issue date")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(tr("Художник", "Artist")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = sculptor,
                    onValueChange = { sculptor = it },
                    label = { Text(tr("Скульптор", "Sculptor")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = estimatedValueStr,
                    onValueChange = { v -> estimatedValueStr = v.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                    label = { Text(tr("Орієнтовна вартість, ₴", "Estimated value, ₴")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(tr("Опис", "Description")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val catalogId = "MAN_${System.currentTimeMillis()}"
                    val estimatedValue = estimatedValueStr
                        .replace(',', '.')
                        .toDoubleOrNull() ?: 0.0
                    val product = Product(
                        catalogId = catalogId,
                        name = name.trim(),
                        series = series.trim(),
                        material = material.trim(),
                        nominal = nominal.trim(),
                        category = category.trim(),
                        quality = quality.trim(),
                        diameter = diameter.trim(),
                        weight = weight.trim(),
                        mintageAnnounced = mintageAnnounced.trim(),
                        mintageActual = mintageActual.trim(),
                        issueDate = issueDate.trim(),
                        artist = artist.trim(),
                        sculptor = sculptor.trim(),
                        photoPath = photoFront,
                        photoPathBack = photoBack,
                        estimatedValue = estimatedValue,
                        description = description.trim(),
                        isManual = true
                    )
                    onSave(product)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text(tr("Зберегти", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Скасувати", "Cancel"))
            }
        }
    )
}

@Composable
private fun ManualProductPhotoBox(
    photoPath: String,
    label: String,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(IOSDesign.CardCornerRadiusSmall))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPick() },
            contentAlignment = Alignment.Center
        ) {
            if (photoPath.isBlank()) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(36.dp)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(photoPath).build(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (photoPath.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text(tr("Видалити", "Remove"), fontSize = 11.sp)
            }
        } else {
            TextButton(onClick = onPick) {
                Text(tr("Вибрати", "Pick"), fontSize = 11.sp)
            }
        }
    }
}
