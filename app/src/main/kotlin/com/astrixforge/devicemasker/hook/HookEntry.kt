package com.astrixforge.devicemasker.hook

import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.xposed.XposedHookLoader
import com.highcapable.yukihookapi.YukiHookAPI
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit

/**
 * LSPosed/Xposed module entry point.
 *
 * This class is automatically detected by LSPosed via the @InjectYukiHookWithXposed annotation.
 * YukiHookAPI's KSP processor generates the necessary Xposed init class.
 *
 * In Multi-Module AIDL architecture:
 * - This entry point stays in :app module (required for KSP)
 * - Loads XposedHookLoader from :xposed module
 * - XposedHookLoader handles system_server init and target app hooks
 */
@InjectYukiHookWithXposed
object HookEntry : IYukiHookXposedInit {

    /** Called once when the module is first loaded. Used to configure YukiHookAPI settings. */
    override fun onInit() =
        YukiHookAPI.configs {
            // Debug logging configuration
            debugLog {
                tag = "DeviceMasker"
                // IMPORTANT: Always enable logging for log export functionality
                // Previously was: isEnable = BuildConfig.DEBUG (disabled in release!)
                isEnable = true
                // Enable log recording for export functionality
                isRecord = true
            }

            // General debug mode (this only affects verbose internal logs)
            isDebug = BuildConfig.DEBUG
        }

    /**
     * Called for each app process where the module is active.
     *
     * Delegates all hook logic to XposedHookLoader in the :xposed module. The XposedHookLoader
     * handles:
     * - system_server hooks (anti-detection and file-based config)
     * - Target app hook loading (AntiDetect, Device, Network, etc.)
     */
    override fun onHook() = encase {
        // Load the XposedHookLoader from :xposed module
        // It handles all safety checks and hooker loading
        loadHooker(XposedHookLoader)
    }
}
