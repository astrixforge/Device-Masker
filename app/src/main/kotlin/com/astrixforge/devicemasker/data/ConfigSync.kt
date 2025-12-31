package com.astrixforge.devicemasker.data

import android.content.Context
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import timber.log.Timber

/**
 * Syncs JsonConfig to XposedPrefs for cross-process access.
 *
 * The UI uses JsonConfig (group-based, JSON file). But hooks need XSharedPreferences with per-app
 * keys. This class bridges the gap.
 *
 * ## Sync Flow:
 * ```
 * UI Change → ConfigManager → JsonConfig (file) → ConfigSync → XposedPrefs
 *                                                       ↓
 *                                            MODE_WORLD_READABLE
 *                                                       ↓
 *                                            XSharedPreferences (read by hooks)
 * ```
 *
 * ## Important Notes:
 * - XSharedPreferences CACHES values in hooked apps
 * - Config changes will NOT be visible until the target app restarts
 * - The config version bump is for debugging only, not live reload
 *
 * Call [syncFromConfig] whenever the config changes in the UI.
 */
object ConfigSync {

    /**
     * Syncs everything from JsonConfig to XposedPrefs. This should be called after any config
     * change in the UI.
     *
     * @param context Application context
     * @param config The JsonConfig to sync from
     */
    fun syncFromConfig(context: Context, config: JsonConfig) {
        val xprefs = XposedPrefs(context)

        Timber.d("Syncing config to XposedPrefs...")

        // Sync global settings
        xprefs.isModuleEnabled = config.isModuleEnabled

        // Sync each group's apps
        for (group in config.groups.values) {
            for (packageName in group.assignedApps) {
                // App enabled = group enabled
                xprefs.setAppEnabled(packageName, group.isEnabled)

                if (!group.isEnabled) continue

                // Sync each spoof type
                for (type in SpoofType.entries) {
                    val isEnabled = group.isTypeEnabled(type)
                    val value = if (isEnabled) group.getValue(type) else null

                    xprefs.setSpoofTypeEnabled(packageName, type, isEnabled)
                    xprefs.setSpoofValue(packageName, type, value)
                }
            }
        }

        // Bump version for debugging (NOT for live reload - won't work)
        xprefs.notifyConfigChanged()

        Timber.d("Config synced to XposedPrefs (version: ${xprefs.configVersion})")
        Timber.d("Note: Target apps must restart to see changes (XSharedPreferences caches)")
    }

    /**
     * Quick sync for a single app. Use when only one app's config changed.
     *
     * @param context Application context
     * @param config The JsonConfig to sync from
     * @param packageName The package to sync
     */
    fun syncApp(context: Context, config: JsonConfig, packageName: String) {
        val xprefs = XposedPrefs(context)

        val group = config.getGroupForApp(packageName)
        if (group == null) {
            // App not in any group - disable it
            xprefs.setAppEnabled(packageName, false)
            return
        }

        xprefs.setAppEnabled(packageName, group.isEnabled)

        for (type in SpoofType.entries) {
            val isEnabled = group.isTypeEnabled(type)
            val value = if (isEnabled) group.getValue(type) else null

            xprefs.setSpoofTypeEnabled(packageName, type, isEnabled)
            xprefs.setSpoofValue(packageName, type, value)
        }

        xprefs.notifyConfigChanged()
        Timber.d("App $packageName synced to XposedPrefs")
    }

    /** Clears all settings for a package from XposedPrefs. */
    fun clearApp(context: Context, packageName: String) {
        val xprefs = XposedPrefs(context)
        xprefs.clearAppSettings(packageName)
        xprefs.notifyConfigChanged()
        Timber.d("App $packageName cleared from XposedPrefs")
    }
}
