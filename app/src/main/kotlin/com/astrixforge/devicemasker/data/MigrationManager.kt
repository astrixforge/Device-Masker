package com.astrixforge.devicemasker.data

import android.content.Context
import android.content.SharedPreferences
import com.astrixforge.devicemasker.data.models.SpoofProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages data migrations between app versions.
 *
 * Migration versions:
 * - V1: Initial schema (profiles without assignedApps)
 * - V2: Profile-centric workflow (profiles with assignedApps, independent profiles)
 */
object MigrationManager {

    private const val PREFS_NAME = "migration_prefs"
    private const val KEY_MIGRATION_VERSION = "migration_version"

    // Current schema version - increment when adding new migrations
    const val CURRENT_MIGRATION_VERSION = 2

    // Previous version for fresh installs
    private const val INITIAL_VERSION = 0

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    /**
     * Runs any pending migrations if needed. Should be called on app startup before any data
     * access.
     *
     * @param context Application context
     * @return true if migrations were run, false if already up-to-date
     */
    suspend fun runMigrationsIfNeeded(context: Context): Boolean {
        val prefs = getMigrationPrefs(context)
        val currentVersion = prefs.getInt(KEY_MIGRATION_VERSION, INITIAL_VERSION)

        if (currentVersion >= CURRENT_MIGRATION_VERSION) {
            return false
        }

        // Run migrations in order
        var version = currentVersion

        if (version < 1) {
            // Fresh install or pre-migration app - initialize to V1
            version = 1
        }

        if (version < 2) {
            migrateV1ToV2(context)
            version = 2
        }

        // Save new version
        prefs.edit().putInt(KEY_MIGRATION_VERSION, version).apply()

        return true
    }

    /** Blocking version for hook context. */
    fun runMigrationsIfNeededBlocking(context: Context): Boolean {
        return runBlocking { runMigrationsIfNeeded(context) }
    }

    /**
     * Migration V1 → V2: Profile-centric workflow
     *
     * Changes:
     * 1. Read AppConfig entries and add apps to corresponding profiles' assignedApps
     * 2. Ensure profiles have isEnabled field (defaults to true)
     */
    private suspend fun migrateV1ToV2(context: Context) =
        withContext(Dispatchers.IO) {
            val dataStore = SpoofDataStore(context)

            // 1. Read existing profiles
            val profilesJson = dataStore.profilesJson.first()
            if (profilesJson.isNullOrBlank()) {
                // No profiles exist, create a default profile
                val defaultProfile = SpoofProfile.createDefaultProfile()
                val profiles = listOf(defaultProfile)
                dataStore.saveProfilesJson(json.encodeToString(profiles))
                return@withContext
            }

            // 2. Parse existing profiles
            val existingProfiles =
                try {
                    json.decodeFromString<List<SpoofProfile>>(profilesJson)
                } catch (e: Exception) {
                    // If parsing fails, create fresh
                    val defaultProfile = SpoofProfile.createDefaultProfile()
                    dataStore.saveProfilesJson(json.encodeToString(listOf(defaultProfile)))
                    return@withContext
                }

            // 3. Read AppConfig entries and migrate app assignments
            val appConfigsJson = dataStore.appConfigsJson.first()
            if (!appConfigsJson.isNullOrBlank()) {
                try {
                    // Parse AppConfig entries to get app-profile mappings
                    val appConfigs = json.decodeFromString<List<AppConfigEntry>>(appConfigsJson)

                    // Build map of profileId -> set of packageNames
                    val profileApps = mutableMapOf<String, MutableSet<String>>()
                    for (config in appConfigs) {
                        if (config.profileId != null) {
                            profileApps
                                .getOrPut(config.profileId) { mutableSetOf() }
                                .add(config.packageName)
                        }
                    }

                    // Update profiles with assigned apps
                    val updatedProfiles =
                        existingProfiles.map { profile ->
                            val apps = profileApps[profile.id] ?: emptySet()
                            if (apps.isNotEmpty()) {
                                profile.copy(assignedApps = apps)
                            } else {
                                profile
                            }
                        }

                    // Save updated profiles
                    dataStore.saveProfilesJson(json.encodeToString(updatedProfiles))
                } catch (e: Exception) {
                    // Migration failed for app configs, profiles remain unchanged
                    // This is not fatal - users can re-assign apps manually
                }
            }

            // 4. Ensure at least one default profile exists
            val finalProfilesJson = dataStore.profilesJson.first()
            if (finalProfilesJson.isNullOrBlank()) {
                val defaultProfile = SpoofProfile.createDefaultProfile()
                dataStore.saveProfilesJson(json.encodeToString(listOf(defaultProfile)))
            } else {
                val profiles =
                    try {
                        json.decodeFromString<List<SpoofProfile>>(finalProfilesJson)
                    } catch (e: Exception) {
                        emptyList()
                    }

                if (profiles.isEmpty() || profiles.none { it.isDefault }) {
                    // No default profile, make first one default or create new
                    val updated =
                        if (profiles.isEmpty()) {
                            listOf(SpoofProfile.createDefaultProfile())
                        } else {
                            listOf(profiles.first().copy(isDefault = true)) + profiles.drop(1)
                        }
                    dataStore.saveProfilesJson(json.encodeToString(updated))
                }
            }
        }

    private fun getMigrationPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Helper class for parsing old AppConfig entries during migration. */
    @kotlinx.serialization.Serializable
    private data class AppConfigEntry(
        val packageName: String,
        val profileId: String? = null,
        val isEnabled: Boolean = true,
    )
}
