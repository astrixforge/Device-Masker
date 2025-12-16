package com.astrixforge.devicemasker.data.repository

import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing spoof profiles.
 *
 * Handles CRUD operations for profiles with DataStore persistence
 * using Kotlinx Serialization for JSON storage.
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

    /**
     * Flow of all saved profiles.
     */
    val profiles: Flow<List<SpoofProfile>> = dataStore.profilesJson.map { jsonString ->
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

    /**
     * Flow of the currently active profile.
     */
    val activeProfile: Flow<SpoofProfile?> = dataStore.activeProfileId.map { profileId ->
        if (profileId == null) {
            getDefaultProfile()
        } else {
            getProfileById(profileId)
        }
    }

    /**
     * Gets a profile by its ID.
     */
    suspend fun getProfileById(id: String): SpoofProfile? {
        return profiles.first().find { it.id == id }
    }

    /**
     * Gets the default profile (marked as isDefault = true).
     */
    suspend fun getDefaultProfile(): SpoofProfile? {
        return profiles.first().find { it.isDefault }
            ?: profiles.first().firstOrNull()
    }

    /**
     * Creates a new profile.
     */
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

    /**
     * Updates an existing profile.
     */
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

    /**
     * Deletes a profile by ID.
     */
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

    /**
     * Duplicates a profile with a new name.
     */
    suspend fun duplicateProfile(id: String, newName: String): SpoofProfile? {
        val original = getProfileById(id) ?: return null
        val currentProfiles = profiles.first().toMutableList()

        val duplicated = original.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = newName,
            isDefault = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        currentProfiles.add(duplicated)
        saveProfiles(currentProfiles)
        return duplicated
    }

    /**
     * Sets a profile as the default.
     */
    suspend fun setAsDefault(id: String) {
        val currentProfiles = profiles.first().toMutableList()

        currentProfiles.replaceAll { profile ->
            profile.copy(isDefault = profile.id == id)
        }

        saveProfiles(currentProfiles)
    }

    // ═══════════════════════════════════════════════════════════
    // PROFILE VALUE OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Updates a specific spoof value in a profile.
     */
    suspend fun updateProfileValue(profileId: String, type: SpoofType, value: String?) {
        val profile = getProfileById(profileId) ?: return
        val updatedProfile = profile.withValue(type, value)
        updateProfile(updatedProfile)
    }

    /**
     * Toggles a spoof type enabled state in a profile.
     */
    suspend fun toggleProfileSpoofType(profileId: String, type: SpoofType) {
        val profile = getProfileById(profileId) ?: return
        val updatedProfile = profile.withTypeToggled(type)
        updateProfile(updatedProfile)
    }

    /**
     * Gets the spoof value for a type from the active profile.
     */
    suspend fun getActiveValue(type: SpoofType): String? {
        val active = activeProfile.first() ?: return null
        return active.getValue(type)
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    /**
     * Saves profiles to DataStore as JSON.
     */
    private suspend fun saveProfiles(profileList: List<SpoofProfile>) {
        val jsonString = json.encodeToString(profileList)
        dataStore.saveProfilesJson(jsonString)
    }

    /**
     * Exports all profiles as JSON string.
     */
    suspend fun exportProfiles(): String {
        return json.encodeToString(profiles.first())
    }

    /**
     * Imports profiles from JSON string.
     */
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

    /**
     * Gets the active profile synchronously (blocking).
     */
    fun getActiveProfileBlocking(): SpoofProfile? {
        return kotlinx.coroutines.runBlocking {
            activeProfile.first()
        }
    }

    /**
     * Gets a spoof value from the active profile synchronously (blocking).
     */
    fun getActiveValueBlocking(type: SpoofType): String? {
        return kotlinx.coroutines.runBlocking {
            getActiveValue(type)
        }
    }
}
