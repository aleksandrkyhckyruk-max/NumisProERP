package com.numisproerp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.numisproerp.R

/**
 * Провайдер Google Fonts: завантажує шрифти через Google Play Services
 * (без необхідності бандлити TTF у застосунок). Сертифікати беруться з
 * стандартного масиву `com_google_android_gms_fonts_certs`, який поставляється
 * разом із залежністю `androidx.compose.ui:ui-text-google-fonts`.
 */
private val googleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Будує [FontFamily] для одного шрифту з Google Fonts з типовими вагами.
 * Завантаження відбувається лінько при першому використанні.
 */
private fun gFontFamily(name: String): FontFamily {
    val gFont = GoogleFont(name)
    return FontFamily(
        Font(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
        Font(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
        Font(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
        Font(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Bold)
    )
}

/**
 * Описує один варіант шрифту, який користувач може обрати в Налаштуваннях.
 * `key` — стабільний ключ для SharedPreferences; `familyName` — рівно як у
 * каталозі Google Fonts (з пробілами та регістром).
 */
data class FontOption(val key: String, val displayName: String, val familyName: String)

/**
 * Google Fonts, які користувач може обрати, разом із системними сімействами.
 * Порядок відповідає тому, як вони показуються у [FontsDialog].
 */
val GoogleFontOptions: List<FontOption> = listOf(
    FontOption("roboto", "Roboto", "Roboto"),
    FontOption("montserrat", "Montserrat", "Montserrat"),
    FontOption("inter", "Inter", "Inter"),
    FontOption("lora", "Lora", "Lora"),
    FontOption("playfair", "Playfair Display", "Playfair Display"),
    FontOption("poppins", "Poppins", "Poppins"),
    FontOption("nunito", "Nunito", "Nunito"),
    FontOption("open-sans", "Open Sans", "Open Sans"),
)

/**
 * Перетворює ключ шрифту з налаштувань на [FontFamily] для Compose.
 * Підтримуються системні сімейства (system / sans-serif / serif / monospace)
 * та Google Fonts з [GoogleFontOptions].
 */
fun fontFamilyOf(key: String?): FontFamily {
    return when (key) {
        "serif" -> FontFamily.Serif
        "sans-serif" -> FontFamily.SansSerif
        "monospace" -> FontFamily.Monospace
        null, "", "system" -> FontFamily.Default
        else -> {
            val option = GoogleFontOptions.firstOrNull { it.key == key }
            if (option != null) gFontFamily(option.familyName) else FontFamily.Default
        }
    }
}

/**
 * Будує [Typography] з заданим [family] (з налаштувань шрифту) та опціональним
 * [textColor] (якщо користувач у Налаштуваннях обрав конкретний колір тексту,
 * він застосовується до кожного TextStyle).
 */
fun buildTypography(
    family: FontFamily = FontFamily.Default,
    textColor: Color = Color.Unspecified
): Typography {
    fun TextStyle.skin(): TextStyle = this.copy(fontFamily = family, color = textColor)
    return Typography(
        displayLarge = Typography.displayLarge.skin(),
        displayMedium = Typography.displayMedium.skin(),
        displaySmall = Typography.displaySmall.skin(),
        headlineLarge = Typography.headlineLarge.skin(),
        headlineMedium = Typography.headlineMedium.skin(),
        headlineSmall = Typography.headlineSmall.skin(),
        titleLarge = Typography.titleLarge.skin(),
        titleMedium = Typography.titleMedium.skin(),
        titleSmall = Typography.titleSmall.skin(),
        bodyLarge = Typography.bodyLarge.skin(),
        bodyMedium = Typography.bodyMedium.skin(),
        bodySmall = Typography.bodySmall.skin(),
        labelLarge = Typography.labelLarge.skin(),
        labelMedium = Typography.labelMedium.skin(),
        labelSmall = Typography.labelSmall.skin()
    )
}

// iOS-стиль типографіки (приблизно SF Pro): великі заголовки жирні,
// тіло — звичайної ваги, компактна шкала розмірів.
val Typography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = 0.37.sp,
        fontFamily = FontFamily.Default
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = 0.36.sp,
        fontFamily = FontFamily.Default
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = 0.35.sp,
        fontFamily = FontFamily.Default
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.38.sp,
        fontFamily = FontFamily.Default
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        letterSpacing = (-0.43).sp,
        fontFamily = FontFamily.Default
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = (-0.23).sp,
        fontFamily = FontFamily.Default
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFamily = FontFamily.Default
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        letterSpacing = (-0.43).sp,
        fontFamily = FontFamily.Default
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        letterSpacing = (-0.23).sp,
        fontFamily = FontFamily.Default
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        fontFamily = FontFamily.Default
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        letterSpacing = (-0.23).sp,
        fontFamily = FontFamily.Default
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        fontFamily = FontFamily.Default
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        fontFamily = FontFamily.Default
    )
)
