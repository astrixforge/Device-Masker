package com.astrixforge.devicemasker.data

import android.content.Context
import android.content.SharedPreferences
import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType

/**
 * Xposed Preferences Writer - Writes to SharedPreferences with MODE_WORLD_READABLE.
 *
 * This is the ONLY way to share configuration with Xposed/LSPosed modules:
 * - UI app writes with MODE_WORLD_READABLE
 * - Hooked apps read via XSharedPreferences (prefs() in YukiHookAPI)
 *
 * ⚠️ CRITICAL: All key generation DELEGATES to SharedPrefsKeys in :common module to ensure keys are
 * IDENTICAL between app and xposed module.
 *
 * IMPORTANT: LSPosed automatically handles making these prefs readable when xposedsharedprefs=true
 * in AndroidManifest.xml.
 *
 * Note: DataStore is NOT compatible with XSharedPreferences!
 */
@Suppress("DEPRECATION") // MODE_WORLD_READABLE is intentionally used for Xposed
class XposedPrefs(context: Context) {

    companion object {
        // ═══════════════════════════════════════════════════════════
        // KEY GENERATORS - Delegate to SharedPrefsKeys
        // ═══════════════════════════════════════════════════════════

        fun getAppEnabledKey(packageName: String): String =
            SharedPrefsKeys.getAppEnabledKey(packageName)

        fun getSpoofEnabledKey(packageName: String, type: SpoofType): String =
            SharedPrefsKeys.getSpoofEnabledKey(packageName, type)

        fun getSpoofValueKey(packageName: String, type: SpoofType): String =
            SharedPrefsKeys.getSpoofValueKey(packageName, type)
    }

    /**
     * SharedPreferences with MODE_WORLD_READABLE for Xposed access. LSPosed handles the
     * world-readable aspect when xposedsharedprefs=true.
     */
    private val prefs: SharedPreferences =
        try {
            context.getSharedPreferences(
                context.packageName + "_preferences",
                Context.MODE_WORLD_READABLE,
            )
        } catch (e: SecurityException) {
            // Fallback for non-Xposed contexts (shouldn't happen in production)
            context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
        }

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Master switch for the module. */
    var isModuleEnabled: Boolean
        get() = prefs.getBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(SharedPrefsKeys.KEY_MODULE_ENABLED, value).apply()

    /** Debug logging enabled. */
    var isDebugEnabled: Boolean
        get() = prefs.getBoolean(SharedPrefsKeys.KEY_DEBUG_ENABLED, false)
        set(value) = prefs.edit().putBoolean(SharedPrefsKeys.KEY_DEBUG_ENABLED, value).apply()

    /** Config version - used for debugging, not for live reload (XSharedPreferences caches). */
    var configVersion: Int
        get() = prefs.getInt(SharedPrefsKeys.KEY_CONFIG_VERSION, 1)
        set(value) = prefs.edit().putInt(SharedPrefsKeys.KEY_CONFIG_VERSION, value).apply()

    // ═══════════════════════════════════════════════════════════
    // PER-APP SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Sets whether spoofing is enabled for an app. */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        prefs.edit().putString(getAppEnabledKey(packageName), enabled.toString()).apply()
    }

    /** Gets whether spoofing is enabled for an app. */
    fun isAppEnabled(packageName: String): Boolean {
        return prefs.getString(getAppEnabledKey(packageName), "true") == "true"
    }

    /** Sets whether a specific spoof type is enabled for an app. */
    fun setSpoofTypeEnabled(packageName: String, type: SpoofType, enabled: Boolean) {
        prefs.edit().putString(getSpoofEnabledKey(packageName, type), enabled.toString()).apply()
    }

    /** Gets whether a specific spoof type is enabled for an app. */
    fun isSpoofTypeEnabled(packageName: String, type: SpoofType): Boolean {
        return prefs.getString(getSpoofEnabledKey(packageName, type), "false") == "true"
    }

    /** Sets the spoof value for a specific type and app. */
    fun setSpoofValue(packageName: String, type: SpoofType, value: String?) {
        if (value != null) {
            prefs.edit().putString(getSpoofValueKey(packageName, type), value).apply()
        } else {
            prefs.edit().remove(getSpoofValueKey(packageName, type)).apply()
        }
    }

    /** Gets the spoof value for a specific type and app. */
    fun getSpoofValue(packageName: String, type: SpoofType): String? {
        return prefs.getString(getSpoofValueKey(packageName, type), null)
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /** Configures spoofing for an app. */
    fun configureApp(packageName: String, enabled: Boolean, spoofTypes: Map<SpoofType, String?>) {
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

    /** Clears all settings for an app. */
    fun clearAppSettings(packageName: String) {
        val editor = prefs.edit()
        val prefix = SharedPrefsKeys.sanitize(packageName)

        // Get all keys and remove ones matching this package
        val allKeys = prefs.all.keys.filter { it.contains(prefix) }
        for (key in allKeys) {
            editor.remove(key)
        }

        editor.apply()
    }

    /**
     * Increments config version.
     *
     * NOTE: This does NOT cause hooked apps to reload config! XSharedPreferences caches values.
     * Config changes require app restart. This version is only useful for debugging/logging
     * purposes.
     */
    fun notifyConfigChanged() {
        configVersion++
    }
}
