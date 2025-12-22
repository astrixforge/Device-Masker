package com.astrixforge.devicemasker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.astrixforge.devicemasker.common.CorrelationGroup
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.DeviceHardwareGenerator
import com.astrixforge.devicemasker.common.generators.FingerprintGenerator
import com.astrixforge.devicemasker.common.generators.ICCIDGenerator
import com.astrixforge.devicemasker.common.generators.IMEIGenerator
import com.astrixforge.devicemasker.common.generators.IMSIGenerator
import com.astrixforge.devicemasker.common.generators.MACGenerator
import com.astrixforge.devicemasker.common.generators.PhoneNumberGenerator
import com.astrixforge.devicemasker.common.generators.SerialGenerator
import com.astrixforge.devicemasker.common.generators.SIMGenerator
import com.astrixforge.devicemasker.common.generators.UUIDGenerator
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.DeviceHardwareConfig
import com.astrixforge.devicemasker.common.models.LocationConfig
import com.astrixforge.devicemasker.common.models.SIMConfig
import com.astrixforge.devicemasker.service.ConfigManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Main repository combining all spoof-related data operations.
 *
 * Multi-Module Architecture: This repository now wraps ConfigManager and provides
 * the same API to the UI while using JsonConfig as the backing store.
 *
 * @param context Application context (for legacy compatibility)
 */
class SpoofRepository(private val context: Context) {

    /** Repository for installed apps access. */
    val appScopeRepository: AppScopeRepository = AppScopeRepository(context)

    // ═══════════════════════════════════════════════════════════
    // CORRELATION CACHING
    // ═══════════════════════════════════════════════════════════

    /**
     * Cached groups for correlated value generation.
     * 
     * These ensure that values in the same correlation group always use
     * the same underlying group, preventing detection from mismatches.
     */
    private var cachedSIMConfig: SIMConfig? = null
    private var cachedLocationConfig: LocationConfig? = null
    private var cachedDeviceHardwareConfig: DeviceHardwareConfig? = null

    // ═══════════════════════════════════════════════════════════
    // UI STATE FLOWS
    // ═══════════════════════════════════════════════════════════

    /** Flow of module enabled state. */
    val moduleEnabled: Flow<Boolean> = ConfigManager.config.map { it.isModuleEnabled }

    /** Flow of all groups. */
    val groups: Flow<List<SpoofGroup>> = ConfigManager.config.map { it.getAllGroups() }

    /** Flow of the active group (default group). */
    val activeGroup: Flow<SpoofGroup?> = ConfigManager.config.map { it.getDefaultGroup() }

    /** Flow of enabled app count. */
    val enabledAppCount: Flow<Int> =
        groups.map { groupList ->
            groupList.filter { it.isEnabled }.flatMap { it.assignedApps }.distinct().size
        }

    /** Combined UI state flow for dashboard. */
    data class DashboardState(
        val isModuleEnabled: Boolean,
        val activeGroup: SpoofGroup?,
        val enabledAppCount: Int,
        val groupCount: Int,
    )

    val dashboardState: Flow<DashboardState> =
        combine(moduleEnabled, activeGroup, enabledAppCount, groups) { enabled,
                                                                           group,
                                                                           appCount,
                                                                           groupList ->
            DashboardState(
                isModuleEnabled = enabled,
                activeGroup = group,
                enabledAppCount = appCount,
                groupCount = groupList.size,
            )
        }

    // ═══════════════════════════════════════════════════════════
    // MODULE SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Enables or disables the module globally. */
    suspend fun setModuleEnabled(enabled: Boolean) {
        ConfigManager.setModuleEnabled(enabled)
    }

