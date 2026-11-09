package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.FluxAccentPalette
import com.example.ui.theme.FluxThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

val Context.fluxDataStore: DataStore<Preferences> by preferencesDataStore(name = "flux_preferences")

enum class IconShapeOption(val id: String, val label: String) {
    SQUIRCLE("squircle", "Squircle"),
    CIRCLE("circle", "Circle"),
    ROUNDED_SQUARE("rounded_square", "Sharp Square"),
    HEXAGON("hexagon", "Hexagon"),
    TEARDROP("teardrop", "Teardrop");

    companion object {
        fun fromId(id: String): IconShapeOption = entries.firstOrNull { it.id == id } ?: SQUIRCLE
    }
}

enum class HomeLayoutStyle(val id: String, val label: String) {
    CANVAS("canvas", "Spatial Canvas"),
    EDITORIAL_LIST("editorial_list", "Editorial Stream"),
    SPATIAL_GRID("spatial_grid", "Structured Matrix");

    companion object {
        fun fromId(id: String): HomeLayoutStyle = entries.firstOrNull { it.id == id } ?: CANVAS
    }
}

enum class LayoutDensityOption(val id: String, val label: String, val spacingDp: Int) {
    COMPACT("compact", "Compact", 10),
    BALANCED("balanced", "Balanced", 16),
    SPACIOUS("spacious", "Spacious", 24);

