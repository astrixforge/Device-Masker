package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.IDeviceMaskerService
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import com.highcapable.yukihookapi.hook.log.YLog
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Device Masker Service - AIDL implementation running in system_server.
 *
 * This service is the core of the Multi-Module AIDL architecture:
 * - Holds configuration in memory for instant access by hooks
 * - Persists configuration to /data/system/devicemasker/config.json
 * - Provides AIDL interface for UI app communication
 *
 * The service is initialized in system_server via XposedEntry.loadSystem{}
 * and hooks read from [config] without any file I/O.
 */
class DeviceMaskerService private constructor() : IDeviceMaskerService.Stub() {

    companion object {
        private const val TAG = "DeviceMaskerService"
        private const val SERVICE_VERSION = 1
        private const val MAX_LOGS = 1000

        @Volatile
        var instance: DeviceMaskerService? = null
            private set

        /**
         * Initializes the service singleton.
         * Called from XposedEntry.loadSystem{}.
         */
        fun init() {
            if (instance != null) {
                YLog.warn("$TAG: Service already initialized")
                return
            }

            instance = DeviceMaskerService().apply {
                searchDataDir()
                loadConfig()
                YLog.info("$TAG: Service initialized, config version: ${config.version}")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // STATE
    // ═══════════════════════════════════════════════════════════

    /** In-memory configuration - read by hooks without file I/O */
    @Volatile
    var config: JsonConfig = JsonConfig.createDefault()
        private set

    /** Data directory path */
    private var dataDir: File? = null

    /** Log buffer for diagnostics */
    private val logBuffer = CopyOnWriteArrayList<String>()

    /** Hook count (for diagnostics) */
    @Volatile
    private var hookCount: Int = 0

    // ═══════════════════════════════════════════════════════════
    // DATA DIRECTORY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Finds or creates the data directory.
     * Uses /data/system/devicemasker/ which is accessible by system_server.
     */
    private fun searchDataDir() {
        val systemDir = File(Constants.SYSTEM_DATA_DIR)

        runCatching {
            if (!systemDir.exists()) {
                systemDir.mkdirs()
                // Set permissions for system_server access
                systemDir.setReadable(true, false)
                systemDir.setWritable(true, true)
            }
            dataDir = systemDir
            YLog.info("$TAG: Data directory: ${systemDir.absolutePath}")
        }.onFailure { e ->
            YLog.error("$TAG: Failed to create data directory", e)
            // Fallback to a temporary location
            dataDir = File("/data/local/tmp/devicemasker").also { it.mkdirs() }
        }
    }

    /**
     * Gets the config file path.
     */
    private fun getConfigFile(): File {
        return File(dataDir, Constants.CONFIG_FILE_NAME)
    }

    // ═══════════════════════════════════════════════════════════
    // CONFIG MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Loads configuration from disk into memory.
     */
    private fun loadConfig() {
        val file = getConfigFile()

        config = if (file.exists()) {
            runCatching {
                val json = file.readText()
                JsonConfig.parse(json)
            }.getOrElse { e ->
                YLog.error("$TAG: Failed to parse config, using default", e)
                JsonConfig.createDefault()
            }
        } else {
            YLog.info("$TAG: No config file, creating default")
            val defaultConfig = JsonConfig.createDefault()
            saveConfigInternal(defaultConfig)
            defaultConfig
        }
    }

    /**
     * Saves configuration to disk.
     */
    private fun saveConfigInternal(newConfig: JsonConfig) {
        val file = getConfigFile()

        runCatching {
            // Ensure parent directory exists before writing
            file.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    val created = parentDir.mkdirs()
                    if (created) {
                        parentDir.setReadable(true, false)
                        parentDir.setWritable(true, true)
                        YLog.debug("$TAG: Created data directory: ${parentDir.absolutePath}")
                    }
                }
            }
            file.writeText(newConfig.toPrettyJsonString())
            YLog.debug("$TAG: Config saved to ${file.absolutePath}")
        }.onFailure { e ->
            YLog.error("$TAG: Failed to save config", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // AIDL INTERFACE IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════

    override fun getServiceVersion(): Int = SERVICE_VERSION

    override fun stopService(cleanEnv: Boolean) {
        YLog.info("$TAG: stopService called, cleanEnv=$cleanEnv")
        if (cleanEnv) {
            config = JsonConfig.createDefault()
            logBuffer.clear()
        }
    }

    override fun readConfig(): String {
        return config.toJsonString()
    }

    override fun writeConfig(json: String) {
        runCatching {
            val newConfig = JsonConfig.parse(json)
            config = newConfig
            saveConfigInternal(newConfig)
            YLog.info("$TAG: Config updated from UI")
        }.onFailure { e ->
            YLog.error("$TAG: Failed to parse config from UI", e)
        }
    }

    override fun getLogs(): Array<String> {
        return logBuffer.toTypedArray()
    }

    override fun clearLogs() {
        logBuffer.clear()
    }

    override fun log(level: Int, tag: String, message: String) {
        val timestamp = System.currentTimeMillis()
        val levelStr = when (level) {
            0 -> "V"
            1 -> "D"
            2 -> "I"
            3 -> "W"
            4 -> "E"
            else -> "?"
        }
        val logEntry = "[$timestamp][$levelStr][$tag] $message"

        // Add to buffer, trim if too large
        logBuffer.add(logEntry)
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.removeAt(0)
        }
    }

    override fun isModuleEnabled(): Boolean {
        return config.isModuleEnabled
    }

    override fun getHookCount(): Int = hookCount

    override fun getSpoofValue(packageName: String, spoofType: String): String? {
        // Get the group for this app
        val group = config.getGroupForApp(packageName) ?: return null

        // Check if group is enabled
        if (!group.isEnabled) return null

        // Find the SpoofType
        val type = SpoofType.fromName(spoofType) ?: return null

        // Check if this type is enabled
        if (!group.isTypeEnabled(type)) return null

        // Return the value
        return group.getValue(type)
    }

    // ═══════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Increments hook count (called by hookers).
     */
    fun incrementHookCount() {
        hookCount++
    }
}
