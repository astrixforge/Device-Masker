package com.astrixforge.devicemasker.data.models

import kotlinx.serialization.Serializable

/**
 * Configuration for spoofing behavior on a per-app basis.
 *
 * This data class defines which apps should have their device identifiers spoofed and which
 * profile/types to use for each app.
 *
 * @property packageName The app's package name (e.g., "com.example.app")
 * @property appLabel The human-readable app name
 * @property isEnabled Whether spoofing is enabled for this app
 * @property profileId The ID of the profile to use (null = use default)
 * @property enabledSpoofs Set of enabled spoof types (empty = use all from profile)
 * @property addedAt Timestamp when app was added to scope
 * @property lastModified Timestamp of last configuration change
 */
@Serializable
data class AppConfig(
    val packageName: String,
    val appLabel: String = "",
    val isEnabled: Boolean = true,
    val profileId: String? = null,
    val enabledSpoofs: Set<SpoofType> = emptySet(),
    val addedAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
) {
    /**
     * Checks if a specific spoof type is enabled for this app. If enabledSpoofs is empty, all types
     * are enabled by default.
     *
     * @param type The spoof type to check
     * @return True if this spoof type should be active for this app
     */
    fun isSpoofTypeEnabled(type: SpoofType): Boolean {
        // If no specific types are set, all are enabled
        return enabledSpoofs.isEmpty() || type in enabledSpoofs
    }

    /** Creates a copy with toggled enabled state. */
    fun toggleEnabled(): AppConfig {
        return copy(isEnabled = !isEnabled, lastModified = System.currentTimeMillis())
    }

    /** Creates a copy with a new profile assigned. */
    fun withProfile(profileId: String?): AppConfig {
        return copy(profileId = profileId, lastModified = System.currentTimeMillis())
    }

    /** Creates a copy with a specific spoof type toggled. */
    fun toggleSpoofType(type: SpoofType): AppConfig {
        val newSet = enabledSpoofs.toMutableSet()
        if (type in newSet) {
            newSet.remove(type)
        } else {
            newSet.add(type)
        }
        return copy(enabledSpoofs = newSet, lastModified = System.currentTimeMillis())
    }

    /** Creates a copy with all spoof types enabled. */
    fun enableAllSpoofTypes(): AppConfig {
        return copy(
            enabledSpoofs = SpoofType.entries.toSet(),
            lastModified = System.currentTimeMillis(),
        )
    }

    /** Creates a copy with specified spoof types enabled. */
    fun withEnabledSpoofTypes(types: Set<SpoofType>): AppConfig {
        return copy(enabledSpoofs = types, lastModified = System.currentTimeMillis())
    }

    /**
     * Returns the count of enabled spoof types. If empty set (all enabled), returns total count.
     */
    fun enabledSpoofCount(): Int {
        return if (enabledSpoofs.isEmpty()) {
            SpoofType.entries.size
        } else {
            enabledSpoofs.size
        }
    }

    /** Returns a summary string for display. */
    fun summary(): String {
        return if (isEnabled) {
            val spoofCount = enabledSpoofCount()
            val profileInfo = if (profileId != null) "Custom Profile" else "Default Profile"
            "$spoofCount spoofs active • $profileInfo"
        } else {
            "Disabled"
        }
    }

    companion object {
        /**
         * Creates a new AppConfig with default settings.
         *
         * @param packageName The app's package name
         * @param appLabel The app's display name
         * @return A new AppConfig with spoofing enabled and using default profile
         */
        fun createNew(packageName: String, appLabel: String = ""): AppConfig {
            return AppConfig(
                packageName = packageName,
                appLabel = appLabel,
                isEnabled = true,
                profileId = null, // Use default profile
                enabledSpoofs = emptySet(), // All types enabled
            )
        }

        /** Creates an AppConfig for a system app (disabled by default). */
        fun createForSystemApp(packageName: String, appLabel: String = ""): AppConfig {
            return AppConfig(
                packageName = packageName,
                appLabel = appLabel,
                isEnabled = false, // Default disabled for system apps
                profileId = null,
                enabledSpoofs = emptySet(),
            )
        }

        /** Creates an AppConfig with a specific profile. */
        fun createWithProfile(packageName: String, appLabel: String, profileId: String): AppConfig {
            return AppConfig(
                packageName = packageName,
                appLabel = appLabel,
                isEnabled = true,
                profileId = profileId,
                enabledSpoofs = emptySet(),
            )
        }
    }
}

/**
 * Represents an installed app for display in the selection UI.
 *
 * @property packageName The app's package name
 * @property label The app's display name
 * @property isSystemApp Whether this is a system app
 * @property versionName The app's version string
 * @property config The current spoofing configuration (null if not configured)
 */
@Serializable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val versionName: String = "",
    val config: AppConfig? = null,
) {
    /** Whether spoofing is currently enabled for this app. */
    val isSpoofEnabled: Boolean
        get() = config?.isEnabled == true

    /** Whether this app has been configured for spoofing. */
    val isConfigured: Boolean
        get() = config != null

    /** Returns the app with updated config. */
    fun withConfig(newConfig: AppConfig?): InstalledApp {
        return copy(config = newConfig)
    }
}