    companion object {
        fun fromId(id: String): LayoutDensityOption = entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

enum class GestureTargetAction(val id: String, val label: String, val requiresSystemNote: String = "") {
    OPEN_ACTION_FIELD("open_action_field", "Open Action Field"),
    EXPAND_NOTIFICATIONS("expand_notifications", "Expand Notification Shade"),
    SWITCH_SPACES("switch_spaces", "Switch Active Space"),
    OPEN_SPACES_OVERVIEW("open_spaces_overview", "Spaces & Activities Overview"),
    OPEN_APP_INDEX("open_app_index", "Open Application Index"),
    ENTER_EDIT_MODE("enter_edit_mode", "Enter Home Layout Editor"),
    OPEN_UNIVERSAL_SEARCH("open_universal_search", "Open Universal Search"),
    OPEN_ASSISTANT("open_assistant", "Open FLUX Local Assistant"),
    LOCK_OR_DIM_OVERLAY("lock_or_dim_overlay", "AMOLED Rest / Lock Prompt", "True hardware screen-off requires Device Admin or AccessibilityService; falls back to AMOLED Dim Screen & System Lock settings."),
    DISABLED("disabled", "Disabled");

    companion object {
        fun fromId(id: String): GestureTargetAction =
            entries.firstOrNull { it.id == id } ?: OPEN_ACTION_FIELD
    }
}

data class FluxSettingsState(
    val isFirstRunInitialized: Boolean = false,
    val themeMode: FluxThemeMode = FluxThemeMode.DARK,
    val accentPalette: FluxAccentPalette = FluxAccentPalette.ELECTRIC_BLUE,
    val surfaceOpacity: Float = 0.90f,
    val cornerRadiusDp: Int = 18,
    val showThinBorders: Boolean = true,
    val animationIntensity: Float = 1.0f,
    val iconShape: IconShapeOption = IconShapeOption.SQUIRCLE,
    val iconSizeDp: Int = 52,
    val showAppLabels: Boolean = true,
    val selectedIconPackPackage: String = "",
    val homeLayoutStyle: HomeLayoutStyle = HomeLayoutStyle.CANVAS,
    val layoutDensity: LayoutDensityOption = LayoutDensityOption.BALANCED,
    val showWeatherHeader: Boolean = true,
    val showRecentContinuity: Boolean = true,
    val showPinnedPeopleSection: Boolean = true,
    val showQuickDock: Boolean = true,
    val showWidgetsSection: Boolean = true,
    val swipeUpAction: GestureTargetAction = GestureTargetAction.OPEN_ACTION_FIELD,
    val swipeDownAction: GestureTargetAction = GestureTargetAction.EXPAND_NOTIFICATIONS,
    val swipeHorizontalAction: GestureTargetAction = GestureTargetAction.SWITCH_SPACES,
    val doubleTapAction: GestureTargetAction = GestureTargetAction.LOCK_OR_DIM_OVERLAY,
    val pinchInAction: GestureTargetAction = GestureTargetAction.OPEN_SPACES_OVERVIEW,
    val pinchOutAction: GestureTargetAction = GestureTargetAction.OPEN_APP_INDEX,
    val longPressAction: GestureTargetAction = GestureTargetAction.ENTER_EDIT_MODE,
    val enablePersonalizedSuggestions: Boolean = true,
    val enableLocalAssistant: Boolean = true,
    val recordRecentHistory: Boolean = true,
    val enableBackgroundWeatherRefresh: Boolean = true,
    val dismissedSuggestionIds: Set<String> = emptySet(),
    val weatherCity: String = "San Francisco",
    val weatherLatitude: Float = 37.7749f,
    val weatherLongitude: Float = -122.4194f,
    val cachedTempC: Int = 21,
    val cachedWeatherSummary: String = "Clear Sky"
)

class FluxPreferencesRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val FIRST_RUN_INIT = booleanPreferencesKey("first_run_init")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val SURFACE_OPACITY = floatPreferencesKey("surface_opacity")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val SHOW_THIN_BORDERS = booleanPreferencesKey("show_thin_borders")
        val ANIMATION_INTENSITY = floatPreferencesKey("animation_intensity")
        val ICON_SHAPE = stringPreferencesKey("icon_shape")
        val ICON_SIZE_DP = intPreferencesKey("icon_size_dp")
        val SHOW_APP_LABELS = booleanPreferencesKey("show_app_labels")
        val ICON_PACK_PKG = stringPreferencesKey("icon_pack_pkg")
        val HOME_LAYOUT_STYLE = stringPreferencesKey("home_layout_style")
        val LAYOUT_DENSITY = stringPreferencesKey("layout_density")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_RECENT = booleanPreferencesKey("show_recent")
        val SHOW_PEOPLE = booleanPreferencesKey("show_people")
        val SHOW_DOCK = booleanPreferencesKey("show_dock")
        val SHOW_WIDGETS = booleanPreferencesKey("show_widgets")
        val SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
        val SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
        val SWIPE_HORIZONTAL = stringPreferencesKey("gesture_swipe_horizontal")
        val DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
        val PINCH_IN = stringPreferencesKey("gesture_pinch_in")
        val PINCH_OUT = stringPreferencesKey("gesture_pinch_out")
        val LONG_PRESS = stringPreferencesKey("gesture_long_press")
        val ENABLE_SUGGESTIONS = booleanPreferencesKey("enable_suggestions")
        val ENABLE_ASSISTANT = booleanPreferencesKey("enable_assistant")
        val RECORD_RECENT = booleanPreferencesKey("record_recent")
        val ENABLE_BG_WEATHER = booleanPreferencesKey("enable_bg_weather")
        val DISMISSED_SUGGESTIONS = stringPreferencesKey("dismissed_suggestions")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val WEATHER_LAT = floatPreferencesKey("weather_lat")
        val WEATHER_LON = floatPreferencesKey("weather_lon")
        val CACHED_TEMP_C = intPreferencesKey("cached_temp_c")
        val CACHED_WEATHER_SUMMARY = stringPreferencesKey("cached_weather_summary")
    }

