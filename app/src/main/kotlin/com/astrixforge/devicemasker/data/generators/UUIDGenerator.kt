package com.astrixforge.devicemasker.data.generators

import java.util.UUID

/**
 * UUID and ID Generator for various Android identifiers.
 *
 * This generator handles:
 * - Android ID (16 hex characters)
 * - Advertising ID (UUID format: 8-4-4-4-12)
 * - GSF ID (Google Services Framework ID - 16 hex characters)
 * - Media DRM ID (Widevine device ID)
 * - GUID (Generic UUID v4)
 */
object UUIDGenerator {

    /** Hex characters for ID generation. */
    private const val HEX_CHARS = "0123456789abcdef"

    /**
     * Generates a random Android ID. Android ID is a 64-bit number (16 hex characters) unique to
     * each app/device combination.
     *
     * @return A 16-character lowercase hex string
     */
    fun generateAndroidId(): String {
        return buildString { repeat(16) { append(HEX_CHARS.random()) } }
    }

    /**
     * Generates a random Advertising ID (AAID). The Advertising ID is a UUID v4 format identifier
     * used for ad tracking.
     *
     * Format: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx Where y is one of: 8, 9, a, b
     *
     * @return A UUID-formatted advertising ID
     */
    fun generateAdvertisingId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Generates a random GSF ID (Google Services Framework ID). GSF ID is a 64-bit number (16 hex
     * characters) assigned by Google Play Services.
     *
     * @return A 16-character lowercase hex string
     */
    fun generateGSFId(): String {
        // GSF ID is essentially the same format as Android ID
        return buildString { repeat(16) { append(HEX_CHARS.random()) } }
    }

    /**
     * Generates a random Media DRM ID (Widevine Device ID). This is typically a 32-byte (64 hex
     * characters) identifier.
     *
     * @return A 64-character hex string representing a device DRM ID
     */
    fun generateMediaDrmId(): String {
        return buildString { repeat(64) { append(HEX_CHARS.random()) } }
    }

    /**
     * Generates a random Instance ID (used by Firebase/Google Play Services).
     * Typically a 22-character random string.
     *
     * @return A random alphanumeric string
     */
    fun generateInstanceId(): String {
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_-"
        return buildString { repeat(22) { append(chars.random()) } }
    }
}
