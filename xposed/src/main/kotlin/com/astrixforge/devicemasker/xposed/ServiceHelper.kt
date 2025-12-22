package com.astrixforge.devicemasker.xposed

import android.os.Bundle
import android.os.IBinder
import com.astrixforge.devicemasker.common.Constants
import com.highcapable.yukihookapi.hook.log.YLog

/**
 * Service Helper - Manages binder access for UI app.
 *
 * In the AIDL-based architecture, the service runs in system_server and the
 * UI app needs to get the binder to communicate with it.
 *
 * This simplified implementation provides direct binder access.
 * The actual binder delivery is handled via a ContentProvider in the app module.
 */
object ServiceHelper {

    private const val TAG = "ServiceHelper"

    /**
     * Registers any necessary hooks in system_server.
     * Called from XposedHookLoader.initSystemServer().
     */
    fun registerBinderHook() {
        // In this simplified implementation, we don't hook system_server.
        // Instead, the UI app queries the service directly when needed.
        // The binder is made available via DeviceMaskerService.instance.

        YLog.info("$TAG: Service binder available for app queries")
    }

    /**
     * Gets the service binder for direct access.
     * Used by the app module's ServiceProvider.
     */
    fun getBinder(): IBinder? {
        return DeviceMaskerService.instance?.asBinder()
    }

    /**
     * Creates a Bundle containing the service binder.
     * Used for ContentProvider-based binder delivery.
     */
    fun createBinderBundle(): Bundle {
        return Bundle().apply {
            DeviceMaskerService.instance?.let { service ->
                putBinder("binder", service.asBinder())
                putInt("version", service.serviceVersion)
            }
        }
    }

    /**
     * Checks if the service is available.
     */
    fun isServiceAvailable(): Boolean {
        return DeviceMaskerService.instance != null
    }
}
