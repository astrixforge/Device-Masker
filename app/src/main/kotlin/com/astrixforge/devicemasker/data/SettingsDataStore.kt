package com.astrixforge.devicemasker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore for UI/app settings only.
 *
 * In the Multi-Module architecture, this is separate from hook configuration. Hook config uses
 * JsonConfig via ConfigManager. This store only handles UI preferences like theme settings.
 */
private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.settingsDataStore

    // Preference keys
    private object Keys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
        val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    }

    // ═══════════════════════════════════════════════════════════
    // THEME SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Flow of theme mode preference. */
    val themeMode: Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            when (prefs[Keys.THEME_MODE]) {
                0 -> ThemeMode.SYSTEM
                1 -> ThemeMode.LIGHT
                2 -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    /** Sets the theme mode. */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] =
                when (mode) {
                    ThemeMode.SYSTEM -> 0
                    ThemeMode.LIGHT -> 1
                    ThemeMode.DARK -> 2
                }
        }
    }

    /** Flow of AMOLED dark mode preference. */
    val amoledMode: Flow<Boolean> = dataStore.data.map { prefs -> prefs[Keys.AMOLED_MODE] ?: true }

    /** Sets AMOLED dark mode. */
    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AMOLED_MODE] = enabled }
    }

    /** Flow of dynamic colors preference. */
    val dynamicColors: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[Keys.DYNAMIC_COLORS] ?: true }

    /** Sets dynamic colors. */
    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.DYNAMIC_COLORS] = enabled }
    }
}
