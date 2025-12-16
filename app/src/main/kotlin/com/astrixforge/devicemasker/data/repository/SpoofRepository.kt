package com.astrixforge.devicemasker.data.repository

import android.content.Context
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.generators.FingerprintGenerator
import com.astrixforge.devicemasker.data.generators.IMEIGenerator
import com.astrixforge.devicemasker.data.generators.MACGenerator
import com.astrixforge.devicemasker.data.generators.SerialGenerator
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.astrixforge.devicemasker.data.models.AppConfig
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Main repository combining all spoof-related data operations.
 *
 * This is the primary data access layer for the UI, combining:
 * - Profile management (ProfileRepository)
 * - App scope management (AppScopeRepository)
 * - Direct DataStore access (SpoofDataStore)
 * - Value generation (Generators)
 *
 * @param context Application context
 * @param dataStore The SpoofDataStore instance
 */
class SpoofRepository(
    private val context: Context,
    private val dataStore: SpoofDataStore
) {
    val profileRepository = ProfileRepository(dataStore)
    val appScopeRepository = AppScopeRepository(context, dataStore)

    // ═══════════════════════════════════════════════════════════
    // UI STATE FLOWS
    // ═══════════════════════════════════════════════════════════

    /**
     * Flow of module enabled state.
     */
    val moduleEnabled: Flow<Boolean> = dataStore.moduleEnabled

    /**
     * Flow of all profiles.
     */
    val profiles: Flow<List<SpoofProfile>> = profileRepository.profiles

    /**
     * Flow of the active profile.
     */
    val activeProfile: Flow<SpoofProfile?> = profileRepository.activeProfile

    /**
     * Flow of enabled app count.
     */
    val enabledAppCount: Flow<Int> = appScopeRepository.appConfigs.map { configs ->
        configs.values.count { it.isEnabled }
    }

    /**
     * Combined UI state flow for dashboard.
     */
    data class DashboardState(
        val isModuleEnabled: Boolean,
        val activeProfile: SpoofProfile?,
        val enabledAppCount: Int,
        val profileCount: Int
    )

    val dashboardState: Flow<DashboardState> = combine(
        moduleEnabled,
        activeProfile,
        enabledAppCount,
        profiles
    ) { enabled, profile, appCount, profileList ->
        DashboardState(
            isModuleEnabled = enabled,
            activeProfile = profile,
            enabledAppCount = appCount,
            profileCount = profileList.size
        )
    }

    // ═══════════════════════════════════════════════════════════
    // MODULE SETTINGS
    // ═══════════════════════════════════════════════════════════

    /**
     * Enables or disables the module globally.
     */
    suspend fun setModuleEnabled(enabled: Boolean) {
        dataStore.setModuleEnabled(enabled)
    }

    /**
     * Sets the active profile by ID.
     */
    suspend fun setActiveProfile(profileId: String) {
        dataStore.setActiveProfileId(profileId)
    }

    // ═══════════════════════════════════════════════════════════
    // VALUE GENERATION
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates a new random value for a spoof type.
     */
    fun generateValue(type: SpoofType): String {
        return when (type) {
            // Device
            SpoofType.IMEI -> IMEIGenerator.generate()
            SpoofType.MEID -> IMEIGenerator.generate()
            SpoofType.IMSI -> generateImsi()
            SpoofType.SERIAL -> SerialGenerator.generate()
            SpoofType.ICCID -> generateIccid()
            SpoofType.PHONE_NUMBER -> generatePhoneNumber()

            // Network
            SpoofType.WIFI_MAC -> MACGenerator.generateWiFiMAC()
            SpoofType.BLUETOOTH_MAC -> MACGenerator.generateBluetoothMAC()
            SpoofType.WIFI_SSID -> "\"Network_${(1000..9999).random()}\""
            SpoofType.WIFI_BSSID -> MACGenerator.generate()
            SpoofType.CARRIER_NAME -> listOf("Carrier", "T-Mobile", "Verizon", "AT&T").random()
            SpoofType.CARRIER_MCC_MNC -> "310${(100..999).random()}"

            // Advertising
            SpoofType.ANDROID_ID -> UUIDGenerator.generateAndroidId()
            SpoofType.GSF_ID -> UUIDGenerator.generateGSFId()
            SpoofType.ADVERTISING_ID -> UUIDGenerator.generateAdvertisingId()
            SpoofType.MEDIA_DRM_ID -> UUIDGenerator.generateMediaDrmId()

            // System
            SpoofType.BUILD_FINGERPRINT -> FingerprintGenerator.generate()
            SpoofType.BUILD_MODEL -> FingerprintGenerator.generateBuildProperties()["MODEL"] ?: "Pixel 8"
            SpoofType.BUILD_MANUFACTURER -> FingerprintGenerator.generateBuildProperties()["MANUFACTURER"] ?: "Google"
            SpoofType.BUILD_BRAND -> FingerprintGenerator.generateBuildProperties()["BRAND"] ?: "google"
            SpoofType.BUILD_DEVICE -> FingerprintGenerator.generateBuildProperties()["DEVICE"] ?: "shiba"
            SpoofType.BUILD_PRODUCT -> FingerprintGenerator.generateBuildProperties()["PRODUCT"] ?: "shiba"
            SpoofType.BUILD_BOARD -> FingerprintGenerator.generateBuildProperties()["BOARD"] ?: "shiba"

            // Location
            SpoofType.LOCATION_LATITUDE -> String.format("%.6f", (-90.0..90.0).random())
            SpoofType.LOCATION_LONGITUDE -> String.format("%.6f", (-180.0..180.0).random())
            SpoofType.TIMEZONE -> listOf(
                "America/New_York", "America/Los_Angeles", "Europe/London",
                "Asia/Tokyo", "Australia/Sydney"
            ).random()
            SpoofType.LOCALE -> listOf("en_US", "en_GB", "ja_JP", "de_DE", "fr_FR").random()
        }
    }

    private fun generateImsi(): String {
        return "310" + (100..999).random().toString() + (100000000L..999999999L).random().toString()
    }

    private fun generateIccid(): String {
        return "8901" + List(16) { (0..9).random() }.joinToString("")
    }

    private fun generatePhoneNumber(): String {
        return "+1${(200..999).random()}${(100..999).random()}${(1000..9999).random()}"
    }

    private fun ClosedFloatingPointRange<Double>.random(): Double {
        return start + (endInclusive - start) * kotlin.random.Random.nextDouble()
    }

    /**
     * Regenerates a value in the active profile.
     */
    suspend fun regenerateValue(type: SpoofType) {
        val profile = activeProfile.first() ?: return
        val newValue = generateValue(type)
        profileRepository.updateProfileValue(profile.id, type, newValue)
    }

    /**
     * Regenerates all values in the active profile.
     */
    suspend fun regenerateAllValues() {
        val profile = activeProfile.first() ?: return
        
        SpoofType.entries.forEach { type ->
            val newValue = generateValue(type)
            profileRepository.updateProfileValue(profile.id, type, newValue)
        }
    }

    /**
     * Regenerates values for a specific category.
     */
    suspend fun regenerateCategory(category: SpoofCategory) {
        val profile = activeProfile.first() ?: return
        
        SpoofType.byCategory(category).forEach { type ->
            val newValue = generateValue(type)
            profileRepository.updateProfileValue(profile.id, type, newValue)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // APP MANAGEMENT DELEGATES
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets installed apps with their configuration as a Flow.
     */
    fun getInstalledApps(): Flow<List<InstalledApp>> {
        return appScopeRepository.getInstalledAppsFlow()
    }

    /**
     * Gets installed apps with their configuration (suspend).
     */
    suspend fun getInstalledAppsList(includeSystem: Boolean = false): List<InstalledApp> {
        return appScopeRepository.getInstalledApps(includeSystem)
    }

    /**
     * Gets the set of enabled package names as a Flow.
     */
    fun getEnabledPackages(): Flow<Set<String>> {
        return appScopeRepository.appConfigs.map { configs ->
            configs.filterValues { it.isEnabled }.keys
        }
    }

    /**
     * Enables/disables an app for spoofing.
     */
    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        appScopeRepository.setAppEnabled(packageName, enabled)
    }

    /**
     * Assigns a profile to an app.
     */
    suspend fun setAppProfile(packageName: String, profileId: String?) {
        appScopeRepository.setAppProfile(packageName, profileId)
    }

    /**
     * Gets the effective spoof value for an app and type.
     * Considers app config, profile, and generates if needed.
     */
    suspend fun getEffectiveValue(packageName: String, type: SpoofType): String? {
        // Check if spoofing is enabled for this app
        val appConfig = appScopeRepository.getAppConfig(packageName)
        if (appConfig?.isEnabled != true) return null

        // Check if this spoof type is enabled for this app
        if (!appConfig.isSpoofTypeEnabled(type)) return null

        // Get the profile for this app
        val profileId = appConfig.profileId
        val profile = if (profileId != null) {
            profileRepository.getProfileById(profileId)
        } else {
            profileRepository.getDefaultProfile()
        }

        // Get the value from the profile
        var value = profile?.getValue(type)

        // Generate if not set
        if (value == null) {
            value = generateValue(type)
            // Optionally save the generated value
            profile?.let {
                profileRepository.updateProfileValue(it.id, type, value)
            }
        }

        return value
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════

    /**
     * Checks if module is enabled (blocking).
     */
    fun isModuleEnabledBlocking(): Boolean {
        return dataStore.isModuleEnabledBlocking()
    }

    /**
     * Gets effective spoof value for hook (blocking).
     */
    fun getEffectiveValueBlocking(packageName: String, type: SpoofType): String? {
        return kotlinx.coroutines.runBlocking {
            getEffectiveValue(packageName, type)
        }
    }

    /**
     * Checks if app is enabled for spoofing (blocking).
     */
    fun isAppEnabledBlocking(packageName: String): Boolean {
        return appScopeRepository.isAppEnabledBlocking(packageName)
    }

    /**
     * Gets the active profile (blocking).
     */
    fun getActiveProfileBlocking(): SpoofProfile? {
        return kotlinx.coroutines.runBlocking {
            activeProfile.first()
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE MANAGEMENT (For ProfileScreen)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets all profiles as a Flow.
     */
    fun getAllProfiles(): Flow<List<SpoofProfile>> = profiles

    /**
     * Gets the active profile ID as a Flow.
     */
    fun getActiveProfileId(): Flow<String?> = dataStore.activeProfileId

    /**
     * Creates a new profile with generated values.
     */
    fun createProfile(name: String, description: String = "") {
        kotlinx.coroutines.runBlocking {
            // Create the profile first
            val newProfile = profileRepository.createProfile(name)
            
            // Generate all values for the new profile
            var updatedProfile = newProfile.copy(description = description)
            SpoofType.entries.forEach { type ->
                val value = generateValue(type)
                updatedProfile = updatedProfile.withValue(type, value)
            }
            profileRepository.updateProfile(updatedProfile)
        }
    }

    /**
     * Updates an existing profile.
     */
    fun updateProfile(profile: SpoofProfile) {
        kotlinx.coroutines.runBlocking {
            profileRepository.updateProfile(profile)
        }
    }

    /**
     * Deletes a profile by ID.
     */
    fun deleteProfile(profileId: String) {
        kotlinx.coroutines.runBlocking {
            profileRepository.deleteProfile(profileId)
        }
    }

    /**
     * Sets a profile as the default.
     */
    fun setDefaultProfile(profileId: String) {
        kotlinx.coroutines.runBlocking {
            profileRepository.setAsDefault(profileId)
            dataStore.setActiveProfileId(profileId)
        }
    }


    companion object {
        @Volatile
        private var INSTANCE: SpoofRepository? = null

        /**
         * Gets the singleton instance.
         */
        fun getInstance(context: Context): SpoofRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SpoofRepository(
                    context.applicationContext,
                    SpoofDataStore(context.applicationContext)
                ).also { INSTANCE = it }
            }
        }
    }
}
