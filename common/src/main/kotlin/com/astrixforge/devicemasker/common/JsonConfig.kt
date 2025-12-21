package com.astrixforge.devicemasker.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Main configuration container for Device Masker.
 *
 * This is the root object that holds all module configuration, including:
 * - Module enabled state
 * - All spoof profiles
 * - Per-app configurations
 *
 * The configuration is serialized to JSON and stored both:
 * - In the app's data directory (for UI persistence)
 * - In /data/system/devicemasker/ (for xposed module access)
 *
 * @property version Configuration format version for migrations
 * @property isModuleEnabled Global module enable/disable switch
 * @property profiles Map of profile ID to SpoofProfile
 * @property appConfigs Map of package name to AppConfig
 */
@Serializable
data class JsonConfig(
    val version: Int = Constants.CONFIG_VERSION,
    val isModuleEnabled: Boolean = true,
    val profiles: Map<String, SpoofProfile> = emptyMap(),
    val appConfigs: Map<String, AppConfig> = emptyMap(),
) {
    // ═══════════════════════════════════════════════════════════
    // PROFILE MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets a profile by ID.
     * @return The profile or null if not found
     */
    fun getProfile(profileId: String): SpoofProfile? {
        return profiles[profileId]
    }

    /**
     * Gets the default profile (the one with isDefault = true).
     * @return The default profile or null if none is set
     */
    fun getDefaultProfile(): SpoofProfile? {
        return profiles.values.find { it.isDefault }
    }

    /**
     * Gets all profiles as a list.
     */
    fun getAllProfiles(): List<SpoofProfile> {
        return profiles.values.toList()
    }

    /**
     * Adds or updates a profile.
     * @return Updated JsonConfig
     */
    fun withProfile(profile: SpoofProfile): JsonConfig {
        val newProfiles = profiles.toMutableMap()
        newProfiles[profile.id] = profile
        return copy(profiles = newProfiles)
    }

    /**
     * Alias for withProfile - adds or updates a profile.
     * @return Updated JsonConfig
     */
    fun addOrUpdateProfile(profile: SpoofProfile): JsonConfig = withProfile(profile)

    /**
     * Removes a profile by ID.
     * @return Updated JsonConfig
     */
    fun removeProfile(profileId: String): JsonConfig {
        val newProfiles = profiles.toMutableMap()
        newProfiles.remove(profileId)
        return copy(profiles = newProfiles)
    }

    // ═══════════════════════════════════════════════════════════
    // APP CONFIG MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets app configuration for a package.
     * @return The AppConfig or null if not configured
     */
    fun getAppConfig(packageName: String): AppConfig? {
        return appConfigs[packageName]
    }

    /**
     * Gets the profile assigned to an app.
     * Falls back to default profile if no specific assignment.
     * @return The assigned profile, default profile, or null
     */
    fun getProfileForApp(packageName: String): SpoofProfile? {
        val appConfig = appConfigs[packageName]

        // If app is explicitly disabled, return null
        if (appConfig?.isEnabled == false) return null

        // If app has a specific profile assignment, use it
        appConfig?.profileId?.let { profileId ->
            profiles[profileId]?.let { return it }
        }

        // Fall back to default profile
        return getDefaultProfile()
    }

    /**
     * Adds or updates an app configuration.
     * @return Updated JsonConfig
     */
    fun withAppConfig(appConfig: AppConfig): JsonConfig {
        val newAppConfigs = appConfigs.toMutableMap()
        newAppConfigs[appConfig.packageName] = appConfig
        return copy(appConfigs = newAppConfigs)
    }

    /**
     * Alias for withAppConfig - sets app configuration.
     * @return Updated JsonConfig
     */
    fun setAppConfig(appConfig: AppConfig): JsonConfig = withAppConfig(appConfig)

    /**
     * Removes app configuration for a package.
     * @return Updated JsonConfig
     */
    fun removeAppConfig(packageName: String): JsonConfig {
        val newAppConfigs = appConfigs.toMutableMap()
        newAppConfigs.remove(packageName)
        return copy(appConfigs = newAppConfigs)
    }

    // ═══════════════════════════════════════════════════════════
    // SERIALIZATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Serializes this config to a JSON string.
     */
    fun toJsonString(): String {
        return jsonSerializer.encodeToString(this)
    }

    /**
     * Serializes this config to a pretty-printed JSON string.
     */
    fun toPrettyJsonString(): String {
        return prettyJsonSerializer.encodeToString(this)
    }

    companion object {
        // JSON serializer for compact output
        private val jsonSerializer = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        // JSON serializer for pretty output
        private val prettyJsonSerializer = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = true
        }

        /**
         * Parses a JSON string into a JsonConfig.
         * @param json The JSON string to parse
         * @return Parsed JsonConfig
         * @throws Exception if parsing fails
         */
        fun parse(json: String): JsonConfig {
            return jsonSerializer.decodeFromString<JsonConfig>(json)
        }

        /**
         * Safely parses a JSON string, returning a default config on failure.
         * @param json The JSON string to parse
         * @return Parsed JsonConfig or default on error
         */
        fun parseOrDefault(json: String?): JsonConfig {
            if (json.isNullOrBlank()) return createDefault()
            return try {
                parse(json)
            } catch (e: Exception) {
                createDefault()
            }
        }

        /**
         * Creates a default configuration with an initial profile.
         */
        fun createDefault(): JsonConfig {
            val defaultProfile = SpoofProfile.createDefaultProfile()
            return JsonConfig(
                profiles = mapOf(defaultProfile.id to defaultProfile),
            )
        }
    }
}
