package com.astrixforge.devicemasker.data.repository

import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.hook.HookDataProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Repository for managing spoof profiles.
 *
 * Handles CRUD operations for profiles with DataStore persistence using Kotlinx Serialization for
 * JSON storage.
 *
 * @param dataStore The SpoofDataStore instance for persistence
 */
class ProfileRepository(private val dataStore: SpoofDataStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /** Flow of all saved profiles. */
    val profiles: Flow<List<SpoofProfile>> =
        dataStore.profilesJson.map { jsonString ->
            if (jsonString.isNullOrEmpty()) {
                // Return default profile if none exist
                listOf(SpoofProfile.createDefaultProfile())
            } else {
                try {
                    json.decodeFromString<List<SpoofProfile>>(jsonString)
                } catch (e: Exception) {
                    listOf(SpoofProfile.createDefaultProfile())
                }
            }
        }

    /** Flow of the currently active profile. */
    val activeProfile: Flow<SpoofProfile?> =
        dataStore.activeProfileId.map { profileId ->
            if (profileId == null) {
                getDefaultProfile()
            } else {
                getProfileById(profileId)
            }
        }

    /** Gets a profile by its ID. */
    suspend fun getProfileById(id: String): SpoofProfile? {
        return profiles.first().find { it.id == id }
    }

    /** Gets the default profile (marked as isDefault = true). */
    suspend fun getDefaultProfile(): SpoofProfile? {
        return profiles.first().find { it.isDefault } ?: profiles.first().firstOrNull()
    }

    /** Creates a new profile. */
    suspend fun createProfile(name: String, isDefault: Boolean = false): SpoofProfile {
        val currentProfiles = profiles.first().toMutableList()

        // If this is default, unset other defaults
        if (isDefault) {
            currentProfiles.replaceAll { it.copy(isDefault = false) }
        }

        val newProfile = SpoofProfile.createNew(name, isDefault)
        currentProfiles.add(newProfile)

        saveProfiles(currentProfiles)
        return newProfile
    }

    /** Updates an existing profile. */
    suspend fun updateProfile(profile: SpoofProfile) {
        val currentProfiles = profiles.first().toMutableList()
        val index = currentProfiles.indexOfFirst { it.id == profile.id }

        if (index != -1) {
            // If this is becoming default, unset other defaults
            if (profile.isDefault) {
                currentProfiles.replaceAll {
                    if (it.id != profile.id) it.copy(isDefault = false) else it
                }
            }
            currentProfiles[index] = profile.copy(updatedAt = System.currentTimeMillis())
            saveProfiles(currentProfiles)
        }
    }

    /** Deletes a profile by ID. */
    suspend fun deleteProfile(id: String) {
        val currentProfiles = profiles.first().toMutableList()
        val removed = currentProfiles.removeAll { it.id == id }

        if (removed) {
            // Ensure at least one default profile exists
            if (currentProfiles.none { it.isDefault } && currentProfiles.isNotEmpty()) {
                currentProfiles[0] = currentProfiles[0].copy(isDefault = true)
            }

            // If all profiles deleted, create a new default
            if (currentProfiles.isEmpty()) {
                currentProfiles.add(SpoofProfile.createDefaultProfile())
            }

            saveProfiles(currentProfiles)
        }
    }


    /** Sets a profile as the default. */
    suspend fun setAsDefault(id: String) {
        val currentProfiles = profiles.first().toMutableList()

        currentProfiles.replaceAll { profile -> profile.copy(isDefault = profile.id == id) }

        saveProfiles(currentProfiles)
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE VALUE OPERATIONS
    // ═══════════════════════════════════════════════════════════


    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    /** Saves profiles to DataStore as JSON. */
    private suspend fun saveProfiles(profileList: List<SpoofProfile>) {
        val jsonString = json.encodeToString(profileList)
        dataStore.saveProfilesJson(jsonString)
        // Invalidate hook cache to ensure fresh data for any hooks running in the current process
        HookDataProvider.invalidateAll()
    }

    /** Exports all profiles as JSON string. */
    suspend fun exportProfiles(): String {
        return json.encodeToString(profiles.first())
    }

    /** Imports profiles from JSON string. */
    suspend fun importProfiles(jsonString: String): Boolean {
        return try {
            val imported = json.decodeFromString<List<SpoofProfile>>(jsonString)
            saveProfiles(imported)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════



    // ═══════════════════════════════════════════════════════════
    // ASSIGNED APPS MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets the profile for an app synchronously (blocking). Falls back to default profile if app is
     * not assigned to any profile.
     *
     * @param packageName The app's package name to look up
     * @return The profile for this app (assigned or default)
     */

    /**
     * Adds an app to a profile's assignedApps. If the app is already in another profile, it will be
     * removed from that profile first.
     *
     * @param profileId The profile to add the app to
     * @param packageName The app's package name to add
     */
    suspend fun addAppToProfile(profileId: String, packageName: String) {
        val currentProfiles = profiles.first().toMutableList()

        // Remove from any existing profile first (one app = one profile)
        currentProfiles.replaceAll { profile ->
            if (packageName in profile.assignedApps && profile.id != profileId) {
                profile.removeApp(packageName)
            } else {
                profile
            }
        }

        // Add to target profile
        val targetIndex = currentProfiles.indexOfFirst { it.id == profileId }
        if (targetIndex != -1) {
            val targetProfile = currentProfiles[targetIndex]
            currentProfiles[targetIndex] = targetProfile.addApp(packageName)
        }

        saveProfiles(currentProfiles)
    }

    /**
     * Removes an app from a profile's assignedApps.
     *
     * @param profileId The profile to remove the app from
     * @param packageName The app's package name to remove
     */
    suspend fun removeAppFromProfile(profileId: String, packageName: String) {
        val profile = getProfileById(profileId) ?: return
        val updatedProfile = profile.removeApp(packageName)
        updateProfile(updatedProfile)
    }

    /**
     * Checks if an app is assigned to any profile.
     *
     * @param packageName The app's package name to check
     * @return True if the app is assigned to some profile
     */

    /**
     * Gets the profile ID that has a specific app assigned.
     *
     * @param packageName The app's package name to look up
     * @return The profile ID, or null if not assigned
     */

    /**
     * Gets all apps assigned across all profiles.
     *
     * @return Map of packageName to profileId
     */
}
