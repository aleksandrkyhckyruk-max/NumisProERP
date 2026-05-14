package com.numisproerp.ui.theme

import androidx.compose.runtime.compositionLocalOf
import com.numisproerp.data.settings.AppTheme

/**
 * Поточна активна тема — доступ із будь-якого Composable через `LocalAppTheme.current`.
 * Дозволяє екранам реагувати на тему (наприклад, dashboard показує
 * 3D-плитки тільки коли активна OlegSmile).
 */
val LocalAppTheme = compositionLocalOf { AppTheme.DEFAULT }
