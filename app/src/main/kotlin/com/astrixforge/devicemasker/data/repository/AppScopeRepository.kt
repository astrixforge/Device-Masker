package com.astrixforge.devicemasker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.models.AppConfig
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.models.SpoofType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository for managing per-app spoofing configuration.
 *
 * Handles app scope management - which apps have spoofing enabled
 * and what profile/settings they use.
 *
 * @param context Application context for PackageManager access
 * @param dataStore The SpoofDataStore instance for persistence
 */
class AppScopeRepository(
    private val context: Context,
    private val dataStore: SpoofDataStore
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    private val packageManager: PackageManager
        get() = context.packageManager

    // Cache for installed apps (expensive to query repeatedly)
    private var cachedApps: List<InstalledApp>? = null

    // ═══════════════════════════════════════════════════════════
    // APP CONFIG OPERATIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Flow of all app configurations.
     */
    val appConfigs: Flow<Map<String, AppConfig>> = dataStore.appConfigsJson.map { jsonString ->
        if (jsonString.isNullOrEmpty()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<String, AppConfig>>(jsonString)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    /**
     * Gets the configuration for a specific app.
     */
    suspend fun getAppConfig(packageName: String): AppConfig? {
        return appConfigs.first()[packageName]
    }

    /**
     * Checks if spoofing is enabled for an app.
     */
    suspend fun isAppEnabled(packageName: String): Boolean {
        return getAppConfig(packageName)?.isEnabled ?: false
    }

    /**
     * Enables or disables spoofing for an app.
     */
    suspend fun setAppEnabled(packageName: String, enabled: Boolean) {
        val currentConfigs = appConfigs.first().toMutableMap()
        val existing = currentConfigs[packageName]

        if (existing != null) {
            currentConfigs[packageName] = existing.copy(
                isEnabled = enabled,
                lastModified = System.currentTimeMillis()
            )
        } else {
            // Create new config
            val appLabel = getAppLabel(packageName)
            currentConfigs[packageName] = AppConfig.createNew(packageName, appLabel).copy(
                isEnabled = enabled
            )
        }

        saveAppConfigs(currentConfigs)
    }

    /**
     * Assigns a profile to an app.
     */
    suspend fun setAppProfile(packageName: String, profileId: String?) {
        val currentConfigs = appConfigs.first().toMutableMap()
        val existing = currentConfigs[packageName]

        if (existing != null) {
            currentConfigs[packageName] = existing.withProfile(profileId)
        } else {
            val appLabel = getAppLabel(packageName)
            currentConfigs[packageName] = AppConfig.createNew(packageName, appLabel).copy(
                profileId = profileId
            )
        }

        saveAppConfigs(currentConfigs)
    }

    /**
     * Toggles a spoof type for an app.
     */
    suspend fun toggleAppSpoofType(packageName: String, type: SpoofType) {
        val currentConfigs = appConfigs.first().toMutableMap()
        val existing = currentConfigs[packageName] ?: return

        currentConfigs[packageName] = existing.toggleSpoofType(type)
        saveAppConfigs(currentConfigs)
    }

    /**
     * Removes app configuration (reset to defaults).
     */
    suspend fun removeAppConfig(packageName: String) {
        val currentConfigs = appConfigs.first().toMutableMap()
        currentConfigs.remove(packageName)
        saveAppConfigs(currentConfigs)
    }

    // ═══════════════════════════════════════════════════════════
    // INSTALLED APPS
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets installed apps as a Flow that updates when configs change.
     */
    fun getInstalledAppsFlow(): Flow<List<InstalledApp>> {
        return appConfigs.map { configs ->
            // Ensure apps are loaded
            if (cachedApps == null) {
                cachedApps = queryInstalledApps(includeSystem = true)
            }
            cachedApps!!.map { app ->
                app.withConfig(configs[app.packageName])
            }
        }
    }

    /**
     * Gets list of installed apps with their spoofing configuration.
     */
    suspend fun getInstalledApps(
        includeSystem: Boolean = false,
        refreshCache: Boolean = false
    ): List<InstalledApp> = withContext(Dispatchers.IO) {
        if (cachedApps == null || refreshCache) {
            cachedApps = queryInstalledApps(includeSystem)
        }

        val configs = appConfigs.first()

        cachedApps!!.map { app ->
            app.withConfig(configs[app.packageName])
        }
    }

    /**
     * Gets enabled apps only.
     */
    suspend fun getEnabledApps(): List<InstalledApp> {
        return getInstalledApps().filter { it.isSpoofEnabled }
    }

    /**
     * Searches installed apps by name or package.
     */
    suspend fun searchApps(query: String, includeSystem: Boolean = false): List<InstalledApp> {
        val lowercaseQuery = query.lowercase()
        return getInstalledApps(includeSystem).filter { app ->
            app.label.lowercase().contains(lowercaseQuery) ||
                app.packageName.lowercase().contains(lowercaseQuery)
        }
    }

    /**
     * Queries installed apps from PackageManager.
     */
    private fun queryInstalledApps(includeSystem: Boolean): List<InstalledApp> {
        val flags = PackageManager.GET_META_DATA

        return packageManager.getInstalledApplications(flags)
            .filter { appInfo ->
                includeSystem || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    versionName = try {
                        packageManager.getPackageInfo(appInfo.packageName, 0).versionName ?: ""
                    } catch (e: Exception) {
                        ""
                    }
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Gets the app label for a package name.
     */
    private fun getAppLabel(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    // ═══════════════════════════════════════════════════════════
    // PERSISTENCE
    // ═══════════════════════════════════════════════════════════

    /**
     * Saves app configs to DataStore as JSON.
     */
    private suspend fun saveAppConfigs(configs: Map<String, AppConfig>) {
        val jsonString = json.encodeToString(configs)
        dataStore.saveAppConfigsJson(jsonString)
    }

    /**
     * Clears the app cache.
     */
    fun clearCache() {
        cachedApps = null
    }

    // ═══════════════════════════════════════════════════════════
    // BLOCKING FUNCTIONS (For Hook Context)
    // ═══════════════════════════════════════════════════════════

    /**
     * Gets app config synchronously (blocking).
     */
    fun getAppConfigBlocking(packageName: String): AppConfig? {
        return kotlinx.coroutines.runBlocking {
            getAppConfig(packageName)
        }
    }

    /**
     * Checks if app is enabled synchronously (blocking).
     */
    fun isAppEnabledBlocking(packageName: String): Boolean {
        return kotlinx.coroutines.runBlocking {
            isAppEnabled(packageName)
        }
    }
}
