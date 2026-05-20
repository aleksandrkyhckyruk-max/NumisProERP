package com.numisproerp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocalAtm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavHostController
import com.numisproerp.R
import com.numisproerp.data.settings.AppTheme
import com.numisproerp.ui.i18n.tr
import com.numisproerp.ui.navigation.Screen
import com.numisproerp.ui.theme.AccentBlue
import com.numisproerp.ui.theme.AccentGreen
import com.numisproerp.ui.theme.AccentOrange
import com.numisproerp.ui.theme.AccentPurple
import com.numisproerp.ui.theme.AccentRed
import com.numisproerp.ui.theme.AccentTeal
import com.numisproerp.ui.theme.IOSDesign
import com.numisproerp.ui.theme.IOSIconChip
import com.numisproerp.ui.theme.LocalAppTheme
import com.numisproerp.ui.theme.LocalDashboardHeaderColor
import com.numisproerp.ui.theme.LocalDashboardHeaderFontSize
import com.numisproerp.ui.theme.LocalDashboardTitle
import com.numisproerp.ui.theme.LocalDashboardTitleColor
import com.numisproerp.ui.theme.LocalDashboardTitleOffsetX
import com.numisproerp.ui.theme.LocalDashboardTitleOffsetY
import com.numisproerp.ui.theme.LocalDashboardTitleSize
import com.numisproerp.ui.theme.LocalEmblemImagePath
import com.numisproerp.ui.theme.LocalEmblemOffsetX
import com.numisproerp.ui.theme.LocalEmblemOffsetY
import com.numisproerp.ui.theme.LocalEmblemSize
import com.numisproerp.ui.theme.LocalTileGridColumns
import com.numisproerp.ui.theme.LocalTileGridRows
import com.numisproerp.ui.theme.LocalTileLabelColor
import com.numisproerp.ui.theme.LocalTileLabelFontSize
import com.numisproerp.ui.theme.LocalTileLabels
import com.numisproerp.ui.theme.LocalTileOrder
import com.numisproerp.ui.i18n.LocalAppLanguage
import com.numisproerp.data.settings.AppLanguage
import com.numisproerp.ui.theme.LocalInfoCardBackgroundAlpha
import com.numisproerp.ui.theme.LocalInfoCardBackgroundColor
import com.numisproerp.ui.theme.LocalTileBackgroundAlpha
import com.numisproerp.ui.theme.LocalTileBackgroundColor
import com.numisproerp.ui.theme.LocalTextShadowConfig
import com.numisproerp.ui.theme.LocalTileIconSize
import com.numisproerp.ui.theme.LocalUserTilePhotos
import com.numisproerp.ui.theme.OlegPremiumTitleCoral
import com.numisproerp.ui.theme.parseHexColorOrNull
import com.numisproerp.ui.theme.toComposeShadow
import androidx.compose.ui.platform.LocalDensity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import java.io.File
import com.numisproerp.ui.viewmodel.DashboardViewModel
import com.numisproerp.ui.viewmodel.DashboardData
import com.numisproerp.ui.viewmodel.RecentTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        DashboardContent(
            data = dashboardData,
            onNavigate = { route -> navController.navigate(route) },
            onNavigateToReports = { navController.navigate(Screen.Reports.route) },
            onNavigateToDetails = { type, title ->
                navController.navigate("details/$type/$title")
            }
        )
    }
}

@Composable
fun DashboardContent(
    data: DashboardData,
    onNavigate: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDetails: (String, String) -> Unit
) {
    val currentDate = SimpleDateFormat("LLLL yyyy", Locale("uk")).format(Date())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DashboardHeader(currentDate = currentDate)
        }

        item {
            val balanceTitle = tr("Загальний баланс", "Total balance")
            StatsCardClickable(
                title = balanceTitle,
                value = String.format("%,.2f ₴", data.totalBalance),
                valueColor = if (data.totalBalance >= 0) AccentGreen else AccentRed,
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                onClick = { onNavigateToReports() }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val purchasesTitle = tr("Закупівлі", "Purchases")
                val profitTitle = tr("Прибуток", "Profit")
                MonthlyStatCardClickable(
                    modifier = Modifier.weight(1f),
                    title = purchasesTitle,
                    value = String.format("%,.2f ₴", data.monthlyPurchases),
                    icon = Icons.Outlined.LocalAtm,
                    iconColor = AccentOrange,
                    onClick = { onNavigateToDetails("purchases", purchasesTitle) }
                )
                MonthlyStatCardClickable(
                    modifier = Modifier.weight(1f),
                    title = profitTitle,
                    value = String.format("%,.2f ₴", data.monthlyProfit),
                    icon = Icons.Outlined.BarChart,
                    iconColor = AccentGreen,
                    onClick = { onNavigateToDetails("profit", profitTitle) }
                )
            }
        }

        item {
            SectionHeader(title = tr("Швидкий доступ", "Quick access"))
        }

        item {
            // Сітка швидкого доступу. Розмір сітки (cols×rows), порядок плиток
            // та назви беруться з Налаштувань → «Інтерфейс» → «Плитки головного
            // меню». Дозволяє користувачу повністю кастомізувати головний екран.
            QuickAccessGrid(onNavigate = onNavigate)
        }

        item {
            SectionHeader(title = tr("Останні операції", "Recent operations"))
        }

        items(data.recentTransactions) { transaction ->
            RecentTransactionItem(transaction = transaction)
        }
    }
}

