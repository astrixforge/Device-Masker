package com.astrixforge.devicemasker.common

/**
 * Utility functions shared between the app UI and xposed module.
 */
@Suppress("unused") // Utility functions for cross-module use
object Utils {

    /**
     * Log levels for service logging.
     */
    object LogLevel {
        const val VERBOSE = 0
        const val DEBUG = 1
        const val INFO = 2
        const val WARN = 3
        const val ERROR = 4
    }

    /**
     * Masks a value for display (e.g., in UI).
     * Shows first and last 2 characters, rest as asterisks.
     *
     * @param value The value to mask
     * @param visibleChars Number of characters to show at start and end
     * @return Masked string or original if too short
     */
    fun maskValue(value: String?, visibleChars: Int = 2): String {
        if (value.isNullOrBlank()) return "••••••••"
        if (value.length <= visibleChars * 2) return value

        val start = value.take(visibleChars)
        val end = value.takeLast(visibleChars)
        val maskLength = value.length - (visibleChars * 2)
        val mask = "•".repeat(maskLength.coerceAtMost(8))

        return "$start$mask$end"
    }

    /**
     * Validates an IMEI using Luhn checksum.
     *
     * @param imei The IMEI to validate (15 digits)
     * @return True if valid
     */
    fun isValidImei(imei: String): Boolean {
        if (imei.length != 15) return false
        if (!imei.all { it.isDigit() }) return false

        return calculateLuhnChecksum(imei.dropLast(1)) == imei.last().digitToInt()
    }

    /**
     * Calculates Luhn checksum for IMEI validation.
     */
    private fun calculateLuhnChecksum(digits: String): Int {
        var sum = 0
        for ((index, char) in digits.reversed().withIndex()) {
            var digit = char.digitToInt()
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return (10 - (sum % 10)) % 10
    }

    /**
     * Validates a MAC address format.
     *
     * @param mac The MAC address (XX:XX:XX:XX:XX:XX)
     * @return True if valid format
     */
    fun isValidMac(mac: String): Boolean {
        val macRegex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        return macRegex.matches(mac)
    }

    /**
     * Validates a UUID format.
     *
     * @param uuid The UUID string
     * @return True if valid format
     */
    fun isValidUuid(uuid: String): Boolean {
        val uuidRegex = Regex(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        )
        return uuidRegex.matches(uuid)
    }

    /**
     * Formats a timestamp as a human-readable string.
     *
     * @param epochMillis Timestamp in epoch milliseconds
     * @return Formatted date/time string
     */
    fun formatTimestamp(epochMillis: Long): String {
        val date = java.util.Date(epochMillis)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        return format.format(date)
    }
}
