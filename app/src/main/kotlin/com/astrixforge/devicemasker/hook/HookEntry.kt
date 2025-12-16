package com.astrixforge.devicemasker.hook

import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.hook.hooker.AdvertisingHooker
import com.astrixforge.devicemasker.hook.hooker.AntiDetectHooker
import com.astrixforge.devicemasker.hook.hooker.DeviceHooker
import com.astrixforge.devicemasker.hook.hooker.LocationHooker
import com.astrixforge.devicemasker.hook.hooker.NetworkHooker
import com.astrixforge.devicemasker.hook.hooker.SystemHooker
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

/**
 * LSPosed/Xposed module entry point.
 *
 * This class is automatically detected by LSPosed via the @InjectYukiHookWithXposed annotation.
 * YukiHookAPI's KSP processor generates the necessary Xposed init class.
 *
 * Hook Loading Order (CRITICAL):
 * 1. AntiDetectHooker - MUST load first to hide Xposed presence before detection
 * 2. DeviceHooker - IMEI, Serial, Hardware IDs
 * 3. NetworkHooker - MAC, WiFi, Bluetooth
 * 4. AdvertisingHooker - GSF ID, Advertising ID, Android ID
 * 5. SystemHooker - Build.*, SystemProperties
 * 6. LocationHooker - GPS, Timezone, Locale
 */
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    /** Called once when the module is first loaded. Used to configure YukiHookAPI settings. */
    override fun onInit() =
            YukiHookAPI.configs {
                // Debug logging configuration
                debugLog {
                    tag = "DeviceMasker"
                    isEnable = BuildConfig.DEBUG
                }

                // General debug mode
                isDebug = BuildConfig.DEBUG
            }

    /**
     * Called for each app process where the module is active. This is where all hooking logic is
     * registered.
     *
     * CRITICAL SAFETY RULES:
     * 1. NEVER hook the module app itself (causes infinite recursion/crash)
     * 2. NEVER hook critical system processes like "android" (causes system instability)
     * 3. Only hook specific user-selected apps via loadApp() scope
     */
    override fun onHook() = encase {
        // ═══════════════════════════════════════════════════════════════
        // CRITICAL SAFETY CHECK #1: Skip our own module app
        // This prevents the module from hooking itself and crashing
        // ═══════════════════════════════════════════════════════════════
        val selfPackage = "com.astrixforge.devicemasker"
        val selfApplicationId = BuildConfig.APPLICATION_ID

        if (packageName == selfPackage ||
                        packageName == selfApplicationId ||
                        processName == selfPackage ||
                        processName == selfApplicationId ||
                        processName.startsWith(selfPackage) ||
                        processName.startsWith(selfApplicationId)
        ) {
            YLog.debug(
                    "DeviceMasker: Skipping hooks for SELF (module app): pkg=$packageName, process=$processName"
            )
            return@encase
        }

        // ═══════════════════════════════════════════════════════════════
        // CRITICAL SAFETY CHECK #2: Skip system-critical processes
        // Hooking "android" core process breaks all apps including ours
        // ═══════════════════════════════════════════════════════════════
        val forbiddenProcesses =
                listOf(
                        "android", // Core Android framework
                        "system_server", // System server (already dangerous)
                        "com.android.systemui", // SystemUI - hooks can cause UI glitches
                )

        if (packageName in forbiddenProcesses || processName in forbiddenProcesses) {
            YLog.debug(
                    "DeviceMasker: Skipping hooks for SYSTEM process: pkg=$packageName, process=$processName"
            )
            return@encase
        }

        // ═══════════════════════════════════════════════════════════════
        // SAFE TO PROCEED: Log and apply hooks to target app
        // ═══════════════════════════════════════════════════════════════
        YLog.info(
                "DeviceMasker: Starting hooks for target app: $packageName (process: $processName)"
        )

        // ═══════════════════════════════════════════════════════════════
        // AntiDetectHooker must load FIRST to hide our presence
        // ═══════════════════════════════════════════════════════════════
        loadHooker(AntiDetectHooker)

        // ═══════════════════════════════════════════════════════════════
        // Device Spoofing Hookers
        // ═══════════════════════════════════════════════════════════════
        loadHooker(DeviceHooker)
        loadHooker(NetworkHooker)
        loadHooker(AdvertisingHooker)
        loadHooker(SystemHooker)
        loadHooker(LocationHooker)

        YLog.info("DeviceMasker: Hooks registered successfully for: $packageName")
    }
}
