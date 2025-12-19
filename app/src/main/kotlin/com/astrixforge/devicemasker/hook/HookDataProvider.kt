package com.astrixforge.devicemasker.hook

import android.content.Context
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import com.highcapable.yukihookapi.hook.log.YLog
import kotlinx.serialization.json.Json

/**
 * Provides access to spoof data during hook execution.
 *
 * This class handles reading SpoofProfile data from DataStore in a blocking manner suitable for
 * hook operations. It caches the data to minimize I/O during hooks.
 *
 * Each profile works independently with its own isEnabled flag and per-type toggles.
 *
 * Usage in hookers:
 * ```kotlin
 * val provider = HookDataProvider.getInstance(context, packageName)
 * val value = provider.getSpoofValue(SpoofType.IMEI)
 * ```
 */
class HookDataProvider
private constructor(private val dataStore: SpoofDataStore, private val packageName: String) {
    private val json = Json { ignoreUnknownKeys = true }

    // Cached data - lazily loaded on first access
    private var _profiles: List<SpoofProfile>? = null
    private var _profileForApp: SpoofProfile? = null
    private var _profileLoaded = false

    /** Gets all profiles, loading from DataStore if not cached. */
    val profiles: List<SpoofProfile>
        get() {
            if (_profiles == null) {
                _profiles = loadProfiles()
            }
            return _profiles ?: emptyList()
        }

    /**
     * Gets the profile to use for the current app.
     *
     * Resolution order:
     * 1. Profile with this app explicitly assigned
     * 2. Default profile (isDefault = true)
     * 3. First profile if any exist
     * 4. Null if no profiles exist
     */
    val profileForApp: SpoofProfile?
        get() {
            if (!_profileLoaded) {
                _profileForApp = resolveProfileForApp()
                _profileLoaded = true
            }
            return _profileForApp
        }

    /**
     * Gets the spoofed value for a type, considering profile settings.
     *
     * Returns null if:
     * - No profile is assigned
     * - Profile is disabled (isEnabled = false)
     * - Type is disabled in the profile
     * - No value is set
     */
    fun getSpoofValue(type: SpoofType): String? {
        // Get profile for this app
        val profile = profileForApp
        if (profile == null) {
            YLog.debug("HookDataProvider: No profile for $packageName, skipping $type")
            return null
        }

        // Check if profile is enabled (master switch)
        if (!profile.isEnabled) {
            YLog.debug(
                "HookDataProvider: Profile '${profile.name}' is disabled, skipping $type for $packageName"
            )
            return null
        }

        // Check if type is enabled in profile
        if (!profile.isTypeEnabled(type)) {
            YLog.debug(
                "HookDataProvider: Type $type is disabled in profile ${profile.name}, skipping"
            )
            return null
        }

        // Get value from profile
        val value = profile.getValue(type)
        YLog.debug("HookDataProvider: Using profile '${profile.name}' value for $type: $value")
        return value
    }



    /** Clears cached data, forcing reload on next access. */
    fun invalidateCache() {
        _profiles = null
        _profileForApp = null
        _profileLoaded = false
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE METHODS
    // ═══════════════════════════════════════════════════════════

    private fun loadProfiles(): List<SpoofProfile>? {
        return try {
            val jsonString = dataStore.getProfilesJsonBlocking()
            if (jsonString.isNullOrBlank()) {
                YLog.debug("HookDataProvider: No profiles found")
                emptyList()
            } else {
                json.decodeFromString<List<SpoofProfile>>(jsonString)
            }
        } catch (e: Exception) {
            YLog.error("HookDataProvider: Failed to load profiles: ${e.message}")
            emptyList()
        }
    }

    private fun resolveProfileForApp(): SpoofProfile? {
        val allProfiles = profiles
        if (allProfiles.isEmpty()) {
            YLog.debug("HookDataProvider: No profiles available for $packageName")
            return null
        }

        // 1. Find profile with this app explicitly assigned
        val assignedProfile = allProfiles.find { it.isAppAssigned(packageName) }
        if (assignedProfile != null) {
            YLog.info(
                "HookDataProvider: Using assigned profile '${assignedProfile.name}' for $packageName"
            )
            return assignedProfile
        }

        // 2. Use default profile
        val defaultProfile = allProfiles.find { it.isDefault }
        if (defaultProfile != null) {
            YLog.debug(
                "HookDataProvider: Using default profile '${defaultProfile.name}' for $packageName (no explicit assignment)"
            )
            return defaultProfile
        }

        // 3. Use first profile as fallback
        val firstProfile = allProfiles.firstOrNull()
        if (firstProfile != null) {
            YLog.debug(
                "HookDataProvider: Using first profile '${firstProfile.name}' for $packageName (no default)"
            )
            return firstProfile
        }

        return null
    }

    companion object {
        @Volatile private var instances = mutableMapOf<String, HookDataProvider>()

        /**
         * Gets or creates a HookDataProvider for the given context and package.
         *
         * @param context Android context for DataStore access
         * @param packageName The package name of the app being hooked
         */
        fun getInstance(context: Context, packageName: String): HookDataProvider {
            return instances.getOrPut(packageName) {
                val dataStore = SpoofDataStore(context)
                HookDataProvider(dataStore, packageName)
            }
        }

        /** Clears all cached providers. Call when data changes externally. */
        fun invalidateAll() {
            instances.values.forEach { it.invalidateCache() }
        }
    }
}