    val settingsFlow: Flow<FluxSettingsState> = dataStore.data.map { prefs ->
        val dismissedRaw = prefs[Keys.DISMISSED_SUGGESTIONS].orEmpty()
        val dismissedSet = if (dismissedRaw.isBlank()) emptySet() else dismissedRaw.split(",").toSet()
        FluxSettingsState(
            isFirstRunInitialized = prefs[Keys.FIRST_RUN_INIT] ?: false,
            themeMode = FluxThemeMode.fromId(prefs[Keys.THEME_MODE] ?: FluxThemeMode.DARK.id),
            accentPalette = FluxAccentPalette.fromId(prefs[Keys.ACCENT_PALETTE] ?: FluxAccentPalette.ELECTRIC_BLUE.id),
            surfaceOpacity = prefs[Keys.SURFACE_OPACITY] ?: 0.90f,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: 18,
            showThinBorders = prefs[Keys.SHOW_THIN_BORDERS] ?: true,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY] ?: 1.0f,
            iconShape = IconShapeOption.fromId(prefs[Keys.ICON_SHAPE] ?: IconShapeOption.SQUIRCLE.id),
            iconSizeDp = prefs[Keys.ICON_SIZE_DP] ?: 52,
            showAppLabels = prefs[Keys.SHOW_APP_LABELS] ?: true,
            selectedIconPackPackage = prefs[Keys.ICON_PACK_PKG] ?: "",
            homeLayoutStyle = HomeLayoutStyle.fromId(prefs[Keys.HOME_LAYOUT_STYLE] ?: HomeLayoutStyle.CANVAS.id),
            layoutDensity = LayoutDensityOption.fromId(prefs[Keys.LAYOUT_DENSITY] ?: LayoutDensityOption.BALANCED.id),
            showWeatherHeader = prefs[Keys.SHOW_WEATHER] ?: true,
            showRecentContinuity = prefs[Keys.SHOW_RECENT] ?: true,
            showPinnedPeopleSection = prefs[Keys.SHOW_PEOPLE] ?: true,
            showQuickDock = prefs[Keys.SHOW_DOCK] ?: true,
            showWidgetsSection = prefs[Keys.SHOW_WIDGETS] ?: true,
            swipeUpAction = GestureTargetAction.fromId(prefs[Keys.SWIPE_UP] ?: GestureTargetAction.OPEN_ACTION_FIELD.id),
            swipeDownAction = GestureTargetAction.fromId(prefs[Keys.SWIPE_DOWN] ?: GestureTargetAction.EXPAND_NOTIFICATIONS.id),
            swipeHorizontalAction = GestureTargetAction.fromId(prefs[Keys.SWIPE_HORIZONTAL] ?: GestureTargetAction.SWITCH_SPACES.id),
            doubleTapAction = GestureTargetAction.fromId(prefs[Keys.DOUBLE_TAP] ?: GestureTargetAction.LOCK_OR_DIM_OVERLAY.id),
            pinchInAction = GestureTargetAction.fromId(prefs[Keys.PINCH_IN] ?: GestureTargetAction.OPEN_SPACES_OVERVIEW.id),
            pinchOutAction = GestureTargetAction.fromId(prefs[Keys.PINCH_OUT] ?: GestureTargetAction.OPEN_APP_INDEX.id),
            longPressAction = GestureTargetAction.fromId(prefs[Keys.LONG_PRESS] ?: GestureTargetAction.ENTER_EDIT_MODE.id),
            enablePersonalizedSuggestions = prefs[Keys.ENABLE_SUGGESTIONS] ?: true,
            enableLocalAssistant = prefs[Keys.ENABLE_ASSISTANT] ?: true,
            recordRecentHistory = prefs[Keys.RECORD_RECENT] ?: true,
            enableBackgroundWeatherRefresh = prefs[Keys.ENABLE_BG_WEATHER] ?: true,
            dismissedSuggestionIds = dismissedSet,
            weatherCity = prefs[Keys.WEATHER_CITY] ?: "San Francisco",
            weatherLatitude = prefs[Keys.WEATHER_LAT] ?: 37.7749f,
            weatherLongitude = prefs[Keys.WEATHER_LON] ?: -122.4194f,
            cachedTempC = prefs[Keys.CACHED_TEMP_C] ?: 21,
            cachedWeatherSummary = prefs[Keys.CACHED_WEATHER_SUMMARY] ?: "Clear Sky"
        )
    }

    suspend fun markFirstRunInitialized() {
        dataStore.edit { it[Keys.FIRST_RUN_INIT] = true }
    }

    suspend fun setThemeMode(mode: FluxThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.id }
    }

    suspend fun setAccentPalette(palette: FluxAccentPalette) {
        dataStore.edit { it[Keys.ACCENT_PALETTE] = palette.id }
    }

    suspend fun setSurfaceOpacity(opacity: Float) {
        dataStore.edit { it[Keys.SURFACE_OPACITY] = opacity.coerceIn(0.45f, 1.0f) }
    }

    suspend fun setCornerRadiusDp(radiusDp: Int) {
        dataStore.edit { it[Keys.CORNER_RADIUS] = radiusDp.coerceIn(0, 32) }
    }

    suspend fun setShowThinBorders(enabled: Boolean) {
        dataStore.edit { it[Keys.SHOW_THIN_BORDERS] = enabled }
    }

    suspend fun setAnimationIntensity(intensity: Float) {
        dataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.coerceIn(0f, 1.5f) }
    }

    suspend fun setIconShape(shape: IconShapeOption) {
        dataStore.edit { it[Keys.ICON_SHAPE] = shape.id }
    }

    suspend fun setIconSizeDp(sizeDp: Int) {
        dataStore.edit { it[Keys.ICON_SIZE_DP] = sizeDp.coerceIn(40, 68) }
    }

    suspend fun setShowAppLabels(show: Boolean) {
        dataStore.edit { it[Keys.SHOW_APP_LABELS] = show }
    }

    suspend fun setSelectedIconPack(packageName: String) {
        dataStore.edit { it[Keys.ICON_PACK_PKG] = packageName }
    }

    suspend fun setHomeLayoutStyle(style: HomeLayoutStyle) {
        dataStore.edit { it[Keys.HOME_LAYOUT_STYLE] = style.id }
    }

    suspend fun setLayoutDensity(density: LayoutDensityOption) {
        dataStore.edit { it[Keys.LAYOUT_DENSITY] = density.id }
    }

    suspend fun setSectionVisibility(
        weather: Boolean? = null,
        recent: Boolean? = null,
        people: Boolean? = null,
        dock: Boolean? = null,
        widgets: Boolean? = null
    ) {
        dataStore.edit { prefs ->
            weather?.let { prefs[Keys.SHOW_WEATHER] = it }
            recent?.let { prefs[Keys.SHOW_RECENT] = it }
            people?.let { prefs[Keys.SHOW_PEOPLE] = it }
            dock?.let { prefs[Keys.SHOW_DOCK] = it }
            widgets?.let { prefs[Keys.SHOW_WIDGETS] = it }
        }
    }

    suspend fun updateGestureMapping(gestureKey: String, target: GestureTargetAction) {
        dataStore.edit { prefs ->
            when (gestureKey) {
                "swipe_up" -> prefs[Keys.SWIPE_UP] = target.id
                "swipe_down" -> prefs[Keys.SWIPE_DOWN] = target.id
                "swipe_horizontal" -> prefs[Keys.SWIPE_HORIZONTAL] = target.id
                "double_tap" -> prefs[Keys.DOUBLE_TAP] = target.id
                "pinch_in" -> prefs[Keys.PINCH_IN] = target.id
                "pinch_out" -> prefs[Keys.PINCH_OUT] = target.id
                "long_press" -> prefs[Keys.LONG_PRESS] = target.id
            }
        }
    }

    suspend fun setPrivacyAndAiOptions(
        personalizedSuggestions: Boolean? = null,
        localAssistant: Boolean? = null,
        recordRecent: Boolean? = null,
        backgroundWeather: Boolean? = null
    ) {
        dataStore.edit { prefs ->
            personalizedSuggestions?.let { prefs[Keys.ENABLE_SUGGESTIONS] = it }
            localAssistant?.let { prefs[Keys.ENABLE_ASSISTANT] = it }
            recordRecent?.let { prefs[Keys.RECORD_RECENT] = it }
            backgroundWeather?.let { prefs[Keys.ENABLE_BG_WEATHER] = it }
        }
    }

    suspend fun dismissSuggestion(suggestionId: String) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.DISMISSED_SUGGESTIONS].orEmpty()
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableSet()
            current.add(suggestionId)
            prefs[Keys.DISMISSED_SUGGESTIONS] = current.joinToString(",")
        }
    }

    suspend fun resetDismissedSuggestions() {
        dataStore.edit { it[Keys.DISMISSED_SUGGESTIONS] = "" }
    }

    suspend fun updateWeatherCache(city: String, lat: Float, lon: Float, tempC: Int, summary: String) {
        dataStore.edit { prefs ->
            prefs[Keys.WEATHER_CITY] = city
            prefs[Keys.WEATHER_LAT] = lat
            prefs[Keys.WEATHER_LON] = lon
            prefs[Keys.CACHED_TEMP_C] = tempC
            prefs[Keys.CACHED_WEATHER_SUMMARY] = summary
        }
    }

    suspend fun resetAllCustomizationsToDefault() {
        dataStore.edit { prefs ->
            val initialized = prefs[Keys.FIRST_RUN_INIT] ?: true
            prefs.clear()
            prefs[Keys.FIRST_RUN_INIT] = initialized
        }
    }

    fun exportConfigurationJson(state: FluxSettingsState): String {
        val root = JSONObject()
        root.put("flux_schema_version", 1)
        root.put("themeMode", state.themeMode.id)
        root.put("accentPalette", state.accentPalette.id)
        root.put("surfaceOpacity", state.surfaceOpacity.toDouble())
        root.put("cornerRadiusDp", state.cornerRadiusDp)
        root.put("showThinBorders", state.showThinBorders)
        root.put("animationIntensity", state.animationIntensity.toDouble())
        root.put("iconShape", state.iconShape.id)
        root.put("iconSizeDp", state.iconSizeDp)
        root.put("showAppLabels", state.showAppLabels)
        root.put("homeLayoutStyle", state.homeLayoutStyle.id)
        root.put("layoutDensity", state.layoutDensity.id)
        root.put("showWeatherHeader", state.showWeatherHeader)
        root.put("showRecentContinuity", state.showRecentContinuity)
        root.put("showPinnedPeopleSection", state.showPinnedPeopleSection)
        root.put("showQuickDock", state.showQuickDock)
        root.put("showWidgetsSection", state.showWidgetsSection)
        root.put("swipeUpAction", state.swipeUpAction.id)
        root.put("swipeDownAction", state.swipeDownAction.id)
        root.put("doubleTapAction", state.doubleTapAction.id)
        root.put("pinchInAction", state.pinchInAction.id)
        root.put("pinchOutAction", state.pinchOutAction.id)
        root.put("longPressAction", state.longPressAction.id)
        return root.toString(2)
    }

    suspend fun importConfigurationJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            dataStore.edit { prefs ->
                if (root.has("themeMode")) prefs[Keys.THEME_MODE] = root.getString("themeMode")
                if (root.has("accentPalette")) prefs[Keys.ACCENT_PALETTE] = root.getString("accentPalette")
                if (root.has("surfaceOpacity")) prefs[Keys.SURFACE_OPACITY] = root.getDouble("surfaceOpacity").toFloat().coerceIn(0.45f, 1.0f)
                if (root.has("cornerRadiusDp")) prefs[Keys.CORNER_RADIUS] = root.getInt("cornerRadiusDp").coerceIn(0, 32)
                if (root.has("showThinBorders")) prefs[Keys.SHOW_THIN_BORDERS] = root.getBoolean("showThinBorders")
                if (root.has("animationIntensity")) prefs[Keys.ANIMATION_INTENSITY] = root.getDouble("animationIntensity").toFloat().coerceIn(0f, 1.5f)
                if (root.has("iconShape")) prefs[Keys.ICON_SHAPE] = root.getString("iconShape")
                if (root.has("iconSizeDp")) prefs[Keys.ICON_SIZE_DP] = root.getInt("iconSizeDp").coerceIn(40, 68)
                if (root.has("showAppLabels")) prefs[Keys.SHOW_APP_LABELS] = root.getBoolean("showAppLabels")
                if (root.has("homeLayoutStyle")) prefs[Keys.HOME_LAYOUT_STYLE] = root.getString("homeLayoutStyle")
                if (root.has("layoutDensity")) prefs[Keys.LAYOUT_DENSITY] = root.getString("layoutDensity")
                if (root.has("showWeatherHeader")) prefs[Keys.SHOW_WEATHER] = root.getBoolean("showWeatherHeader")
                if (root.has("showRecentContinuity")) prefs[Keys.SHOW_RECENT] = root.getBoolean("showRecentContinuity")
                if (root.has("showPinnedPeopleSection")) prefs[Keys.SHOW_PEOPLE] = root.getBoolean("showPinnedPeopleSection")
                if (root.has("showQuickDock")) prefs[Keys.SHOW_DOCK] = root.getBoolean("showQuickDock")
                if (root.has("showWidgetsSection")) prefs[Keys.SHOW_WIDGETS] = root.getBoolean("showWidgetsSection")
                if (root.has("swipeUpAction")) prefs[Keys.SWIPE_UP] = root.getString("swipeUpAction")
                if (root.has("swipeDownAction")) prefs[Keys.SWIPE_DOWN] = root.getString("swipeDownAction")
                if (root.has("doubleTapAction")) prefs[Keys.DOUBLE_TAP] = root.getString("doubleTapAction")
                if (root.has("pinchInAction")) prefs[Keys.PINCH_IN] = root.getString("pinchInAction")
                if (root.has("pinchOutAction")) prefs[Keys.PINCH_OUT] = root.getString("pinchOutAction")
                if (root.has("longPressAction")) prefs[Keys.LONG_PRESS] = root.getString("longPressAction")
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
