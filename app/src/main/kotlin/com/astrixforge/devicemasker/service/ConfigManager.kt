package com.astrixforge.devicemasker.service

import android.content.Context
import com.astrixforge.devicemasker.common.AppConfig
import com.astrixforge.devicemasker.common.Constants
import com.astrixforge.devicemasker.common.DeviceIdentifier
import com.astrixforge.devicemasker.common.JsonConfig
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
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
 * Config Manager - Manages application configuration in HMA-OSS architecture.
 *
 * Responsibilities:
 * 1. Load/save configuration from/to local JSON file
 * 2. Sync configuration to DeviceMaskerService via AIDL
 * 3. Provide StateFlow for UI reactivity
 * 4. CRUD operations for profiles and app configs
 *
 * Data Flow:
 * - Read: Local file → ConfigManager → UI StateFlow
 * - Write: UI → ConfigManager → Local file + ServiceClient (if connected)
 */
object ConfigManager {

    private const val TAG = "ConfigManager"
    private const val CONFIG_FILE_NAME = "config.json"

    private lateinit var configFile: File
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
    // Profile Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets all profiles.
     */
    fun getAllProfiles(): List<SpoofProfile> = _config.value.profiles.values.toList()

    /**
     * Gets a profile by ID.
     */
    fun getProfile(profileId: String): SpoofProfile? = _config.value.getProfile(profileId)

    /**
     * Creates a new profile.
     */
    fun createProfile(name: String, copyFromProfileId: String? = null): SpoofProfile {
        val baseProfile = copyFromProfileId?.let { getProfile(it) }
        val newProfile = if (baseProfile != null) {
            baseProfile.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                assignedApps = emptySet() // Don't copy app assignments
            )
        } else {
            SpoofProfile.createNew(name = name, isDefault = false)
        }

        updateConfig { it.addOrUpdateProfile(newProfile) }
        return newProfile
    }

    /**
     * Updates an existing profile.
     */
    fun updateProfile(profile: SpoofProfile) {
        val updatedProfile = profile.copy(updatedAt = System.currentTimeMillis())
        updateConfig { it.addOrUpdateProfile(updatedProfile) }
    }

    /**
     * Deletes a profile.
     */
    fun deleteProfile(profileId: String) {
        updateConfig { it.removeProfile(profileId) }
    }

    /**
     * Gets the profile for a specific app.
     */
    fun getProfileForApp(packageName: String): SpoofProfile? {
        return _config.value.getProfileForApp(packageName)
    }

    // ═══════════════════════════════════════════════════════════
    // Profile Identifier Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Sets an identifier value in a profile.
     */
    fun setIdentifierValue(profileId: String, type: SpoofType, value: String?) {
        val profile = getProfile(profileId) ?: return
        val identifier = profile.getIdentifier(type)?.copy(value = value)
            ?: DeviceIdentifier.createDefault(type).copy(value = value)
        val updatedProfile = profile.setIdentifier(identifier)
        updateProfile(updatedProfile)
    }

    /**
     * Sets whether a spoof type is enabled in a profile.
     */
    fun setTypeEnabled(profileId: String, type: SpoofType, enabled: Boolean) {
        val profile = getProfile(profileId) ?: return
        val identifier = profile.getIdentifier(type)?.copy(isEnabled = enabled)
            ?: DeviceIdentifier.createDefault(type).copy(isEnabled = enabled)
        val updatedProfile = profile.setIdentifier(identifier)
        updateProfile(updatedProfile)
    }

    /**
     * Regenerates all values in a profile.
     */
    fun regenerateAllValues(profileId: String) {
        val profile = getProfile(profileId) ?: return
        val regenerated = profile.regenerateAll()
        updateProfile(regenerated)
    }

    // ═══════════════════════════════════════════════════════════
    // App Config Management
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets app config for a package.
     */
    fun getAppConfig(packageName: String): AppConfig? = _config.value.getAppConfig(packageName)

    /**
     * Assigns an app to a profile.
     */
    fun assignAppToProfile(packageName: String, profileId: String) {
        // Remove from old profile if any
        val oldProfile = getProfileForApp(packageName)
        if (oldProfile != null && oldProfile.id != profileId) {
            val updatedOld = oldProfile.copy(assignedApps = oldProfile.assignedApps - packageName)
            updateConfig { it.addOrUpdateProfile(updatedOld) }
        }

        // Add to new profile
        val newProfile = getProfile(profileId)
        if (newProfile != null) {
            val updatedNew = newProfile.copy(assignedApps = newProfile.assignedApps + packageName)
            updateConfig { it.addOrUpdateProfile(updatedNew) }
        }

        // Update app config
        val appConfig = getAppConfig(packageName)?.copy(profileId = profileId)
            ?: AppConfig(packageName = packageName, profileId = profileId)
        updateConfig { it.setAppConfig(appConfig) }
    }

    /**
     * Unassigns an app from its profile.
     */
    fun unassignApp(packageName: String) {
        val profile = getProfileForApp(packageName)
        if (profile != null) {
            val updated = profile.copy(assignedApps = profile.assignedApps - packageName)
            updateConfig { it.addOrUpdateProfile(updated) }
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
