package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge

/**
 * Helper object for reading XSharedPreferences values.
 *
 * This provides utility functions for the hookers to use with YukiHookAPI's prefs.
 * All functions take a YukiHookPrefsBridge instance as the first parameter,
 * which you get from `prefs` property in PackageParam context.
 */
object PrefsHelper {

    private const val TAG = "PrefsHelper"

    /**
     * Checks if the module is enabled globally.
     */
    fun isModuleEnabled(prefs: YukiHookPrefsBridge): Boolean {
        return runCatching {
            prefs.get(PrefsKeys.MODULE_ENABLED)
        }.getOrDefault(true)
    }

    /**
     * Checks if spoofing is enabled for a specific app.
     */
    fun isAppEnabled(prefs: YukiHookPrefsBridge, packageName: String): Boolean {
        return runCatching {
            val key = PrefsKeys.getAppEnabledKey(packageName)
            prefs.getString(key, "true") == "true"
        }.getOrDefault(true)
    }

    /**
     * Checks if a specific spoof type is enabled for an app.
     */
    fun isSpoofTypeEnabled(prefs: YukiHookPrefsBridge, packageName: String, type: SpoofType): Boolean {
        return runCatching {
            val key = PrefsKeys.getSpoofEnabledKey(packageName, type)
            prefs.getString(key, "false") == "true"
        }.getOrDefault(false)
    }

    /**
     * Gets the spoofed value for a specific type and package.
     *
     * @param prefs The YukiHookPrefsBridge from PackageParam
     * @param packageName The package being hooked
     * @param type The type of value to spoof
     * @param fallback Fallback value generator if not configured
     * @return The spoofed value, or fallback if not configured
     */
    fun getSpoofValue(
        prefs: YukiHookPrefsBridge,
        packageName: String,
        type: SpoofType,
        fallback: () -> String
    ): String {
        return runCatching {
            // Note: XSharedPreferences caches values in hooked apps.
            // Config changes may not be visible until app restarts.
            // YukiHookAPI handles this transparently for most cases.
            
            // Check if module is enabled
            if (!isModuleEnabled(prefs)) {
                return fallback()
            }

            // Check if app is enabled
            if (!isAppEnabled(prefs, packageName)) {
                return fallback()
            }

            // Check if this spoof type is enabled
            if (!isSpoofTypeEnabled(prefs, packageName, type)) {
                return fallback()
            }

            // Get the spoof value
            val key = PrefsKeys.getSpoofKey(packageName, type)
            val value = prefs.getString(key, "")
            
            if (value.isEmpty()) {
                return fallback()
            }

            DualLog.debug(TAG, "Spoofing ${type.name} for $packageName")
            value
        }.getOrElse { e ->
            DualLog.warn(TAG, "Error reading prefs: ${e.message}")
            fallback()
        }
    }
}
