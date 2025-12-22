package com.astrixforge.devicemasker.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Main configuration container for Device Masker.
 *
 * This is the root object that holds all module configuration, including:
 * - Module enabled state
 * - All spoof groups
 * - Per-app configurations
 *
 * The configuration is serialized to JSON and stored both:
 * - In the app's data directory (for UI persistence)
 * - In /data/system/devicemasker/ (for xposed module access)
 *
 * @property version Configuration format version for migrations
 * @property isModuleEnabled Global module enable/disable switch
 * @property groups Map of group ID to SpoofGroup
 * @property appConfigs Map of package name to AppConfig
 */
@Serializable
data class JsonConfig(
    val version: Int = Constants.CONFIG_VERSION,
    val isModuleEnabled: Boolean = true,
    @SerialName("profiles")
    val groups: Map<String, SpoofGroup> = emptyMap(),
    val appConfigs: Map<String, AppConfig> = emptyMap(),
) {
    // ═══════════════════════════════════════════════════════════
    // GROUP MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets a group by ID.
     * @return The group or null if not found
     */
    fun getGroup(groupId: String): SpoofGroup? {
        return groups[groupId]
    }

    /**
     * Gets the default group (the one with isDefault = true).
     * @return The default group or null if none is set
     */
    fun getDefaultGroup(): SpoofGroup? {
        return groups.values.find { it.isDefault }
    }

    /**
     * Gets all groups as a list.
     */
    fun getAllGroups(): List<SpoofGroup> {
        return groups.values.toList()
    }

    /**
     * Adds or updates a group.
     * @return Updated JsonConfig
     */
    fun withGroup(group: SpoofGroup): JsonConfig {
        val newGroups = groups.toMutableMap()
        newGroups[group.id] = group
        return copy(groups = newGroups)
    }

    /**
     * Alias for withGroup - adds or updates a group.
     * @return Updated JsonConfig
     */
    fun addOrUpdateGroup(group: SpoofGroup): JsonConfig = withGroup(group)

    /**
     * Removes a group by ID.
     * @return Updated JsonConfig
     */
    fun removeGroup(groupId: String): JsonConfig {
        val newGroups = groups.toMutableMap()
        newGroups.remove(groupId)
        return copy(groups = newGroups)
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
     * Gets the group assigned to an app.
     * Falls back to default group if no specific assignment.
     * @return The assigned group, default group, or null
     */
    fun getGroupForApp(packageName: String): SpoofGroup? {
        val appConfig = appConfigs[packageName]

        // If app is explicitly disabled, return null
        if (appConfig?.isEnabled == false) return null

        // If app has a specific group assignment, use it
        appConfig?.groupId?.let { groupId ->
            groups[groupId]?.let { return it }
        }

        // Fall back to default group
        return getDefaultGroup()
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
         * Creates a default configuration with an initial group.
         */
        fun createDefault(): JsonConfig {
            val defaultGroup = SpoofGroup.createDefaultGroup()
            return JsonConfig(
                groups = mapOf(defaultGroup.id to defaultGroup),
            )
        }
    }
}
