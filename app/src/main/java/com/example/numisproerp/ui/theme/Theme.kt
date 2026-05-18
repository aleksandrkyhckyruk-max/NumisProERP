package com.numisproerp.ui.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.numisproerp.R
import com.numisproerp.data.settings.AppTheme
import com.numisproerp.data.settings.SettingsManager
import java.io.File

private val DarkColorScheme = darkColorScheme(
    primary = IOSBlueDark,
    onPrimary = DarkOnPrimary,
    primaryContainer = Color(0xFF0A2540),
    onPrimaryContainer = IOSBlueDark,
    secondary = IOSTeal,
    onSecondary = DarkOnPrimary,
    tertiary = IOSPurple,
    onTertiary = DarkOnPrimary,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = IOSRedDark,
    onError = DarkOnPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = IOSBlue,
    onPrimary = LightOnPrimary,
    primaryContainer = IOSBlueContainer,
    onPrimaryContainer = Color(0xFF003C7A),
    secondary = IOSTeal,
    onSecondary = LightOnPrimary,
    tertiary = IOSPurple,
    onTertiary = LightOnPrimary,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = IOSRed,
    onError = LightOnPrimary
)

private val OlegSmileColorScheme = darkColorScheme(
    primary = OlegGold,
    onPrimary = Color(0xFF0B0B0D),
    primaryContainer = OlegPrimaryContainer,
    onPrimaryContainer = OlegGoldBright,
    secondary = OlegGoldBright,
    onSecondary = Color(0xFF0B0B0D),
    tertiary = OlegOrange,
    onTertiary = Color(0xFF0B0B0D),
    background = OlegBackground,
    onBackground = OlegOnSurface,
    surface = OlegSurface,
    onSurface = OlegOnSurface,
    surfaceVariant = OlegSurfaceVariant,
    onSurfaceVariant = OlegOnSurfaceVariant,
    error = OlegRed,
    onError = Color(0xFF0B0B0D)
)

private val OlegSmileV2ColorScheme = darkColorScheme(
    primary = OlegGold,
    onPrimary = Color(0xFF121212),
    primaryContainer = OlegV2PrimaryContainer,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFBBBBBB),
    onSecondary = Color(0xFF121212),
    tertiary = Color(0xFF999999),
    onTertiary = Color(0xFF121212),
    background = OlegV2Background,
    onBackground = OlegV2OnSurface,
    surface = OlegV2Surface,
    onSurface = OlegV2OnSurface,
    surfaceVariant = OlegV2SurfaceVariant,
    onSurfaceVariant = OlegV2OnSurfaceVariant,
    error = OlegRed,
    onError = Color(0xFF121212)
)

private val OlegSmileLightColorScheme = lightColorScheme(
    primary = OlegLightGold,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = OlegLightPrimaryContainer,
    onPrimaryContainer = OlegLightOnPrimaryContainer,
    secondary = OlegGold,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = OlegOrange,
    onTertiary = Color(0xFFFFFFFF),
    background = OlegLightBackground,
    onBackground = OlegLightOnSurface,
    surface = OlegLightSurface,
    onSurface = OlegLightOnSurface,
    surfaceVariant = OlegLightSurfaceVariant,
    onSurfaceVariant = OlegLightOnSurfaceVariant,
    error = IOSRed,
    onError = Color(0xFFFFFFFF)
)

private val OlegSmilePremiumColorScheme = lightColorScheme(
    primary = OlegPremiumGold,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = OlegPremiumPrimaryContainer,
    onPrimaryContainer = OlegPremiumOnPrimaryContainer,
    secondary = OlegGold,
    onSecondary = Color(0xFFFFFFFF),
    tertiary = OlegPremiumTitleCoral,
    onTertiary = Color(0xFFFFFFFF),
    background = OlegPremiumBackground,
    onBackground = OlegPremiumOnSurface,
    surface = OlegPremiumSurface,
    onSurface = OlegPremiumOnSurface,
    surfaceVariant = OlegPremiumSurfaceVariant,
    onSurfaceVariant = OlegPremiumOnSurfaceVariant,
    error = IOSRed,
    onError = Color(0xFFFFFFFF)
)

