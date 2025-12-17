package com.astrixforge.devicemasker.data.models

/**
 * Enumeration of all spoofable device identifiers.
 *
 * Each type represents a specific identifier that can be intercepted and replaced with a spoofed
 * value.
 */
enum class SpoofType(
    val displayName: String,
    val category: SpoofCategory,
    val description: String,
) {
    // ═══════════════════════════════════════════════════════════
    // DEVICE IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /**
     * International Mobile Equipment Identity - Primary device identifier. 15 digits with Luhn
     * checksum validation.
     */
    IMEI(
        displayName = "IMEI",
        category = SpoofCategory.DEVICE,
        description = "International Mobile Equipment Identity",
    ),

    /** Mobile Equipment Identifier - CDMA device identifier. 14 digits, similar to IMEI. */
    MEID(
        displayName = "MEID",
        category = SpoofCategory.DEVICE,
        description = "Mobile Equipment Identifier (CDMA)",
    ),

    /**
     * International Mobile Subscriber Identity - SIM card identifier. 15 digits identifying the
     * subscriber.
     */
    IMSI(
        displayName = "IMSI",
        category = SpoofCategory.DEVICE,
        description = "International Mobile Subscriber Identity",
    ),

    /** Device serial number. Alphanumeric string, format varies by manufacturer. */
    SERIAL(
        displayName = "Serial Number",
        category = SpoofCategory.DEVICE,
        description = "Device Serial Number",
    ),

    /**
     * Integrated Circuit Card Identifier - SIM card serial. 19-20 digits identifying the SIM card.
     */
    ICCID(
        displayName = "ICCID",
        category = SpoofCategory.DEVICE,
        description = "SIM Card Identifier",
    ),

    /** Phone number associated with the SIM. */
    PHONE_NUMBER(
        displayName = "Phone Number",
        category = SpoofCategory.DEVICE,
        description = "Device Phone Number",
    ),

    // ═══════════════════════════════════════════════════════════
    // NETWORK IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** WiFi MAC address. 48-bit address in XX:XX:XX:XX:XX:XX format. */
    WIFI_MAC(
        displayName = "WiFi MAC",
        category = SpoofCategory.NETWORK,
        description = "WiFi MAC Address",
    ),

    /** Bluetooth MAC address. 48-bit address in XX:XX:XX:XX:XX:XX format. */
    BLUETOOTH_MAC(
        displayName = "Bluetooth MAC",
        category = SpoofCategory.NETWORK,
        description = "Bluetooth MAC Address",
    ),

    /** Connected WiFi network name. */
    WIFI_SSID(
        displayName = "WiFi SSID",
        category = SpoofCategory.NETWORK,
        description = "WiFi Network Name",
    ),

    /** Connected WiFi access point MAC. */
    WIFI_BSSID(
        displayName = "WiFi BSSID",
        category = SpoofCategory.NETWORK,
        description = "WiFi Access Point MAC",
    ),

    /** Carrier/operator name. */
    CARRIER_NAME(
        displayName = "Carrier Name",
        category = SpoofCategory.NETWORK,
        description = "Network Operator Name",
    ),

    /** Mobile Country Code + Mobile Network Code. */
    CARRIER_MCC_MNC(
        displayName = "MCC/MNC",
        category = SpoofCategory.NETWORK,
        description = "Mobile Country/Network Code",
    ),

    // ═══════════════════════════════════════════════════════════
    // ADVERTISING & TRACKING IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** Settings.Secure.ANDROID_ID - Per-app identifier. 16 hex characters. */
    ANDROID_ID(
        displayName = "Android ID",
        category = SpoofCategory.ADVERTISING,
        description = "Settings.Secure.ANDROID_ID",
    ),

    /** Google Services Framework ID. 16 hex characters, used by Google Play. */
    GSF_ID(
        displayName = "GSF ID",
        category = SpoofCategory.ADVERTISING,
        description = "Google Services Framework ID",
    ),

    /** Google Advertising ID. UUID format, used for ad tracking. */
    ADVERTISING_ID(
        displayName = "Advertising ID",
        category = SpoofCategory.ADVERTISING,
        description = "Google Advertising ID (GAID)",
    ),

    /** Media DRM Widevine device ID. 64 hex characters. */
    MEDIA_DRM_ID(
        displayName = "Media DRM ID",
        category = SpoofCategory.ADVERTISING,
        description = "Widevine Device ID",
    ),

    // ═══════════════════════════════════════════════════════════
    // SYSTEM PROPERTIES
    // ═══════════════════════════════════════════════════════════

    /** Build.FINGERPRINT - Device/build identification. */
    BUILD_FINGERPRINT(
        displayName = "Fingerprint",
        category = SpoofCategory.SYSTEM,
        description = "Build Fingerprint",
    ),

    /** Build.MODEL - Device model name. */
    BUILD_MODEL(
        displayName = "Model",
        category = SpoofCategory.SYSTEM,
        description = "Device Model",
    ),

    /** Build.MANUFACTURER - Device manufacturer. */
    BUILD_MANUFACTURER(
        displayName = "Manufacturer",
        category = SpoofCategory.SYSTEM,
        description = "Device Manufacturer",
    ),

    /** Build.BRAND - Device brand. */
    BUILD_BRAND(
        displayName = "Brand",
        category = SpoofCategory.SYSTEM,
        description = "Device Brand",
    ),

    /** Build.DEVICE - Device code name. */
    BUILD_DEVICE(
        displayName = "Device",
        category = SpoofCategory.SYSTEM,
        description = "Device Code Name",
    ),

    /** Build.PRODUCT - Product code name. */
    BUILD_PRODUCT(
        displayName = "Product",
        category = SpoofCategory.SYSTEM,
        description = "Product Code Name",
    ),

    /** Build.BOARD - Board/hardware name. */
    BUILD_BOARD(
        displayName = "Board",
        category = SpoofCategory.SYSTEM,
        description = "Hardware Board",
    ),

    // ═══════════════════════════════════════════════════════════
    // LOCATION IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** GPS latitude coordinate. */
    LOCATION_LATITUDE(
        displayName = "Latitude",
        category = SpoofCategory.LOCATION,
        description = "GPS Latitude",
    ),

    /** GPS longitude coordinate. */
    LOCATION_LONGITUDE(
        displayName = "Longitude",
        category = SpoofCategory.LOCATION,
        description = "GPS Longitude",
    ),

    /** Device timezone. */
    TIMEZONE(
        displayName = "Timezone",
        category = SpoofCategory.LOCATION,
        description = "Device Timezone",
    ),

    /** Device locale/language setting. */
    LOCALE(
        displayName = "Locale",
        category = SpoofCategory.LOCATION,
        description = "Device Locale",
    );

    companion object {
        /** Returns all spoof types for a given category. */
        fun byCategory(category: SpoofCategory): List<SpoofType> {
            return entries.filter { it.category == category }
        }

        /** Returns all categories. */
        fun allCategories(): List<SpoofCategory> {
            return SpoofCategory.entries
        }
    }
}

/** Categories for grouping spoof types in the UI. */
enum class SpoofCategory(val displayName: String, val iconName: String) {
    DEVICE("Device", "smartphone"),
    NETWORK("Network", "wifi"),
    ADVERTISING("Advertising", "ads_click"),
    SYSTEM("System", "settings"),
    LOCATION("Location", "location_on"),
}
