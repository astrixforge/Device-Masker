package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.xposed.prefs.data.PrefsData

/**
 * Prefs Keys for Cross-Process Configuration Sharing.
 *
 * Uses YukiHookAPI's YukiHookPrefsBridge which internally uses XSharedPreferences
 * for cross-process communication between the module app and hooked apps.
 *
 * This is the CORRECT way to share config in LSPosed modules:
 * - UI app writes using Context.getSharedPreferences with MODE_WORLD_READABLE
 * - Hooked apps read using prefs() which wraps XSharedPreferences
 *
 * IMPORTANT: Requires in AndroidManifest.xml:
 * <meta-data android:name="xposedsharedprefs" android:value="true" />
 */
@Suppress("unused") // API keys for XSharedPreferences - used dynamically
object PrefsKeys {

    // ═══════════════════════════════════════════════════════════
    // GLOBAL SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Master switch for the entire module. */
    val MODULE_ENABLED = PrefsData("module_enabled", true)

    /** Debug logging enabled. */
    val DEBUG_ENABLED = PrefsData("debug_enabled", false)

    // ═══════════════════════════════════════════════════════════
    // PER-APP SPOOF VALUES
    // Key format: spoof_{packageName}_{spoofType}
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the pref key for a specific app and spoof type.
     */
    fun getSpoofKey(packageName: String, type: SpoofType): String {
        return "spoof_${packageName.replace('.', '_')}_${type.name}"
    }

    /**
     * Gets the pref key for checking if a spoof type is enabled for an app.
     */
    fun getSpoofEnabledKey(packageName: String, type: SpoofType): String {
        return "spoof_enabled_${packageName.replace('.', '_')}_${type.name}"
    }

    /**
     * Gets the pref key for checking if an app is enabled for spoofing.
     */
    fun getAppEnabledKey(packageName: String): String {
        return "app_enabled_${packageName.replace('.', '_')}"
    }

    /**
     * Gets the device profile preset key for an app.
     */
    fun getDeviceProfileKey(packageName: String): String {
        return getSpoofKey(packageName, SpoofType.DEVICE_PROFILE)
    }

    // ═══════════════════════════════════════════════════════════
    // DEFAULT VALUES
    // ═══════════════════════════════════════════════════════════

    /** List of apps enabled for spoofing (comma-separated). */
    val ENABLED_APPS = PrefsData("enabled_apps", "")

    /** Config version for migration. */
    val CONFIG_VERSION = PrefsData("config_version", 1)
}
