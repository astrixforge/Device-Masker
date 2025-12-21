package com.astrixforge.devicemasker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.astrixforge.devicemasker.common.CorrelationGroup
import com.astrixforge.devicemasker.common.SpoofProfile
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.generators.DeviceHardwareProfileGenerator
import com.astrixforge.devicemasker.common.generators.FingerprintGenerator
import com.astrixforge.devicemasker.common.generators.ICCIDGenerator
import com.astrixforge.devicemasker.common.generators.IMEIGenerator
import com.astrixforge.devicemasker.common.generators.IMSIGenerator
import com.astrixforge.devicemasker.common.generators.MACGenerator
import com.astrixforge.devicemasker.common.generators.PhoneNumberGenerator
import com.astrixforge.devicemasker.common.generators.SerialGenerator
import com.astrixforge.devicemasker.common.generators.SIMProfileGenerator
import com.astrixforge.devicemasker.common.generators.UUIDGenerator
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.models.DeviceHardwareProfile
import com.astrixforge.devicemasker.common.models.LocationProfile
import com.astrixforge.devicemasker.common.models.SIMProfile
import com.astrixforge.devicemasker.service.ConfigManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Main repository combining all spoof-related data operations.
 *
 * HMA-OSS Architecture: This repository now wraps ConfigManager and provides
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
     * Cached profiles for correlated value generation.
     * 
     * These ensure that values in the same correlation group always use
     * the same underlying profile, preventing detection from mismatches.
     */
    private var cachedSIMProfile: SIMProfile? = null
    private var cachedLocationProfile: LocationProfile? = null
    private var cachedDeviceHardware: DeviceHardwareProfile? = null

    // ═══════════════════════════════════════════════════════════
    // UI STATE FLOWS
    // ═══════════════════════════════════════════════════════════

    /** Flow of module enabled state. */
    val moduleEnabled: Flow<Boolean> = ConfigManager.config.map { it.isModuleEnabled }

    /** Flow of all profiles. */
    val profiles: Flow<List<SpoofProfile>> = ConfigManager.config.map { it.getAllProfiles() }

    /** Flow of the active profile (default profile). */
    val activeProfile: Flow<SpoofProfile?> = ConfigManager.config.map { it.getDefaultProfile() }

    /** Flow of enabled app count. */
    val enabledAppCount: Flow<Int> =
        profiles.map { profileList ->
            profileList.filter { it.isEnabled }.flatMap { it.assignedApps }.distinct().size
        }

    /** Combined UI state flow for dashboard. */
    data class DashboardState(
        val isModuleEnabled: Boolean,
        val activeProfile: SpoofProfile?,
        val enabledAppCount: Int,
        val profileCount: Int,
    )

    val dashboardState: Flow<DashboardState> =
        combine(moduleEnabled, activeProfile, enabledAppCount, profiles) { enabled,
                                                                           profile,
                                                                           appCount,
                                                                           profileList ->
            DashboardState(
                isModuleEnabled = enabled,
                activeProfile = profile,
                enabledAppCount = appCount,
                profileCount = profileList.size,
            )
        }

    // ═══════════════════════════════════════════════════════════
    // MODULE SETTINGS
    // ═══════════════════════════════════════════════════════════

    /** Enables or disables the module globally. */
    suspend fun setModuleEnabled(enabled: Boolean) {
        ConfigManager.setModuleEnabled(enabled)
    }

    /** Sets the active profile by ID (makes it default). */
    suspend fun setActiveProfile(profileId: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        // Set this profile as default
        val updatedProfile = profile.copy(isDefault = true)
        ConfigManager.updateProfile(updatedProfile)
        
        // Unset other profiles as default
        ConfigManager.getAllProfiles().forEach { other ->
            if (other.id != profileId && other.isDefault) {
                ConfigManager.updateProfile(other.copy(isDefault = false))
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
        // Generate SIM profile if not cached
        if (cachedSIMProfile == null) {
            cachedSIMProfile = SIMProfileGenerator.generate()
        }

        return when (type) {
            SpoofType.IMSI -> cachedSIMProfile!!.imsi
            SpoofType.ICCID -> cachedSIMProfile!!.iccid
            SpoofType.CARRIER_NAME -> cachedSIMProfile!!.carrierName
            SpoofType.CARRIER_MCC_MNC -> cachedSIMProfile!!.mccMnc
            SpoofType.PHONE_NUMBER -> cachedSIMProfile!!.phoneNumber
            // NEW: Additional SIM values for comprehensive spoofing
            SpoofType.SIM_COUNTRY_ISO -> cachedSIMProfile!!.simCountryIso
            SpoofType.NETWORK_COUNTRY_ISO -> cachedSIMProfile!!.networkCountryIso
            SpoofType.SIM_OPERATOR_NAME -> cachedSIMProfile!!.simOperatorName
            SpoofType.NETWORK_OPERATOR -> cachedSIMProfile!!.networkOperator
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
        // Get the current carrier from cache, or generate new profile if no cache
        val currentCarrier = cachedSIMProfile?.carrier ?: Carrier.random()
        
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
        // Generate location profile if not cached
        if (cachedLocationProfile == null) {
            cachedLocationProfile = LocationProfile.generate()
        }

        return when (type) {
            SpoofType.TIMEZONE -> cachedLocationProfile!!.timezone
            SpoofType.LOCALE -> cachedLocationProfile!!.locale
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
        // Generate device hardware profile if not cached
        if (cachedDeviceHardware == null) {
            cachedDeviceHardware = DeviceHardwareProfileGenerator.generate()
        }

        return when (type) {
            SpoofType.IMEI -> cachedDeviceHardware!!.imei
            SpoofType.SERIAL -> cachedDeviceHardware!!.serial
            SpoofType.WIFI_MAC -> cachedDeviceHardware!!.wifiMAC
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
        cachedSIMProfile = null
        cachedLocationProfile = null
        cachedDeviceHardware = null
    }
    
    /**
     * Resets a specific correlation group's cache.
     * Call this before regenerating correlated values to get fresh values.
     */
    fun resetCorrelationGroup(group: CorrelationGroup) {
        when (group) {
            CorrelationGroup.SIM_CARD -> cachedSIMProfile = null
            CorrelationGroup.LOCATION -> cachedLocationProfile = null
            CorrelationGroup.DEVICE_HARDWARE -> cachedDeviceHardware = null
            CorrelationGroup.NONE -> { /* No cache for independent values */ }
        }
    }
    
    /**
     * Regenerates both timezone and locale values together.
     * 
     * This ensures both come from the SAME location profile (same country),
     * avoiding mismatches like "Asia/Kolkata" with "en_US".
     * 
     * @param profileId Profile to update
     */
    suspend fun regenerateLocationValues(profileId: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        
        // Reset cache to get fresh location profile
        cachedLocationProfile = null
        
        // Generate new location profile
        val locationProfile = LocationProfile.generate()
        cachedLocationProfile = locationProfile
        
        // Update both values from the SAME profile
        var updatedProfile = profile.withValue(SpoofType.TIMEZONE, locationProfile.timezone)
        updatedProfile = updatedProfile.withValue(SpoofType.LOCALE, locationProfile.locale)
        
        ConfigManager.updateProfile(updatedProfile)
    }
    /**
     * Updates a profile with SIM values from a specific carrier.
     * 
     * This generates all SIM-related values (IMSI, ICCID, Phone, etc.) 
     * from the selected carrier and updates the profile.
     * 
     * ALSO syncs Location values (timezone/locale) to match carrier's country.
     * This prevents detection from SIM/Location country mismatches.
     * 
     * @param profileId Profile to update
     * @param carrier The carrier to use for generation
     */
    suspend fun updateProfileWithCarrier(profileId: String, carrier: com.astrixforge.devicemasker.common.models.Carrier) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        
        // Generate SIM profile from specific carrier
        val simProfile = SIMProfileGenerator.generate(carrier)
        cachedSIMProfile = simProfile
        
        // NEW: Also generate location matching carrier's country
        // This ensures timezone/locale/GPS match the SIM country
        val locationProfile = LocationProfile.generateForCarrier(carrier)
        cachedLocationProfile = locationProfile
        
        // Update all SIM-related values in the profile
        var updatedProfile = profile.copy(selectedCarrierMccMnc = carrier.mccMnc)
        updatedProfile = updatedProfile.withValue(SpoofType.IMSI, simProfile.imsi)
        updatedProfile = updatedProfile.withValue(SpoofType.ICCID, simProfile.iccid)
        updatedProfile = updatedProfile.withValue(SpoofType.PHONE_NUMBER, simProfile.phoneNumber)
        updatedProfile = updatedProfile.withValue(SpoofType.CARRIER_NAME, simProfile.carrierName)
        updatedProfile = updatedProfile.withValue(SpoofType.CARRIER_MCC_MNC, simProfile.mccMnc)
        updatedProfile = updatedProfile.withValue(SpoofType.SIM_COUNTRY_ISO, simProfile.simCountryIso)
        updatedProfile = updatedProfile.withValue(SpoofType.NETWORK_COUNTRY_ISO, simProfile.networkCountryIso)
        updatedProfile = updatedProfile.withValue(SpoofType.SIM_OPERATOR_NAME, simProfile.simOperatorName)
        updatedProfile = updatedProfile.withValue(SpoofType.NETWORK_OPERATOR, simProfile.networkOperator)
        
        // Sync Location to carrier country (prevents detection)
        updatedProfile = updatedProfile.withValue(SpoofType.TIMEZONE, locationProfile.timezone)
        updatedProfile = updatedProfile.withValue(SpoofType.LOCALE, locationProfile.locale)
        
        // NEW: Sync GPS coordinates to carrier country
        updatedProfile = updatedProfile.withValue(
            SpoofType.LOCATION_LATITUDE,
            String.format(java.util.Locale.US, "%.6f", locationProfile.latitude)
        )
        updatedProfile = updatedProfile.withValue(
            SpoofType.LOCATION_LONGITUDE,
            String.format(java.util.Locale.US, "%.6f", locationProfile.longitude)
        )
        
        ConfigManager.updateProfile(updatedProfile)
    }
    
    /**
     * Updates a profile with hardware values matching the device profile.
     * 
     * When user selects a Device Profile (e.g., "Pixel 8 Pro"), this ensures:
     * - IMEI uses appropriate TAC prefix
     * - Serial matches manufacturer pattern (e.g., FA6AB for Google)
     * - WiFi MAC may use manufacturer OUI
     * 
     * @param profileId Profile to update
     * @param presetId The device preset ID to use
     */
    suspend fun updateProfileWithDeviceProfile(profileId: String, presetId: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val preset = com.astrixforge.devicemasker.common.DeviceProfilePreset.findById(presetId) ?: return
        
        // Generate hardware matching the device profile
        val hardwareProfile = DeviceHardwareProfileGenerator.generate(preset)
        cachedDeviceHardware = hardwareProfile
        
        var updatedProfile = profile.withValue(SpoofType.DEVICE_PROFILE, presetId)
        updatedProfile = updatedProfile.withValue(SpoofType.IMEI, hardwareProfile.imei)
        updatedProfile = updatedProfile.withValue(SpoofType.SERIAL, hardwareProfile.serial)
        updatedProfile = updatedProfile.withValue(SpoofType.WIFI_MAC, hardwareProfile.wifiMAC)
        
        ConfigManager.updateProfile(updatedProfile)
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

    /** Gets the active profile (blocking). */
    fun getActiveProfileBlocking(): SpoofProfile? {
        return runBlocking { activeProfile.first() }
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE MANAGEMENT (For ProfileScreen)
    // ═══════════════════════════════════════════════════════════

    /** Gets all profiles as a Flow. */
    fun getAllProfiles(): Flow<List<SpoofProfile>> = profiles

    /** Creates a new profile with generated spoof values. */
    suspend fun createProfile(name: String, description: String = "") {
        // Create the profile with generated values
        var newProfile = ConfigManager.createProfile(name)
        
        // Add description and initialize with generated values
        var updatedProfile = newProfile.copy(description = description)
        SpoofType.entries.forEach { type ->
            val value = generateValue(type)
            updatedProfile = updatedProfile.withValue(type, value)
        }
        ConfigManager.updateProfile(updatedProfile)
    }

    /** Updates an existing profile. */
    suspend fun updateProfile(profile: SpoofProfile) {
        ConfigManager.updateProfile(profile)
    }

    /** Deletes a profile by ID. */
    suspend fun deleteProfile(profileId: String) {
        ConfigManager.deleteProfile(profileId)
    }

    /** Sets a profile as the default. */
    suspend fun setDefaultProfile(profileId: String) {
        setActiveProfile(profileId)
    }

    /** Sets whether a profile is enabled (master switch for all its apps). */
    suspend fun setProfileEnabled(profileId: String, enabled: Boolean) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.withEnabled(enabled)
        ConfigManager.updateProfile(updatedProfile)
    }

    // ═══════════════════════════════════════════════════════════
    // APP ASSIGNMENT (For ProfileDetailScreen)
    // ═══════════════════════════════════════════════════════════

    /** Adds an app to a profile. */
    suspend fun addAppToProfile(profileId: String, packageName: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.addApp(packageName)
        ConfigManager.updateProfile(updatedProfile)
    }

    /** Removes an app from a profile. */
    suspend fun removeAppFromProfile(profileId: String, packageName: String) {
        val profile = ConfigManager.getProfile(profileId) ?: return
        val updatedProfile = profile.removeApp(packageName)
        ConfigManager.updateProfile(updatedProfile)
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT / IMPORT
    // ═══════════════════════════════════════════════════════════

    /** Exports all profiles as JSON string. */
    suspend fun exportProfiles(): String {
        return ConfigManager.config.first().toJsonString()
    }

    /** Imports profiles from JSON string. Returns true on success. */
    suspend fun importProfiles(jsonString: String): Boolean {
        return try {
            val config = com.astrixforge.devicemasker.common.JsonConfig.parse(jsonString)
            if (config != null) {
                config.getAllProfiles().forEach { profile ->
                    ConfigManager.updateProfile(profile)
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