    /** Sets the active group by ID (makes it default). */
    suspend fun setActiveGroup(groupId: String) {
        val group = ConfigManager.getGroup(groupId) ?: return
        // Set this group as default
        val updatedGroup = group.copy(isDefault = true)
        ConfigManager.updateGroup(updatedGroup)
        
        // Unset other groups as default
        ConfigManager.getAllGroups().forEach { other ->
            if (other.id != groupId && other.isDefault) {
                ConfigManager.updateGroup(other.copy(isDefault = false))
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VALUE GENERATION (WITH CORRELATION)
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a new random value for a spoof type.
     * 
     * Values in the same correlation group will use the same underlying
     * profile to ensure consistency (e.g., IMSI and ICCID both use same carrier).
     */
    fun generateValue(type: SpoofType): String {
        return when (type.correlationGroup) {
            CorrelationGroup.SIM_CARD -> generateSIMValue(type)
            CorrelationGroup.LOCATION -> generateLocationValue(type)
            CorrelationGroup.DEVICE_HARDWARE -> generateDeviceHardwareValue(type)
            CorrelationGroup.NONE -> generateIndependentValue(type)
        }
    }

    /**
     * Generates correlated SIM card values.
     * All SIM values use the same carrier profile for consistency.
     */
    private fun generateSIMValue(type: SpoofType): String {
        // Generate SIM config if not cached
        if (cachedSIMConfig == null) {
            cachedSIMConfig = SIMGenerator.generate()
        }

        return when (type) {
            SpoofType.IMSI -> cachedSIMConfig!!.imsi
            SpoofType.ICCID -> cachedSIMConfig!!.iccid
            SpoofType.CARRIER_NAME -> cachedSIMConfig!!.carrierName
            SpoofType.CARRIER_MCC_MNC -> cachedSIMConfig!!.mccMnc
            SpoofType.PHONE_NUMBER -> cachedSIMConfig!!.phoneNumber
            // NEW: Additional SIM values for comprehensive spoofing
            SpoofType.SIM_COUNTRY_ISO -> cachedSIMConfig!!.simCountryIso
            SpoofType.NETWORK_COUNTRY_ISO -> cachedSIMConfig!!.networkCountryIso
            SpoofType.SIM_OPERATOR_NAME -> cachedSIMConfig!!.simOperatorName
            SpoofType.NETWORK_OPERATOR -> cachedSIMConfig!!.networkOperator
            else -> throw IllegalArgumentException("Not a SIM value: $type")
        }
    }

    /**
     * Regenerates ONLY a specific SIM value while keeping the same carrier.
     * 
     * This is used when the user wants to regenerate just the phone number, IMSI, or ICCID
     * without changing the carrier. This prevents the bug where regenerating phone number
     * would show wrong country code.
     * 
     * @param type The specific SIM type to regenerate
     * @return The new value, using the SAME carrier as currently cached
     */
    fun regenerateSIMValueOnly(type: SpoofType): String {
        // Get the current carrier from cache, or generate new config if no cache
        val currentCarrier = cachedSIMConfig?.carrier ?: Carrier.random()
        
        // Generate ONLY the specific value using the SAME carrier
        return when (type) {
            SpoofType.PHONE_NUMBER -> PhoneNumberGenerator.generate(currentCarrier)
            SpoofType.IMSI -> IMSIGenerator.generate(currentCarrier)
            SpoofType.ICCID -> ICCIDGenerator.generate(currentCarrier)
            // For carrier-derived values, always use the cached profile
            SpoofType.CARRIER_NAME -> currentCarrier.name
            SpoofType.CARRIER_MCC_MNC -> currentCarrier.mccMnc
            SpoofType.SIM_COUNTRY_ISO -> currentCarrier.countryIsoLower
            SpoofType.NETWORK_COUNTRY_ISO -> currentCarrier.countryIsoLower
            SpoofType.SIM_OPERATOR_NAME -> currentCarrier.name
            SpoofType.NETWORK_OPERATOR -> currentCarrier.mccMnc
            else -> throw IllegalArgumentException("Not a SIM value: $type")
        }
    }


    /**
     * Generates correlated location values.
     * Timezone and locale are from the same country.
     */
    private fun generateLocationValue(type: SpoofType): String {
        // Generate location config if not cached
        if (cachedLocationConfig == null) {
            cachedLocationConfig = LocationConfig.generate()
        }

        return when (type) {
            SpoofType.TIMEZONE -> cachedLocationConfig!!.timezone
            SpoofType.LOCALE -> cachedLocationConfig!!.locale
            else -> throw IllegalArgumentException("Not a location value: $type")
        }
    }

    /**
     * Generates correlated device hardware values.
     * IMEI, Serial, WiFi MAC match the device profile.
     * 
     * Note: MEID removed - CDMA deprecated since 2022.
     */
    private fun generateDeviceHardwareValue(type: SpoofType): String {
        // Generate device hardware config if not cached
        if (cachedDeviceHardwareConfig == null) {
            cachedDeviceHardwareConfig = DeviceHardwareGenerator.generate()
        }

        return when (type) {
            SpoofType.IMEI -> cachedDeviceHardwareConfig!!.imei
            SpoofType.SERIAL -> cachedDeviceHardwareConfig!!.serial
            SpoofType.WIFI_MAC -> cachedDeviceHardwareConfig!!.wifiMAC
            else -> throw IllegalArgumentException("Not a device hardware value: $type")
        }
    }

    /**
     * Generates independent values (no correlation needed).
     */
    private fun generateIndependentValue(type: SpoofType): String {
        return when (type) {
            // Network (independent)
            SpoofType.BLUETOOTH_MAC -> MACGenerator.generateBluetoothMAC()
            SpoofType.WIFI_SSID -> generateRealisticSSID()
            SpoofType.WIFI_BSSID -> MACGenerator.generate()

            // Advertising (all independent)
            SpoofType.ANDROID_ID -> UUIDGenerator.generateAndroidId()
            SpoofType.GSF_ID -> UUIDGenerator.generateGSFId()
            SpoofType.ADVERTISING_ID -> UUIDGenerator.generateAdvertisingId()
            SpoofType.MEDIA_DRM_ID -> UUIDGenerator.generateMediaDrmId()

            // System
            SpoofType.DEVICE_PROFILE ->
                com.astrixforge.devicemasker.common.DeviceProfilePreset.PRESETS.random().id

            // Location (independent coordinates)
            SpoofType.LOCATION_LATITUDE -> 
                String.format(java.util.Locale.US, "%.6f", (-90.0..90.0).random())
            SpoofType.LOCATION_LONGITUDE -> 
                String.format(java.util.Locale.US, "%.6f", (-180.0..180.0).random())

            else -> throw IllegalArgumentException("Unknown independent type: $type")
        }
    }

    /**
     * Clears cached correlation profiles.
     * Call this to force generation of new correlated values.
     */
    fun resetCorrelations() {
        cachedSIMConfig = null
        cachedLocationConfig = null
        cachedDeviceHardwareConfig = null
    }
    
    /**
     * Resets a specific correlation group's cache.
     * Call this before regenerating correlated values to get fresh values.
     */
    fun resetCorrelationGroup(group: CorrelationGroup) {
        when (group) {
            CorrelationGroup.SIM_CARD -> cachedSIMConfig = null
            CorrelationGroup.LOCATION -> cachedLocationConfig = null
            CorrelationGroup.DEVICE_HARDWARE -> cachedDeviceHardwareConfig = null
            CorrelationGroup.NONE -> { /* No cache for independent values */ }
        }
    }
    
    /**
     * Regenerates both timezone and locale values together.
     * 
     * This ensures both come from the SAME location config (same country),
     * avoiding mismatches like "Asia/Kolkata" with "en_US".
     * 
     * @param groupId Group to update
     */
    suspend fun regenerateLocationValues(groupId: String) {
        val group = ConfigManager.getGroup(groupId) ?: return

        // Reset cache to get fresh location config
        cachedLocationConfig = null

        // Generate new location config
        val locationConfig = LocationConfig.generate()
        cachedLocationConfig = locationConfig

        // Update both values from the SAME config
        var updatedGroup = group.withValue(SpoofType.TIMEZONE, locationConfig.timezone)
        updatedGroup = updatedGroup.withValue(SpoofType.LOCALE, locationConfig.locale)

        ConfigManager.updateGroup(updatedGroup)
    }
    /**
     * Updates a group with SIM values from a specific carrier.
     * 
     * This generates all SIM-related values (IMSI, ICCID, Phone, etc.) 
     * from the selected carrier and updates the group.
     * 
     * ALSO syncs Location values (timezone/locale) to match carrier's country.
     * This prevents detection from SIM/Location country mismatches.
     * 
     * @param groupId Group to update
     * @param carrier The carrier to use for generation
     */
    suspend fun updateGroupWithCarrier(groupId: String, carrier: com.astrixforge.devicemasker.common.models.Carrier) {
        val group = ConfigManager.getGroup(groupId) ?: return

        // Generate SIM config from specific carrier
        val simConfig = SIMGenerator.generate(carrier)
        cachedSIMConfig = simConfig

        // NEW: Also generate location matching carrier's country
        // This ensures timezone/locale/GPS match the SIM country
        val locationConfig = LocationConfig.generateForCarrier(carrier)
        cachedLocationConfig = locationConfig

        // Update all SIM-related values in the group
        var updatedGroup = group.copy(selectedCarrierMccMnc = carrier.mccMnc)
        updatedGroup = updatedGroup.withValue(SpoofType.IMSI, simConfig.imsi)
        updatedGroup = updatedGroup.withValue(SpoofType.ICCID, simConfig.iccid)
        updatedGroup = updatedGroup.withValue(SpoofType.PHONE_NUMBER, simConfig.phoneNumber)
        updatedGroup = updatedGroup.withValue(SpoofType.CARRIER_NAME, simConfig.carrierName)
        updatedGroup = updatedGroup.withValue(SpoofType.CARRIER_MCC_MNC, simConfig.mccMnc)
        updatedGroup = updatedGroup.withValue(SpoofType.SIM_COUNTRY_ISO, simConfig.simCountryIso)
        updatedGroup = updatedGroup.withValue(SpoofType.NETWORK_COUNTRY_ISO, simConfig.networkCountryIso)
        updatedGroup = updatedGroup.withValue(SpoofType.SIM_OPERATOR_NAME, simConfig.simOperatorName)
        updatedGroup = updatedGroup.withValue(SpoofType.NETWORK_OPERATOR, simConfig.networkOperator)

        // Sync Location to carrier country (prevents detection)
        updatedGroup = updatedGroup.withValue(SpoofType.TIMEZONE, locationConfig.timezone)
        updatedGroup = updatedGroup.withValue(SpoofType.LOCALE, locationConfig.locale)

        // NEW: Sync GPS coordinates to carrier country
        updatedGroup = updatedGroup.withValue(
            SpoofType.LOCATION_LATITUDE,
            String.format(java.util.Locale.US, "%.6f", locationConfig.latitude)
        )
        updatedGroup = updatedGroup.withValue(
            SpoofType.LOCATION_LONGITUDE,
            String.format(java.util.Locale.US, "%.6f", locationConfig.longitude)
        )

        ConfigManager.updateGroup(updatedGroup)
    }
    
    /**
     * Updates a group with hardware values matching the device profile.
     * 
     * When user selects a Device Profile (e.g., "Pixel 8 Pro"), this ensures:
     * - IMEI uses appropriate TAC prefix
     * - Serial matches manufacturer pattern (e.g., FA6AB for Google)
     * - WiFi MAC may use manufacturer OUI
     * 
     * @param groupId Group to update
     * @param presetId The device preset ID to use
     */
    suspend fun updateGroupWithDeviceProfile(groupId: String, presetId: String) {
        val group = ConfigManager.getGroup(groupId) ?: return
        val preset = com.astrixforge.devicemasker.common.DeviceProfilePreset.findById(presetId) ?: return

        // Generate hardware matching the device profile
        val hardwareConfig = DeviceHardwareGenerator.generate(preset)
        cachedDeviceHardwareConfig = hardwareConfig

        var updatedGroup = group.withValue(SpoofType.DEVICE_PROFILE, presetId)
        updatedGroup = updatedGroup.withValue(SpoofType.IMEI, hardwareConfig.imei)
        updatedGroup = updatedGroup.withValue(SpoofType.SERIAL, hardwareConfig.serial)
        updatedGroup = updatedGroup.withValue(SpoofType.WIFI_MAC, hardwareConfig.wifiMAC)

        ConfigManager.updateGroup(updatedGroup)
    }
    
    /**
     * Generates realistic WiFi SSID names.
     * Uses common router brands and patterns.
     */
    private fun generateRealisticSSID(): String {
        val patterns = listOf(
            // US ISP Routers
            { "\"NETGEAR-${randomHex(4)}\"" },
            { "\"NETGEAR-5G-${randomHex(2)}\"" },
            { "\"xfinitywifi\"" },
            { "\"XFINITY\"" },
            { "\"ATT${randomHex(6)}\"" },
            { "\"ATTWiFi-${randomHex(4)}\"" },
            // Common router brands
            { "\"TP-LINK_${randomHex(4)}\"" },
            { "\"TP-Link_${randomHex(4)}_5G\"" },
            { "\"ASUS_${randomHex(4)}\"" },
            { "\"ASUS_RT-${randomHex(4)}\"" },
            { "\"Linksys${randomDigits(5)}\"" },
            { "\"dlink-${randomHex(4)}\"" },
            { "\"ORBI${randomDigits(2)}\"" },
            { "\"eero-${randomHex(4)}\"" },
            { "\"GoogleWifi-${randomHex(4)}\"" },
            { "\"ARRIS-${randomHex(4)}\"" },
            // Generic home networks
            { "\"Home_WiFi\"" },
            { "\"MyNetwork-5G\"" },
            { "\"WiFi-${randomDigits(4)}\"" },
            { "\"Guest_Network\"" },
            { "\"${randomFamilyName()}_WiFi\"" },
            { "\"${randomFamilyName()}_5G\"" },
        )
        return patterns.random()()
    }
    
    private fun randomHex(length: Int) = buildString {
        repeat(length) { append("0123456789ABCDEF".random()) }
    }
    
    private fun randomDigits(length: Int) = buildString {
        repeat(length) { append((0..9).random()) }
    }
    
    private fun randomFamilyName(): String {
        val names = listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Miller", "Davis", "Wilson", "Moore", "Taylor")
        return names.random()
    }

    private fun ClosedFloatingPointRange<Double>.random(): Double {
        return start + (endInclusive - start) * kotlin.random.Random.nextDouble()
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════

    /** Gets the active group (blocking). */
    fun getActiveGroupBlocking(): SpoofGroup? {
        return runBlocking { activeGroup.first() }
    }

    // ═══════════════════════════════════════════════════════════
    // GROUP MANAGEMENT (For GroupsScreen)
    // ═══════════════════════════════════════════════════════════

    /** Gets all groups as a Flow. */
    fun getAllGroups(): Flow<List<SpoofGroup>> = groups

    /** Creates a new group with generated spoof values. */
    suspend fun createGroup(name: String, description: String = "") {
        // Create the group with generated values
        var newGroup = ConfigManager.createGroup(name)
        
        // Add description and initialize with generated values
        var updatedGroup = newGroup.copy(description = description)
        SpoofType.entries.forEach { type ->
            val value = generateValue(type)
            updatedGroup = updatedGroup.withValue(type, value)
        }
        ConfigManager.updateGroup(updatedGroup)
    }

    /** Updates an existing group. */
    suspend fun updateGroup(group: SpoofGroup) {
        ConfigManager.updateGroup(group)
    }

    /** Deletes a group by ID. */
    suspend fun deleteGroup(groupId: String) {
        ConfigManager.deleteGroup(groupId)
    }

    /** Sets a group as the default. */
    suspend fun setDefaultGroup(groupId: String) {
        setActiveGroup(groupId)
    }

    /** Sets whether a group is enabled (master switch for all its apps). */
    suspend fun setGroupEnabled(groupId: String, enabled: Boolean) {
        val group = ConfigManager.getGroup(groupId) ?: return
        val updatedGroup = group.withEnabled(enabled)
        ConfigManager.updateGroup(updatedGroup)
    }

    // ═══════════════════════════════════════════════════════════
    // APP ASSIGNMENT (For ProfileDetailScreen)
    // ═══════════════════════════════════════════════════════════

    /** Adds an app to a group. */
    suspend fun addAppToGroup(groupId: String, packageName: String) {
        val group = ConfigManager.getGroup(groupId) ?: return
        val updatedGroup = group.addApp(packageName)
        ConfigManager.updateGroup(updatedGroup)
    }

    /** Removes an app from a group. */
    suspend fun removeAppFromGroup(groupId: String, packageName: String) {
        val group = ConfigManager.getGroup(groupId) ?: return
        val updatedGroup = group.removeApp(packageName)
        ConfigManager.updateGroup(updatedGroup)
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT / IMPORT
    // ═══════════════════════════════════════════════════════════

    /** Exports all groups as JSON string. */
    suspend fun exportGroups(): String {
        return ConfigManager.config.first().toJsonString()
    }

    /** Imports groups from JSON string. Returns true on success. */
    suspend fun importGroups(jsonString: String): Boolean {
        return try {
            val config = com.astrixforge.devicemasker.common.JsonConfig.parse(jsonString)
            if (config != null) {
                config.getAllGroups().forEach { group ->
                    ConfigManager.updateGroup(group)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SpoofRepository? = null

        /** Gets the singleton instance. */
        fun getInstance(context: Context): SpoofRepository {
            return INSTANCE
                ?: synchronized(this) {
                    INSTANCE
                        ?: SpoofRepository(context.applicationContext)
                            .also { INSTANCE = it }
                }
        }
    }
}