private val OceanGlassColorScheme = darkColorScheme(
    primary = OceanMint,
    onPrimary = Color(0xFF062017),
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanMint,
    secondary = OceanCyan,
    onSecondary = Color(0xFF052431),
    tertiary = OceanOrange,
    onTertiary = Color(0xFF241000),
    background = OceanBackground,
    onBackground = OceanOnSurface,
    surface = OceanSurface,
    onSurface = OceanOnSurface,
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = OceanOnSurfaceVariant,
    error = OceanRed,
    onError = Color(0xFF1A0606)
)

private val IOSShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NumisProERPTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    fontFamilyKey: String = SettingsManager.DEFAULT_FONT_FAMILY,
    fontSizeSp: Int = SettingsManager.DEFAULT_FONT_SIZE,
    fontColorHex: String = "",
    backgroundImagePath: String = "",
    tilePhotoPaths: Map<String, String> = emptyMap(),
    tileBackgroundAlpha: Float = SettingsManager.DEFAULT_TILE_BG_ALPHA,
    tileIconSize: Int = SettingsManager.DEFAULT_TILE_ICON_SIZE,
    tileBackgroundColorHex: String = "",
    emblemImagePath: String = "",
    emblemSize: Int = SettingsManager.DEFAULT_EMBLEM_SIZE,
    infoCardBackgroundColorHex: String = "",
    infoCardBackgroundAlpha: Float = SettingsManager.DEFAULT_INFO_CARD_BG_ALPHA,
    topBarColorHex: String = "",
    topBarBrightness: Float = 0f,
    bottomBarColorHex: String = "",
    bottomBarBrightness: Float = 0f,
    drawerColorHex: String = "",
    drawerBrightness: Float = 0f,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val baseColorScheme = when (appTheme) {
        AppTheme.OLEG_SMILE -> OlegSmileColorScheme
        AppTheme.OLEG_SMILE_V2 -> OlegSmileV2ColorScheme
        AppTheme.OLEG_SMILE_LIGHT -> OlegSmileLightColorScheme
        AppTheme.OLEG_SMILE_PREMIUM -> OlegSmilePremiumColorScheme
        AppTheme.OCEAN_GLASS -> OceanGlassColorScheme
        AppTheme.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    // Якщо користувач вибрав фоновий малюнок — робимо `colorScheme.background`
    // прозорим, щоб усі `Surface(color = colorScheme.background)` та
    // `Modifier.background(colorScheme.background)` у MainActivity і екранах
    // не закривали фото. Картки/панелі мають свої `surface`/`primaryContainer`
    // кольори і залишаються непрозорими.
    val colorScheme = if (backgroundImagePath.isNotBlank()) {
        baseColorScheme.copy(background = Color.Transparent)
    } else {
        baseColorScheme
    }

    // Шрифт: сімейство + колір + масштаб.
    val customFontFamily = remember(fontFamilyKey) { fontFamilyOf(fontFamilyKey) }
    val customFontColor: Color = remember(fontColorHex) {
        if (fontColorHex.isBlank()) Color.Unspecified
        else runCatching { Color(android.graphics.Color.parseColor("#$fontColorHex")) }
            .getOrDefault(Color.Unspecified)
    }
    val typography = remember(customFontFamily, customFontColor) {
        buildTypography(family = customFontFamily, textColor = customFontColor)
    }

    // Масштаб розміру шрифту: користувацький вибір в sp / базове 14 sp.
    // Пропускаємо через LocalDensity — всі .sp в UI перераховуються.
    val baseDensity = LocalDensity.current
    val userScale = fontSizeSp.toFloat() / SettingsManager.DEFAULT_FONT_SIZE.toFloat()
    val scaledDensity = remember(baseDensity, userScale) {
        Density(baseDensity.density, baseDensity.fontScale * userScale)
    }

    CompositionLocalProvider(
        LocalAppTheme provides appTheme,
        LocalDensity provides scaledDensity,
        LocalUserTilePhotos provides tilePhotoPaths,
        LocalTileBackgroundAlpha provides tileBackgroundAlpha,
        LocalTileIconSize provides tileIconSize,
        LocalTileBackgroundColor provides tileBackgroundColorHex,
        LocalEmblemImagePath provides emblemImagePath,
        LocalEmblemSize provides emblemSize,
        LocalInfoCardBackgroundColor provides infoCardBackgroundColorHex,
        LocalInfoCardBackgroundAlpha provides infoCardBackgroundAlpha,
        LocalTopBarColor provides topBarColorHex,
        LocalTopBarBrightness provides topBarBrightness,
        LocalBottomBarColor provides bottomBarColorHex,
        LocalBottomBarBrightness provides bottomBarBrightness,
        LocalDrawerColor provides drawerColorHex,
        LocalDrawerBrightness provides drawerBrightness
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = IOSShapes
        ) {
            // Для Text() без явного кольору пропонуємо LocalContentColor.
            val contentWithFontColor: @Composable () -> Unit = if (customFontColor != Color.Unspecified) {
                { CompositionLocalProvider(LocalContentColor provides customFontColor) { content() } }
            } else content

            // Користувацький фоновий малюнок (якщо вибрано) — малюємо поверх тематичного фону.
            // Картки і панелі залишаться видимими (вони мають окремий `surface`/`primaryContainer`),
            // а основний фон сторінок (Surface і `Modifier.background(colorScheme.background)`) тепер
            // прозорий (див. вище), тож фото буде проглядати в порожніх місцях.
            val userBgOverlay: @Composable () -> Unit = userBgOverlay@{
                if (backgroundImagePath.isBlank()) return@userBgOverlay
                val context = androidx.compose.ui.platform.LocalContext.current
                val request = remember(backgroundImagePath) {
                    ImageRequest.Builder(context).data(File(backgroundImagePath)).build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            when (appTheme) {
                AppTheme.OLEG_SMILE -> {
                    Box(modifier = Modifier.fillMaxSize().background(OlegBackgroundSolid)) {
                        Image(
                            painter = painterResource(id = R.drawable.oleg_smile_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xAA000000),
                                            Color(0x99000000),
                                            Color(0xAA000000)
                                        )
                                    )
                                )
                        )
                        userBgOverlay()
                        contentWithFontColor()
                    }
                }
                AppTheme.OLEG_SMILE_V2 -> {
                    Box(modifier = Modifier.fillMaxSize().background(OlegV2BackgroundSolid)) {
                        Image(
                            painter = painterResource(id = R.drawable.oleg_smile_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.15f
                        )
                        userBgOverlay()
                        contentWithFontColor()
                    }
                }
                AppTheme.OLEG_SMILE_LIGHT -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OlegLightBackground)
                    ) {
                        userBgOverlay()
                        contentWithFontColor()
                    }
                }
                AppTheme.OLEG_SMILE_PREMIUM -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(OlegPremiumBackground)
                    ) {
                        userBgOverlay()
                        contentWithFontColor()
                    }
                }
                AppTheme.OCEAN_GLASS -> {
                    // Глибокий синій вертикальний градієнт + radial sheen зверху.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        OceanBackgroundTop,
                                        OceanBackgroundMid,
                                        OceanBackgroundBottom
                                    )
                                )
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0x224DD0E1),
                                            Color(0x00000000)
                                        ),
                                        radius = 900f
                                    )
                                )
                        )
                        userBgOverlay()
                        contentWithFontColor()
                    }
                }
                else -> {
                    if (backgroundImagePath.isNotBlank()) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            userBgOverlay()
                            contentWithFontColor()
                        }
                    } else {
                        contentWithFontColor()
                    }
                }
            }
        }
    }
}
