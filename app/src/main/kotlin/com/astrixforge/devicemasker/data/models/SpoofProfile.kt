package com.astrixforge.devicemasker.data.models

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * A named collection of spoofed device identifiers.
 *
 * Profiles allow users to save and switch between different device identities. Each profile
 * contains values for all supported spoof types and can be assigned to specific apps or set as the
 * default.
 *
 * @property id Unique identifier for this profile
 * @property name User-defined profile name
 * @property description Optional description
 * @property isEnabled Whether spoofing is active for this profile (master switch)
 * @property isDefault Whether this is the default profile for new apps
 * @property createdAt Timestamp when profile was created (epoch millis)
 * @property updatedAt Timestamp of last modification (epoch millis)
 * @property identifiers Map of SpoofType to DeviceIdentifier values
 */
@Serializable
data class SpoofProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val identifiers: Map<SpoofType, DeviceIdentifier> = emptyMap(),
    val assignedApps: Set<String> = emptySet(),
) {
    /**
     * Gets a specific identifier value from this profile.
     *
     * @param type The spoof type to look up
     * @return The DeviceIdentifier or null if not set
     */
    fun getIdentifier(type: SpoofType): DeviceIdentifier? {
        return identifiers[type]
    }

    /**
     * Gets the spoofed value for a specific type.
     *
     * @param type The spoof type to look up
     * @return The spoofed value or null if not set
     */
    fun getValue(type: SpoofType): String? {
        return identifiers[type]?.value
    }

    /**
     * Checks if a specific spoof type is enabled in this profile.
     *
     * @param type The spoof type to check
     * @return True if the type is enabled
     */
    fun isTypeEnabled(type: SpoofType): Boolean {
        return identifiers[type]?.isEnabled ?: true
    }

    /** Returns the count of configured (non-null) identifier values. */
    fun configuredCount(): Int {
        return identifiers.count { it.value.value != null }
    }

    /** Returns the count of enabled identifiers. */
    fun enabledCount(): Int {
        return identifiers.count { it.value.isEnabled }
    }

    /** Returns all identifiers for a specific category. */
    fun getIdentifiersByCategory(category: SpoofCategory): List<DeviceIdentifier> {
        return identifiers.values.filter { it.type.category == category }
    }

    /** Creates a copy with an updated identifier. */
    fun withIdentifier(identifier: DeviceIdentifier): SpoofProfile {
        val newIdentifiers = identifiers.toMutableMap()
        newIdentifiers[identifier.type] = identifier
        return copy(identifiers = newIdentifiers, updatedAt = System.currentTimeMillis())
    }

    /** Creates a copy with an updated value for a specific type. */
    fun withValue(type: SpoofType, value: String?): SpoofProfile {
        val existing = identifiers[type] ?: DeviceIdentifier.createDefault(type)
        return withIdentifier(existing.withValue(value))
    }

    /** Creates a copy with toggled enabled state for a specific type. */
    fun withTypeToggled(type: SpoofType): SpoofProfile {
        val existing = identifiers[type] ?: DeviceIdentifier.createDefault(type)
        return withIdentifier(existing.toggleEnabled())
    }

    /** Creates a copy with updated name. */
    fun withName(newName: String): SpoofProfile {
        return copy(name = newName, updatedAt = System.currentTimeMillis())
    }

    /** Creates a copy marked as default (or not). */
    fun withDefault(isDefault: Boolean): SpoofProfile {
        return copy(isDefault = isDefault, updatedAt = System.currentTimeMillis())
    }

    /** Creates a copy with updated enabled state. */
    fun withEnabled(enabled: Boolean): SpoofProfile {
        return copy(isEnabled = enabled, updatedAt = System.currentTimeMillis())
    }

    /** Creates a copy with toggled enabled state. */
    fun toggleEnabled(): SpoofProfile {
        return copy(isEnabled = !isEnabled, updatedAt = System.currentTimeMillis())
    }

    // ═══════════════════════════════════════════════════════════
    // ASSIGNED APPS MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks if an app is assigned to this profile.
     *
     * @param packageName The app's package name to check
     * @return True if the app is assigned to this profile
     */
    fun isAppAssigned(packageName: String): Boolean {
        return packageName in assignedApps
    }

    /**
     * Creates a copy with an app added to assignedApps.
     *
     * @param packageName The app's package name to add
     * @return Updated SpoofProfile with the app assigned
     */
    fun addApp(packageName: String): SpoofProfile {
        val newAssignedApps = assignedApps + packageName
        return copy(assignedApps = newAssignedApps, updatedAt = System.currentTimeMillis())
    }

    /**
     * Creates a copy with an app removed from assignedApps.
     *
     * @param packageName The app's package name to remove
     * @return Updated SpoofProfile with the app removed
     */
    fun removeApp(packageName: String): SpoofProfile {
        val newAssignedApps = assignedApps - packageName
        return copy(assignedApps = newAssignedApps, updatedAt = System.currentTimeMillis())
    }

    /**
     * Creates a copy with a new set of assigned apps.
     *
     * @param apps The new set of assigned app package names
     * @return Updated SpoofProfile with the new assigned apps
     */
    fun withAssignedApps(apps: Set<String>): SpoofProfile {
        return copy(assignedApps = apps, updatedAt = System.currentTimeMillis())
    }

    /** Returns the count of assigned apps. */
    fun assignedAppCount(): Int = assignedApps.size

    /** Returns a summary string for display in lists. */
    fun summary(): String {
        val enabled = enabledCount()
        val configured = configuredCount()
        return "$configured configured, $enabled enabled"
    }

    companion object {
        /**
         * Creates a new profile with default values.
         *
         * @param name The profile name
         * @param isDefault Whether this should be the default profile
         * @return A new SpoofProfile with all identifier types initialized
         */
        fun createNew(name: String, isDefault: Boolean = false): SpoofProfile {
            val defaultIdentifiers =
                SpoofType.entries.associateWith { type -> DeviceIdentifier.createDefault(type) }

            return SpoofProfile(
                name = name,
                isDefault = isDefault,
                identifiers = defaultIdentifiers,
            )
        }

        /** Creates a "Default" profile that is automatically applied. */
        fun createDefaultProfile(): SpoofProfile {
            return createNew(name = "Default", isDefault = true)
        }

        /**
         * Creates a profile pre-configured for a specific manufacturer.
         *
         * @param name Profile name
         * @param manufacturer Target manufacturer (google, samsung, etc.)
         */
        fun createForManufacturer(name: String, manufacturer: String): SpoofProfile {
            val profile = createNew(name)

            // Import build properties for the manufacturer
            val buildProps =
                com.astrixforge.devicemasker.data.generators.FingerprintGenerator
                    .generateBuildProperties(manufacturer)

            var result = profile

            buildProps["FINGERPRINT"]?.let {
                result = result.withValue(SpoofType.BUILD_FINGERPRINT, it)
            }
            buildProps["MODEL"]?.let { result = result.withValue(SpoofType.BUILD_MODEL, it) }
            buildProps["MANUFACTURER"]?.let {
                result = result.withValue(SpoofType.BUILD_MANUFACTURER, it)
            }
            buildProps["BRAND"]?.let { result = result.withValue(SpoofType.BUILD_BRAND, it) }
            buildProps["DEVICE"]?.let { result = result.withValue(SpoofType.BUILD_DEVICE, it) }
            buildProps["PRODUCT"]?.let { result = result.withValue(SpoofType.BUILD_PRODUCT, it) }

            return result
        }
    }
}
