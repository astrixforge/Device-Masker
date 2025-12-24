package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.xposed.hooker.AdvertisingHooker
import com.astrixforge.devicemasker.xposed.hooker.AntiDetectHooker
import com.astrixforge.devicemasker.xposed.hooker.DeviceHooker
import com.astrixforge.devicemasker.xposed.hooker.LocationHooker
import com.astrixforge.devicemasker.xposed.hooker.NetworkHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Xposed Module Hook Loader - Using YukiHookAPI's XSharedPreferences.
 *
 * This uses the CORRECT approach for cross-process config sharing:
 * - UI app writes to SharedPreferences with MODE_WORLD_READABLE
 * - Hooks read via prefs property which uses XSharedPreferences internally
 * - No need for ServiceManager (blocked by SELinux) or custom IPC
 *
 * Requirements:
 * 1. AndroidManifest.xml: xposedsharedprefs = true
 * 2. xposedminversion >= 93
 * 3. LSPosed scope includes target apps
 *
 * Hook Loading Order (CRITICAL):
 * 1. AntiDetectHooker - MUST load first to hide Xposed presence
 * 2. DeviceHooker - IMEI, Serial, Hardware IDs
 * 3. NetworkHooker - MAC, WiFi, Bluetooth
 * 4. AdvertisingHooker - GSF ID, Advertising ID
 * 5. SystemHooker - Build.*, SystemProperties
 * 6. LocationHooker - GPS, Timezone, Locale
 */
object XposedHookLoader : YukiBaseHooker() {

    private const val TAG = "DeviceMasker"
    private const val SELF_PACKAGE = "com.astrixforge.devicemasker"

    override fun onHook() {
        // Skip our own module
        if (packageName == SELF_PACKAGE || processName.startsWith(SELF_PACKAGE)) {
            DualLog.debug(TAG, "Skipping hooks for SELF: $packageName")
            return
        }

        // Skip system-critical processes
        val forbiddenProcesses = listOf(
            "android",
            "com.android.systemui",
            "com.android.phone",
        )
        if (packageName in forbiddenProcesses || processName in forbiddenProcesses) {
            DualLog.debug(TAG, "Skipping system process: $packageName")
            return
        }

        // Check if module is enabled via XSharedPreferences
        val moduleEnabled = PrefsHelper.isModuleEnabled(prefs)
        
        if (!moduleEnabled) {
            DualLog.debug(TAG, "Module disabled globally, skipping: $packageName")
            return
        }

        // Check if this app is enabled for spoofing
        val appEnabled = PrefsHelper.isAppEnabled(prefs, packageName)

        if (!appEnabled) {
            DualLog.debug(TAG, "Spoofing disabled for: $packageName")
            return
        }

        DualLog.info(TAG, "Starting hooks for: $packageName (process: $processName)")

        // ═══════════════════════════════════════════════════════════
        // AntiDetectHooker must load FIRST to hide our presence
        // ═══════════════════════════════════════════════════════════
        loadHooker(AntiDetectHooker)

        // ═══════════════════════════════════════════════════════════
        // Device Spoofing Hookers
        // ═══════════════════════════════════════════════════════════
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
        loadHooker(AdvertisingHooker)
        loadHooker(SystemHooker)
        loadHooker(LocationHooker)

        DualLog.info(TAG, "Hooks registered for: $packageName")
    }
}
