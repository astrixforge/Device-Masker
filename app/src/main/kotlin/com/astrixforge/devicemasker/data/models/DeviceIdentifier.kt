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
    /** Creates a copy with an updated value. */
    fun withValue(newValue: String?): DeviceIdentifier {
        return copy(value = newValue, lastModified = System.currentTimeMillis())
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
    }
}
