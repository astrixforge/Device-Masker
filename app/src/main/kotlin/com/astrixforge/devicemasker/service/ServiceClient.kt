package com.astrixforge.devicemasker.service

import android.os.IBinder
import android.os.RemoteException
import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.IDeviceMaskerService
import timber.log.Timber

/**
 * Service Client - Proxy for communicating with DeviceMaskerService via AIDL.
 *
 * In HMA-OSS architecture:
 * - DeviceMaskerService runs in system_server
 * - UI app communicates via AIDL binder
 * - This client wraps the AIDL interface for convenient access
 *
 * The binder is received via ServiceProvider (ContentProvider).
 */
object ServiceClient : IBinder.DeathRecipient {

    private const val TAG = "ServiceClient"

    @Volatile
    private var service: IDeviceMaskerService? = null

    @Volatile
    private var binder: IBinder? = null

    /**
     * Links to the service binder received from system_server.
     * Called by ServiceProvider when it receives the binder.
     *
     * @param binder The binder to the DeviceMaskerService
     * @return true if linking succeeded
     */
    fun linkService(binder: IBinder): Boolean {
        return try {
            // Unlink previous binder if exists
            this.binder?.unlinkToDeath(this, 0)

            // Link to new binder
            binder.linkToDeath(this, 0)
            this.binder = binder
            this.service = IDeviceMaskerService.Stub.asInterface(binder)

            Timber.tag(TAG).i("Service linked successfully (version: ${getServiceVersion()})")
            true
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "Failed to link service")
            false
        }
    }

    /**
     * Called when the service dies (e.g., system_server restart).
     */
    override fun binderDied() {
        Timber.tag(TAG).w("Service died, clearing reference")
        service = null
        binder = null
    }

    /**
     * Checks if the service is available.
     */
    fun isServiceAvailable(): Boolean = service != null

    // ═══════════════════════════════════════════════════════════
    // AIDL Interface Proxy Methods
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the service version.
     */
    fun getServiceVersion(): Int {
        return try {
            service?.serviceVersion ?: -1
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "getServiceVersion failed")
            -1
        }
    }

    /**
     * Stops the service.
     */
    fun stopService(cleanEnv: Boolean = false) {
        try {
            service?.stopService(cleanEnv)
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "stopService failed")
        }
    }

    /**
     * Reads the configuration JSON from the service.
     */
    fun readConfig(): String? {
        return try {
            service?.readConfig()
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "readConfig failed")
            null
        }
    }

    /**
     * Writes the configuration JSON to the service.
     * This updates the in-memory config and persists to file.
     */
    fun writeConfig(json: String): Boolean {
        return try {
            service?.writeConfig(json)
            true
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "writeConfig failed")
            false
        }
    }

    /**
     * Gets the service logs.
     */
    fun getLogs(): Array<String> {
        return try {
            service?.logs ?: emptyArray()
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "getLogs failed")
            emptyArray()
        }
    }

    /**
     * Clears the service logs.
     */
    fun clearLogs() {
        try {
            service?.clearLogs()
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "clearLogs failed")
        }
    }

    /**
     * Logs a message to the service buffer.
     */
    fun log(level: Int, tag: String, message: String) {
        try {
            service?.log(level, tag, message)
        } catch (e: RemoteException) {
            // Don't log this failure to avoid recursion
        }
    }

    /**
     * Checks if the module is enabled.
     */
    fun isModuleEnabled(): Boolean {
        return try {
            service?.isModuleEnabled ?: false
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "isModuleEnabled failed")
            false
        }
    }

    /**
     * Gets the hook count.
     */
    fun getHookCount(): Int {
        return try {
            service?.hookCount ?: 0
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "getHookCount failed")
            0
        }
    }

    /**
     * Gets a specific spoof value for a package.
     */
    fun getSpoofValue(packageName: String, spoofType: String): String? {
        return try {
            service?.getSpoofValue(packageName, spoofType)
        } catch (e: RemoteException) {
            Timber.tag(TAG).e(e, "getSpoofValue failed")
            null
        }
    }
}
