package com.astrixforge.devicemasker.common

import kotlinx.serialization.Serializable

/**
 * Predefined device profile preset containing all Build.* values.
 * 
 * All values in a preset are consistent and should be applied together
 * to create a realistic device fingerprint.
 *
 * @property id Unique identifier for this preset
 * @property name Human-readable name (e.g., "Google Pixel 8 Pro")
 * @property brand Build.BRAND - Device brand (e.g., "google")
 * @property manufacturer Build.MANUFACTURER - Device manufacturer (e.g., "Google")
 * @property model Build.MODEL - Device model (e.g., "Pixel 8 Pro")
 * @property device Build.DEVICE - Device codename (e.g., "husky")
 * @property product Build.PRODUCT - Product codename (e.g., "husky")
 * @property board Build.BOARD - Board name (e.g., "husky")
 * @property fingerprint Build.FINGERPRINT - Full fingerprint string
 * @property securityPatch Security patch date (e.g., "2024-12-05")
 */
@Serializable
data class DeviceProfilePreset(
    val id: String,
    val name: String,
    val brand: String,
    val manufacturer: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val fingerprint: String,
    val securityPatch: String = "",
) {
    companion object {
        /**
         * List of predefined device profiles.
         * These are based on real device fingerprints from stock ROMs.
         */
        val PRESETS = listOf(
            // Google Pixel 8 Pro (December 2024)
            DeviceProfilePreset(
                id = "pixel_8_pro",
                name = "Google Pixel 8 Pro",
                brand = "google",
                manufacturer = "Google",
                model = "Pixel 8 Pro",
                device = "husky",
                product = "husky",
                board = "husky",
                fingerprint = "google/husky/husky:14/AD1A.240530.047/11777660:user/release-keys",
                securityPatch = "2024-12-05",
            ),
            
            // Google Pixel 7
            DeviceProfilePreset(
                id = "pixel_7",
                name = "Google Pixel 7",
                brand = "google",
                manufacturer = "Google",
                model = "Pixel 7",
                device = "panther",
                product = "panther",
                board = "panther",
                fingerprint = "google/panther/panther:14/AP2A.240805.005/12025142:user/release-keys",
                securityPatch = "2024-08-05",
            ),
            
            // Samsung Galaxy S24 Ultra
            DeviceProfilePreset(
                id = "samsung_s24_ultra",
                name = "Samsung Galaxy S24 Ultra",
                brand = "samsung",
                manufacturer = "samsung",
                model = "SM-S928B",
                device = "dm3q",
                product = "dm3qxxx",
                board = "pineapple",
                fingerprint = "samsung/dm3qxxx/dm3q:14/UP1A.231005.007/S928BXXS2AXL5:user/release-keys",
                securityPatch = "2024-12-01",
            ),
            
            // Samsung Galaxy S23
            DeviceProfilePreset(
                id = "samsung_s23",
                name = "Samsung Galaxy S23",
                brand = "samsung",
                manufacturer = "samsung",
                model = "SM-S911B",
                device = "dm1q",
                product = "dm1qxxx",
                board = "kalama",
                fingerprint = "samsung/dm1qxxx/dm1q:14/UP1A.231005.007/S911BXXS6CXJ1:user/release-keys",
                securityPatch = "2024-10-01",
            ),
            
            // OnePlus 12
            DeviceProfilePreset(
                id = "oneplus_12",
                name = "OnePlus 12",
                brand = "OnePlus",
                manufacturer = "OnePlus",
                model = "CPH2581",
                device = "CPH2581",
                product = "OP5D13L1",
                board = "pineapple",
                fingerprint = "OnePlus/CPH2581/OP5D13L1:14/UP1A.231005.007/T.R4T3.15eeb8c-1:user/release-keys",
                securityPatch = "2024-10-05",
            ),
            
            // OnePlus 11
            DeviceProfilePreset(
                id = "oneplus_11",
                name = "OnePlus 11",
                brand = "OnePlus",
                manufacturer = "OnePlus",
                model = "PHB110",
                device = "OP5958L1",
                product = "OP5958L1",
                board = "kalama",
                fingerprint = "OnePlus/PHB110/OP5958L1:14/RKQ1.211119.001/T.R9T1.16fdf3b-1e9:user/release-keys",
                securityPatch = "2024-09-05",
            ),
            
            // Xiaomi 14 Pro
            DeviceProfilePreset(
                id = "xiaomi_14_pro",
                name = "Xiaomi 14 Pro",
                brand = "Xiaomi",
                manufacturer = "Xiaomi",
                model = "23116PN5BC",
                device = "shennong",
                product = "shennong",
                board = "pineapple",
                fingerprint = "Xiaomi/shennong/shennong:14/UKQ1.231003.002/V816.0.25.0.UNCCNXM:user/release-keys",
                securityPatch = "2024-11-01",
            ),
            
            // POCO F6 Pro
            DeviceProfilePreset(
                id = "poco_f6_pro",
                name = "POCO F6 Pro",
                brand = "POCO",
                manufacturer = "Xiaomi",
                model = "23113RKC6G",
                device = "vermeer",
                product = "vermeer_global",
                board = "kalama",
                fingerprint = "POCO/vermeer_global/vermeer:14/UKQ1.231003.002/V816.0.7.0.UNKGIXM:user/release-keys",
                securityPatch = "2024-10-01",
            ),
            
            // Sony Xperia 1 VI
            DeviceProfilePreset(
                id = "sony_xperia_1_vi",
                name = "Sony Xperia 1 VI",
                brand = "Sony",
                manufacturer = "Sony",
                model = "XQ-EC54",
                device = "pdx245",
                product = "XQ-EC54",
                board = "pineapple",
                fingerprint = "Sony/XQ-EC54/pdx245:14/65.2.A.0.328/065002A000328:user/release-keys",
                securityPatch = "2024-11-01",
            ),
            
            // Nothing Phone (2)
            DeviceProfilePreset(
                id = "nothing_phone_2",
                name = "Nothing Phone (2)",
                brand = "Nothing",
                manufacturer = "Nothing",
                model = "A065",
                device = "Pong",
                product = "Pong",
                board = "Pong",
                fingerprint = "Nothing/Pong/Pong:14/AP31.240617.009/2409251803:user/release-keys",
                securityPatch = "2024-09-05",
            ),
        )
        
        /**
         * Finds a preset by its ID.
         */
        fun findById(id: String): DeviceProfilePreset? {
            return PRESETS.find { it.id == id }
        }
        
        /**
         * Returns presets grouped by manufacturer.
         */
        fun groupedByManufacturer(): Map<String, List<DeviceProfilePreset>> {
            return PRESETS.groupBy { it.manufacturer }
        }
    }
}
