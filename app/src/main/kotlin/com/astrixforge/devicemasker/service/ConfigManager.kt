package com.astrixforge.devicemasker.service

import android.content.Context
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.DeviceIdentifier
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.data.ConfigSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Config Manager - Manages application configuration in Multi-Module AIDL architecture.
 *
 * Responsibilities:
 * 1. Load/save configuration from/to local JSON file
 * 2. Sync configuration to DeviceMaskerService via AIDL
 * 3. Provide StateFlow for UI reactivity
 * 4. CRUD operations for groups and app configs
 *
 * Data Flow:
 * - Read: Local file → ConfigManager → UI StateFlow
 * - Write: UI → ConfigManager → Local file + ServiceClient (if connected)
 */
object ConfigManager {

    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE_NAME = "config.json"

    private lateinit var configFile: File
    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // In-memory configuration
    private val _config = MutableStateFlow(JsonConfig.createDefault())
    val config: StateFlow<JsonConfig> = _config.asStateFlow()

    // Initialization state
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // Service connection state
    val isServiceConnected: Boolean
        get() = ServiceClient.isServiceAvailable()

    /**
     * Initializes the ConfigManager with application context.
     * Must be called in Application.onCreate() or MainActivity.
     */
    fun init(context: Context) {
        if (_isInitialized.value) {
            Timber.tag(TAG).d("Already initialized")
            return
        }

        appContext = context.applicationContext
        configFile = File(context.filesDir, CONFIG_FILE_NAME)
        Timber.tag(TAG).d("Config file: ${configFile.absolutePath}")

        scope.launch {
            loadConfig()
            _isInitialized.value = true
        }
    }

    /**
     * Loads configuration from local file.
     * Falls back to service config if local doesn't exist and service is connected.
     */
    private suspend fun loadConfig() {
        withContext(Dispatchers.IO) {
            try {
                // Try loading from local file first
                if (configFile.exists()) {
                    val json = configFile.readText()
                    val loadedConfig = JsonConfig.parse(json)
                    if (loadedConfig != null) {
                        _config.value = loadedConfig
                        Timber.tag(TAG).i("Config loaded from local file")
                        return@withContext
                    }
                }

                // Try loading from service if connected
                if (ServiceClient.isServiceAvailable()) {
                    val serviceJson = ServiceClient.readConfig()
                    if (serviceJson != null) {
                        val serviceConfig = JsonConfig.parse(serviceJson)
                        if (serviceConfig != null) {
                            _config.value = serviceConfig
                            saveConfigInternal(serviceConfig) // Cache locally
                            Timber.tag(TAG).i("Config loaded from service")
                            return@withContext
                        }
                    }
                }

                // Use default config
                val defaultConfig = JsonConfig.createDefault()
                _config.value = defaultConfig
                saveConfigInternal(defaultConfig)
                Timber.tag(TAG).i("Created default config")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to load config")
                _config.value = JsonConfig.createDefault()
            }
        }
    }

    /**
     * Saves the current configuration to local file and syncs to service.
     */
    fun saveConfig() {
        scope.launch {
            saveConfigInternal(_config.value)
        }
    }

    /**
     * Internal save method.
     */
    private suspend fun saveConfigInternal(config: JsonConfig) {
        withContext(Dispatchers.IO) {
            try {
                val json = config.toJsonString()

                // Save to local file
                configFile.writeText(json)
                Timber.tag(TAG).d("Config saved to local file")

                // Sync to service if connected
                if (ServiceClient.isServiceAvailable()) {
                    if (ServiceClient.writeConfig(json)) {
                        Timber.tag(TAG).d("Config synced to service")
                    } else {
                        Timber.tag(TAG).w("Failed to sync config to service")
                    }
                }

                // CRITICAL: Sync to XposedPrefs for cross-process access
                // This enables hooked apps to read the config via XSharedPreferences
                ConfigSync.syncFromConfig(appContext, config)
                Timber.tag(TAG).d("Config synced to XposedPrefs")

            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to save config")
            }
        }
    }

    /**
     * Updates the configuration and saves.
     */
    private fun updateConfig(transform: (JsonConfig) -> JsonConfig) {
        val newConfig = transform(_config.value)
        _config.value = newConfig
        saveConfig()
    }

    // ═══════════════════════════════════════════════════════════
    // Module Settings
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets whether the module is enabled.
     */
    fun isModuleEnabled(): Boolean = _config.value.isModuleEnabled

    /**
     * Sets whether the module is enabled.
     */
    fun setModuleEnabled(enabled: Boolean) {
        updateConfig { it.copy(isModuleEnabled = enabled) }
    }

    // ═══════════════════════════════════════════════════════════
    // Group Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets all groups.
     */
    fun getAllGroups(): List<SpoofGroup> = _config.value.groups.values.toList()

    /**
     * Gets a group by ID.
     */
    fun getGroup(groupId: String): SpoofGroup? = _config.value.getGroup(groupId)

