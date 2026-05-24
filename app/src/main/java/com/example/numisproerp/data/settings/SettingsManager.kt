package com.numisproerp.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Тема додатку. Користувач обирає в Налаштуваннях.
 *
 * - [DEFAULT] — класична iOS-Blue палітра, з якою додаток був до цього.
 * - [OLEG_SMILE] — фірмова чорно-золота тема із емблемою лева.
 * - [OCEAN_GLASS] — глибокий темно-синій фон з бірюзовими фрост-картками
 *   і м'ятним монетарним акцентом ("OceanGlass").
 */
enum class AppTheme {
    DEFAULT,
    OLEG_SMILE,
    OLEG_SMILE_V2,
    OLEG_SMILE_LIGHT,
    OLEG_SMILE_PREMIUM,
    OCEAN_GLASS;

    companion object {
        fun fromKey(key: String?): AppTheme = when (key) {
            OLEG_SMILE.name -> OLEG_SMILE
            OLEG_SMILE_V2.name -> OLEG_SMILE_V2
            OLEG_SMILE_LIGHT.name -> OLEG_SMILE_LIGHT
            OLEG_SMILE_PREMIUM.name -> OLEG_SMILE_PREMIUM
            OCEAN_GLASS.name -> OCEAN_GLASS
            else -> DEFAULT
        }
    }
}

/**
 * Мова інтерфейсу. Перемикається миттєво без рестарту Activity через
 * `LocalAppLanguage` CompositionLocal та helper [com.numisproerp.ui.i18n.tr].
 */
enum class AppLanguage {
    UA,
    EN;

    companion object {
        fun fromKey(key: String?): AppLanguage = when (key) {
            EN.name -> EN
            else -> UA
        }
    }
}

