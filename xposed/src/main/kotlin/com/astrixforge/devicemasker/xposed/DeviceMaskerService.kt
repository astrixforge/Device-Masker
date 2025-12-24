package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.IDeviceMaskerService
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofType
import java.io.File

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
        @Suppress("unused") // Reserved for future log buffer limit
        private const val MAX_LOGS = 1000

        @Volatile
        var instance: DeviceMaskerService? = null
            private set

        /**
         * Initializes the service singleton.
         * Called from XposedHookLoader when running in system_server.
         * 
         * The service manages config files that are read by hooked apps.
         * ServiceManager registration was removed as it doesn't work due to SELinux.
         */
        fun init() {
            if (instance != null) {
                DualLog.warn(TAG, "Service already initialized")
                return
            }

            val service = DeviceMaskerService()
            instance = service.apply {
                searchDataDir()
                loadConfig()
            }

            // Note: ServiceManager registration removed - doesn't work due to SELinux
            // Hooks now read config directly from file via ConfigReader

            DualLog.info(TAG, "Service initialized, config version: ${service.config.version}")
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

    /** Hook count (for diagnostics) */
    @Volatile
    private var hookCount: Int = 0

    // ═══════════════════════════════════════════════════════════
    // DATA DIRECTORY MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Finds or creates the data directory.
     * 
     * Priority order:
     * 1. /data/system/devicemasker/ - Preferred for system_server access
     * 2. /data/local/tmp/devicemasker/ - Fallback for testing
     * 3. In-memory only (no persistence) - Last resort
     */
    private fun searchDataDir() {
        // Try primary location: /data/system/devicemasker
        val systemDir = File(Constants.SYSTEM_DATA_DIR)
        
        if (tryCreateDirectory(systemDir)) {
            dataDir = systemDir
            DualLog.info(TAG, "Data directory: ${systemDir.absolutePath}")
            return
        }
        
        // Try fallback location: /data/local/tmp/devicemasker
        val tmpDir = File("/data/local/tmp/devicemasker")
        if (tryCreateDirectory(tmpDir)) {
            dataDir = tmpDir
            DualLog.warn(TAG, "Using fallback data directory: ${tmpDir.absolutePath}")
            return
        }
        
        // Last resort: use cache in app's data directory if available
        // Note: This won't persist well but prevents crashes
        dataDir = null
        DualLog.error(TAG, "Could not create any data directory! Config will not persist.")
    }
    
    /**
     * Attempts to create a directory with proper permissions.
     * @return true if directory exists or was successfully created
     */
    private fun tryCreateDirectory(dir: File): Boolean {
        return runCatching {
            if (dir.exists()) {
                if (dir.isDirectory && dir.canWrite()) {
                    return@runCatching true
                }
                DualLog.warn(TAG, "${dir.absolutePath} exists but is not writable")
                return@runCatching false
            }
            
            // Attempt to create the directory
            val created = dir.mkdirs()
            if (created) {
                @Suppress("SetWorldReadable") // Required for Xposed hooks to access config
                dir.setReadable(true, false)
                dir.setWritable(true, true)
                dir.setExecutable(true, false)
                DualLog.debug(TAG, "Created directory: ${dir.absolutePath}")
                return@runCatching true
            }
            
            // mkdirs() returned false - try using Runtime.exec as fallback
            // This works in system_server context with proper SELinux permissions
            runCatching {
                val process = Runtime.getRuntime().exec(arrayOf("mkdir", "-p", dir.absolutePath))
                val exitCode = process.waitFor()
                if (exitCode == 0 && dir.exists()) {
                    Runtime.getRuntime().exec(arrayOf("chmod", "755", dir.absolutePath)).waitFor()
                    DualLog.debug(TAG, "Created directory via shell: ${dir.absolutePath}")
                    return@runCatching true
                }
            }
            
            DualLog.warn(TAG, "Failed to create directory: ${dir.absolutePath}")
            false
        }.getOrElse { e ->
            DualLog.warn(TAG, "Exception creating directory ${dir.absolutePath}: ${e.message}")
            false
        }
    }

    /**
     * Gets the config file path.
     * @return File object or null if dataDir is not available
     */
    private fun getConfigFile(): File? {
        val dir = dataDir ?: return null
        return File(dir, Constants.CONFIG_FILE_NAME)
    }

    // ═══════════════════════════════════════════════════════════
    // CONFIG MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Loads configuration from disk into memory.
     */
    private fun loadConfig() {
        val file = getConfigFile()
        
        // If no data directory, just use default config (in-memory only)
        if (file == null) {
            DualLog.warn(TAG, "No data directory available, using in-memory config only")
            config = JsonConfig.createDefault()
            return
        }

        config = if (file.exists()) {
            runCatching {
                val json = file.readText()
                JsonConfig.parse(json)
            }.getOrElse { e ->
                DualLog.error(TAG, "Failed to parse config, using default", e)
                JsonConfig.createDefault()
            }
        } else {
            DualLog.info(TAG, "No config file, creating default")
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
        
        // If no data directory available, skip saving (config is in-memory only)
        if (file == null) {
            DualLog.debug(TAG, "No data directory, skipping config save")
            return
        }

        runCatching {
            // Ensure parent directory exists before writing
            file.parentFile?.let { parentDir ->
                if (!parentDir.exists()) {
                    if (tryCreateDirectory(parentDir)) {
                        DualLog.debug(TAG, "Created data directory: ${parentDir.absolutePath}")
                    } else {
                        DualLog.warn(TAG, "Could not create parent directory for config")
                        return
                    }
                }
            }
            file.writeText(newConfig.toPrettyJsonString())
            DualLog.debug(TAG, "Config saved to ${file.absolutePath}")
        }.onFailure { e ->
            DualLog.error(TAG, "Failed to save config", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // AIDL INTERFACE IMPLEMENTATION
    // ═══════════════════════════════════════════════════════════

    override fun getServiceVersion(): Int = SERVICE_VERSION

    override fun stopService(cleanEnv: Boolean) {
        DualLog.info(TAG, "stopService called, cleanEnv=$cleanEnv")
        if (cleanEnv) {
            config = JsonConfig.createDefault()
            DualLog.clearLogs()
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
            DualLog.info(TAG, "Config updated from UI")
        }.onFailure { e ->
            DualLog.error(TAG, "Failed to parse config from UI", e)
        }
    }

    override fun getLogs(): Array<String> {
        return DualLog.getLogs()
    }

    override fun clearLogs() {
        DualLog.clearLogs()
    }

    override fun log(level: Int, tag: String, message: String) {
        // Route to DualLog based on level
        when (level) {
            0, 1 -> DualLog.debug(tag, message) // Verbose, Debug
            2 -> DualLog.info(tag, message)     // Info
            3 -> DualLog.warn(tag, message)     // Warning
            4 -> DualLog.error(tag, message)    // Error
            else -> DualLog.debug(tag, message)
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
    @Suppress("unused") // API method for diagnostics
    fun incrementHookCount() {
        hookCount++
    }
}
