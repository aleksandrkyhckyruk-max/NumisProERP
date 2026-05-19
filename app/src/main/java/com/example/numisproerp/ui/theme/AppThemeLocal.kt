package com.numisproerp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import com.numisproerp.data.settings.AppTheme
import com.numisproerp.data.settings.SettingsManager

/**
 * Перетворює користувацький вибір кольору фону бару + повзунок «світліший/темніший»
 * у фінальний непрозорий [Color] для застосування у `TopAppBar`/`NavigationBar`/`ModalDrawerSheet`.
 *
 * Логіка:
 *  - Якщо `hex` порожній — використовуємо переданий `fallback` (стандартний колір з теми);
 *  - Якщо `hex` валідний — парсимо як основу;
 *  - Далі домішуємо білий (якщо `brightness > 0`) або чорний (якщо `brightness < 0`) пропорційно до
 *    значення повзунка (-0.5..+0.5). Прозорість завжди 100% — бари не просвічуються.
 *
 * За `brightness == 0` та порожнім `hex` повертається `fallback` як є — старий вигляд без налаштувань.
 */
fun resolveBarColor(
    hex: String,
    brightness: Float,
    fallback: Color,
    opacity: Float = SettingsManager.DEFAULT_BAR_OPACITY
): Color {
    val base: Color = if (hex.isBlank()) {
        fallback
    } else {
        runCatching { Color(android.graphics.Color.parseColor("#$hex")) }.getOrDefault(fallback)
    }
    val b = brightness.coerceIn(SettingsManager.MIN_BAR_BRIGHTNESS, SettingsManager.MAX_BAR_BRIGHTNESS)
    val alpha = opacity.coerceIn(SettingsManager.MIN_BAR_OPACITY, SettingsManager.MAX_BAR_OPACITY)
    if (b == 0f) return base.copy(alpha = alpha)
    // Перетворюємо -0.5..+0.5 у [0..1] силу домішування з білим/чорним.
    val mixAmount = (kotlin.math.abs(b) * 2f).coerceIn(0f, 1f)
    val target = if (b > 0f) Color.White else Color.Black
    val r = base.red + (target.red - base.red) * mixAmount
    val g = base.green + (target.green - base.green) * mixAmount
    val bl = base.blue + (target.blue - base.blue) * mixAmount
    return Color(red = r, green = g, blue = bl, alpha = alpha)
}

/**
 * Поточна активна тема — доступ із будь-якого Composable через `LocalAppTheme.current`.
 * Дозволяє екранам реагувати на тему (наприклад, dashboard показує
 * 3D-плитки тільки коли активна OlegSmile).
 */
val LocalAppTheme = compositionLocalOf { AppTheme.DEFAULT }

/**
 * Користувацькі фото для плиток швидкого доступу головного меню: tileId -> file path.
 * Якщо для конкретного `tileId` є запис — `QuickAccessButton` рендерить це фото
 * замість тематичної іконки. Заповнюється в `NumisProERPTheme` із SettingsManager.
 */
val LocalUserTilePhotos = compositionLocalOf<Map<String, String>> { emptyMap() }

/**
 * Глобальна прозорість фону плиток швидкого доступу (0..1).
 * 0 — повністю прозорий фон (видно фон додатку крізь плитку),
 * 1 — непрозорий `surface`-фон як на iOS.
 * Заповнюється в `NumisProERPTheme` із SettingsManager.
 */
val LocalTileBackgroundAlpha = compositionLocalOf { SettingsManager.DEFAULT_TILE_BG_ALPHA }

/**
 * Користувацький розмір значка/фото на плитці швидкого доступу у dp.
 * За замовчуванням 68dp (попередній жорсткий розмір).
 */
val LocalTileIconSize = compositionLocalOf { SettingsManager.DEFAULT_TILE_ICON_SIZE }

/**
 * Користувацький колір фону плиток (hex без `#`). Порожній рядок —
 * використовується `colorScheme.surface` як раніше.
 */
val LocalTileBackgroundColor = compositionLocalOf { "" }

/**
 * Шлях до користувацької емблеми головного екрана. Порожній рядок —
 * показуємо стандартну емблему теми (OlegSmile-тільки) або нічого.
 */
val LocalEmblemImagePath = compositionLocalOf { "" }

/**
 * Розмір емблеми над інформаційними картками головного екрана у dp.
 */
val LocalEmblemSize = compositionLocalOf { SettingsManager.DEFAULT_EMBLEM_SIZE }

/**
 * Зсув емблеми по горизонталі у dp. Дозволяє користувачу переміщати емблему
 * праворуч/ліворуч у шапці головного екрана; за замовчуванням 0 (без зсуву).
 */
val LocalEmblemOffsetX = compositionLocalOf { 0 }

/**
 * Зсув емблеми по вертикалі у dp.
 */
val LocalEmblemOffsetY = compositionLocalOf { 0 }