@Composable
private fun DashboardHeader(currentDate: String) {
    val theme = LocalAppTheme.current
    val customEmblemPath = LocalEmblemImagePath.current
    val emblemSizeDp = LocalEmblemSize.current.dp
    val offsetX = LocalEmblemOffsetX.current.dp
    val offsetY = LocalEmblemOffsetY.current.dp
    val userTitle = LocalDashboardTitle.current
    val titleSizeSp = LocalDashboardTitleSize.current.sp
    val titleColorHex = LocalDashboardTitleColor.current
    val titleOffsetX = LocalDashboardTitleOffsetX.current.dp
    val titleOffsetY = LocalDashboardTitleOffsetY.current.dp
    if (theme == AppTheme.OLEG_SMILE_PREMIUM) {
        PremiumDashboardHeader(currentDate = currentDate)
        return
    }
    if (customEmblemPath.isNotBlank() ||
        theme == AppTheme.OLEG_SMILE || theme == AppTheme.OLEG_SMILE_V2 || theme == AppTheme.OLEG_SMILE_LIGHT
    ) {
        val defaultTitleText = when {
            theme == AppTheme.OLEG_SMILE_LIGHT -> "OlegSmile Light"
            theme == AppTheme.OLEG_SMILE || theme == AppTheme.OLEG_SMILE_V2 -> "OlegSmile"
            else -> "NumisProERP"
        }
        val titleText = if (userTitle.isNotBlank()) userTitle else defaultTitleText
        val resolvedTitleColor = parseHexColorOrNull(titleColorHex)
            ?: MaterialTheme.colorScheme.primary
        val titleShadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmblemImage(
                customPath = customEmblemPath,
                defaultRes = R.drawable.oleg_smile_emblem,
                contentDescription = titleText,
                size = emblemSizeDp,
                modifier = Modifier.offset(x = offsetX, y = offsetY)
            )
            Spacer(modifier = Modifier.width(12.dp))
            // Підпис "NumisProERP — облік та каталогізація" прибрано на прохання користувача:
            // він візуально шумить біля основного заголовка. Дата залишається.
            Column {
                Text(
                    text = titleText,
                    fontSize = titleSizeSp,
                    fontWeight = FontWeight.Bold,
                    color = resolvedTitleColor,
                    style = MaterialTheme.typography.bodyLarge.copy(shadow = titleShadow),
                    modifier = Modifier.offset(x = titleOffsetX, y = titleOffsetY)
                )
                Text(
                    text = currentDate,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = titleShadow),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    } else {
        val titleText = if (userTitle.isNotBlank()) userTitle else "NumisProERP"
        val resolvedTitleColor = parseHexColorOrNull(titleColorHex)
            ?: MaterialTheme.colorScheme.primary
        val titleShadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
        Column {
            Text(
                text = titleText,
                fontSize = titleSizeSp,
                fontWeight = FontWeight.Bold,
                color = resolvedTitleColor,
                style = MaterialTheme.typography.bodyLarge.copy(shadow = titleShadow),
                modifier = Modifier.offset(x = titleOffsetX, y = titleOffsetY)
            )
            Text(
                text = currentDate,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium.copy(shadow = titleShadow),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Емблема в шапці головного екрана. Якщо користувач вибрав власне фото
 * через Налаштування — рендеримо його; інакше — стандартний ресурс теми.
 * Радіус скруглення = половина розміру (як було жорстко 36dp при 72dp).
 */
@Composable
private fun EmblemImage(
    customPath: String,
    defaultRes: Int,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val corner = size / 2
    if (customPath.isNotBlank()) {
        val context = LocalContext.current
        val request = androidx.compose.runtime.remember(customPath) {
            ImageRequest.Builder(context).data(File(customPath)).build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(corner)),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(id = defaultRes),
            contentDescription = contentDescription,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
        )
    }
}

@Composable
private fun PremiumDashboardHeader(currentDate: String) {
    // Преміум 3D — окрема емблема "OLEG-SMILE Coin" з прозорим фоном.
    val emblemSizeDp = LocalEmblemSize.current.dp
    val offsetX = LocalEmblemOffsetX.current.dp
    val offsetY = LocalEmblemOffsetY.current.dp
    val userTitle = LocalDashboardTitle.current
    val titleSizeSp = LocalDashboardTitleSize.current.sp
    val titleColorHex = LocalDashboardTitleColor.current
    val titleOffsetX = LocalDashboardTitleOffsetX.current.dp
    val titleOffsetY = LocalDashboardTitleOffsetY.current.dp
    val userTitleColor = parseHexColorOrNull(titleColorHex)
    val titleOffsetModifier = Modifier.offset(x = titleOffsetX, y = titleOffsetY)
    val shadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.oleg_smile_coin_emblem),
            contentDescription = "OLEG-SMILE Coin",
            modifier = Modifier
                .size(emblemSizeDp)
                .offset(x = offsetX, y = offsetY)
        )
        Spacer(modifier = Modifier.width(12.dp))
        // Підпис "Облік та автоматизація" прибрано на прохання користувача — він
        // дублював інформацію, яка вже зрозуміла з контексту головного меню.
        Column {
            if (userTitle.isNotBlank()) {
                Text(
                    text = userTitle,
                    fontSize = titleSizeSp,
                    fontWeight = FontWeight.Bold,
                    color = userTitleColor ?: OlegPremiumTitleCoral,
                    style = MaterialTheme.typography.bodyLarge.copy(shadow = shadow),
                    modifier = titleOffsetModifier
                )
            } else {
                Row(
                    modifier = titleOffsetModifier,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "NumisPro",
                        fontSize = titleSizeSp,
                        fontWeight = FontWeight.Bold,
                        color = userTitleColor ?: OlegPremiumTitleCoral,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = shadow)
                    )
                    Text(
                        text = "ERP",
                        fontSize = titleSizeSp,
                        fontWeight = FontWeight.Bold,
                        color = userTitleColor ?: MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge.copy(shadow = shadow)
                    )
                }
            }
            Text(
                text = currentDate,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium.copy(shadow = shadow),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun StatsCardClickable(
    title: String,
    value: String,
    valueColor: Color,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    // "Загальний баланс" — велика інформаційна картка зверху Dashboard.
    // Беремо стандартний колір (primaryContainer) і застосовуємо лише
    // користувацький альфа-канал, бо повний override втратив би акцент-колір.
    val infoAlpha = LocalInfoCardBackgroundAlpha.current.coerceIn(0f, 1f)
    val resolvedColor = parseHexColorOrNull(LocalInfoCardBackgroundColor.current)
        ?.copy(alpha = infoAlpha)
        ?: backgroundColor.copy(alpha = infoAlpha)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = resolvedColor),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun MonthlyStatCardClickable(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    val infoAlpha = LocalInfoCardBackgroundAlpha.current.coerceIn(0f, 1f)
    val infoBg = parseHexColorOrNull(LocalInfoCardBackgroundColor.current)
        ?: MaterialTheme.colorScheme.surface
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = infoBg.copy(alpha = infoAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IOSIconChip(
                icon = icon,
                tint = iconColor
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    val sizeSp = LocalDashboardHeaderFontSize.current
    val customColor = parseHexColorOrNull(LocalDashboardHeaderColor.current)
    val shadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
    Text(
        text = title,
        fontSize = sizeSp.sp,
        fontWeight = FontWeight.SemiBold,
        color = customColor ?: MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.titleMedium.copy(shadow = shadow)
    )
}

/**
 * Сітка швидкого доступу 3×3 (9 плиток). Раніше показувався один ряд з 6 плиток,
 * але на вузьких екранах вони не влазили в одну лінію. Тепер плитки рендеряться
 * в три ряди по три, що дає рівне розміщення на будь-якій ширині та лишає
 * місце для додаткових ярликів (Звіти / Витрати / Документи) прямо на головному
 * робочому столі.
 *
 * Розмір плитки розраховується через [BoxWithConstraints]: ділимо доступну ширину
 * на 3 з урахуванням проміжків. Якщо стандартних 80dp на плитку вистачає — рендеримо
 * без override (щоб не ломати користувацький розмір іконки з Налаштувань). Інакше —
 * передаємо `tileSizeOverride`, що пропорційно зменшує плитку.
 */
@Composable
fun QuickAccessGrid(onNavigate: (String) -> Unit) {
    val columns = LocalTileGridColumns.current.coerceAtLeast(1)
    val rows = LocalTileGridRows.current.coerceAtLeast(1)
    val rawOrder = LocalTileOrder.current
    val labels = LocalTileLabels.current
    val language = LocalAppLanguage.current

    // Беремо лише cols*rows перших валідних id з порядку. Якщо id невідомий
    // (наприклад, користувач оновлювався з версії, де його не було), пропускаємо.
    val totalSlots = columns * rows
    val actions: List<QuickAccessAction> = rawOrder
        .mapNotNull { QuickAccessActionRegistry.findById(it) }
        .take(totalSlots)
    if (actions.isEmpty()) return

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val spacingDp = 8f
        val rawAvailable = maxWidth.value
        val perTile = ((rawAvailable - spacingDp * (columns - 1)) / columns).coerceAtLeast(40f)
        val defaultSize = com.numisproerp.data.settings.SettingsManager.TILE_BOX_SIZE_DP.toFloat()
        // Якщо плитка не вміщається в дефолтні 80dp — підрізаємо розмір. Інакше
        // лишаємо `null`, щоб працював користувацький слайдер розміру іконки.
        val tileSizeOverride: Float? = if (perTile >= defaultSize) null else perTile

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacingDp.dp)
        ) {
            actions.chunked(columns).forEach { rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    rowActions.forEach { action ->
                        val customLabel = labels[action.id]
                        val label = when {
                            !customLabel.isNullOrBlank() -> customLabel
                            language == AppLanguage.EN -> action.labelEn
                            else -> action.labelUa
                        }
                        QuickAccessButton(
                            tileId = action.id,
                            icon = action.icon,
                            tileRes = action.tileRes,
                            lightTileRes = action.lightTileRes,
                            premiumTileRes = action.premiumTileRes,
                            lightTint = action.lightTint,
                            label = label,
                            tileSizeOverride = tileSizeOverride,
                            onClick = { onNavigate(action.route) }
                        )
                    }
                    // Допaковуємо порожніми місцями, щоб останній неповний ряд
                    // лишався вирівняним до лівого краю (SpaceEvenly з меншою
                    // кількістю плиток інакше розтягне їх по всій ширині).
                    val missing = columns - rowActions.size
                    repeat(missing) {
                        Spacer(
                            modifier = Modifier.width(
                                (tileSizeOverride ?: defaultSize).dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessButton(
    modifier: Modifier = Modifier,
    /**
     * Стабільний ідентифікатор плитки. Має збігатися зі значеннями
     * [com.numisproerp.data.settings.SettingsManager.TILE_IDS].
     * Використовується для пошуку користувацького фото в [LocalUserTilePhotos]
     * та для збереження в SharedPreferences з ключем `tile_photo_<tileId>`.
     */
    tileId: String,
    icon: ImageVector,
    /**
     * Drawable для тем OLEG_SMILE/OLEG_SMILE_V2. Якщо `null` або `0` — рендериться
     * векторна іконка [icon] як fallback (поведінка така ж, як у DEFAULT-темі), щоб
     * можна було підставити плитку без власного PNG-арту (напр. "Документи").
     */
    tileRes: Int?,
    lightTileRes: Int? = null,
    /**
     * Нові 3D-іконки для преміум-теми ([AppTheme.OLEG_SMILE_PREMIUM]).
     * Коли `null` — fallback на векторну іконку без фону (iOS/Android-стиль),
     * щоб не змішувати стилі двох різних тем.
     */
    premiumTileRes: Int? = null,
    lightTint: Color = Color.Unspecified,
    label: String,
    /**
     * Перевизначення розміру плитки у dp (без одиниць) — використовується в [QuickAccessGrid]
     * для вузьких екранів, де стандартних 80dp на 3 плитки в ряд не влізає. `null` —
     * використовуються стандартні розміри.
     */
    tileSizeOverride: Float? = null,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    val userPhotoPath = LocalUserTilePhotos.current[tileId].orEmpty()
    val bgAlpha = LocalTileBackgroundAlpha.current.coerceIn(0f, 1f)
    val customBgColor = parseHexColorOrNull(LocalTileBackgroundColor.current)
    val baseIconSize = LocalTileIconSize.current
    // Якщо перевизначено розмір плитки (compact row) — масштабуємо іконку пропорційно
    // до базових 76/80 = 0.95, щоб обрамлення залишалося.
    val iconSizeDp = if (tileSizeOverride != null) {
        (tileSizeOverride * (baseIconSize.toFloat() / com.numisproerp.data.settings.SettingsManager.TILE_BOX_SIZE_DP.toFloat())).dp
    } else {
        baseIconSize.dp
    }
    if (theme == AppTheme.OLEG_SMILE_PREMIUM && userPhotoPath.isBlank()) {
        PremiumQuickAccessButton(
            modifier = modifier,
            icon = icon,
            premiumTileRes = premiumTileRes,
            label = label,
            tileSizeOverride = tileSizeOverride,
            onClick = onClick
        )
        return
    }
    // Фоновий заокруглений квадрат завжди МАЄ ФІКСОВАНИЙ розмір (80dp). Повзунок
    // розміру іконки масштабує лише саму іконку/фото всередині — рамка довкола
    // не змінюється. За замовчуванням іконка = 76dp (фон мінус 4dp = майже впритул).
    // Якщо користувач збільшить іконку понад 80dp — вона візуально перекриє фон,
    // але "рамка" (Box) залишається ним самим.
    val tileBoxSize = (tileSizeOverride ?: com.numisproerp.data.settings.SettingsManager.TILE_BOX_SIZE_DP.toFloat()).dp
    val tileCorner = 18.dp
    // Заокругленість іконки/фото масштабується пропорційно її розміру, відносно
    // дефолтних 76dp: при більшій іконці — більший radius, при меншій — менший.
    val iconCornerRadius = (iconSizeDp.value * 14f / 76f).dp
    val baseBgColor = customBgColor ?: MaterialTheme.colorScheme.surface
    // Колонка має вміщати як фон (80dp), так і потенційно більшу іконку (до 120dp),
    // плюс 4dp для повітря довкола. Підпис рендериться ОКРЕМО нижче.
    val columnWidth = maxOf(tileBoxSize.value, iconSizeDp.value).dp + 4.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onClick() }
            .padding(2.dp)
            .width(columnWidth)
    ) {
        // Контейнер висотою з найбільший елемент: завжди вміщає і фон, і іконку,
        // навіть якщо вона ширша за фон. Box сам по собі НЕ клiпує дітей.
        Box(
            modifier = Modifier
                .size(maxOf(tileBoxSize.value, iconSizeDp.value).dp),
            contentAlignment = Alignment.Center
        ) {
            // ФОН: завжди фіксований 80dp.
            Box(
                modifier = Modifier
                    .size(tileBoxSize)
                    .clip(RoundedCornerShape(tileCorner))
                    .background(baseBgColor.copy(alpha = bgAlpha))
            )
            // ІКОНКА: розмір з повзунка, центрована поверх фону.
            if (userPhotoPath.isNotBlank()) {
                UserTilePhoto(path = userPhotoPath, label = label, size = iconSizeDp, corner = iconCornerRadius)
            } else when (theme) {
                AppTheme.OLEG_SMILE, AppTheme.OLEG_SMILE_V2 -> {
                    if (tileRes != null && tileRes != 0) {
                        Image(
                            painter = painterResource(id = tileRes),
                            contentDescription = label,
                            modifier = Modifier
                                .size(iconSizeDp)
                                .clip(RoundedCornerShape(iconCornerRadius))
                        )
                    } else {
                        val fallbackTint = if (lightTint == Color.Unspecified) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            lightTint
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = fallbackTint,
                            modifier = Modifier.size(iconSizeDp * 0.55f)
                        )
                    }
                }
                AppTheme.OLEG_SMILE_PREMIUM -> {
                    if (premiumTileRes != null) {
                        Image(
                            painter = painterResource(id = premiumTileRes),
                            contentDescription = label,
                            modifier = Modifier.size(iconSizeDp)
                        )
                    } else {
                        // У преміум-темі без 3D-арту показуємо саму іконку (без додаткового
                        // чіпа всередині — фон зовнішнього квадрата вже виконує цю роль),
                        // щоб іконка займала майже весь розмір повзунка.
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(iconSizeDp * 0.85f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                AppTheme.OLEG_SMILE_LIGHT -> {
                    if (lightTileRes != null) {
                        Image(
                            painter = painterResource(id = lightTileRes),
                            contentDescription = label,
                            modifier = Modifier.size(iconSizeDp)
                        )
                    } else {
                        val resolvedTint = if (lightTint != Color.Unspecified) lightTint else MaterialTheme.colorScheme.primary
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            modifier = Modifier.size(iconSizeDp * 0.85f),
                            tint = resolvedTint
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.size(iconSizeDp * 0.85f),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        val labelSize = LocalTileLabelFontSize.current.sp
        val labelColor = parseHexColorOrNull(LocalTileLabelColor.current)
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        val labelShadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
        Text(
            text = label,
            fontSize = labelSize,
            color = labelColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            lineHeight = labelSize * 1.2f,
            style = MaterialTheme.typography.bodySmall.copy(shadow = labelShadow)
        )
    }
}

/**
 * Рендерить користувацьке фото для плитки з файлу. Використовується
 * у [QuickAccessButton], коли в [LocalUserTilePhotos] є запис
 * для відповідного `tileId`.
 */
@Composable
private fun UserTilePhoto(
    path: String,
    label: String,
    size: androidx.compose.ui.unit.Dp,
    corner: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    val request = androidx.compose.runtime.remember(path) {
        ImageRequest.Builder(context).data(File(path)).build()
    }
    AsyncImage(
        model = request,
        contentDescription = label,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner)),
        contentScale = ContentScale.Crop
    )
}

/**
 * Окрема версія кнопки для теми "Преміум 3D".
 * Без сірого фону-чипа, у стилі іконок Samsung / iOS — велика 3D-плитка з підписом.
 * Для розділів без преміум-іконки (напр. "Витрати") показуємо просту векторну іконку
 * без кольорового фону.
 */
@Composable
private fun PremiumQuickAccessButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    premiumTileRes: Int?,
    label: String,
    tileSizeOverride: Float? = null,
    onClick: () -> Unit
) {
    val columnWidth = (tileSizeOverride?.let { it + 2f } ?: 82f).dp
    val tileSize = (tileSizeOverride ?: 72f).dp
    val iconSize = ((tileSizeOverride ?: 72f) * 40f / 72f).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(columnWidth)
            .clickable { onClick() }
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        if (premiumTileRes != null) {
            Image(
                painter = painterResource(id = premiumTileRes),
                contentDescription = label,
                modifier = Modifier.size(tileSize)
            )
        } else {
            Box(
                modifier = Modifier.size(tileSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val labelSize = LocalTileLabelFontSize.current.sp
        val labelColor = parseHexColorOrNull(LocalTileLabelColor.current)
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        val labelShadow = LocalTextShadowConfig.current.toComposeShadow(LocalDensity.current.density)
        Text(
            text = label,
            fontSize = labelSize,
            color = labelColor,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            lineHeight = labelSize * 1.2f,
            style = MaterialTheme.typography.bodySmall.copy(shadow = labelShadow)
        )
    }
}

@Composable
fun RecentTransactionItem(transaction: RecentTransaction) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(transaction.date))
    val isPurchase = transaction.type == "Покупка"
    val infoAlpha = LocalInfoCardBackgroundAlpha.current.coerceIn(0f, 1f)
    val infoBg = parseHexColorOrNull(LocalInfoCardBackgroundColor.current)
        ?: MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(IOSDesign.CardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = infoBg.copy(alpha = infoAlpha)),
        elevation = CardDefaults.cardElevation(defaultElevation = IOSDesign.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IOSIconChip(
                icon = if (isPurchase) Icons.Outlined.LocalAtm else Icons.Filled.ShoppingCart,
                tint = if (isPurchase) AccentOrange else AccentGreen
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.productName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = "${transaction.counterpartyName} • $formattedDate",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = if (isPurchase) "-${String.format("%,.2f", transaction.amount)} ₴" else "+${String.format("%,.2f", transaction.amount)} ₴",
                fontWeight = FontWeight.Bold,
                color = if (isPurchase) AccentOrange else AccentGreen
            )
        }
    }
}
