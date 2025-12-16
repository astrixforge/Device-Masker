package com.astrixforge.devicemasker.data.generators

import kotlin.random.Random

/**
 * IMEI Generator with Luhn checksum validation.
 *
 * IMEI Structure (15 digits):
 * - TAC (Type Allocation Code): 8 digits - Identifies device manufacturer/model
 * - Serial Number: 6 digits - Unique device identifier
 * - Check Digit: 1 digit - Luhn checksum for validation
 *
 * This generator creates valid IMEI numbers that pass Luhn validation
 * using realistic TAC prefixes from major manufacturers.
 */
object IMEIGenerator {

    /**
     * Realistic TAC prefixes from major manufacturers.
     * These are 8-digit prefixes that identify the device type.
     *
     * Format: First 2 digits = Reporting Body Identifier
     *         Next 6 digits = Manufacturer/Model code
     */
    private val TAC_PREFIXES = listOf(
        // Samsung Galaxy series
        "35332509", "35391810", "35405607", "35421910",
        // Apple iPhone series
        "35332410", "35391105", "35420108", "35925006",
        // Google Pixel series
        "35826010", "35331510", "35380110",
        // Xiaomi/Redmi
        "86783403", "86076203", "86893003",
        // OnePlus
        "86831803", "86809403", "86468503",
        // Huawei
        "86156403", "86180603", "86445403",
        // Oppo
        "86768803", "86429203", "86720203",
        // Vivo
        "86566203", "86780203", "86608003",
        // Generic TACs (various manufacturers)
        "01234567", "45123478", "35000000"
    )

    /**
     * Generates a valid 15-digit IMEI number with Luhn checksum.
     *
     * @return A valid IMEI string (15 digits)
     */
    fun generate(): String {
        // Select a random TAC prefix
        val tac = TAC_PREFIXES.random()

        // Generate 6 random digits for the serial number
        val serial = buildString {
            repeat(6) {
                append(Random.nextInt(0, 10))
            }
        }

        // Combine TAC and serial (14 digits without check digit)
        val imeiWithoutCheck = tac + serial

        // Calculate and append Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(imeiWithoutCheck)

        return imeiWithoutCheck + checkDigit
    }

    /**
     * Generates multiple unique IMEI numbers.
     *
     * @param count Number of IMEIs to generate
     * @return List of valid IMEI strings
     */
    fun generateMultiple(count: Int): List<String> {
        return List(count) { generate() }.distinct()
    }

    /**
     * Validates an IMEI string using the Luhn algorithm.
     *
     * @param imei The IMEI string to validate
     * @return True if the IMEI is valid (15 digits and passes Luhn check)
     */
    fun isValid(imei: String): Boolean {
        // IMEI must be exactly 15 digits
        if (imei.length != 15 || !imei.all { it.isDigit() }) {
            return false
        }

        // Validate using Luhn algorithm
        return validateLuhn(imei)
    }

    /**
     * Calculates the Luhn check digit for a 14-digit partial IMEI.
     *
     * Luhn Algorithm:
     * 1. From rightmost digit, double every second digit
     * 2. If doubling results in > 9, subtract 9
     * 3. Sum all digits
     * 4. Check digit = (10 - (sum mod 10)) mod 10
     *
     * @param partial The 14-digit IMEI without check digit
     * @return The single check digit (0-9)
     */
    private fun calculateLuhnCheckDigit(partial: String): Int {
        require(partial.length == 14) { "Partial IMEI must be 14 digits" }
        require(partial.all { it.isDigit() }) { "Partial IMEI must contain only digits" }

        var sum = 0

        // Process from left to right, doubling even positions (0-indexed)
        // In typical Luhn, we double from the right; for IMEI we adjust
        for (i in partial.indices) {
            var digit = partial[i].digitToInt()

            // Double every second digit starting from position 0
            // (which becomes odd position when check digit is added)
            if (i % 2 != 0) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
        }

        return (10 - (sum % 10)) % 10
    }

    /**
     * Validates a complete IMEI using the Luhn algorithm.
     *
     * @param imei The complete 15-digit IMEI
     * @return True if the IMEI passes Luhn validation
     */
    private fun validateLuhn(imei: String): Boolean {
        var sum = 0

        for (i in imei.indices) {
            var digit = imei[i].digitToInt()

            // Double every second digit from the right (odd positions from left in 15-digit string)
            if (i % 2 != 0) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
        }

        return sum % 10 == 0
    }

    /**
     * Generates an IMEI with a specific manufacturer prefix.
     *
     * @param manufacturerHint Hint for manufacturer selection (samsung, apple, google, etc.)
     * @return A valid IMEI string for the specified manufacturer
     */
    fun generateForManufacturer(manufacturerHint: String): String {
        val tac = when (manufacturerHint.lowercase()) {
            "samsung" -> TAC_PREFIXES.filter { it.startsWith("35332") || it.startsWith("35391") }.randomOrNull()
            "apple" -> TAC_PREFIXES.filter { it.startsWith("35332") || it.startsWith("35420") || it.startsWith("35925") }.randomOrNull()
            "google", "pixel" -> TAC_PREFIXES.filter { it.startsWith("35826") || it.startsWith("35331") || it.startsWith("35380") }.randomOrNull()
            "xiaomi", "redmi" -> TAC_PREFIXES.filter { it.startsWith("867") || it.startsWith("860") || it.startsWith("868") }.randomOrNull()
            "oneplus" -> TAC_PREFIXES.filter { it.startsWith("868") }.randomOrNull()
            "huawei" -> TAC_PREFIXES.filter { it.startsWith("861") || it.startsWith("864") }.randomOrNull()
            "oppo" -> TAC_PREFIXES.filter { it.startsWith("867") || it.startsWith("864") }.randomOrNull()
            "vivo" -> TAC_PREFIXES.filter { it.startsWith("865") || it.startsWith("866") }.randomOrNull()
            else -> null
        } ?: TAC_PREFIXES.random()

        val serial = buildString {
            repeat(6) {
                append(Random.nextInt(0, 10))
            }
        }

        val imeiWithoutCheck = tac + serial
        val checkDigit = calculateLuhnCheckDigit(imeiWithoutCheck)

        return imeiWithoutCheck + checkDigit
    }
}
