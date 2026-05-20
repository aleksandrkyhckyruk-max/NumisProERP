package com.numisproerp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font as GFont
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font as LocalFont
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
        GFont(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Normal),
        GFont(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Medium),
        GFont(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.SemiBold),
        GFont(googleFont = gFont, fontProvider = googleFontsProvider, weight = FontWeight.Bold)
    )
}

/**
 * Будує [FontFamily] для варіативного шрифту, запакованого в `res/font/`.
 * Один файл TTF підтримує розряд wght (400/500/600/700) через
 * [FontVariation.weight]. Працює повністю офлайн — не вимагає Google Play Services.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variableFontFamily(resId: Int): FontFamily {
    return FontFamily(
        LocalFont(resId, weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400))),
        LocalFont(resId, weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500))),
        LocalFont(resId, weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600))),
        LocalFont(resId, weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)))
    )
}

/**
 * Будує [FontFamily] з окремих TTF файлів на Regular та Bold. Для фонтів,
 * у яких немає варіативного файлу (як Poppins).
 */
private fun staticFontFamily(regularResId: Int, boldResId: Int): FontFamily {
    return FontFamily(
        LocalFont(regularResId, weight = FontWeight.Normal),
        LocalFont(regularResId, weight = FontWeight.Medium),
        LocalFont(boldResId, weight = FontWeight.SemiBold),
        LocalFont(boldResId, weight = FontWeight.Bold)
    )
}

/**
 * Описує один варіант шрифту, який користувач може обрати в Налаштуваннях.
 * `key` — стабільний ключ для SharedPreferences; `familyName` — рівно як у
 * каталозі Google Fonts (з пробілами та регістром).
 */
data class FontOption(val key: String, val displayName: String, val familyName: String)

/**
 * Офлайн-шрифти, бандлаться разом із застосунком (файли в `res/font/`).
 * Не вимагають Google Play Services / інтернету — працюють однаково
 * на будь-якому пристрої. Користувач бачить їх як секцію "Офлайн" у виборі шрифту.
 */
val OfflineFontOptions: List<FontOption> = listOf(
    FontOption("offline-poppins", "Poppins (офлайн)", "Poppins"),
    FontOption("offline-montserrat", "Montserrat (офлайн)", "Montserrat"),
    FontOption("offline-open-sans", "Open Sans (офлайн)", "Open Sans"),
    FontOption("offline-nunito", "Nunito (офлайн)", "Nunito"),
    FontOption("offline-lora", "Lora (офлайн)", "Lora"),
    FontOption("offline-playfair", "Playfair Display (офлайн)", "Playfair Display"),
)

/**
 * Google Fonts раніше були списком з 8 онлайн-шрифтів, але на практиці вони
 * вимагають Google Play Services + інтернет — на пристроях без сервісів Google
 * (або без сертифіката від GMS) шрифт ніколи не завантажується і просто
 * віддає системний фолбек-шрифт, що користувач сприймав як «нічого не робиться».
 *
 * Тому в UI вибору шрифту ми залишили лише `OfflineFontOptions` (TTF у `res/font/`).
 * Сам список тут лишається порожнім, щоб не зламати збережені користувацькі
 * ключі: `fontFamilyOf("roboto"|...)` нижче все одно поверне коректний
 * `FontFamily.Default`, тобто додаток не впаде, якщо у SharedPreferences
 * лежить старий онлайн-ключ.
 */
val GoogleFontOptions: List<FontOption> = emptyList()

/**
 * Перетворює ключ шрифту з налаштувань на [FontFamily] для Compose.
 * Порядок перевірки:
 *  1) `offline-*` — бандл шрифт з `res/font/` (працює без інтернету).
 *  2) системні сімейства (system / sans-serif / serif / monospace).
 *  3) Google Fonts з [GoogleFontOptions] (потребують інтернет і Google Play Services).
 */
fun fontFamilyOf(key: String?): FontFamily {
    return when (key) {
        "offline-poppins" -> staticFontFamily(R.font.poppins_regular, R.font.poppins_bold)
        "offline-montserrat" -> variableFontFamily(R.font.montserrat_variable)
        "offline-open-sans" -> variableFontFamily(R.font.open_sans_variable)
        "offline-nunito" -> variableFontFamily(R.font.nunito_variable)
        "offline-lora" -> variableFontFamily(R.font.lora_variable)
        "offline-playfair" -> variableFontFamily(R.font.playfair_display_variable)
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
