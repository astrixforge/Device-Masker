package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge

/**
 * Helper object for reading XSharedPreferences values.
 *
 * This provides utility functions for the hookers to use with YukiHookAPI's prefs. All functions
 * take a YukiHookPrefsBridge instance as the first parameter, which you get from `prefs` property
 * in PackageParam context.
 *
 * ## Key Sync Architecture:
 * ```
 * UI App writes to: XposedPrefs (MODE_WORLD_READABLE)
 *                        ↓
 *              Uses SharedPrefsKeys.getXXXKey()
 *                        ↓
 * Xposed reads from: YukiHookPrefsBridge.getString()
 *                        ↓
 *              Uses PrefsKeys.getXXXKey() → SharedPrefsKeys.getXXXKey()
 * ```
 *
 * Both sides use the SAME key generators from :common module.
 *
 * ## Caching Note:
 * XSharedPreferences CACHES values. Config changes require target app restart.
 */
object PrefsHelper {

    private const val TAG = "PrefsHelper"

    /** Checks if the module is enabled globally. */
    fun isModuleEnabled(prefs: YukiHookPrefsBridge): Boolean {
        return runCatching { prefs.get(PrefsKeys.MODULE_ENABLED) }.getOrDefault(true)
    }

    /** Checks if spoofing is enabled for a specific app. */
    fun isAppEnabled(prefs: YukiHookPrefsBridge, packageName: String): Boolean {
        return runCatching {
                val key = PrefsKeys.getAppEnabledKey(packageName)
                prefs.getString(key, "true") == "true"
            }
            .getOrDefault(true)
    }

    /** Checks if a specific spoof type is enabled for an app. */
    fun isSpoofTypeEnabled(
        prefs: YukiHookPrefsBridge,
        packageName: String,
        type: SpoofType,
    ): Boolean {
        return runCatching {
                val key = PrefsKeys.getSpoofEnabledKey(packageName, type)
                prefs.getString(key, "false") == "true"
            }
            .getOrDefault(false)
    }

    /**
     * Gets the spoofed value for a specific type and package.
     *
     * Flow:
     * 1. Check if module is enabled globally
     * 2. Check if app is enabled for spoofing
     * 3. Check if this spoof type is enabled for the app
     * 4. Return the configured value, or fallback if empty
     *
     * @param prefs The YukiHookPrefsBridge from PackageParam
     * @param packageName The package being hooked
     * @param type The type of value to spoof
     * @param fallback Fallback value generator if not configured
     * @return The spoofed value, or fallback if not configured/enabled
     */
    fun getSpoofValue(
        prefs: YukiHookPrefsBridge,
        packageName: String,
        type: SpoofType,
        fallback: () -> String,
    ): String {
        return runCatching {
                // Check cascade: module → app → type
                if (!isModuleEnabled(prefs)) return fallback()
                if (!isAppEnabled(prefs, packageName)) return fallback()
                if (!isSpoofTypeEnabled(prefs, packageName, type)) return fallback()

                // Get the spoof value using key from SharedPrefsKeys
                val key = PrefsKeys.getSpoofKey(packageName, type)
                val value = prefs.getString(key, "")

                if (value.isEmpty()) return fallback()

                DualLog.debug(TAG, "Spoofing ${type.name} for $packageName")
                value
            }
            .getOrElse { e ->
                DualLog.warn(TAG, "Error reading prefs: ${e.message}")
                fallback()
            }
    }

    /**
     * Gets the current config version (for debugging only). Note: This is NOT useful for detecting
     * config changes as XSharedPreferences caches.
     */
    fun getConfigVersion(prefs: YukiHookPrefsBridge): Int {
        return runCatching { prefs.get(PrefsKeys.CONFIG_VERSION) }.getOrDefault(1)
    }
}