    /**
     * Creates a new group.
     */
    fun createGroup(name: String, copyFromGroupId: String? = null): SpoofGroup {
        val baseGroup = copyFromGroupId?.let { getGroup(it) }
        val newGroup = baseGroup?.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            assignedApps = emptySet() // Don't copy app assignments
        ) ?: SpoofGroup.createNew(name = name, isDefault = false)

        updateConfig { it.addOrUpdateGroup(newGroup) }
        return newGroup
    }

    /**
     * Updates an existing group.
     */
    fun updateGroup(group: SpoofGroup) {
        val updatedGroup = group.copy(updatedAt = System.currentTimeMillis())
        updateConfig { it.addOrUpdateGroup(updatedGroup) }
    }

    /**
     * Deletes a group.
     */
    fun deleteGroup(groupId: String) {
        updateConfig { it.removeGroup(groupId) }
    }

    /**
     * Gets the group for a specific app.
     */
    fun getGroupForApp(packageName: String): SpoofGroup? {
        return _config.value.getGroupForApp(packageName)
    }

    // ═══════════════════════════════════════════════════════════
    // Group Identifier Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Sets an identifier value in a group.
     */
    fun setIdentifierValue(groupId: String, type: SpoofType, value: String?) {
        val group = getGroup(groupId) ?: return
        val identifier = group.getIdentifier(type)?.copy(value = value)
            ?: DeviceIdentifier.createDefault(type).copy(value = value)
        val updatedGroup = group.setIdentifier(identifier)
        updateGroup(updatedGroup)
    }

    /**
     * Sets whether a spoof type is enabled in a group.
     */
    fun setTypeEnabled(groupId: String, type: SpoofType, enabled: Boolean) {
        val group = getGroup(groupId) ?: return
        val identifier = group.getIdentifier(type)?.copy(isEnabled = enabled)
            ?: DeviceIdentifier.createDefault(type).copy(isEnabled = enabled)
        val updatedGroup = group.setIdentifier(identifier)
        updateGroup(updatedGroup)
    }

    /**
     * Regenerates all values in a group.
     */
    fun regenerateAllValues(groupId: String) {
        val group = getGroup(groupId) ?: return
        val regenerated = group.regenerateAll()
        updateGroup(regenerated)
    }

    // ═══════════════════════════════════════════════════════════
    // App Config Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets app config for a package.
     */
    fun getAppConfig(packageName: String): AppConfig? = _config.value.getAppConfig(packageName)

    /**
     * Assigns an app to a group.
     */
    fun assignAppToGroup(packageName: String, groupId: String) {
        // Remove from old group if any
        val oldGroup = getGroupForApp(packageName)
        if (oldGroup != null && oldGroup.id != groupId) {
            val updatedOld = oldGroup.copy(assignedApps = oldGroup.assignedApps - packageName)
            updateConfig { it.addOrUpdateGroup(updatedOld) }
        }

        // Add to new group
        val newGroup = getGroup(groupId)
        if (newGroup != null) {
            val updatedNew = newGroup.copy(assignedApps = newGroup.assignedApps + packageName)
            updateConfig { it.addOrUpdateGroup(updatedNew) }
        }

        // Update app config
        val appConfig = getAppConfig(packageName)?.copy(groupId = groupId)
            ?: AppConfig(packageName = packageName, groupId = groupId)
        updateConfig { it.setAppConfig(appConfig) }
    }

    /**
     * Unassigns an app from its group.
     */
    fun unassignApp(packageName: String) {
        val group = getGroupForApp(packageName)
        if (group != null) {
            val updated = group.copy(assignedApps = group.assignedApps - packageName)
            updateConfig { it.addOrUpdateGroup(updated) }
        }

        updateConfig { it.removeAppConfig(packageName) }
    }

    /**
     * Sets whether spoofing is enabled for an app.
     */
    fun setAppEnabled(packageName: String, enabled: Boolean) {
        val appConfig = getAppConfig(packageName)?.copy(isEnabled = enabled)
            ?: AppConfig(packageName = packageName, isEnabled = enabled)
        updateConfig { it.setAppConfig(appConfig) }
    }

    // ═══════════════════════════════════════════════════════════
    // Service Sync
    // ═══════════════════════════════════════════════════════════

    /**
     * Forces a sync with the service.
     */
    fun syncWithService() {
        scope.launch {
            if (ServiceClient.isServiceAvailable()) {
                saveConfigInternal(_config.value)
                Timber.tag(TAG).i("Forced sync with service completed")
            } else {
                Timber.tag(TAG).w("Service not available for sync")
            }
        }
    }

    /**
     * Pulls config from service (overwriting local).
     */
    fun pullFromService() {
        scope.launch {
            if (ServiceClient.isServiceAvailable()) {
                val json = ServiceClient.readConfig()
                if (json != null) {
                    val serviceConfig = JsonConfig.parse(json)
                    if (serviceConfig != null) {
                        _config.value = serviceConfig
                        saveConfigInternal(serviceConfig)
                        Timber.tag(TAG).i("Pulled config from service")
                    }
                }
            }
        }
    }
}