/**
 * Простий менеджер користувацьких налаштувань на основі SharedPreferences.
 * Тримає Compose-friendly реактивний стан через [MutableState], щоб тема
 * та мова перемикалися миттєво без рестарту Activity.
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme: MutableState<AppTheme> =
        mutableStateOf(AppTheme.fromKey(prefs.getString(KEY_THEME, null)))

    val themeState: MutableState<AppTheme>
        get() = _theme

    var theme: AppTheme
        get() = _theme.value
        set(value) {
            _theme.value = value
            prefs.edit().putString(KEY_THEME, value.name).apply()
        }

    private val _language: MutableState<AppLanguage> =
        mutableStateOf(AppLanguage.fromKey(prefs.getString(KEY_LANGUAGE, null)))

    val languageState: MutableState<AppLanguage>
        get() = _language

    var language: AppLanguage
        get() = _language.value
        set(value) {
            _language.value = value
            prefs.edit().putString(KEY_LANGUAGE, value.name).apply()
        }

    /**
     * Поріг низького залишку для in-app сповіщень (товар з `1..threshold` шт.
     * показується як WARNING). 0 — функцію вимкнено.
     */
    private val _lowStockThreshold: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_LOW_STOCK_THRESHOLD, DEFAULT_LOW_STOCK_THRESHOLD))

    val lowStockThresholdState: MutableState<Int>
        get() = _lowStockThreshold

    var lowStockThreshold: Int
        get() = _lowStockThreshold.value
        set(value) {
            val clamped = value.coerceIn(0, MAX_LOW_STOCK_THRESHOLD)
            _lowStockThreshold.value = clamped
            prefs.edit().putInt(KEY_LOW_STOCK_THRESHOLD, clamped).apply()
        }

    /**
     * URI звуку нагадування для замітки. Може бути:
     * - порожнім рядком (тоді використовується системний DEFAULT_NOTIFICATION_URI),
     * - системним RingtoneManager URI (`content://media/...`),
     * - локальним файлом з cacheDir/custom_sounds/ (`file://...`).
     */
    private val _noteAlarmSoundUri: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_NOTE_ALARM_SOUND_URI, "") ?: "")

    val noteAlarmSoundUriState: MutableState<String>
        get() = _noteAlarmSoundUri

    var noteAlarmSoundUri: String
        get() = _noteAlarmSoundUri.value
        set(value) {
            _noteAlarmSoundUri.value = value
            prefs.edit().putString(KEY_NOTE_ALARM_SOUND_URI, value).apply()
        }

    private val _noteAlarmSoundLabel: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_NOTE_ALARM_SOUND_LABEL, "") ?: "")

    val noteAlarmSoundLabelState: MutableState<String>
        get() = _noteAlarmSoundLabel

    var noteAlarmSoundLabel: String
        get() = _noteAlarmSoundLabel.value
        set(value) {
            _noteAlarmSoundLabel.value = value
            prefs.edit().putString(KEY_NOTE_ALARM_SOUND_LABEL, value).apply()
        }

    // ==================== ШРИФТИ ====================
    private val _fontSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE))

    val fontSizeState: MutableState<Int>
        get() = _fontSize

    var fontSize: Int
        get() = _fontSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
            _fontSize.value = clamped
            prefs.edit().putInt(KEY_FONT_SIZE, clamped).apply()
        }

    private val _fontFamily: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY) ?: DEFAULT_FONT_FAMILY)

    val fontFamilyState: MutableState<String>
        get() = _fontFamily

    var fontFamily: String
        get() = _fontFamily.value
        set(value) {
            _fontFamily.value = value
            prefs.edit().putString(KEY_FONT_FAMILY, value).apply()
        }

    private val _fontColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_FONT_COLOR, "") ?: "")

    val fontColorState: MutableState<String>
        get() = _fontColor

    var fontColor: String
        get() = _fontColor.value
        set(value) {
            _fontColor.value = value
            prefs.edit().putString(KEY_FONT_COLOR, value).apply()
        }

    // ==================== ФОНОВИЙ МАЛЮНОК ====================
    private val _backgroundImagePath: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_BG_IMAGE_PATH, "") ?: "")

    val backgroundImagePathState: MutableState<String>
        get() = _backgroundImagePath

    var backgroundImagePath: String
        get() = _backgroundImagePath.value
        set(value) {
            _backgroundImagePath.value = value
            prefs.edit().putString(KEY_BG_IMAGE_PATH, value).apply()
        }

    // ==================== ЗНАЧКИ ГОЛОВНОГО МЕНЮ ====================
    // Користувач може завантажити власне фото на кожну плитку швидкого доступу
    // ("Закупівля", "Продаж", і т.д.) і регулювати прозорість фону плитки.
    // Шляхи зберігаються по одному ключу на плитку (`tile_photo_<id>`), а агрегований
    // мап подається в Compose через [tilePhotoPathsState] для реактивної перерисовки.

    private fun loadTilePhotoPaths(): Map<String, String> =
        TILE_IDS.mapNotNull { id ->
            val path = prefs.getString("$KEY_TILE_PHOTO_PREFIX$id", "") ?: ""
            if (path.isNotBlank()) id to path else null
        }.toMap()

    private val _tilePhotoPaths: MutableState<Map<String, String>> =
        mutableStateOf(loadTilePhotoPaths())

    val tilePhotoPathsState: MutableState<Map<String, String>>
        get() = _tilePhotoPaths

    fun getTilePhotoPath(tileId: String): String =
        _tilePhotoPaths.value[tileId].orEmpty()

    fun setTilePhotoPath(tileId: String, path: String) {
        val newMap = _tilePhotoPaths.value.toMutableMap()
        if (path.isBlank()) newMap.remove(tileId) else newMap[tileId] = path
        _tilePhotoPaths.value = newMap.toMap()
        prefs.edit().putString("$KEY_TILE_PHOTO_PREFIX$tileId", path).apply()
    }

    private val _tileBackgroundAlpha: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TILE_BG_ALPHA, DEFAULT_TILE_BG_ALPHA))

    val tileBackgroundAlphaState: MutableState<Float>
        get() = _tileBackgroundAlpha

    var tileBackgroundAlpha: Float
        get() = _tileBackgroundAlpha.value
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            _tileBackgroundAlpha.value = clamped
            prefs.edit().putFloat(KEY_TILE_BG_ALPHA, clamped).apply()
        }

    /**
     * Розмір значка плитки (іконки/фото) у dp. Дозволяє користувачу збільшити
     * власне фото на плитці швидкого доступу, якщо стандартне 68dp здається замалим.
     */
    private val _tileIconSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_TILE_ICON_SIZE, DEFAULT_TILE_ICON_SIZE))

    val tileIconSizeState: MutableState<Int>
        get() = _tileIconSize

    var tileIconSize: Int
        get() = _tileIconSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_TILE_ICON_SIZE, MAX_TILE_ICON_SIZE)
            _tileIconSize.value = clamped
            prefs.edit().putInt(KEY_TILE_ICON_SIZE, clamped).apply()
        }

    /**
     * Колір фону плитки швидкого доступу (hex, без `#`). Порожній рядок — використовувати
     * `colorScheme.surface` (як раніше). Прозорість керується окремо [tileBackgroundAlpha].
     */
    private val _tileBackgroundColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_TILE_BG_COLOR, "") ?: "")

    val tileBackgroundColorState: MutableState<String>
        get() = _tileBackgroundColor

    var tileBackgroundColor: String
        get() = _tileBackgroundColor.value
        set(value) {
            _tileBackgroundColor.value = value
            prefs.edit().putString(KEY_TILE_BG_COLOR, value).apply()
        }

    // ==================== ЕМБЛЕМА НАД ІНФО-ЕКРАНАМИ ====================
    // Користувач може замінити стандартну емблему OlegSmile, що показується
    // на головному екрані ("Dashboard") та задати її розмір.

    private val _emblemImagePath: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_EMBLEM_IMAGE_PATH, "") ?: "")

    val emblemImagePathState: MutableState<String>
        get() = _emblemImagePath

    var emblemImagePath: String
        get() = _emblemImagePath.value
        set(value) {
            _emblemImagePath.value = value
            prefs.edit().putString(KEY_EMBLEM_IMAGE_PATH, value).apply()
        }

    private val _emblemSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_EMBLEM_SIZE, DEFAULT_EMBLEM_SIZE))

    val emblemSizeState: MutableState<Int>
        get() = _emblemSize

    var emblemSize: Int
        get() = _emblemSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_EMBLEM_SIZE, MAX_EMBLEM_SIZE)
            _emblemSize.value = clamped
            prefs.edit().putInt(KEY_EMBLEM_SIZE, clamped).apply()
        }

    // Позиція емблеми по горизонталі (dp). 0 — по замовчуванню (зліва).
    // Позитивні значення зсувають емблему праворуч, негативні — ліворуч.
    // coerceIn — для зворотної сумісності: старі версії дозволяли ±600,
    // після зменшення діапазону до ±250 збережені великі значення
    // підрізаються при першому читанні.
    private val _emblemOffsetX: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_EMBLEM_OFFSET_X, 0)
            .coerceIn(MIN_EMBLEM_OFFSET, MAX_EMBLEM_OFFSET))

    val emblemOffsetXState: MutableState<Int>
        get() = _emblemOffsetX

    var emblemOffsetX: Int
        get() = _emblemOffsetX.value
        set(value) {
            val clamped = value.coerceIn(MIN_EMBLEM_OFFSET, MAX_EMBLEM_OFFSET)
            _emblemOffsetX.value = clamped
            prefs.edit().putInt(KEY_EMBLEM_OFFSET_X, clamped).apply()
        }

    // Позиція емблеми по вертикалі (dp). 0 — по замовчуванню.
    private val _emblemOffsetY: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_EMBLEM_OFFSET_Y, 0)
            .coerceIn(MIN_EMBLEM_OFFSET, MAX_EMBLEM_OFFSET))

    val emblemOffsetYState: MutableState<Int>
        get() = _emblemOffsetY

    var emblemOffsetY: Int
        get() = _emblemOffsetY.value
        set(value) {
            val clamped = value.coerceIn(MIN_EMBLEM_OFFSET, MAX_EMBLEM_OFFSET)
            _emblemOffsetY.value = clamped
            prefs.edit().putInt(KEY_EMBLEM_OFFSET_Y, clamped).apply()
        }

    // Поля користувацької назви проєкту в шапці Dashboard та розміру шрифту.
    // Порожній рядок — використовується предвизначена назва теми (OlegSmile / NumisProERP).
    private val _dashboardTitle: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_DASHBOARD_TITLE, "") ?: "")

    val dashboardTitleState: MutableState<String>
        get() = _dashboardTitle

    var dashboardTitle: String
        get() = _dashboardTitle.value
        set(value) {
            _dashboardTitle.value = value
            prefs.edit().putString(KEY_DASHBOARD_TITLE, value).apply()
        }

    private val _dashboardTitleSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_DASHBOARD_TITLE_SIZE, DEFAULT_DASHBOARD_TITLE_SIZE))

    val dashboardTitleSizeState: MutableState<Int>
        get() = _dashboardTitleSize

    var dashboardTitleSize: Int
        get() = _dashboardTitleSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_DASHBOARD_TITLE_SIZE, MAX_DASHBOARD_TITLE_SIZE)
            _dashboardTitleSize.value = clamped
            prefs.edit().putInt(KEY_DASHBOARD_TITLE_SIZE, clamped).apply()
        }

    // Колір заголовка ("напис біля емблеми") в hex без `#`. Порожньо —
    // використовується колір з теми (`colorScheme.primary`), як раніше.
    private val _dashboardTitleColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_DASHBOARD_TITLE_COLOR, "") ?: "")

    val dashboardTitleColorState: MutableState<String>
        get() = _dashboardTitleColor

    var dashboardTitleColor: String
        get() = _dashboardTitleColor.value
        set(value) {
            _dashboardTitleColor.value = value
            prefs.edit().putString(KEY_DASHBOARD_TITLE_COLOR, value).apply()
        }

    // Зсув заголовка головного екрана по горизонталі (dp). 0 — без зсуву.
    // coerceIn для зворотної сумісності з раніше збереженим діапазоном ±600.
    private val _dashboardTitleOffsetX: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_DASHBOARD_TITLE_OFFSET_X, 0)
            .coerceIn(MIN_DASHBOARD_TITLE_OFFSET, MAX_DASHBOARD_TITLE_OFFSET))

    val dashboardTitleOffsetXState: MutableState<Int>
        get() = _dashboardTitleOffsetX

    var dashboardTitleOffsetX: Int
        get() = _dashboardTitleOffsetX.value
        set(value) {
            val clamped = value.coerceIn(MIN_DASHBOARD_TITLE_OFFSET, MAX_DASHBOARD_TITLE_OFFSET)
            _dashboardTitleOffsetX.value = clamped
            prefs.edit().putInt(KEY_DASHBOARD_TITLE_OFFSET_X, clamped).apply()
        }

    // Зсув заголовка головного екрана по вертикалі (dp).
    private val _dashboardTitleOffsetY: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_DASHBOARD_TITLE_OFFSET_Y, 0)
            .coerceIn(MIN_DASHBOARD_TITLE_OFFSET, MAX_DASHBOARD_TITLE_OFFSET))

    val dashboardTitleOffsetYState: MutableState<Int>
        get() = _dashboardTitleOffsetY

    var dashboardTitleOffsetY: Int
        get() = _dashboardTitleOffsetY.value
        set(value) {
            val clamped = value.coerceIn(MIN_DASHBOARD_TITLE_OFFSET, MAX_DASHBOARD_TITLE_OFFSET)
            _dashboardTitleOffsetY.value = clamped
            prefs.edit().putInt(KEY_DASHBOARD_TITLE_OFFSET_Y, clamped).apply()
        }

    // ==================== СТИЛЬ ЗАГОЛОВКІВ ТА ПІДПИСІВ НА ДАШБОРДІ ====================
    // Дозволяє змінювати розмір і колір заголовків "Швидкий доступ"/"Останні операції"
    // та підписів плиток (Закупівля, Склад тощо), щоб на будь-якому фоні
    // (включаючи світлі фони) текст був читко видно.
    // Порожній hex — використовується колір з теми (як раніше).

    private val _dashboardHeaderFontSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_DASHBOARD_HEADER_FONT_SIZE, DEFAULT_DASHBOARD_HEADER_FONT_SIZE))

    val dashboardHeaderFontSizeState: MutableState<Int>
        get() = _dashboardHeaderFontSize

    var dashboardHeaderFontSize: Int
        get() = _dashboardHeaderFontSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_DASHBOARD_HEADER_FONT_SIZE, MAX_DASHBOARD_HEADER_FONT_SIZE)
            _dashboardHeaderFontSize.value = clamped
            prefs.edit().putInt(KEY_DASHBOARD_HEADER_FONT_SIZE, clamped).apply()
        }

    private val _dashboardHeaderColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_DASHBOARD_HEADER_COLOR, "") ?: "")

    val dashboardHeaderColorState: MutableState<String>
        get() = _dashboardHeaderColor

    var dashboardHeaderColor: String
        get() = _dashboardHeaderColor.value
        set(value) {
            _dashboardHeaderColor.value = value
            prefs.edit().putString(KEY_DASHBOARD_HEADER_COLOR, value).apply()
        }

    private val _tileLabelFontSize: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_TILE_LABEL_FONT_SIZE, DEFAULT_TILE_LABEL_FONT_SIZE))

    val tileLabelFontSizeState: MutableState<Int>
        get() = _tileLabelFontSize

    var tileLabelFontSize: Int
        get() = _tileLabelFontSize.value
        set(value) {
            val clamped = value.coerceIn(MIN_TILE_LABEL_FONT_SIZE, MAX_TILE_LABEL_FONT_SIZE)
            _tileLabelFontSize.value = clamped
            prefs.edit().putInt(KEY_TILE_LABEL_FONT_SIZE, clamped).apply()
        }

    private val _tileLabelColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_TILE_LABEL_COLOR, "") ?: "")

    val tileLabelColorState: MutableState<String>
        get() = _tileLabelColor

    var tileLabelColor: String
        get() = _tileLabelColor.value
        set(value) {
            _tileLabelColor.value = value
            prefs.edit().putString(KEY_TILE_LABEL_COLOR, value).apply()
        }

    // ==================== ІНФОРМАЦІЙНІ КАРТКИ НА DASHBOARD ====================
    // Колір фону + прозорість для карток зведення (баланс, місячні стати, останні операції).

    private val _infoCardBackgroundColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_INFO_CARD_BG_COLOR, "") ?: "")

    val infoCardBackgroundColorState: MutableState<String>
        get() = _infoCardBackgroundColor

    var infoCardBackgroundColor: String
        get() = _infoCardBackgroundColor.value
        set(value) {
            _infoCardBackgroundColor.value = value
            prefs.edit().putString(KEY_INFO_CARD_BG_COLOR, value).apply()
        }

    private val _infoCardBackgroundAlpha: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_INFO_CARD_BG_ALPHA, DEFAULT_INFO_CARD_BG_ALPHA))

    val infoCardBackgroundAlphaState: MutableState<Float>
        get() = _infoCardBackgroundAlpha

    var infoCardBackgroundAlpha: Float
        get() = _infoCardBackgroundAlpha.value
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            _infoCardBackgroundAlpha.value = clamped
            prefs.edit().putFloat(KEY_INFO_CARD_BG_ALPHA, clamped).apply()
        }

    // ==================== ОБВОДКА (КОНТУР) ІНФО-КАРТОК ====================
    // Користувач у фідбеку попросив можливість додати контур навколо
    // інформаційних карток на робочому столі. Це особливо корисно коли
    // прозорість фону ставлять у 0 — без контуру картки «зливаються» з
    // фоном. Окремий вкл/викл-тогл, колір + прозорість.

    private val _infoCardBorderEnabled: MutableState<Boolean> =
        mutableStateOf(prefs.getBoolean(KEY_INFO_CARD_BORDER_ENABLED, false))

    val infoCardBorderEnabledState: MutableState<Boolean>
        get() = _infoCardBorderEnabled

    var infoCardBorderEnabled: Boolean
        get() = _infoCardBorderEnabled.value
        set(value) {
            _infoCardBorderEnabled.value = value
            prefs.edit().putBoolean(KEY_INFO_CARD_BORDER_ENABLED, value).apply()
        }

    // Колір обводки у hex без `#`. За замовчуванням — нейтральний сірий, який
    // буде помітний і на світлих, і на темних фонах.
    private val _infoCardBorderColor: MutableState<String> =
        mutableStateOf(
            prefs.getString(KEY_INFO_CARD_BORDER_COLOR, DEFAULT_INFO_CARD_BORDER_COLOR)
                ?: DEFAULT_INFO_CARD_BORDER_COLOR
        )

    val infoCardBorderColorState: MutableState<String>
        get() = _infoCardBorderColor

    var infoCardBorderColor: String
        get() = _infoCardBorderColor.value
        set(value) {
            _infoCardBorderColor.value = value
            prefs.edit().putString(KEY_INFO_CARD_BORDER_COLOR, value).apply()
        }

    private val _infoCardBorderOpacity: MutableState<Float> =
        mutableStateOf(
            prefs.getFloat(KEY_INFO_CARD_BORDER_OPACITY, DEFAULT_INFO_CARD_BORDER_OPACITY)
                .coerceIn(0f, 1f)
        )

    val infoCardBorderOpacityState: MutableState<Float>
        get() = _infoCardBorderOpacity

    var infoCardBorderOpacity: Float
        get() = _infoCardBorderOpacity.value
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            _infoCardBorderOpacity.value = clamped
            prefs.edit().putFloat(KEY_INFO_CARD_BORDER_OPACITY, clamped).apply()
        }

    // Товщина обводки у dp (фіксована, без слайдера — щоб не плодити
    // налаштування). 1.5dp — стандартний візуальний поріг видимості.
    val infoCardBorderWidthDp: Float
        get() = DEFAULT_INFO_CARD_BORDER_WIDTH_DP

    // ==================== ТІНІ ТЕКСТУ ====================
    // Глобальне налаштування тіні для основних текстів на робочому столі
    // (заголовок головного екрана, підписи плиток, заголовки секцій).
    // Дозволяє зробити текст читабельним на будь-якому фоні — на світлих фото
    // ставимо темну тінь, на темних — світлу. Якщо `enabled = false` — текст
    // рендериться без тіні (старий вигляд).

    private val _textShadowEnabled: MutableState<Boolean> =
        mutableStateOf(prefs.getBoolean(KEY_TEXT_SHADOW_ENABLED, false))

    val textShadowEnabledState: MutableState<Boolean>
        get() = _textShadowEnabled

    var textShadowEnabled: Boolean
        get() = _textShadowEnabled.value
        set(value) {
            _textShadowEnabled.value = value
            prefs.edit().putBoolean(KEY_TEXT_SHADOW_ENABLED, value).apply()
        }

    // Колір тіні у hex без `#`. За замовчуванням — чорний, як стандартний drop shadow.
    private val _textShadowColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_TEXT_SHADOW_COLOR, DEFAULT_TEXT_SHADOW_COLOR) ?: DEFAULT_TEXT_SHADOW_COLOR)

    val textShadowColorState: MutableState<String>
        get() = _textShadowColor

    var textShadowColor: String
        get() = _textShadowColor.value
        set(value) {
            _textShadowColor.value = value
            prefs.edit().putString(KEY_TEXT_SHADOW_COLOR, value).apply()
        }

    // Зсув тіні по горизонталі/вертикалі у dp. Дозволяє контролювати «куди падає тінь».
    private val _textShadowOffsetX: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TEXT_SHADOW_OFFSET_X, DEFAULT_TEXT_SHADOW_OFFSET)
            .coerceIn(MIN_TEXT_SHADOW_OFFSET, MAX_TEXT_SHADOW_OFFSET))

    val textShadowOffsetXState: MutableState<Float>
        get() = _textShadowOffsetX

    var textShadowOffsetX: Float
        get() = _textShadowOffsetX.value
        set(value) {
            val clamped = value.coerceIn(MIN_TEXT_SHADOW_OFFSET, MAX_TEXT_SHADOW_OFFSET)
            _textShadowOffsetX.value = clamped
            prefs.edit().putFloat(KEY_TEXT_SHADOW_OFFSET_X, clamped).apply()
        }

    private val _textShadowOffsetY: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TEXT_SHADOW_OFFSET_Y, DEFAULT_TEXT_SHADOW_OFFSET)
            .coerceIn(MIN_TEXT_SHADOW_OFFSET, MAX_TEXT_SHADOW_OFFSET))

    val textShadowOffsetYState: MutableState<Float>
        get() = _textShadowOffsetY

    var textShadowOffsetY: Float
        get() = _textShadowOffsetY.value
        set(value) {
            val clamped = value.coerceIn(MIN_TEXT_SHADOW_OFFSET, MAX_TEXT_SHADOW_OFFSET)
            _textShadowOffsetY.value = clamped
            prefs.edit().putFloat(KEY_TEXT_SHADOW_OFFSET_Y, clamped).apply()
        }

    // Прозорість тіні (0..1). 1 — повністю непрозора тінь (стандартний вигляд),
    // менші значення дають м'якший ефект — наприклад, для світлих тіней
    // на темному фоні.
    private val _textShadowOpacity: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TEXT_SHADOW_OPACITY, DEFAULT_TEXT_SHADOW_OPACITY)
            .coerceIn(0f, 1f))

    val textShadowOpacityState: MutableState<Float>
        get() = _textShadowOpacity

    var textShadowOpacity: Float
        get() = _textShadowOpacity.value
        set(value) {
            val clamped = value.coerceIn(0f, 1f)
            _textShadowOpacity.value = clamped
            prefs.edit().putFloat(KEY_TEXT_SHADOW_OPACITY, clamped).apply()
        }

    // Радіус розмиття тіні. 0 — різкий «hard shadow», вище — м'якший градієнт.
    private val _textShadowRadius: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TEXT_SHADOW_RADIUS, DEFAULT_TEXT_SHADOW_RADIUS)
            .coerceIn(0f, MAX_TEXT_SHADOW_RADIUS))

    val textShadowRadiusState: MutableState<Float>
        get() = _textShadowRadius

    var textShadowRadius: Float
        get() = _textShadowRadius.value
        set(value) {
            val clamped = value.coerceIn(0f, MAX_TEXT_SHADOW_RADIUS)
            _textShadowRadius.value = clamped
            prefs.edit().putFloat(KEY_TEXT_SHADOW_RADIUS, clamped).apply()
        }

    // ==================== ВІДХИЛЕНІ СПОВІЩЕННЯ ====================
    // Сповіщення про низький залишок / OOS генеруються реактивно з реального
    // стану складу. Якщо користувач їх прочитав і хоче прибрати з UI —
    // зберігаємо їхній id у Set. ViewModel фільтрує сповіщення з цього набору.
    // Кожен id містить значення, що змінюється при наступному «природному»
    // спрацюванні, щоб приховане сповіщення не блокувало майбутні попередження
    // про той самий товар:
    //  - `out_<catalog>_<totalPurchased>` — totalPurchased зростає при кожному
    //    поповненні, тому повний цикл «поповнили → знову закінчилось» дає новий id;
    //  - `low_<catalog>_<stock>` — будь-яка зміна залишку дає новий id.

    private val _dismissedNotifications: MutableState<Set<String>> =
        mutableStateOf(prefs.getStringSet(KEY_DISMISSED_NOTIFICATIONS, emptySet())?.toSet() ?: emptySet())

    val dismissedNotificationsState: MutableState<Set<String>>
        get() = _dismissedNotifications

    fun dismissNotification(id: String) {
        if (id.isBlank()) return
        val next = _dismissedNotifications.value + id
        _dismissedNotifications.value = next
        prefs.edit().putStringSet(KEY_DISMISSED_NOTIFICATIONS, next).apply()
    }

    fun dismissNotifications(ids: Collection<String>) {
        val filtered = ids.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return
        val next = _dismissedNotifications.value + filtered
        _dismissedNotifications.value = next
        prefs.edit().putStringSet(KEY_DISMISSED_NOTIFICATIONS, next).apply()
    }

    fun restoreAllNotifications() {
        if (_dismissedNotifications.value.isEmpty()) return
        _dismissedNotifications.value = emptySet()
        prefs.edit().putStringSet(KEY_DISMISSED_NOTIFICATIONS, emptySet()).apply()
    }

    // ==================== СІТКА ПЛИТОК ГОЛОВНОГО ЕКРАНА ====================
    // Параметри керування сіткою плиток на головному екрані: кількість колонок,
    // кількість рядків, порядок плиток та користувацькі назви. Усе зберігається
    // у [prefs] і реактивно віддається через MutableState — DashboardScreen
    // зчитує ці значення через CompositionLocal провайдери з [MainActivity].
    //
    // Розмір сітки обмежений набором пресетів [ALLOWED_TILE_GRID_PRESETS].
    // Загальна кількість плиток у видимій сітці = columns * rows. `tileOrder`
    // може містити більше або менше id — UI рендерить тільки перші N. Порожні
    // позиції зарезервовані для майбутнього (наприклад, додавання нових дій).

    private val _tileGridColumns: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_TILE_GRID_COLUMNS, DEFAULT_TILE_GRID_COLUMNS))

    val tileGridColumnsState: MutableState<Int>
        get() = _tileGridColumns

    private val _tileGridRows: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_TILE_GRID_ROWS, DEFAULT_TILE_GRID_ROWS))

    val tileGridRowsState: MutableState<Int>
        get() = _tileGridRows

    /**
     * Задає сітку плиток. Якщо (cols, rows) немає в [ALLOWED_TILE_GRID_PRESETS],
     * пара ігнорується — це додатковий запобіжник проти пошкоджених преф-файлів.
     */
    fun setTileGrid(cols: Int, rows: Int) {
        if (ALLOWED_TILE_GRID_PRESETS.none { it.first == cols && it.second == rows }) return
        _tileGridColumns.value = cols
        _tileGridRows.value = rows
        prefs.edit()
            .putInt(KEY_TILE_GRID_COLUMNS, cols)
            .putInt(KEY_TILE_GRID_ROWS, rows)
            .apply()
    }

    /**
     * Порядок плиток як список actionId. Якщо в prefs нічого немає або значення
     * пошкоджене — повертаємо [DEFAULT_TILE_ORDER]. Список може бути довшим або
     * коротшим за поточну сітку; UI рендерить cols*rows перших елементів.
     */
    private val _tileOrder: MutableState<List<String>> =
        mutableStateOf(parseTileOrder(prefs.getString(KEY_TILE_ORDER, null)))

    val tileOrderState: MutableState<List<String>>
        get() = _tileOrder

    /**
     * Записує новий порядок плиток. Дублікати та порожні рядки відкидаються,
     * щоб не зламати рендеринг.
     */
    fun setTileOrder(order: List<String>) {
        val clean = order.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        _tileOrder.value = clean
        prefs.edit().putString(KEY_TILE_ORDER, clean.joinToString(",")).apply()
    }

    private fun parseTileOrder(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return DEFAULT_TILE_ORDER
        val parts = raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
        return if (parts.isEmpty()) DEFAULT_TILE_ORDER else parts
    }

    /**
     * Користувацькі назви плиток. Один ключ на одну плитку — `tile_label_<actionId>`.
     * Якщо ключ відсутній або порожній — UI використовує дефолтну назву з
     * `QuickAccessActionRegistry`.
     */
    private fun loadTileLabels(): Map<String, String> {
        val all = prefs.all
        val result = mutableMapOf<String, String>()
        for ((key, value) in all) {
            if (key.startsWith(KEY_TILE_LABEL_PREFIX) && value is String && value.isNotBlank()) {
                val actionId = key.removePrefix(KEY_TILE_LABEL_PREFIX)
                if (actionId.isNotBlank()) result[actionId] = value
            }
        }
        return result
    }

    private val _tileLabels: MutableState<Map<String, String>> =
        mutableStateOf(loadTileLabels())

    val tileLabelsState: MutableState<Map<String, String>>
        get() = _tileLabels

    fun getTileLabelOverride(actionId: String): String =
        _tileLabels.value[actionId].orEmpty()

    /**
     * Записує користувацьку назву плитки. Порожнє значення видаляє override
     * (плитка повертається до дефолтної назви з реєстру).
     */
    fun setTileLabel(actionId: String, label: String) {
        if (actionId.isBlank()) return
        val newMap = _tileLabels.value.toMutableMap()
        val trimmed = label.trim()
        val editor = prefs.edit()
        if (trimmed.isBlank()) {
            newMap.remove(actionId)
            editor.remove("$KEY_TILE_LABEL_PREFIX$actionId")
        } else {
            newMap[actionId] = trimmed
            editor.putString("$KEY_TILE_LABEL_PREFIX$actionId", trimmed)
        }
        _tileLabels.value = newMap.toMap()
        editor.apply()
    }

    // ==================== ВЕРХНІЙ/НИЖНІЙ/БІЧНИЙ БАРИ ====================
    // Користувач може задати колір фону для кожного з трьох барів та повзунком
    // зробити його світлішим або темнішим. Жодної реальної прозорості — бари
    // лишаються повністю непрозорими, щоб вміст не просвічувався під ними.
    //  brightness < 0 — темніший колір (підмішується чорний),
    //  brightness > 0 — світліший (підмішується білий),
    //  brightness = 0 (стандарт) — колір без змін / стандартний бар з теми.

    private val _topBarColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_TOP_BAR_COLOR, "") ?: "")

    val topBarColorState: MutableState<String>
        get() = _topBarColor

    var topBarColor: String
        get() = _topBarColor.value
        set(value) {
            _topBarColor.value = value
            prefs.edit().putString(KEY_TOP_BAR_COLOR, value).apply()
        }

    private val _topBarBrightness: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TOP_BAR_BRIGHTNESS, 0f))

    val topBarBrightnessState: MutableState<Float>
        get() = _topBarBrightness

    var topBarBrightness: Float
        get() = _topBarBrightness.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_BRIGHTNESS, MAX_BAR_BRIGHTNESS)
            _topBarBrightness.value = clamped
            prefs.edit().putFloat(KEY_TOP_BAR_BRIGHTNESS, clamped).apply()
        }

    private val _bottomBarColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_BOTTOM_BAR_COLOR, "") ?: "")

    val bottomBarColorState: MutableState<String>
        get() = _bottomBarColor

    var bottomBarColor: String
        get() = _bottomBarColor.value
        set(value) {
            _bottomBarColor.value = value
            prefs.edit().putString(KEY_BOTTOM_BAR_COLOR, value).apply()
        }

    private val _bottomBarBrightness: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_BOTTOM_BAR_BRIGHTNESS, 0f))

    val bottomBarBrightnessState: MutableState<Float>
        get() = _bottomBarBrightness

    var bottomBarBrightness: Float
        get() = _bottomBarBrightness.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_BRIGHTNESS, MAX_BAR_BRIGHTNESS)
            _bottomBarBrightness.value = clamped
            prefs.edit().putFloat(KEY_BOTTOM_BAR_BRIGHTNESS, clamped).apply()
        }

    private val _drawerColor: MutableState<String> =
        mutableStateOf(prefs.getString(KEY_DRAWER_COLOR, "") ?: "")

    val drawerColorState: MutableState<String>
        get() = _drawerColor

    var drawerColor: String
        get() = _drawerColor.value
        set(value) {
            _drawerColor.value = value
            prefs.edit().putString(KEY_DRAWER_COLOR, value).apply()
        }

    private val _drawerBrightness: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_DRAWER_BRIGHTNESS, 0f))

    val drawerBrightnessState: MutableState<Float>
        get() = _drawerBrightness

    var drawerBrightness: Float
        get() = _drawerBrightness.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_BRIGHTNESS, MAX_BAR_BRIGHTNESS)
            _drawerBrightness.value = clamped
            prefs.edit().putFloat(KEY_DRAWER_BRIGHTNESS, clamped).apply()
        }

    // ==================== ПРОЗОРІСТЬ БАРІВ ====================
    // На відміну від «світліше/темніше» (brightness), ці значення задають
    // саме alpha-канал контейнера бару — від MIN_BAR_OPACITY (майже прозорий,
    // вміст просвічується) до 1.0 (повністю непрозорий, стандартна поведінка).
    // Значення зберігається у SharedPreferences та зчитується на старті —
    // зміни підхоплюються рекомпозицією через MutableState.

    private val _topBarOpacity: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_TOP_BAR_OPACITY, DEFAULT_BAR_OPACITY))

    val topBarOpacityState: MutableState<Float>
        get() = _topBarOpacity

    var topBarOpacity: Float
        get() = _topBarOpacity.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_OPACITY, MAX_BAR_OPACITY)
            _topBarOpacity.value = clamped
            prefs.edit().putFloat(KEY_TOP_BAR_OPACITY, clamped).apply()
        }

    private val _bottomBarOpacity: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_BOTTOM_BAR_OPACITY, DEFAULT_BAR_OPACITY))

    val bottomBarOpacityState: MutableState<Float>
        get() = _bottomBarOpacity

    var bottomBarOpacity: Float
        get() = _bottomBarOpacity.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_OPACITY, MAX_BAR_OPACITY)
            _bottomBarOpacity.value = clamped
            prefs.edit().putFloat(KEY_BOTTOM_BAR_OPACITY, clamped).apply()
        }

    private val _drawerOpacity: MutableState<Float> =
        mutableStateOf(prefs.getFloat(KEY_DRAWER_OPACITY, DEFAULT_BAR_OPACITY))

    val drawerOpacityState: MutableState<Float>
        get() = _drawerOpacity

    var drawerOpacity: Float
        get() = _drawerOpacity.value
        set(value) {
            val clamped = value.coerceIn(MIN_BAR_OPACITY, MAX_BAR_OPACITY)
            _drawerOpacity.value = clamped
            prefs.edit().putFloat(KEY_DRAWER_OPACITY, clamped).apply()
        }

    companion object {
        private const val PREFS_NAME = "numispro_settings"
        private const val KEY_THEME = "app_theme"
        private const val KEY_LANGUAGE = "app_language"
        private const val KEY_LOW_STOCK_THRESHOLD = "low_stock_threshold"
        private const val KEY_NOTE_ALARM_SOUND_URI = "note_alarm_sound_uri"
        private const val KEY_NOTE_ALARM_SOUND_LABEL = "note_alarm_sound_label"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_COLOR = "font_color"
        private const val KEY_BG_IMAGE_PATH = "bg_image_path"
        private const val KEY_TILE_PHOTO_PREFIX = "tile_photo_"
        private const val KEY_TILE_BG_ALPHA = "tile_bg_alpha"
        private const val KEY_TILE_ICON_SIZE = "tile_icon_size"
        private const val KEY_TILE_BG_COLOR = "tile_bg_color"
        private const val KEY_EMBLEM_IMAGE_PATH = "emblem_image_path"
        private const val KEY_EMBLEM_SIZE = "emblem_size"
        private const val KEY_EMBLEM_OFFSET_X = "emblem_offset_x"
        private const val KEY_EMBLEM_OFFSET_Y = "emblem_offset_y"
        private const val KEY_DASHBOARD_TITLE = "dashboard_title"
        private const val KEY_DASHBOARD_TITLE_SIZE = "dashboard_title_size"
        private const val KEY_DASHBOARD_TITLE_COLOR = "dashboard_title_color"
        private const val KEY_DASHBOARD_TITLE_OFFSET_X = "dashboard_title_offset_x"
        private const val KEY_DASHBOARD_TITLE_OFFSET_Y = "dashboard_title_offset_y"
        private const val KEY_DASHBOARD_HEADER_FONT_SIZE = "dashboard_header_font_size"
        private const val KEY_DASHBOARD_HEADER_COLOR = "dashboard_header_color"
        private const val KEY_TILE_LABEL_FONT_SIZE = "tile_label_font_size"
        private const val KEY_TILE_LABEL_COLOR = "tile_label_color"
        private const val KEY_INFO_CARD_BG_COLOR = "info_card_bg_color"
        private const val KEY_INFO_CARD_BG_ALPHA = "info_card_bg_alpha"
        private const val KEY_INFO_CARD_BORDER_ENABLED = "info_card_border_enabled"
        private const val KEY_INFO_CARD_BORDER_COLOR = "info_card_border_color"
        private const val KEY_INFO_CARD_BORDER_OPACITY = "info_card_border_opacity"
        private const val KEY_TOP_BAR_COLOR = "top_bar_color"
        private const val KEY_TOP_BAR_BRIGHTNESS = "top_bar_brightness"
        private const val KEY_BOTTOM_BAR_COLOR = "bottom_bar_color"
        private const val KEY_BOTTOM_BAR_BRIGHTNESS = "bottom_bar_brightness"
        private const val KEY_DRAWER_COLOR = "drawer_color"
        private const val KEY_DRAWER_BRIGHTNESS = "drawer_brightness"
        private const val KEY_TOP_BAR_OPACITY = "top_bar_opacity"
        private const val KEY_BOTTOM_BAR_OPACITY = "bottom_bar_opacity"
        private const val KEY_DRAWER_OPACITY = "drawer_opacity"
        private const val KEY_TEXT_SHADOW_ENABLED = "text_shadow_enabled"
        private const val KEY_TEXT_SHADOW_COLOR = "text_shadow_color"
        private const val KEY_TEXT_SHADOW_OFFSET_X = "text_shadow_offset_x"
        private const val KEY_TEXT_SHADOW_OFFSET_Y = "text_shadow_offset_y"
        private const val KEY_TEXT_SHADOW_OPACITY = "text_shadow_opacity"
        private const val KEY_TEXT_SHADOW_RADIUS = "text_shadow_radius"
        private const val KEY_DISMISSED_NOTIFICATIONS = "dismissed_notifications"
        // ==================== СІТКА ПЛИТОК ГОЛОВНОГО ЕКРАНА ====================
        // Користувацька конфігурація плиток швидкого доступу: розмір сітки,
        // порядок та назви кожної плитки. Дозволяє вибирати сітки 5×1, 3×2,
        // 3×3, 4×2, 4×3 і будь-який порядок дій з [QuickAccessActionRegistry].
        private const val KEY_TILE_GRID_COLUMNS = "tile_grid_columns"
        private const val KEY_TILE_GRID_ROWS = "tile_grid_rows"
        // Порядок плиток — comma-separated список actionId (із реєстру).
        // Дозволяє швидко серіалізувати/десеріалізувати без JSON.
        private const val KEY_TILE_ORDER = "tile_order"
        // Префікс для перейменованих назв плиток. Один ключ на кожен actionId.
        // Використовуємо суфікс `_name_` (а не просто `tile_label_`), бо існуючі
        // ключі `tile_label_font_size`, `tile_label_color` перетиналися б префіксом
        // і випадково потрапляли в [loadTileLabels].
        private const val KEY_TILE_LABEL_PREFIX = "tile_name_"
        const val DEFAULT_LOW_STOCK_THRESHOLD = 3
        const val MAX_LOW_STOCK_THRESHOLD = 20
        const val DEFAULT_FONT_SIZE = 14
        const val MIN_FONT_SIZE = 10
        const val MAX_FONT_SIZE = 24
        const val DEFAULT_FONT_FAMILY = "system"
        // Початкове значення збігається з попереднім жорстким `surface.copy(alpha = 0.55f)`
        // у [DashboardScreen.QuickAccessButton], щоб старі користувачі не побачили
        // зміни в зовнішньому вигляді одразу після оновлення.
        const val DEFAULT_TILE_BG_ALPHA = 0.55f
        // Стандартне значення = `TILE_BOX_SIZE_DP` - 4dp (іконка майже впритул до
        // фону, з невеликим відступом для рамки). Користувач може зменшити іконку
        // повзунком, або зробити її більшою за фон (вона перекриє рамку).
        const val DEFAULT_TILE_ICON_SIZE = 76
        const val MIN_TILE_ICON_SIZE = 40
        const val MAX_TILE_ICON_SIZE = 120
        // Розмір фонового квадрата плитки швидкого доступу. Фіксований, не залежить
        // від повзунка розміру іконки — лише іконка/фото масштабується.
        const val TILE_BOX_SIZE_DP = 80
        // Стандарт збігається з попереднім жорстким 72dp у `DashboardHeader`.
        const val DEFAULT_EMBLEM_SIZE = 72
        const val MIN_EMBLEM_SIZE = 40
        // Розширений діапазон розміру емблеми (до ~всієї ширини шапки), щоб
        // користувач міг зробити її значно більшою, ніж дозволяв старий ліміт 160dp.
        const val MAX_EMBLEM_SIZE = 400
        // Межі зсуву емблеми в dp. Раніше було ±600, що давало завеликий діапазон
        // повзунка — навіть невеликий рух різко зсував емблему за межі шапки.
        // Зменшено до ±250dp: цього достатньо, щоб перетягти емблему по горизонталі
        // практично через усю шапку (на типовому ~360dp екрані), і повзунок стає
        // помітно точнішим.
        const val MIN_EMBLEM_OFFSET = -250
        const val MAX_EMBLEM_OFFSET = 250
        // Назва "NumisProERP" в шапці за замовчуванням рендерилася на 26.sp.
        const val DEFAULT_DASHBOARD_TITLE_SIZE = 26
        const val MIN_DASHBOARD_TITLE_SIZE = 10
        // Розширений верхній ліміт розміру заголовка для тих, хто хоче
        // дуже великий "напис" поряд з емблемою (раніше було 40sp).
        const val MAX_DASHBOARD_TITLE_SIZE = 96
        // Межі зсуву тексту заголовка головного екрана у dp. Узгоджені
        // з межами емблеми, щоб напис можна було тягати в тих самих
        // діапазонах (раніше ±600 — занадто широко для практичного слайдера).
        const val MIN_DASHBOARD_TITLE_OFFSET = -250
        const val MAX_DASHBOARD_TITLE_OFFSET = 250
        // Стандартний розмір заголовків "Швидкий доступ" / "Останні операції" — 18.sp.
        const val DEFAULT_DASHBOARD_HEADER_FONT_SIZE = 18
        const val MIN_DASHBOARD_HEADER_FONT_SIZE = 12
        const val MAX_DASHBOARD_HEADER_FONT_SIZE = 28
        // Підпис під плиткою швидкого доступу за замовчуванням — 10.sp.
        const val DEFAULT_TILE_LABEL_FONT_SIZE = 10
        const val MIN_TILE_LABEL_FONT_SIZE = 8
        const val MAX_TILE_LABEL_FONT_SIZE = 18
        // 1.0 = непрозорі картки (старий вигляд).
        const val DEFAULT_INFO_CARD_BG_ALPHA = 1.0f
        // Обводка інфо-карток: за замовч. вимкнена, сіра, не зовсім непрозора.
        const val DEFAULT_INFO_CARD_BORDER_COLOR = "808080"
        const val DEFAULT_INFO_CARD_BORDER_OPACITY = 1.0f
        const val DEFAULT_INFO_CARD_BORDER_WIDTH_DP = 1.5f
        // Межі «освітлення/затемнення» для барів. -0.5 = повністю чорний,
        // +0.5 = повністю білий, 0 = колір без змін. Бари завжди непрозорі —
        // повзунок лише перетворює базовий колір, а не задає `alpha`.
        const val MIN_BAR_BRIGHTNESS = -0.5f
        const val MAX_BAR_BRIGHTNESS = 0.5f
        // Прозорість бару (alpha-канал контейнера). 1.0 — повністю непрозорий
        // (стандартна поведінка), значення нижче — бар стає напівпрозорим,
        // вміст під ним просвічується. Нижня межа — 0.2 щоб бар повністю
        // не зник і кнопки залишалися видимими.
        const val MIN_BAR_OPACITY = 0.2f
        const val MAX_BAR_OPACITY = 1.0f
        const val DEFAULT_BAR_OPACITY = 1.0f
        // Тіні тексту. Стандарт: вимкнено, чорний колір, зсув 2dp/2dp,
        // непрозорість 0.85, радіус 4dp — нейтральна "drop shadow" поведінка.
        const val DEFAULT_TEXT_SHADOW_COLOR = "000000"
        const val DEFAULT_TEXT_SHADOW_OFFSET = 2f
        const val MIN_TEXT_SHADOW_OFFSET = -12f
        const val MAX_TEXT_SHADOW_OFFSET = 12f
        const val DEFAULT_TEXT_SHADOW_OPACITY = 0.85f
        const val DEFAULT_TEXT_SHADOW_RADIUS = 4f
        const val MAX_TEXT_SHADOW_RADIUS = 24f
        /**
         * Ідентифікатори всіх плиток швидкого доступу головного меню.
         * Збігаються з `tileId`, який передається у [QuickAccessButton].
         */
        val TILE_IDS = listOf(
            "purchase", "sale", "stock", "clients",
            "reports", "suppliers", "expenses", "collection", "documents"
        )

        // Стандартні значення сітки головного екрана: 3 колонки × 2 рядки = 6 плиток.
        const val DEFAULT_TILE_GRID_COLUMNS = 3
        const val DEFAULT_TILE_GRID_ROWS = 2
        // Дозволені пресети сітки. Користувач може обрати тільки з цього списку.
        // Кожна пара (cols, rows) дає cols*rows плиток.
        val ALLOWED_TILE_GRID_PRESETS: List<Pair<Int, Int>> = listOf(
            5 to 1,
            3 to 2,
            3 to 3,
            4 to 2,
            4 to 3
        )
        // Порядок плиток за замовчуванням — 6 плиток як після PR #1.
        val DEFAULT_TILE_ORDER: List<String> = listOf(
            "purchase", "sale", "stock", "clients", "suppliers", "collection"
        )
    }
}
