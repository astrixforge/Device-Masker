package com.astrixforge.devicemasker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.astrixforge.devicemasker.data.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing installed apps information.
 *
 * @param context Application context for PackageManager access
 */
class AppScopeRepository(private val context: Context) {

    private val packageManager: PackageManager
        get() = context.packageManager

    // Cache for installed apps (expensive to query repeatedly)
    private var cachedApps: List<InstalledApp>? = null

    /** Gets installed apps as a Flow. */
    fun getInstalledAppsFlow(includeSystem: Boolean = false): Flow<List<InstalledApp>> = flow {
        emit(getInstalledApps(includeSystem))
    }

    /** Gets list of installed apps. */
    suspend fun getInstalledApps(
        includeSystem: Boolean = false,
        refreshCache: Boolean = false,
    ): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            if (cachedApps == null || refreshCache) {
                cachedApps = queryInstalledApps(includeSystem)
            }
            cachedApps ?: emptyList()
        }


    /** Queries installed apps from PackageManager. */
    private fun queryInstalledApps(includeSystem: Boolean): List<InstalledApp> {
        val flags = PackageManager.GET_META_DATA

        return packageManager
            .getInstalledApplications(flags)
            .filter { appInfo ->
                includeSystem || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    versionName =
                        try {
                            packageManager.getPackageInfo(appInfo.packageName, 0).versionName ?: ""
                        } catch (e: Exception) {
                            ""
                        },
                )
            }
            .sortedBy { it.label.lowercase() }
    }

}
