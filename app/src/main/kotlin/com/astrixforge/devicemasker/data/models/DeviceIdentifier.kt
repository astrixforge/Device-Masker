package com.astrixforge.devicemasker.data.models

import kotlinx.serialization.Serializable

/**
 * Represents a single device identifier that can be spoofed.
 *
 * This data class holds both the spoof type and its current value, along with metadata about
 * modification state.
 *
 * @property type The type of identifier (IMEI, MAC, etc.)
 * @property value The current value (null if not set/randomize on access)
 * @property isEnabled Whether this specific spoof type is active
 * @property lastModified Timestamp of last modification (epoch millis)
 */
@Serializable
data class DeviceIdentifier(
    val type: SpoofType,
    val value: String?,
    val isEnabled: Boolean = true,
    val lastModified: Long = System.currentTimeMillis(),
) {
    /**
     * Returns a masked version of the value for display in UI. Shows first and last few characters
     * with asterisks in between.
     */
    fun maskedValue(): String {
        val v = value ?: return "Not Set"
        if (v.length <= 6) return v

        val visibleCount =
            when (type) {
                SpoofType.IMEI,
                SpoofType.MEID,
                SpoofType.IMSI -> 4
                SpoofType.WIFI_MAC,
                SpoofType.BLUETOOTH_MAC -> 5
                SpoofType.ANDROID_ID,
                SpoofType.GSF_ID -> 4
                SpoofType.ADVERTISING_ID -> 8
                SpoofType.BUILD_FINGERPRINT -> 10
                else -> 4
            }

        return if (v.length > visibleCount * 2) {
            "${v.take(visibleCount)}${"*".repeat(v.length - visibleCount * 2)}${v.takeLast(visibleCount)}"
        } else {
            v
        }
    }

    /** Returns a display-friendly version of the value. */
    fun displayValue(): String {
        return value ?: "Auto-generate"
    }

    /** Returns the category this identifier belongs to. */
    fun category(): SpoofCategory {
        return type.category
    }

    /** Creates a copy with an updated value. */
    fun withValue(newValue: String?): DeviceIdentifier {
        return copy(value = newValue, lastModified = System.currentTimeMillis())
    }

    /** Creates a copy with toggled enabled state. */
    fun toggleEnabled(): DeviceIdentifier {
        return copy(isEnabled = !isEnabled, lastModified = System.currentTimeMillis())
    }

    companion object {
        /**
         * Creates a default DeviceIdentifier for a given type. The value will be null
         * (auto-generate on use).
         */
        fun createDefault(type: SpoofType): DeviceIdentifier {
            return DeviceIdentifier(
                type = type,
                value = null,
                isEnabled = true,
                lastModified = System.currentTimeMillis(),
            )
        }

        /** Creates a list of default identifiers for all types. */
        fun createAllDefaults(): List<DeviceIdentifier> {
            return SpoofType.entries.map { createDefault(it) }
        }

        /** Creates a list of default identifiers for a specific category. */
        fun createDefaultsForCategory(category: SpoofCategory): List<DeviceIdentifier> {
            return SpoofType.byCategory(category).map { createDefault(it) }
        }
    }
}