/**
 * Користувацький текст заголовка головного екрана. Порожній рядок — використовується
 * заголовок теми за замовчуванням ("OlegSmile" / "NumisProERP").
 */
val LocalDashboardTitle = compositionLocalOf { "" }

/**
 * Розмір шрифту заголовка головного екрана у sp.
 */
val LocalDashboardTitleSize = compositionLocalOf { SettingsManager.DEFAULT_DASHBOARD_TITLE_SIZE }

/**
 * Колір тексту заголовка головного екрана у hex без `#`. Порожній рядок —
 * використовується стандартний `colorScheme.primary` з теми.
 */
val LocalDashboardTitleColor = compositionLocalOf { "" }

/**
 * Зсув заголовка головного екрана по горизонталі у dp. Дозволяє "перетягувати"
 * напис вліво/вправо у шапці, аналогічно до емблеми.
 */
val LocalDashboardTitleOffsetX = compositionLocalOf { 0 }

/**
 * Зсув заголовка головного екрана по вертикалі у dp.
 */
val LocalDashboardTitleOffsetY = compositionLocalOf { 0 }

/**
 * Розмір шрифту заголовків секцій Dashboard ("Швидкий доступ", "Останні операції") у sp.
 */
val LocalDashboardHeaderFontSize = compositionLocalOf { SettingsManager.DEFAULT_DASHBOARD_HEADER_FONT_SIZE }

/**
 * Колір заголовків секцій Dashboard у hex без `#`. Порожній рядок — використовується
 * стандартний `onBackground` з теми.
 */
val LocalDashboardHeaderColor = compositionLocalOf { "" }

/**
 * Розмір шрифту підписів плиток швидкого доступу (Закупка, Склад тощо) у sp.
 */
val LocalTileLabelFontSize = compositionLocalOf { SettingsManager.DEFAULT_TILE_LABEL_FONT_SIZE }

/**
 * Колір підписів плиток у hex без `#`. Порожній рядок — використовується стандартний
 * `onSurface` з теми.
 */
val LocalTileLabelColor = compositionLocalOf { "" }

/**
 * Користувацький колір фону інформаційних карток Dashboard (баланс, місячні стати,
 * останні операції). Порожній рядок — використовується `colorScheme.surface` як раніше.
 */
val LocalInfoCardBackgroundColor = compositionLocalOf { "" }

/**
 * Прозорість фону інформаційних карток Dashboard (0..1).
 * 1 — непрозорий фон (старий вигляд).
 */
val LocalInfoCardBackgroundAlpha = compositionLocalOf { SettingsManager.DEFAULT_INFO_CARD_BG_ALPHA }

/**
 * Користувацький колір фону верхнього бару (`TopAppBar`) у hex без `#`.
 * Порожній рядок — використовується стандартний колір з теми
 * (`colorScheme.primaryContainer`/`surface`), як було раніше.
 */
val LocalTopBarColor = compositionLocalOf { "" }

/**
 * «Освітлення/затемнення» верхнього бару (-0.5..+0.5). 0 — без змін.
 * Не задає прозорість — бар лишається повністю непрозорим, лише
 * базовий колір зсувається у білий/чорний.
 */
val LocalTopBarBrightness = compositionLocalOf { 0f }

/**
 * Користувацький колір фону нижнього бару (`NavigationBar`) у hex без `#`.
 * Порожній рядок — використовується стандартний колір з теми.
 */
val LocalBottomBarColor = compositionLocalOf { "" }

/**
 * «Освітлення/затемнення» нижнього бару (-0.5..+0.5). 0 — без змін.
 */
val LocalBottomBarBrightness = compositionLocalOf { 0f }

/**
 * Користувацький колір фону бічного меню (`ModalNavigationDrawer`) у hex без `#`.
 * Порожній рядок — використовується стандартний колір з теми.
 */
val LocalDrawerColor = compositionLocalOf { "" }

/**
 * «Освітлення/затемнення» бічного меню (-0.5..+0.5). 0 — без змін.
 */
val LocalDrawerBrightness = compositionLocalOf { 0f }

/**
 * Прозорість фону верхнього бару (0.2..1.0). 1.0 — повністю непрозорий (старий
 * вигляд), значення нижче — бар стає напівпрозорим. Заповнюється у
 * `NumisProERPTheme` із SettingsManager.
 */
val LocalTopBarOpacity = compositionLocalOf { SettingsManager.DEFAULT_BAR_OPACITY }

/**
 * Прозорість фону нижнього бару (0.2..1.0). 1.0 — повністю непрозорий.
 */
val LocalBottomBarOpacity = compositionLocalOf { SettingsManager.DEFAULT_BAR_OPACITY }

/**
 * Прозорість фону бічного меню (0.2..1.0). 1.0 — повністю непрозорий.
 */
val LocalDrawerOpacity = compositionLocalOf { SettingsManager.DEFAULT_BAR_OPACITY }
