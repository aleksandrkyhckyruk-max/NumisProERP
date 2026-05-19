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
    private val _emblemOffsetX: MutableState<Int> =
        mutableStateOf(prefs.getInt(KEY_EMBLEM_OFFSET_X, 0))

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
        mutableStateOf(prefs.getInt(KEY_EMBLEM_OFFSET_Y, 0))

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
        private const val KEY_DASHBOARD_HEADER_FONT_SIZE = "dashboard_header_font_size"
        private const val KEY_DASHBOARD_HEADER_COLOR = "dashboard_header_color"
        private const val KEY_TILE_LABEL_FONT_SIZE = "tile_label_font_size"
        private const val KEY_TILE_LABEL_COLOR = "tile_label_color"
        private const val KEY_INFO_CARD_BG_COLOR = "info_card_bg_color"
        private const val KEY_INFO_CARD_BG_ALPHA = "info_card_bg_alpha"
        private const val KEY_TOP_BAR_COLOR = "top_bar_color"
        private const val KEY_TOP_BAR_BRIGHTNESS = "top_bar_brightness"
        private const val KEY_BOTTOM_BAR_COLOR = "bottom_bar_color"
        private const val KEY_BOTTOM_BAR_BRIGHTNESS = "bottom_bar_brightness"
        private const val KEY_DRAWER_COLOR = "drawer_color"
        private const val KEY_DRAWER_BRIGHTNESS = "drawer_brightness"
        private const val KEY_TOP_BAR_OPACITY = "top_bar_opacity"
        private const val KEY_BOTTOM_BAR_OPACITY = "bottom_bar_opacity"
        private const val KEY_DRAWER_OPACITY = "drawer_opacity"
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
        const val MAX_EMBLEM_SIZE = 160
        // Межі зсуву емблеми в dp. Дозволяє користувачу вивести емблему поверх
        // картки балансу / зробити її як водяний знак.
        const val MIN_EMBLEM_OFFSET = -200
        const val MAX_EMBLEM_OFFSET = 200
        // Назва "NumisProERP" в шапці за замовчуванням рендерилася на 26.sp.
        const val DEFAULT_DASHBOARD_TITLE_SIZE = 26
        const val MIN_DASHBOARD_TITLE_SIZE = 14
        const val MAX_DASHBOARD_TITLE_SIZE = 40
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
        /**
         * Ідентифікатори всіх плиток швидкого доступу головного меню.
         * Збігаються з `tileId`, який передається у [QuickAccessButton].
         */
        val TILE_IDS = listOf(
            "purchase", "sale", "stock", "clients",
            "reports", "suppliers", "expenses", "collection"
        )
    }
}
