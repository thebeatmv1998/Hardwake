package com.wecker.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wecker.app.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** App-weite Einstellungen: Sprache, Theme, eigene Farbe, Schlummern, Vibration. */
class SettingsRepository(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val colorKey = intPreferencesKey("color_scheme")
    private val customColorKey = intPreferencesKey("custom_primary")
    private val languageKey = stringPreferencesKey("language")
    private val snoozeKey = intPreferencesKey("snooze_minutes")
    private val vibrateKey = booleanPreferencesKey("vibrate")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        prefs[themeKey]
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
    }

    /** Index in [com.wecker.app.ui.theme.AppPalettes] oder -1 für die eigene Farbe. */
    val colorScheme: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[colorKey] ?: 0
    }

    /** Eigene Primärfarbe als ARGB, null wenn noch keine gewählt wurde. */
    val customPrimary: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[customColorKey]
    }

    val language: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        prefs[languageKey]
            ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
            ?: AppLanguage.SYSTEM
    }

    val snoozeMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[snoozeKey] ?: 10
    }

    val vibrate: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[vibrateKey] ?: true
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setColorScheme(index: Int) {
        context.dataStore.edit { it[colorKey] = index }
    }

    suspend fun setCustomPrimary(argb: Int?) {
        context.dataStore.edit { prefs ->
            if (argb == null) prefs.remove(customColorKey) else prefs[customColorKey] = argb
        }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[languageKey] = language.name }
    }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[snoozeKey] = minutes.coerceIn(5, 30) }
    }

    suspend fun setVibrate(enabled: Boolean) {
        context.dataStore.edit { it[vibrateKey] = enabled }
    }
}
