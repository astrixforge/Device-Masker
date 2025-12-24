package com.astrixforge.devicemasker.common

/**
 * Shared Preference Key Generator.
 *
 * This class MUST be used by both the app (XposedPrefs) and the xposed module (PrefsHelper)
 * to ensure keys are IDENTICAL. Any mismatch will cause config to not be read properly.
 *
 * Key Format:
 * - Global: "module_enabled", "debug_enabled", etc.
 * - Per-App: "{prefix}_{sanitizedPackageName}_{type}"
 *
 * Package names are sanitized by replacing '.' with '_' to avoid XML issues.
 */
@Suppress("unused") // Used by XposedPrefs in :app module
object SharedPrefsKeys {

    // ═══════════════════════════════════════════════════════════
    // GLOBAL KEYS
    // ═══════════════════════════════════════════════════════════

    const val KEY_MODULE_ENABLED = "module_enabled"
    const val KEY_DEBUG_ENABLED = "debug_enabled"
    const val KEY_CONFIG_VERSION = "config_version"

    // ═══════════════════════════════════════════════════════════
    // PER-APP KEY PREFIXES
    // ═══════════════════════════════════════════════════════════

    private const val PREFIX_APP_ENABLED = "app_enabled_"
    private const val PREFIX_SPOOF_ENABLED = "spoof_enabled_"
    private const val PREFIX_SPOOF_VALUE = "spoof_"

    // ═══════════════════════════════════════════════════════════
    // KEY GENERATORS
    // ═══════════════════════════════════════════════════════════

    /**
     * Sanitizes a package name for use in preference keys.
     * Replaces '.' with '_' to avoid XML/key issues.
     */
    private fun sanitize(packageName: String): String {
        return packageName.replace('.', '_')
    }

    /**
     * Gets the key for app enabled status.
     * Example: "app_enabled_com_example_app"
     */
    fun getAppEnabledKey(packageName: String): String {
        return "$PREFIX_APP_ENABLED${sanitize(packageName)}"
    }

    /**
     * Gets the key for spoof type enabled status.
     * Example: "spoof_enabled_com_example_app_IMEI"
     */
    fun getSpoofEnabledKey(packageName: String, type: SpoofType): String {
        return "$PREFIX_SPOOF_ENABLED${sanitize(packageName)}_${type.name}"
    }

    /**
     * Gets the key for spoof value.
     * Example: "spoof_com_example_app_IMEI"
     */
    fun getSpoofValueKey(packageName: String, type: SpoofType): String {
        return "$PREFIX_SPOOF_VALUE${sanitize(packageName)}_${type.name}"
    }

    /**
     * Gets the device profile preset key for an app.
     */
    fun getDeviceProfileKey(packageName: String): String {
        return getSpoofValueKey(packageName, SpoofType.DEVICE_PROFILE)
    }
}
