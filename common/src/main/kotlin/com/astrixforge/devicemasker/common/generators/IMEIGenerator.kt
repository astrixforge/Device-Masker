package com.astrixforge.devicemasker.common.generators

import java.security.SecureRandom

/**
 * IMEI Generator with Luhn checksum validation.
 *
 * IMEI Structure (15 digits):
 * - TAC (Type Allocation Code): 8 digits - Identifies device manufacturer/model
 * - Serial Number: 6 digits - Unique device identifier
 * - Check Digit: 1 digit - Luhn checksum for validation
 *
 * This generator creates valid IMEI numbers that pass Luhn validation using realistic TAC prefixes
 * from major manufacturers.
 */
object IMEIGenerator {

    /**
     * Secure random instance for cryptographic-quality randomness.
     */
    private val secureRandom = SecureRandom()

    /**
     * Realistic TAC prefixes from major manufacturers. These are 8-digit prefixes that identify the
     * device type.
     *
     * Format: First 2 digits = Reporting Body Identifier Next 6 digits = Manufacturer/Model code
     */
    private val TAC_PREFIXES =
        listOf(
            // ═══════════════════════════════════════════════════════════
            // Samsung Galaxy (S23, S24, S25, A series, Z Fold/Flip)
            // ═══════════════════════════════════════════════════════════
            "35332509", "35391810", "35405607", "35421910",
            "35512025", "35640125", "35478525", // S24/S25 Ultra
            "35692024", "35703024", // Galaxy Z Fold/Flip 6
            "35284110", "35357615", // Galaxy A series
            
            // ═══════════════════════════════════════════════════════════
            // Apple iPhone (14, 15, 16 series)
            // ═══════════════════════════════════════════════════════════
            "35332410", "35391105", "35420108", "35925006",
            "35393024", "35484916", "35548516", // iPhone 15
            "35769024", "35821024", "35892025", // iPhone 16/16 Pro
            
            // ═══════════════════════════════════════════════════════════
            // Google Pixel (7, 8, 9 series)
            // ═══════════════════════════════════════════════════════════
            "35826010", "35331510", "35380110",
            "35888110", "35123410", "35923011", // Pixel 8/8 Pro
            "35945024", "35967024", "35989025", // Pixel 9/9 Pro
            
            // ═══════════════════════════════════════════════════════════
            // Xiaomi/Redmi (14, 15 series)
            // ═══════════════════════════════════════════════════════════
            "86783403", "86076203", "86893003",
            "86945024", "86967024", // Xiaomi 14/15 Ultra
            "86912024", "86934024", // Redmi Note 13/14
            
            // ═══════════════════════════════════════════════════════════
            // OnePlus (12, 13)
            // ═══════════════════════════════════════════════════════════
            "86831803", "86809403", "86468503",
            "86899603", "86912503", // OnePlus 12/13
            
            // ═══════════════════════════════════════════════════════════
            // Nothing Phone (2, 2a, 3)
            // ═══════════════════════════════════════════════════════════
            "86454403", "86389203",
            "86923024", "86945024", // Nothing Phone 2a/3
            
            // ═══════════════════════════════════════════════════════════
            // Other brands (Huawei, Oppo, Vivo, Realme, Motorola, Sony)
            // ═══════════════════════════════════════════════════════════
            // Huawei
            "86156403", "86180603", "86445403",
            // Oppo
            "86768803", "86429203", "86720203",
            // Vivo / iQOO
            "86566203", "86780203", "86608003",
            "86934024", "86956024", // iQOO 12/13
            // Realme
            "86725403", "86892103", "86934124", // Realme 12/13
            // Motorola
            "35154711", "35185108", "35186609",
            // Sony Xperia
            "35618715", "35846806", "35874108",
            
            // ═══════════════════════════════════════════════════════════
            // Generic TACs (fallback)
            // ═══════════════════════════════════════════════════════════
            "01234567", "45123478", "35000000",
        )

    /**
     * Generates a valid 15-digit IMEI number with Luhn checksum.
     * 
     * @param manufacturer Optional manufacturer name to filter TAC prefixes
     * @return A valid IMEI string (15 digits)
     */
    fun generate(manufacturer: String? = null): String {
        // Filter TAC prefixes if manufacturer is provided
        val filteredTacs = if (manufacturer != null) {
            val lowerManufacturer = manufacturer.lowercase()
            when {
                lowerManufacturer.contains("samsung") -> TAC_PREFIXES.filter { it.startsWith("3533") || it.startsWith("3539") || it.startsWith("354") || it.startsWith("355") || it.startsWith("356") || it.startsWith("357") }
                lowerManufacturer.contains("apple") || lowerManufacturer.contains("iphone") -> TAC_PREFIXES.filter { it.startsWith("35") && !it.startsWith("3533") && !it.startsWith("3539") } // Simplified logic
                lowerManufacturer.contains("google") || lowerManufacturer.contains("pixel") -> TAC_PREFIXES.filter { it.startsWith("3582") || it.startsWith("35331") || it.startsWith("3538") || it.startsWith("3588") || it.startsWith("3512") || it.startsWith("3592") || it.startsWith("359") }
                lowerManufacturer.contains("xiaomi") || lowerManufacturer.contains("redmi") || lowerManufacturer.contains("poco") -> TAC_PREFIXES.filter { it.startsWith("867834") || it.startsWith("860762") || it.startsWith("86") }
                lowerManufacturer.contains("oneplus") -> TAC_PREFIXES.filter { it.startsWith("868318") || it.startsWith("868094") || it.startsWith("864685") || it.startsWith("868996") || it.startsWith("869125") }
                else -> TAC_PREFIXES
            }
        } else {
            TAC_PREFIXES
        }

        // Fallback to all TACs if filtered list is empty
        val finalTacs = filteredTacs.ifEmpty { TAC_PREFIXES }
        
        // Select a random TAC prefix from the eligible ones
        val tac = finalTacs[secureRandom.nextInt(finalTacs.size)]

        // Generate 6 random digits for the serial number
        val serial = buildString { repeat(6) { append(secureRandom.nextInt(10)) } }

        // Combine TAC and serial (14 digits without check digit)
        val imeiWithoutCheck = tac + serial

        // Calculate and append Luhn check digit
        val checkDigit = calculateLuhnCheckDigit(imeiWithoutCheck)

        return imeiWithoutCheck + checkDigit
    }

    /**
     * Calculates the Luhn check digit for a 14-digit partial IMEI.
     *
     * @param partial The 14-digit IMEI without check digit
     * @return The single check digit (0-9)
     */
    private fun calculateLuhnCheckDigit(partial: String): Int {
        require(partial.length == 14) { "Partial IMEI must be 14 digits" }
        require(partial.all { it.isDigit() }) { "Partial IMEI must contain only digits" }

        var sum = 0

        for (i in partial.indices) {
            var digit = partial[i].digitToInt()

            // Double every second digit starting from position 1
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
}
