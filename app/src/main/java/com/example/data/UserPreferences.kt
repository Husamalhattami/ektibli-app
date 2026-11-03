package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ContrastTheme(val id: String, val titleAr: String) {
    YELLOW_ON_BLACK("YELLOW_ON_BLACK", "أصفر على أسود (أعلى تباين)"),
    DARK_OLED("DARK_OLED", "أبيض على أسود داكن"),
    LIGHT_HIGH_CONTRAST("LIGHT_HIGH_CONTRAST", "أسود على أبيض ناصع"),
    NAVY_CYAN("NAVY_CYAN", "أزرق ليلي وسماوي"),
    EMERALD_DARK("EMERALD_DARK", "أخضر زمردي داكن")
}

data class AppPreferences(
    val fontSizeSp: Int = 34,
    val theme: ContrastTheme = ContrastTheme.YELLOW_ON_BLACK,
    val vibrationEnabled: Boolean = true,
    val screenFlashEnabled: Boolean = true,
    val isBoldFont: Boolean = true,
    val languageCode: String = "ar-SA",
    val defaultSpeakerName: String = "المتحدث"
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ektibli_settings", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<AppPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        val themeId = prefs.getString("theme", ContrastTheme.YELLOW_ON_BLACK.id)
            ?: ContrastTheme.YELLOW_ON_BLACK.id
        val theme = ContrastTheme.entries.find { it.id == themeId } ?: ContrastTheme.YELLOW_ON_BLACK

        return AppPreferences(
            fontSizeSp = prefs.getInt("font_size", 34),
            theme = theme,
            vibrationEnabled = prefs.getBoolean("vibration", true),
            screenFlashEnabled = prefs.getBoolean("screen_flash", true),
            isBoldFont = prefs.getBoolean("bold_font", true),
            languageCode = prefs.getString("language", "ar-SA") ?: "ar-SA",
            defaultSpeakerName = prefs.getString("speaker_name", "المتحدث") ?: "المتحدث"
        )
    }

    fun setFontSize(size: Int) {
        val clamped = size.coerceIn(20, 56)
        prefs.edit().putInt("font_size", clamped).apply()
        _preferences.value = _preferences.value.copy(fontSizeSp = clamped)
    }

    fun setTheme(theme: ContrastTheme) {
        prefs.edit().putString("theme", theme.id).apply()
        _preferences.value = _preferences.value.copy(theme = theme)
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibration", enabled).apply()
        _preferences.value = _preferences.value.copy(vibrationEnabled = enabled)
    }

    fun setScreenFlashEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("screen_flash", enabled).apply()
        _preferences.value = _preferences.value.copy(screenFlashEnabled = enabled)
    }

    fun setBoldFont(enabled: Boolean) {
        prefs.edit().putBoolean("bold_font", enabled).apply()
        _preferences.value = _preferences.value.copy(isBoldFont = enabled)
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString("language", lang).apply()
        _preferences.value = _preferences.value.copy(languageCode = lang)
    }

    fun setSpeakerName(name: String) {
        prefs.edit().putString("speaker_name", name).apply()
        _preferences.value = _preferences.value.copy(defaultSpeakerName = name)
    }
}
