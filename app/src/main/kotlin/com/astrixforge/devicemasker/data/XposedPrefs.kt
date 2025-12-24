package com.astrixforge.devicemasker.data

import android.content.Context
import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SpoofType

/**
 * Xposed Preferences Writer - Writes to SharedPreferences with MODE_WORLD_READABLE.
 *
 * This is the ONLY way to share configuration with Xposed/LSPosed modules:
 * - UI app writes with MODE_WORLD_READABLE
 * - Hooked apps read via XSharedPreferences (prefs() in YukiHookAPI)
 *
 * IMPORTANT: LSPosed automatically handles making these prefs readable
 * when xposedsharedprefs=true in AndroidManifest.xml.
 *
 * Note: DataStore is NOT compatible with XSharedPreferences!
 */
@Suppress("DEPRECATION") // MODE_WORLD_READABLE is intentionally used for Xposed
class XposedPrefs(context: Context) {

    companion object {
        // Note: Prefs file name is: {packageName}_preferences (set at runtime)

        // Keys for global settings
        const val KEY_MODULE_ENABLED = "module_enabled"
        const val KEY_DEBUG_ENABLED = "debug_enabled"
        const val KEY_CONFIG_VERSION = "config_version"
        const val KEY_ENABLED_APPS = "enabled_apps"

        // Key prefixes for per-app settings
        private const val PREFIX_APP_ENABLED = "app_enabled_"
        private const val PREFIX_SPOOF_ENABLED = "spoof_enabled_"
        private const val PREFIX_SPOOF_VALUE = "spoof_"

        /**
         * Gets the key for app enabled status.
         */
        fun getAppEnabledKey(packageName: String): String {
            return "$PREFIX_APP_ENABLED${packageName.replace('.', '_')}"
        }

        /**
         * Gets the key for spoof type enabled status.
         */
        fun getSpoofEnabledKey(packageName: String, type: SpoofType): String {
            return "$PREFIX_SPOOF_ENABLED${packageName.replace('.', '_')}_${type.name}"
        }

        /**
         * Gets the key for spoof value.
         */
        fun getSpoofValueKey(packageName: String, type: SpoofType): String {
            return "$PREFIX_SPOOF_VALUE${packageName.replace('.', '_')}_${type.name}"
        }
    }

    /**
     * SharedPreferences with MODE_WORLD_READABLE for Xposed access.
     * LSPosed handles the world-readable aspect when xposedsharedprefs=true.
     */
    private val prefs: SharedPreferences = try {
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_WORLD_READABLE
        )
    } catch (e: SecurityException) {
        // Fallback for non-Xposed contexts (shouldn't happen in production)
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Master switch for the module. */
    var isModuleEnabled: Boolean
        get() = prefs.getBoolean(KEY_MODULE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MODULE_ENABLED, value).apply()

    /** Debug logging enabled. */
    var isDebugEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEBUG_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEBUG_ENABLED, value).apply()

    /** Config version. */
    var configVersion: Int
        get() = prefs.getInt(KEY_CONFIG_VERSION, 1)
        set(value) = prefs.edit().putInt(KEY_CONFIG_VERSION, value).apply()

    // ═══════════════════════════════════════════════════════════
    // PER-APP SETTINGS
    // ═══════════════════════════════════════════════════════════

    /**
     * Sets whether spoofing is enabled for an app.
     */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putString(getAppEnabledKey(packageName), enabled.toString()).apply()
    }

    /**
     * Gets whether spoofing is enabled for an app.
     */
    fun isAppEnabled(packageName: String): Boolean {
        return prefs.getString(getAppEnabledKey(packageName), "true") == "true"
    }

    /**
     * Sets whether a specific spoof type is enabled for an app.
     */
    fun setSpoofTypeEnabled(packageName: String, type: SpoofType, enabled: Boolean) {
        prefs.edit().putString(getSpoofEnabledKey(packageName, type), enabled.toString()).apply()
    }

    /**
     * Gets whether a specific spoof type is enabled for an app.
     */
    fun isSpoofTypeEnabled(packageName: String, type: SpoofType): Boolean {
        return prefs.getString(getSpoofEnabledKey(packageName, type), "false") == "true"
    }

    /**
     * Sets the spoof value for a specific type and app.
     */
    fun setSpoofValue(packageName: String, type: SpoofType, value: String?) {
        if (value != null) {
            prefs.edit().putString(getSpoofValueKey(packageName, type), value).apply()
        } else {
            prefs.edit().remove(getSpoofValueKey(packageName, type)).apply()
        }
    }

    /**
     * Gets the spoof value for a specific type and app.
     */
    fun getSpoofValue(packageName: String, type: SpoofType): String? {
        return prefs.getString(getSpoofValueKey(packageName, type), null)
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Configures spoofing for an app.
     */
    fun configureApp(
        packageName: String,
        enabled: Boolean,
        spoofTypes: Map<SpoofType, String?>
    ) {
        val editor = prefs.edit()

        // Set app enabled
        editor.putString(getAppEnabledKey(packageName), enabled.toString())

        // Set each spoof type
        for ((type, value) in spoofTypes) {
            editor.putString(getSpoofEnabledKey(packageName, type), (value != null).toString())
            if (value != null) {
                editor.putString(getSpoofValueKey(packageName, type), value)
            } else {
                editor.remove(getSpoofValueKey(packageName, type))
            }
        }

        editor.apply()
    }

    /**
     * Clears all settings for an app.
     */
    fun clearAppSettings(packageName: String) {
        val editor = prefs.edit()
        val prefix = packageName.replace('.', '_')

        // Get all keys and remove ones matching this package
        val allKeys = prefs.all.keys.filter { it.contains(prefix) }
        for (key in allKeys) {
            editor.remove(key)
        }

        editor.apply()
    }

    /**
     * Triggers a config update notification for hooked apps.
     * Increments the version so apps know to reload.
     */
    fun notifyConfigChanged() {
        configVersion++
    }
}
