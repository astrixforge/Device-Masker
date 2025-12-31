package com.astrixforge.devicemasker.xposed.utils

import java.util.Random

/**
 * Centralized value generators for spoofing.
 *
 * All generators produce valid, realistic values that pass format validation. Thread-safe with lazy
 * initialization where applicable.
 */
object ValueGenerators {

    private val random = Random()

    // ═══════════════════════════════════════════════════════════
    // DEVICE IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a valid IMEI (15 digits, Luhn-valid). Uses realistic TAC (Type Allocation Code)
     * prefixes.
     */
    fun imei(): String {
        // TAC (Type Allocation Code) + Serial + Luhn checksum
        val tac = listOf("35", "86", "01", "45").random()
        val serial = (1000000..9999999).random().toString()
        val base = tac + serial.padStart(12, '0').take(12)
        return base + calculateLuhn(base)
    }

    /** Generates a valid device serial number (16 hex characters). */
    fun serial(): String {
        val chars = "0123456789ABCDEF"
        return (1..16).map { chars.random() }.joinToString("")
    }

    /** Generates a valid Android ID (16 lowercase hex characters). */
    fun androidId(): String {
        val chars = "0123456789abcdef"
        return (1..16).map { chars.random() }.joinToString("")
    }

    // ═══════════════════════════════════════════════════════════
    // SIM IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** Generates a valid IMSI (15 digits). Uses US MCC/MNC prefix (310260 = T-Mobile). */
    fun imsi(): String {
        return "310260" + (100000000L..999999999L).random().toString()
    }

    /**
     * Generates a valid ICCID/SIM serial number (20 digits). Format: 89 (telecom) + 01 (US) + 16
     * random digits
     */
    fun iccid(): String {
        return "8901" + List(16) { (0..9).random() }.joinToString("")
    }

    // ═══════════════════════════════════════════════════════════
    // NETWORK IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a valid MAC address (XX:XX:XX:XX:XX:XX). Sets unicast bit and local bit for locally
     * administered address.
     */
    fun mac(): String {
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        // Clear multicast bit (bit 0 of first byte), set local bit (bit 1)
        bytes[0] = ((bytes[0].toInt() and 0xFC) or 0x02).toByte()
        return bytes.joinToString(":") { String.format("%02X", it) }
    }

    /** Parses a MAC address string to bytes. Returns zero bytes if parsing fails. */
    fun parseMacToBytes(mac: String): ByteArray {
        return try {
            mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
        } catch (e: Exception) {
            ByteArray(6)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VALIDATION FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /** Validates IMEI format (15 digits, Luhn-valid). */
    fun isValidImei(imei: String): Boolean {
        if (imei.length != 15 || !imei.all { it.isDigit() }) {
            return false
        }
        // Luhn check
        var sum = 0
        for (i in imei.indices) {
            var digit = imei[i].digitToInt()
            if (i % 2 == 1) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return sum % 10 == 0
    }

    /** Validates MAC address format (XX:XX:XX:XX:XX:XX). */
    fun isValidMac(mac: String): Boolean {
        val regex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
        return regex.matches(mac)
    }

    /** Validates Android ID format (16 hex characters). */
    fun isValidAndroidId(id: String): Boolean {
        val regex = Regex("^[0-9a-f]{16}$")
        return regex.matches(id.lowercase())
    }

    /** Validates IMSI format (15 digits). */
    fun isValidImsi(imsi: String): Boolean {
        return imsi.length == 15 && imsi.all { it.isDigit() }
    }

    /** Validates ICCID format (19-20 digits). */
    fun isValidIccid(iccid: String): Boolean {
        return iccid.length in 19..20 && iccid.all { it.isDigit() }
    }

    // ═══════════════════════════════════════════════════════════
    // HELPER FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /** Calculates Luhn checksum digit for a numeric string. */
    private fun calculateLuhn(digits: String): Char {
        var sum = 0
        for ((index, char) in digits.reversed().withIndex()) {
            var digit = char.digitToInt()
            if (index % 2 == 0) {
                digit *= 2
                if (digit > 9) digit -= 9
            }
            sum += digit
        }
        return ((10 - (sum % 10)) % 10).digitToChar()
    }
}
