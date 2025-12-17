package com.astrixforge.devicemasker.data.generators

import java.util.UUID
import kotlin.random.Random

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
     * Generates a standard UUID v4.
     *
     * @return A UUID string in standard format
     */
    fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Generates a random instance ID (used by Firebase and other services). Instance IDs are
     * typically 22-character base64-like strings.
     *
     * @return A 22-character instance ID
     */
    fun generateInstanceId(): String {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        return buildString { repeat(22) { append(base64Chars.random()) } }
    }

    /**
     * Generates a random SSAID (Settings.Secure.ANDROID_ID value). This is functionally identical
     * to Android ID.
     *
     * @return A 16-character lowercase hex string
     */
    fun generateSSAID(): String {
        return generateAndroidId()
    }

    /**
     * Validates an Android ID format.
     *
     * @param androidId The Android ID to validate
     * @return True if the format is valid (16 hex characters)
     */
    fun isValidAndroidId(androidId: String): Boolean {
        return androidId.length == 16 && androidId.all { it in HEX_CHARS }
    }

    /**
     * Validates an Advertising ID format.
     *
     * @param advertisingId The Advertising ID to validate
     * @return True if the format is a valid UUID
     */
    fun isValidAdvertisingId(advertisingId: String): Boolean {
        return try {
            UUID.fromString(advertisingId)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Validates a GSF ID format.
     *
     * @param gsfId The GSF ID to validate
     * @return True if the format is valid (16 hex characters)
     */
    fun isValidGSFId(gsfId: String): Boolean {
        return gsfId.length == 16 && gsfId.all { it in HEX_CHARS }
    }

    /**
     * Validates a Media DRM ID format.
     *
     * @param drmId The DRM ID to validate
     * @return True if the format is valid (64 hex characters)
     */
    fun isValidMediaDrmId(drmId: String): Boolean {
        return drmId.length == 64 && drmId.all { it in HEX_CHARS }
    }

    /**
     * Generates multiple unique Android IDs.
     *
     * @param count Number of IDs to generate
     * @return List of unique Android ID strings
     */
    fun generateMultipleAndroidIds(count: Int): List<String> {
        return List(count) { generateAndroidId() }.distinct()
    }

    /**
     * Generates a consistent Android ID based on a seed. Useful for generating reproducible IDs for
     * the same profile.
     *
     * @param seed The seed string (e.g., profile name)
     * @return A deterministic Android ID based on the seed
     */
    fun generateDeterministicAndroidId(seed: String): String {
        val random = Random(seed.hashCode().toLong())
        return buildString { repeat(16) { append(HEX_CHARS[random.nextInt(16)]) } }
    }

    /**
     * Generates a zero/null advertising ID (for opt-out scenarios). This represents a user who has
     * opted out of ad personalization.
     *
     * @return "00000000-0000-0000-0000-000000000000"
     */
    fun generateOptOutAdvertisingId(): String {
        return "00000000-0000-0000-0000-000000000000"
    }
}
