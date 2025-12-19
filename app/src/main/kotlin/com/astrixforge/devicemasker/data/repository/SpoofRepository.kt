package com.astrixforge.devicemasker.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.generators.FingerprintGenerator
import com.astrixforge.devicemasker.data.generators.IMEIGenerator
import com.astrixforge.devicemasker.data.generators.MACGenerator
import com.astrixforge.devicemasker.data.generators.SerialGenerator
import com.astrixforge.devicemasker.data.generators.UUIDGenerator
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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
class SpoofRepository(private val context: Context, private val dataStore: SpoofDataStore) {
    val profileRepository: ProfileRepository = ProfileRepository(dataStore)
    val appScopeRepository: AppScopeRepository = AppScopeRepository(context)

    // ═══════════════════════════════════════════════════════════
    // UI STATE FLOWS
    // ═══════════════════════════════════════════════════════════

    /** Flow of module enabled state. */
    val moduleEnabled: Flow<Boolean> = dataStore.moduleEnabled

    /** Flow of all profiles. */
    val profiles: Flow<List<SpoofProfile>> = profileRepository.profiles

    /** Flow of the active profile. */
    val activeProfile: Flow<SpoofProfile?> = profileRepository.activeProfile

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
        dataStore.setModuleEnabled(enabled)
    }

    /** Sets the active profile by ID. */
    suspend fun setActiveProfile(profileId: String) {
        dataStore.setActiveProfileId(profileId)
    }

    // ═══════════════════════════════════════════════════════════
    // VALUE GENERATION
    // ═══════════════════════════════════════════════════════════

    /** Generates a new random value for a spoof type. */
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
            SpoofType.BUILD_MODEL ->
                FingerprintGenerator.generateBuildProperties()["MODEL"] ?: "Pixel 8"

            SpoofType.BUILD_MANUFACTURER ->
                FingerprintGenerator.generateBuildProperties()["MANUFACTURER"] ?: "Google"

            SpoofType.BUILD_BRAND ->
                FingerprintGenerator.generateBuildProperties()["BRAND"] ?: "google"

            SpoofType.BUILD_DEVICE ->
                FingerprintGenerator.generateBuildProperties()["DEVICE"] ?: "shiba"

            SpoofType.BUILD_PRODUCT ->
                FingerprintGenerator.generateBuildProperties()["PRODUCT"] ?: "shiba"

            SpoofType.BUILD_BOARD ->
                FingerprintGenerator.generateBuildProperties()["BOARD"] ?: "shiba"

            // Location
            SpoofType.LOCATION_LATITUDE -> String.format(java.util.Locale.US, "%.6f", (-90.0..90.0).random())
            SpoofType.LOCATION_LONGITUDE -> String.format(java.util.Locale.US, "%.6f", (-180.0..180.0).random())
            SpoofType.TIMEZONE ->
                listOf(
                    "America/New_York",
                    "America/Los_Angeles",
                    "Europe/London",
                    "Asia/Tokyo",
                    "Australia/Sydney",
                )
                    .random()

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

    // ═══════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════
    // APP MANAGEMENT DELEGATES
    // ═══════════════════════════════════════════════════════════


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
        // Create the profile first
        val newProfile = profileRepository.createProfile(name)

        // Initialize with generated values for all spoof types
        var updatedProfile = newProfile.copy(description = description)
        SpoofType.entries.forEach { type ->
            val value = generateValue(type)
            updatedProfile = updatedProfile.withValue(type, value)
        }
        profileRepository.updateProfile(updatedProfile)
    }

    /** Updates an existing profile. */
    suspend fun updateProfile(profile: SpoofProfile) {
        profileRepository.updateProfile(profile)
    }

    /** Deletes a profile by ID. */
    suspend fun deleteProfile(profileId: String) {
        profileRepository.deleteProfile(profileId)
    }

    /** Sets a profile as the default. */
    suspend fun setDefaultProfile(profileId: String) {
        profileRepository.setAsDefault(profileId)
        dataStore.setActiveProfileId(profileId)
    }

    /** Sets whether a profile is enabled (master switch for all its apps). */
    suspend fun setProfileEnabled(profileId: String, enabled: Boolean) {
        val profile = profileRepository.getProfileById(profileId) ?: return
        val updatedProfile = profile.withEnabled(enabled)
        profileRepository.updateProfile(updatedProfile)
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT / IMPORT
    // ═══════════════════════════════════════════════════════════

    /** Exports all profiles as JSON string. */
    suspend fun exportProfiles(): String {
        return profileRepository.exportProfiles()
    }

    /** Imports profiles from JSON string. Returns true on success. */
    suspend fun importProfiles(jsonString: String): Boolean {
        return profileRepository.importProfiles(jsonString)
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
                        ?: SpoofRepository(
                            context.applicationContext,
                            SpoofDataStore(context.applicationContext),
                        )
                            .also { INSTANCE = it }
                }
        }
    }
}
