package com.astrixforge.devicemasker.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import com.astrixforge.devicemasker.data.models.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppScopeRepository(private val context: Context) {

    private val packageManager: PackageManager
        get() = context.packageManager

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val cacheMutex = Mutex()
    private var isCacheValid = false

    suspend fun loadApps(forceRefresh: Boolean = false) {
        cacheMutex.withLock {
            if (isCacheValid && !forceRefresh) return
            
            _isLoading.value = true
            
            val apps = withContext(Dispatchers.IO) {
                queryInstalledApps()
            }
            
            _installedApps.value = apps
            isCacheValid = true
            _isLoading.value = false
        }
    }

    suspend fun getInstalledApps(
        includeSystem: Boolean = false,
        refreshCache: Boolean = false,
    ): List<InstalledApp> {
        if (refreshCache || !isCacheValid) {
            loadApps(refreshCache)
        }
        return if (includeSystem) {
            _installedApps.value
        } else {
            _installedApps.value.filter { !it.isSystemApp }
        }
    }

    private fun queryInstalledApps(): List<InstalledApp> {
        val flags = PackageManager.GET_META_DATA

        return packageManager
            .getInstalledApplications(flags)
            .mapNotNull { appInfo -> createInstalledApp(appInfo) }
            .sortedBy { it.label.lowercase() }
    }

    private fun createInstalledApp(appInfo: ApplicationInfo): InstalledApp? {
        return try {
            val label = packageManager.getApplicationLabel(appInfo).toString()
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val versionName = try {
                packageManager.getPackageInfo(appInfo.packageName, 0).versionName ?: ""
            } catch (_: Exception) {
                ""
            }
            
            val iconBitmap = loadIconBitmap(appInfo)

            InstalledApp(
                packageName = appInfo.packageName,
                label = label,
                isSystemApp = isSystem,
                versionName = versionName,
                iconBitmap = iconBitmap,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun loadIconBitmap(appInfo: ApplicationInfo): Bitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(appInfo)
            drawable.toBitmap(width = ICON_SIZE, height = ICON_SIZE)
        } catch (_: Exception) {
            null
        }
    }

    fun invalidateCache() {
        isCacheValid = false
    }

    companion object {
        private const val ICON_SIZE = 96
    }
}
