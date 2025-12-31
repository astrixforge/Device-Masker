package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.SharedPrefsKeys
import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData

/**
 * Prefs Keys for Cross-Process Configuration Sharing.
 *
 * ⚠️ DELEGATES to SharedPrefsKeys in :common module to ensure keys are IDENTICAL between app and
 * xposed module.
 *
 * Uses YukiHookAPI's YukiHookPrefsBridge which internally uses XSharedPreferences for cross-process
 * communication between the module app and hooked apps.
 *
 * IMPORTANT: Requires in AndroidManifest.xml: <meta-data android:name="xposedsharedprefs"
 * android:value="true" />
 */
object PrefsKeys {

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS (PrefsData for YukiHookAPI)
    // ═══════════════════════════════════════════════════════════

    /** Master switch for the entire module. */
    val MODULE_ENABLED = PrefsData(SharedPrefsKeys.KEY_MODULE_ENABLED, true)

    /** Debug logging enabled. */
    val DEBUG_ENABLED = PrefsData(SharedPrefsKeys.KEY_DEBUG_ENABLED, false)

    /** List of apps enabled for spoofing (comma-separated). */
    val ENABLED_APPS = PrefsData(SharedPrefsKeys.KEY_ENABLED_APPS, "")

    /** Config version for migration. */
    val CONFIG_VERSION = PrefsData(SharedPrefsKeys.KEY_CONFIG_VERSION, 1)

    // ═══════════════════════════════════════════════════════════
    // PER-APP KEYS - Delegate to SharedPrefsKeys
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the pref key for a specific app and spoof type. DELEGATES to SharedPrefsKeys for
     * consistency.
     */
    fun getSpoofKey(packageName: String, type: SpoofType): String {
        return SharedPrefsKeys.getSpoofValueKey(packageName, type)
    }

    /**
     * Gets the pref key for checking if a spoof type is enabled for an app. DELEGATES to
     * SharedPrefsKeys for consistency.
     */
    fun getSpoofEnabledKey(packageName: String, type: SpoofType): String {
        return SharedPrefsKeys.getSpoofEnabledKey(packageName, type)
    }

    /**
     * Gets the pref key for checking if an app is enabled for spoofing. DELEGATES to
     * SharedPrefsKeys for consistency.
     */
    fun getAppEnabledKey(packageName: String): String {
        return SharedPrefsKeys.getAppEnabledKey(packageName)
    }

    /**
     * Gets the device profile preset key for an app. DELEGATES to SharedPrefsKeys for consistency.
     */
    fun getDeviceProfileKey(packageName: String): String {
        return SharedPrefsKeys.getDeviceProfileKey(packageName)
    }
}
