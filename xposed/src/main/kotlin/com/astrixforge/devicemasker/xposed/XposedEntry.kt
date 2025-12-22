package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.xposed.hooker.AdvertisingHooker
import com.astrixforge.devicemasker.xposed.hooker.AntiDetectHooker
import com.astrixforge.devicemasker.xposed.hooker.DeviceHooker
import com.astrixforge.devicemasker.xposed.hooker.LocationHooker
import com.astrixforge.devicemasker.xposed.hooker.NetworkHooker
import com.astrixforge.devicemasker.xposed.hooker.SystemHooker
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Xposed Module Hook Loader for Multi-Module AIDL architecture.
 *
 * This class is NOT the entry point - the entry point is in the :app module
 * where YukiHookAPI KSP can generate the init class.
 *
 * This provides:
 * 1. Hooker loading for target apps
 * 2. Service initialization in system_server
 *
 * Called from app module's HookEntry via loadHooker().
 *
 * Hook Loading Order (CRITICAL):
 * 1. AntiDetectHooker - MUST load first to hide Xposed presence
 * 2. DeviceHooker - IMEI, Serial, Hardware IDs
 * 3. NetworkHooker - MAC, WiFi, Bluetooth
 * 4. AdvertisingHooker - GSF ID, Advertising ID, Android ID
 * 5. SystemHooker - Build.*, SystemProperties
 * 6. LocationHooker - GPS, Timezone, Locale
 */
object XposedHookLoader : YukiBaseHooker() {

    private const val TAG = "DeviceMasker"
    private const val SELF_PACKAGE = "com.astrixforge.devicemasker"

    override fun onHook() {
        // ═══════════════════════════════════════════════════════════
        // SYSTEM_SERVER: Initialize DeviceMaskerService
        // ═══════════════════════════════════════════════════════════
        val isSystemServer = processName == "system_server" || packageName == "android"
        
        if (isSystemServer) {
            YLog.info("$TAG: Initializing in system_server (process: $processName)")

            runCatching {
                DeviceMaskerService.init()
                YLog.info("$TAG: DeviceMaskerService initialized successfully")
            }.onFailure { e ->
                YLog.error("$TAG: Failed to initialize DeviceMaskerService", e)
            }

            runCatching {
                ServiceHelper.registerBinderHook()
                YLog.info("$TAG: Binder hook registered for app communication")
            }.onFailure { e ->
                YLog.error("$TAG: Failed to register binder hook", e)
            }

            return // Don't apply spoofing hooks in system_server
        }

        // ═══════════════════════════════════════════════════════════
        // TARGET APPS: Apply spoofing hooks
        // ═══════════════════════════════════════════════════════════

        // Skip our own module
        if (packageName == SELF_PACKAGE || processName.startsWith(SELF_PACKAGE)) {
            YLog.debug("$TAG: Skipping hooks for SELF: $packageName")
            return
        }

        // Skip system-critical processes
        val forbiddenProcesses = listOf(
            "android",
            "com.android.systemui",
        )
        if (packageName in forbiddenProcesses || processName in forbiddenProcesses) {
            return
        }

        // Get service instance - it should already be initialized by system_server
        val service = DeviceMaskerService.instance
        if (service == null) {
            // Service not available - this shouldn't happen if module loaded correctly
            YLog.warn("$TAG: Service not available for: $packageName (this is expected on first boot)")
        }

        // Check if this app is configured for spoofing
        val config = service?.config
        val appConfig = config?.getAppConfig(packageName)
        if (appConfig?.isEnabled == false) {
            YLog.debug("$TAG: Spoofing disabled for: $packageName")
            return
        }

        YLog.info("$TAG: Starting hooks for: $packageName (process: $processName)")

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

        YLog.info("$TAG: Hooks registered successfully for: $packageName")
    }

    /**
     * Called to initialize system_server hooks.
     * Should be called from loadSystem {} block.
     */
    fun initSystemServer() {
        YLog.info("$TAG: Initializing system_server hooks")

        runCatching {
            DeviceMaskerService.init()
            YLog.info("$TAG: DeviceMaskerService initialized successfully")
        }.onFailure { e ->
            YLog.error("$TAG: Failed to initialize DeviceMaskerService", e)
        }

        runCatching {
            ServiceHelper.registerBinderHook()
            YLog.info("$TAG: Binder hook registered for app communication")
        }.onFailure { e ->
            YLog.error("$TAG: Failed to register binder hook", e)
        }
    }
}
