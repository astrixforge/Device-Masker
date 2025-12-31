package com.astrixforge.devicemasker.ui.screens.groupspoofing.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofType

/**
 * UI Display Categories - Groups spoof types by correlation for better organization.
 *
 * This is different from SpoofCategory because it groups correlated values together:
 * - SIM Card: All SIM_CARD correlation group values (IMSI, ICCID, PHONE, CARRIER) - CORRELATED
 * - Device Hardware: DEVICE_HARDWARE group + DEVICE_PROFILE - CORRELATED
 * - Network: WiFi/Bluetooth (non-correlated network values) - INDEPENDENT
 * - Advertising: All advertising identifiers - INDEPENDENT
 * - Location: GPS + timezone/locale (location correlation group) - CORRELATED
 *
 * Correlated categories: Single switch + single regenerate button (all values sync) Independent
 * categories: Individual switches + regenerate buttons per item
 */
enum class UIDisplayCategory(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val color: Color,
    val types: List<SpoofType>,
    val isCorrelated: Boolean, // If true, values sync together
) {
    SIM_CARD(
        titleRes = R.string.category_sim_card,
        descriptionRes = R.string.category_sim_card_desc,
        icon = Icons.Outlined.SimCard,
        color = Color(0xFF00BCD4), // Cyan
        types =
            listOf(
                SpoofType.PHONE_NUMBER, // Combined with Carrier Name
                SpoofType.CARRIER_NAME, // Shows below Phone Number
                SpoofType.SIM_COUNTRY_ISO, // SIM country code
                SpoofType.NETWORK_COUNTRY_ISO, // Network country code
                SpoofType.IMSI, // Independent
                SpoofType.ICCID, // Independent
                SpoofType.CARRIER_MCC_MNC, // Independent
                SpoofType.SIM_OPERATOR_NAME, // SIM operator name
                SpoofType.NETWORK_OPERATOR, // Network operator (MCC+MNC)
            ),
        isCorrelated = false, // Custom handling: Phone+Carrier combined, others independent
    ),
    DEVICE_HARDWARE(
        titleRes = R.string.category_device_hardware,
        descriptionRes = R.string.category_device_hardware_desc,
        icon = Icons.Outlined.DevicesOther,
        color = Color(0xFF9C27B0), // Purple
        types = listOf(SpoofType.DEVICE_PROFILE, SpoofType.IMEI, SpoofType.SERIAL),
        isCorrelated =
            false, // Custom handling: Device Profile controls all, IMEI/Serial independent
    ),
    NETWORK(
        titleRes = R.string.category_network,
        descriptionRes = R.string.category_network_desc,
        icon = Icons.Outlined.Wifi,
        color = Color(0xFF4CAF50), // Green
        types =
            listOf(
                SpoofType.WIFI_MAC,
                SpoofType.BLUETOOTH_MAC,
                SpoofType.WIFI_SSID,
                SpoofType.WIFI_BSSID,
            ),
        isCorrelated = false, // Each can be regenerated independently
    ),
    ADVERTISING(
        titleRes = R.string.category_advertising,
        descriptionRes = R.string.category_advertising_desc,
        icon = Icons.Outlined.TrackChanges,
        color = Color(0xFFFF9800), // Orange
        types =
            listOf(
                SpoofType.ANDROID_ID,
                SpoofType.GSF_ID,
                SpoofType.ADVERTISING_ID,
                SpoofType.MEDIA_DRM_ID,
            ),
        isCorrelated = false, // Each can be regenerated independently
    ),
    LOCATION(
        titleRes = R.string.category_location,
        descriptionRes = R.string.category_location_desc,
        icon = Icons.Outlined.LocationOn,
        color = Color(0xFFE91E63), // Pink
        types =
            listOf(
                // Order: Timezone first (controls locale), then Lat/Long (independent GPS)
                SpoofType.TIMEZONE,
                SpoofType.LOCALE,
                SpoofType.LOCATION_LATITUDE,
                SpoofType.LOCATION_LONGITUDE,
            ),
        isCorrelated = false, // Custom handling: Timezone+Locale sync, Lat/Long independent
    ),
}
