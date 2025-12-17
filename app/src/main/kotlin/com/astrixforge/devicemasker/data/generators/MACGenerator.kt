package com.astrixforge.devicemasker.data.generators

import kotlin.random.Random

/**
 * MAC Address Generator for network spoofing.
 *
 * MAC Address Structure (48 bits / 6 octets):
 * - First 3 octets: OUI (Organizationally Unique Identifier) - Identifies manufacturer
 * - Last 3 octets: NIC (Network Interface Controller) - Device-specific
 *
 * Important Bits in First Octet:
 * - Bit 0 (LSB): 0 = Unicast, 1 = Multicast
 * - Bit 1: 0 = Globally Unique (OUI enforced), 1 = Locally Administered
 *
 * For spoofing, we typically want:
 * - Unicast addresses (bit 0 = 0)
 * - Locally administered (bit 1 = 1) OR use real OUIs
 */
object MACGenerator {

    /**
     * Real OUI prefixes from major device manufacturers. These are 24-bit identifiers assigned by
     * IEEE.
     */
    private val MANUFACTURER_OUIS =
        mapOf(
            // Samsung
            "samsung" to
                listOf(
                    "00:16:32",
                    "00:21:19",
                    "00:24:54",
                    "00:26:37",
                    "00:E0:91",
                    "3C:5A:B4",
                    "78:AB:BB",
                ),
            // Apple
            "apple" to
                listOf(
                    "00:03:93",
                    "00:0A:27",
                    "00:0D:93",
                    "00:1D:4F",
                    "00:1E:C2",
                    "00:1F:5B",
                    "00:22:41",
                    "00:26:BB",
                ),
            // Google
            "google" to listOf("3C:5A:B4", "F4:F5:D8", "54:60:09", "94:EB:2C"),
            // Xiaomi
            "xiaomi" to
                listOf("04:CF:8C", "0C:1D:AF", "14:F6:5A", "18:59:36", "28:6C:07", "34:CE:00"),
            // Huawei
            "huawei" to
                listOf("00:1E:10", "00:25:68", "00:25:9E", "00:46:4B", "00:66:4B", "04:F9:38"),
            // Intel (common in laptops)
            "intel" to
                listOf("00:1C:C0", "00:1D:E0", "00:1E:67", "00:1F:3B", "00:22:FA", "00:24:D7"),
            // Qualcomm/Broadcom (common in phones)
            "qualcomm" to listOf("00:00:F0", "00:15:FF", "00:18:0A", "00:1A:6B", "00:1C:57"),
            // Realtek (common WiFi chips)
            "realtek" to listOf("00:1E:E3", "00:60:DE", "00:E0:4C", "18:02:AE", "34:29:12"),
            // Generic random
            "generic" to listOf("02:00:00", "06:00:00", "0A:00:00", "0E:00:00"),
        )

    /**
     * Generates a random valid unicast MAC address. Uses locally administered bit for maximum
     * compatibility.
     *
     * @return A MAC address in XX:XX:XX:XX:XX:XX format
     */
    fun generate(): String {
        val octets = ByteArray(6).apply { Random.nextBytes(this) }

        // Ensure unicast (clear bit 0) and locally administered (set bit 1)
        octets[0] = (octets[0].toInt() and 0xFC or 0x02).toByte()

        return octets.joinToString(":") { "%02X".format(it) }
    }

    /**
     * Generates a MAC address with a real manufacturer OUI prefix. This makes the address appear to
     * be from a legitimate device.
     *
     * @param manufacturer The manufacturer name (samsung, apple, google, etc.)
     * @return A MAC address with the specified manufacturer's OUI
     */
    fun generateForManufacturer(manufacturer: String): String {
        val oui =
            MANUFACTURER_OUIS[manufacturer.lowercase()]?.random()
                ?: MANUFACTURER_OUIS["generic"]?.random()
                ?: return generate()

        // Generate the last 3 octets randomly
        val nicBytes = ByteArray(3).apply { Random.nextBytes(this) }

        val nicPart = nicBytes.joinToString(":") { "%02X".format(it) }

        return "$oui:$nicPart"
    }

    /**
     * Generates a locally administered MAC address. Bit 1 of first octet is set, which indicates
     * this is not a globally unique address.
     *
     * @return A locally administered MAC address
     */
    fun generateLocallyAdministered(): String {
        return generate() // Default implementation already does this
    }

    /**
     * Generates a MAC address with a specific OUI prefix.
     *
     * @param oui The OUI prefix in "XX:XX:XX" format
     * @return A MAC address with the specified OUI
     */
    fun generateWithOUI(oui: String): String {
        require(isValidOUI(oui)) { "Invalid OUI format. Expected XX:XX:XX" }

        val nicBytes = ByteArray(3).apply { Random.nextBytes(this) }

        val nicPart = nicBytes.joinToString(":") { "%02X".format(it) }

        return "$oui:$nicPart"
    }

    /**
     * Validates a MAC address format.
     *
     * @param mac The MAC address to validate
     * @return True if the MAC address format is valid
     */
    fun isValid(mac: String): Boolean {
        val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
        return macRegex.matches(mac)
    }

    /**
     * Validates an OUI prefix format.
     *
     * @param oui The OUI to validate
     * @return True if the OUI format is valid
     */
    private fun isValidOUI(oui: String): Boolean {
        val ouiRegex = Regex("^([0-9A-Fa-f]{2}[:-]){2}([0-9A-Fa-f]{2})$")
        return ouiRegex.matches(oui)
    }

    /**
     * Checks if a MAC address is unicast.
     *
     * @param mac The MAC address to check
     * @return True if the address is unicast (bit 0 of first octet is 0)
     */
    fun isUnicast(mac: String): Boolean {
        if (!isValid(mac)) return false
        val firstOctet = mac.split("[:-]".toRegex())[0].toInt(16)
        return (firstOctet and 0x01) == 0
    }

    /**
     * Checks if a MAC address is locally administered.
     *
     * @param mac The MAC address to check
     * @return True if the address is locally administered (bit 1 of first octet is 1)
     */
    fun isLocallyAdministered(mac: String): Boolean {
        if (!isValid(mac)) return false
        val firstOctet = mac.split("[:-]".toRegex())[0].toInt(16)
        return (firstOctet and 0x02) != 0
    }

    /**
     * Normalizes a MAC address to uppercase with colon separators.
     *
     * @param mac The MAC address to normalize
     * @return Normalized MAC address in XX:XX:XX:XX:XX:XX format
     */
    fun normalize(mac: String): String {
        if (!isValid(mac)) return mac
        return mac.uppercase().replace('-', ':')
    }

    /**
     * Generates multiple unique MAC addresses.
     *
     * @param count Number of MAC addresses to generate
     * @return List of unique MAC address strings
     */
    fun generateMultiple(count: Int): List<String> {
        return List(count) { generate() }.distinct()
    }

    /**
     * Generates a WiFi-typical MAC address (common for mobile devices).
     *
     * @return A MAC address typical for WiFi interfaces
     */
    fun generateWiFiMAC(): String {
        val wifiManufacturers =
            listOf("samsung", "apple", "google", "xiaomi", "qualcomm", "realtek")
        return generateForManufacturer(wifiManufacturers.random())
    }

    /**
     * Generates a Bluetooth-typical MAC address. Note: Bluetooth MACs often use similar OUIs but
     * may have different conventions.
     *
     * @return A MAC address typical for Bluetooth interfaces
     */
    fun generateBluetoothMAC(): String {
        // Bluetooth typically uses same OUI as WiFi for integrated chips
        return generateWiFiMAC()
    }
}
