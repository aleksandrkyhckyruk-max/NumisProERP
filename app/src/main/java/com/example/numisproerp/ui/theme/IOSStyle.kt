package com.numisproerp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Єдина шкала розмірів іконок та контейнерів — задана централізовано,
 * щоб усі значки на всіх екранах були однакового розміру (симетричні).
 *
 * Дотримуємось iOS-стилю: чіпи квадратні з закругленням ~12dp,
 * іконки всередині — фіксованого розміру.
 */
object IOSDesign {
    // Розмір самої векторної іконки (тільки сам гліф)
    val IconSizeSmall: Dp = 18.dp
    val IconSizeMedium: Dp = 22.dp
    val IconSizeLarge: Dp = 26.dp

    // Розмір контейнера-чіпу під іконку (квадрат 1:1)
    val IconChipSmall: Dp = 32.dp
    val IconChipMedium: Dp = 40.dp
    val IconChipLarge: Dp = 56.dp

    // Радіуси скруглення (iOS continuous corner)
    val CardCornerRadius: Dp = 20.dp
    val CardCornerRadiusSmall: Dp = 14.dp
    val ChipCornerRadius: Dp = 12.dp
    val ButtonCornerRadius: Dp = 14.dp

    // Тіні: на iOS — дуже легкі, майже плоский дизайн
    val CardElevation: Dp = 0.dp
    val CardElevationRaised: Dp = 1.dp

    // Внутрішні відступи
    val CardPadding: Dp = 16.dp
    val ScreenPadding: Dp = 16.dp
}

/**
 * Симетричний контейнер під іконку у стилі iOS — квадратний чіп з кольоровою заливкою.
 * Іконка та контейнер мають фіксовані розміри, незалежно від місця використання,
 * тому усі іконки виглядають однакового розміру по всьому додатку.
 */
@Composable
fun IOSIconChip(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    chipSize: Dp = IOSDesign.IconChipMedium,
    iconSize: Dp = IOSDesign.IconSizeMedium,
    backgroundAlpha: Float = 0.15f,
    cornerRadius: Dp = IOSDesign.ChipCornerRadius,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(chipSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
