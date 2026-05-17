package com.numisproerp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.numisproerp.data.settings.AppTheme
import com.numisproerp.data.settings.SettingsManager

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
 * Користувацький колір фону інформаційних карток Dashboard (баланс, місячні стати,
 * останні операції). Порожній рядок — використовується `colorScheme.surface` як раніше.
 */
val LocalInfoCardBackgroundColor = compositionLocalOf { "" }

/**
 * Прозорість фону інформаційних карток Dashboard (0..1).
 * 1 — непрозорий фон (старий вигляд).
 */
val LocalInfoCardBackgroundAlpha = compositionLocalOf { SettingsManager.DEFAULT_INFO_CARD_BG_ALPHA }
