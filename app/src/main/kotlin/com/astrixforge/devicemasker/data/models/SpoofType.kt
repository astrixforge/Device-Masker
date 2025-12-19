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
    ),

    /** Mobile Equipment Identifier - CDMA device identifier. 14 digits, similar to IMEI. */
    MEID(
        displayName = "MEID",
        category = SpoofCategory.DEVICE,
    ),

    /**
     * International Mobile Subscriber Identity - SIM card identifier. 15 digits identifying the
     * subscriber.
     */
    IMSI(
        displayName = "IMSI",
        category = SpoofCategory.DEVICE,
    ),

    /** Device serial number. Alphanumeric string, format varies by manufacturer. */
    SERIAL(
        displayName = "Serial Number",
        category = SpoofCategory.DEVICE,
    ),

    /**
     * Integrated Circuit Card Identifier - SIM card serial. 19-20 digits identifying the SIM card.
     */
    ICCID(
        displayName = "ICCID",
        category = SpoofCategory.DEVICE,
    ),

    /** Phone number associated with the SIM. */
    PHONE_NUMBER(
        displayName = "Phone Number",
        category = SpoofCategory.DEVICE,
    ),

    // ═══════════════════════════════════════════════════════════
    // NETWORK IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** WiFi MAC address. 48-bit address in XX:XX:XX:XX:XX:XX format. */
    WIFI_MAC(
        displayName = "WiFi MAC",
        category = SpoofCategory.NETWORK,
    ),

    /** Bluetooth MAC address. 48-bit address in XX:XX:XX:XX:XX:XX format. */
    BLUETOOTH_MAC(
        displayName = "Bluetooth MAC",
        category = SpoofCategory.NETWORK,
    ),

    /** Connected WiFi network name. */
    WIFI_SSID(
        displayName = "WiFi SSID",
        category = SpoofCategory.NETWORK,
    ),

    /** Connected WiFi access point MAC. */
    WIFI_BSSID(
        displayName = "WiFi BSSID",
        category = SpoofCategory.NETWORK,
    ),

    /** Carrier/operator name. */
    CARRIER_NAME(
        displayName = "Carrier Name",
        category = SpoofCategory.NETWORK,
    ),

    /** Mobile Country Code + Mobile Network Code. */
    CARRIER_MCC_MNC(
        displayName = "MCC/MNC",
        category = SpoofCategory.NETWORK,
    ),

    // ═══════════════════════════════════════════════════════════
    // ADVERTISING & TRACKING IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** Settings.Secure.ANDROID_ID - Per-app identifier. 16 hex characters. */
    ANDROID_ID(
        displayName = "Android ID",
        category = SpoofCategory.ADVERTISING,
    ),

    /** Google Services Framework ID. 16 hex characters, used by Google Play. */
    GSF_ID(
        displayName = "GSF ID",
        category = SpoofCategory.ADVERTISING,
    ),

    /** Google Advertising ID. UUID format, used for ad tracking. */
    ADVERTISING_ID(
        displayName = "Advertising ID",
        category = SpoofCategory.ADVERTISING,
    ),

    /** Media DRM Widevine device ID. 64 hex characters. */
    MEDIA_DRM_ID(
        displayName = "Media DRM ID",
        category = SpoofCategory.ADVERTISING,
    ),

    // ═══════════════════════════════════════════════════════════
    // SYSTEM PROPERTIES
    // ═══════════════════════════════════════════════════════════

    /** Build.FINGERPRINT - Device/build identification. */
    BUILD_FINGERPRINT(
        displayName = "Fingerprint",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.MODEL - Device model name. */
    BUILD_MODEL(
        displayName = "Model",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.MANUFACTURER - Device manufacturer. */
    BUILD_MANUFACTURER(
        displayName = "Manufacturer",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.BRAND - Device brand. */
    BUILD_BRAND(
        displayName = "Brand",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.DEVICE - Device code name. */
    BUILD_DEVICE(
        displayName = "Device",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.PRODUCT - Product code name. */
    BUILD_PRODUCT(
        displayName = "Product",
        category = SpoofCategory.SYSTEM,
    ),

    /** Build.BOARD - Board/hardware name. */
    BUILD_BOARD(
        displayName = "Board",
        category = SpoofCategory.SYSTEM,
    ),

    // ═══════════════════════════════════════════════════════════
    // LOCATION IDENTIFIERS
    // ═══════════════════════════════════════════════════════════

    /** GPS latitude coordinate. */
    LOCATION_LATITUDE(
        displayName = "Latitude",
        category = SpoofCategory.LOCATION,
    ),

    /** GPS longitude coordinate. */
    LOCATION_LONGITUDE(
        displayName = "Longitude",
        category = SpoofCategory.LOCATION,
    ),

    /** Device timezone. */
    TIMEZONE(
        displayName = "Timezone",
        category = SpoofCategory.LOCATION,
    ),

    /** Device locale/language setting. */
    LOCALE(
        displayName = "Locale",
        category = SpoofCategory.LOCATION,
    );

    companion object {
        /** Returns all spoof types for a given category. */
        fun byCategory(category: SpoofCategory): List<SpoofType> {
            return entries.filter { it.category == category }
        }
    }
}

/** Categories for grouping spoof types in the UI. */
enum class SpoofCategory(val displayName: String) {
    DEVICE("Device"),
    NETWORK("Network"),
    ADVERTISING("Advertising"),
    SYSTEM("System"),
    LOCATION("Location"),
}
