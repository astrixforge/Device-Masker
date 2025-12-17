package com.astrixforge.devicemasker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * DataStore extension for module preferences. Uses 'world_readable' file for Xposed module
 * inter-process access.
 */
val Context.spoofDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "spoof_preferences")

/**
 * DataStore manager for spoofed values and module configuration.
 *
 * Provides both suspend functions (for UI/coroutine context) and blocking functions (for hook
 * context where coroutines aren't available).
 *
 * @param context Application context for DataStore access
 */
class SpoofDataStore(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.spoofDataStore

    // ═══════════════════════════════════════════════════════════
    // PREFERENCE KEYS
    // ═══════════════════════════════════════════════════════════

    companion object {
        // Global settings
        val KEY_MODULE_ENABLED = booleanPreferencesKey("module_enabled")
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")

        // Spoof value keys (prefixed by type)
        private fun spoofValueKey(type: SpoofType): Preferences.Key<String> {
            return stringPreferencesKey("spoof_value_${type.name}")
        }

        private fun spoofEnabledKey(type: SpoofType): Preferences.Key<Boolean> {
            return booleanPreferencesKey("spoof_enabled_${type.name}")
        }

        // Profile storage (JSON serialized)
        val KEY_PROFILES_JSON = stringPreferencesKey("profiles_json")

        // App config storage (JSON serialized)
        val KEY_APP_CONFIGS_JSON = stringPreferencesKey("app_configs_json")

        // Per-app keys (dynamically generated)
        private fun appEnabledKey(packageName: String): Preferences.Key<Boolean> {
            return booleanPreferencesKey("app_enabled_$packageName")
        }

        private fun appProfileKey(packageName: String): Preferences.Key<String> {
            return stringPreferencesKey("app_profile_$packageName")
        }

        // Theme settings
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_AMOLED_MODE = booleanPreferencesKey("theme_amoled_mode")
        val KEY_DYNAMIC_COLORS = booleanPreferencesKey("theme_dynamic_colors")
        val KEY_DEBUG_LOGGING = booleanPreferencesKey("debug_logging")

        // Migration version
        val KEY_MIGRATION_VERSION = stringPreferencesKey("migration_version")
    }

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Flow of module enabled state. */
    val moduleEnabled: Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[KEY_MODULE_ENABLED] ?: true }

    /** Flow of active profile ID. */
    val activeProfileId: Flow<String?> =
            dataStore.data.map { prefs -> prefs[KEY_ACTIVE_PROFILE_ID] }

    /** Sets the module enabled state. */
    suspend fun setModuleEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_MODULE_ENABLED] = enabled }
    }

    /** Sets the active profile ID. */
    suspend fun setActiveProfileId(profileId: String?) {
        dataStore.edit { prefs ->
            if (profileId != null) {
                prefs[KEY_ACTIVE_PROFILE_ID] = profileId
            } else {
                prefs.remove(KEY_ACTIVE_PROFILE_ID)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // THEME SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Flow of theme mode (SYSTEM, LIGHT, DARK). */
    val themeMode: Flow<ThemeMode> =
            dataStore.data.map { prefs ->
                val modeName = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
                try {
                    ThemeMode.valueOf(modeName)
                } catch (e: IllegalArgumentException) {
                    ThemeMode.SYSTEM
                }
            }

    /** Flow of AMOLED mode enabled state. */
    val amoledMode: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[KEY_AMOLED_MODE] ?: true // Default true (AMOLED on)
            }

    /** Flow of dynamic colors enabled state. */
    val dynamicColors: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[KEY_DYNAMIC_COLORS] ?: true // Default true (dynamic colors on)
            }

    /** Flow of debug logging enabled state. */
    val debugLogging: Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[KEY_DEBUG_LOGGING] ?: false // Default false
            }

    /** Sets theme mode. */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    /** Sets AMOLED mode. */
    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_AMOLED_MODE] = enabled }
    }

    /** Sets dynamic colors. */
    suspend fun setDynamicColors(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DYNAMIC_COLORS] = enabled }
    }

    /** Sets debug logging. */
    suspend fun setDebugLogging(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DEBUG_LOGGING] = enabled }
    }

    // ═══════════════════════════════════════════════════════════
    // SPOOF VALUES (Per-Type)
    // ═══════════════════════════════════════════════════════════

    /** Flow of a specific spoof value. */
    fun getSpoofValueFlow(type: SpoofType): Flow<String?> =
            dataStore.data.map { prefs -> prefs[spoofValueKey(type)] }

    /** Flow of whether a spoof type is enabled. */
    fun isSpoofEnabledFlow(type: SpoofType): Flow<Boolean> =
            dataStore.data.map { prefs -> prefs[spoofEnabledKey(type)] ?: true }

    /** Sets a spoof value for a specific type. */
    suspend fun setSpoofValue(type: SpoofType, value: String?) {
        dataStore.edit { prefs ->
            if (value != null) {
                prefs[spoofValueKey(type)] = value
            } else {
                prefs.remove(spoofValueKey(type))
            }
        }
    }

    /** Sets whether a spoof type is enabled. */
    suspend fun setSpoofEnabled(type: SpoofType, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[spoofEnabledKey(type)] = enabled }
    }

    /** Sets multiple spoof values at once. */
    suspend fun setSpoofValues(values: Map<SpoofType, String?>) {
        dataStore.edit { prefs ->
            values.forEach { (type, value) ->
                if (value != null) {
                    prefs[spoofValueKey(type)] = value
                } else {
                    prefs.remove(spoofValueKey(type))
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PER-APP SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Flow of whether spoofing is enabled for an app. */
    fun isAppEnabledFlow(packageName: String): Flow<Boolean> =
            dataStore.data.map { prefs ->
                prefs[appEnabledKey(packageName)] ?: false // Default disabled for safety
            }

    /** Flow of the profile ID assigned to an app. */
    fun getAppProfileIdFlow(packageName: String): Flow<String?> =
            dataStore.data.map { prefs -> prefs[appProfileKey(packageName)] }

    /** Enables or disables spoofing for an app. */
    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        dataStore.edit { prefs -> prefs[appEnabledKey(packageName)] = enabled }
    }

    /** Assigns a profile to an app. */
    suspend fun setAppProfile(packageName: String, profileId: String?) {
        dataStore.edit { prefs ->
            if (profileId != null) {
                prefs[appProfileKey(packageName)] = profileId
            } else {
                prefs.remove(appProfileKey(packageName))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // JSON STORAGE (Profiles & App Configs)
    // ═══════════════════════════════════════════════════════════

    /** Flow of serialized profiles JSON. */
    val profilesJson: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_PROFILES_JSON] }

    /** Flow of serialized app configs JSON. */
    val appConfigsJson: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_APP_CONFIGS_JSON] }

    /** Saves profiles as JSON. */
    suspend fun saveProfilesJson(json: String) {
        dataStore.edit { prefs -> prefs[KEY_PROFILES_JSON] = json }
    }

    /** Saves app configs as JSON. */
    suspend fun saveAppConfigsJson(json: String) {
        dataStore.edit { prefs -> prefs[KEY_APP_CONFIGS_JSON] = json }
    }

    /** Gets profiles JSON synchronously (blocking) for hook context. */
    fun getProfilesJsonBlocking(): String? {
        return runBlocking { profilesJson.first() }
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets a spoof value synchronously (blocking). Use only in hook context where coroutines aren't
     * available.
     */
    fun getSpoofValueBlocking(type: SpoofType): String? {
        return runBlocking { getSpoofValueFlow(type).first() }
    }

    /** Checks if a spoof type is enabled synchronously (blocking). */
    fun isSpoofEnabledBlocking(type: SpoofType): Boolean {
        return runBlocking { isSpoofEnabledFlow(type).first() }
    }

    /** Checks if module is enabled synchronously (blocking). */
    fun isModuleEnabledBlocking(): Boolean {
        return runBlocking { moduleEnabled.first() }
    }

    /** Checks if spoofing is enabled for an app synchronously (blocking). */
    fun isAppEnabledBlocking(packageName: String): Boolean {
        return runBlocking { isAppEnabledFlow(packageName).first() }
    }

    /** Gets the profile ID for an app synchronously (blocking). */
    fun getAppProfileIdBlocking(packageName: String): String? {
        return runBlocking { getAppProfileIdFlow(packageName).first() }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /** Clears all preferences. */
    suspend fun clearAll() {
        dataStore.edit { prefs -> prefs.clear() }
    }

    /** Gets all current preferences as a map (for debugging). */
    suspend fun getAllPreferences(): Map<Preferences.Key<*>, Any> {
        return dataStore.data.first().asMap()
    }
}
