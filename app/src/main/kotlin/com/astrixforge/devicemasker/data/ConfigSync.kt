package com.astrixforge.devicemasker.data

import android.content.Context
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.data.ConfigSync.syncFromConfig
import timber.log.Timber

/**
 * Syncs JsonConfig to XposedPrefs for cross-process access.
 *
 * The UI uses JsonConfig (group-based, JSON file). But hooks need
 * XSharedPreferences with per-app keys. This class bridges the gap.
 *
 * Call [syncFromConfig] whenever the config changes in the UI.
 */
object ConfigSync {

    /**
     * Syncs everything from JsonConfig to XposedPrefs.
     * This should be called after any config change in the UI.
     */
    fun syncFromConfig(context: Context, config: JsonConfig) {
        val xprefs = XposedPrefs(context)

        Timber.d("Syncing config to XposedPrefs...")

        // Sync global settings
        xprefs.isModuleEnabled = config.isModuleEnabled

        // Clear old keys first? No - we'll just overwrite.
        // Clearing could cause race conditions with running hooks.

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

        // Bump version to signal hooks to reload (if they support it)
        xprefs.notifyConfigChanged()

        Timber.d("Config synced to XposedPrefs (version: ${xprefs.configVersion})")
    }

    /**
     * Quick sync for a single app.
     * Use when only one app's config changed.
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
    }
}
