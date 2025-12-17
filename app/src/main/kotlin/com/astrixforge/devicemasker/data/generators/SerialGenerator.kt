package com.astrixforge.devicemasker.data.generators

import kotlin.random.Random

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

    /** Numeric-only characters for certain serial sections. */
    private const val NUMERIC_CHARS = "0123456789"

    /** Letter-only characters for certain serial sections. */
    private const val LETTER_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ"

    /** Common serial prefixes by manufacturer. */
    private val MANUFACTURER_PREFIXES =
        mapOf(
            "samsung" to listOf("R58M", "R5CT", "R9WM", "RFXM", "R5CN", "R3CN"),
            "google" to listOf("HT7", "89A", "9B1", "FA6", "19A"),
            "xiaomi" to listOf("XM", "HB"),
            "oneplus" to listOf("A1", "IN"),
            "huawei" to listOf("HW", "P3"),
            "oppo" to listOf("OP", "RMX"),
            "vivo" to listOf("VV", "V2"),
            "generic" to listOf("UN", "XX", "00"),
        )

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

    /**
     * Generates a serial number with a manufacturer-specific format.
     *
     * @param manufacturer The manufacturer name (samsung, google, xiaomi, etc.)
     * @return A formatted serial string matching the manufacturer's pattern
     */
    fun generateForManufacturer(manufacturer: String): String {
        return when (manufacturer.lowercase()) {
            "samsung" -> generateSamsungSerial()
            "google",
            "pixel" -> generateGoogleSerial()
            "xiaomi",
            "redmi" -> generateXiaomiSerial()
            "oneplus" -> generateOnePlusSerial()
            "huawei" -> generateHuaweiSerial()
            "oppo" -> generateOppoSerial()
            "vivo" -> generateVivoSerial()
            else -> generate()
        }
    }

    /**
     * Generates a Samsung-style serial number. Format: R + 3 alphanumeric + 8 alphanumeric (e.g.,
     * "R58M12345ABC")
     */
    private fun generateSamsungSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["samsung"]?.random() ?: "R58M"
        val suffix = buildString {
            repeat(11 - prefix.length) { append(ALPHANUMERIC_CHARS.random()) }
        }
        return prefix + suffix
    }

    /** Generates a Google Pixel-style serial number. Format: 3-letter prefix + 8-9 alphanumeric */
    private fun generateGoogleSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["google"]?.random() ?: "HT7"
        val suffix = buildString {
            repeat(12 - prefix.length) { append(ALPHANUMERIC_CHARS.random()) }
        }
        return prefix + suffix
    }

    /** Generates a Xiaomi-style serial number. Format: 2-letter prefix + 10-14 alphanumeric */
    private fun generateXiaomiSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["xiaomi"]?.random() ?: "XM"
        val length = Random.nextInt(12, 17) // 12-16 total characters
        val suffix = buildString {
            repeat(length - prefix.length) { append(ALPHANUMERIC_CHARS.random()) }
        }
        return prefix + suffix
    }

    /** Generates a OnePlus-style serial number. Format: 2-letter prefix + 10 alphanumeric */
    private fun generateOnePlusSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["oneplus"]?.random() ?: "A1"
        val suffix = buildString { repeat(10) { append(ALPHANUMERIC_CHARS.random()) } }
        return prefix + suffix
    }

    /** Generates a Huawei-style serial number. Format: 2-letter prefix + 10 alphanumeric */
    private fun generateHuaweiSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["huawei"]?.random() ?: "HW"
        val suffix = buildString { repeat(12) { append(ALPHANUMERIC_CHARS.random()) } }
        return prefix + suffix
    }

    /** Generates an Oppo-style serial number. Format: 2-3 letter prefix + 9-11 alphanumeric */
    private fun generateOppoSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["oppo"]?.random() ?: "OP"
        val suffix = buildString {
            repeat(12 - prefix.length) { append(ALPHANUMERIC_CHARS.random()) }
        }
        return prefix + suffix
    }

    /** Generates a Vivo-style serial number. Format: 2-letter prefix + 10 alphanumeric */
    private fun generateVivoSerial(): String {
        val prefix = MANUFACTURER_PREFIXES["vivo"]?.random() ?: "VV"
        val suffix = buildString { repeat(12) { append(ALPHANUMERIC_CHARS.random()) } }
        return prefix + suffix
    }

    /**
     * Validates a serial number format.
     *
     * @param serial The serial number to validate
     * @return True if the serial appears to be valid (8-16 alphanumeric characters)
     */
    fun isValid(serial: String): Boolean {
        return serial.length in 8..16 &&
            serial.all { it.isLetterOrDigit() } &&
            serial.all { it.isUpperCase() || it.isDigit() }
    }

    /**
     * Generates multiple unique serial numbers.
     *
     * @param count Number of serials to generate
     * @return List of unique serial strings
     */
    fun generateMultiple(count: Int): List<String> {
        return List(count) { generate() }.distinct()
    }
}
