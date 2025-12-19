package com.astrixforge.devicemasker.data.generators


/**
 * Serial Number Generator for device spoofing.
 *
 * Device serial numbers vary by manufacturer but typically follow these patterns:
 * - Samsung: 11 alphanumeric characters (e.g., "R58M12345AB")
 * - Google Pixel: 11-12 alphanumeric characters
 * - Xiaomi: 12-16 alphanumeric characters
 * - Generic: 8-16 alphanumeric characters
 *
 * All generated serials are uppercase alphanumeric.
 */
object SerialGenerator {

    /**
     * Characters used in serial number generation. Excludes easily confused characters (I, O, Q).
     */
    private const val ALPHANUMERIC_CHARS = "0123456789ABCDEFGHJKLMNPRSTUVWXYZ"

    /**
     * Generates a random device serial number.
     *
     * @param length The length of the serial (default: 11)
     * @return A random alphanumeric serial string
     */
    fun generate(length: Int = 11): String {
        require(length in 8..16) { "Serial length must be between 8 and 16 characters" }

        return buildString { repeat(length) { append(ALPHANUMERIC_CHARS.random()) } }
    }
}
