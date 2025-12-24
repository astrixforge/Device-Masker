package com.astrixforge.devicemasker.ui.screens.settings

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.service.ServiceClient
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the Settings screen.
 *
 * Manages theme and debug settings via SettingsDataStore.
 *
 * @param application Application for context access
 * @param settingsStore The SettingsDataStore for persistence
 */
class SettingsViewModel(
    application: Application,
    private val settingsStore: SettingsDataStore
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Collect theme mode
        viewModelScope.launch {
            settingsStore.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }

        // Collect AMOLED mode
        viewModelScope.launch {
            settingsStore.amoledMode.collect { enabled ->
                _state.update { it.copy(amoledMode = enabled) }
            }
        }

        // Collect dynamic colors
        viewModelScope.launch {
            settingsStore.dynamicColors.collect { enabled ->
                _state.update { it.copy(dynamicColors = enabled) }
            }
        }

        // Collect debug logging
        viewModelScope.launch {
            settingsStore.debugLogging.collect { enabled ->
                _state.update { it.copy(debugLogging = enabled) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsStore.setThemeMode(mode)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAmoledMode(enabled)
        }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDynamicColors(enabled)
        }
    }

    fun setDebugLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setDebugLogging(enabled)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // LOG EXPORT FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    /**
     * Exports logs from the Xposed service to a file in Downloads folder.
     */
    fun exportLogs() {
        viewModelScope.launch {
            _state.update { it.copy(isExportingLogs = true, exportResult = null) }

            val result = withContext(Dispatchers.IO) {
                try {
                    // Get logs from service
                    val logs = ServiceClient.getLogs()

                    if (logs.isEmpty()) {
                        return@withContext ExportResult.NoLogs
                    }

                    // Create timestamp for filename
                    val timestamp =
                        SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
                    val fileName = "devicemasker_logs_$timestamp.txt"

                    // Get Downloads directory
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS
                    )
                    val logFile = File(downloadsDir, fileName)

                    // Build log content with header
                    val content = buildString {
                        appendLine("═══════════════════════════════════════════════════════════")
                        appendLine("  Device Masker - Debug Logs")
                        appendLine(
                            "  Exported: ${
                                SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    Locale.US
                                ).format(Date())
                            }"
                        )
                        appendLine("  Total Entries: ${logs.size}")
                        appendLine("═══════════════════════════════════════════════════════════")
                        appendLine()
                        logs.forEach { log ->
                            appendLine(log)
                        }
                        appendLine()
                        appendLine("═══════════════════════════════════════════════════════════")
                        appendLine("  End of Log")
                        appendLine("═══════════════════════════════════════════════════════════")
                    }

                    // Write to file
                    logFile.writeText(content)

                    ExportResult.Success(logFile.absolutePath, logs.size)
                } catch (e: Exception) {
                    ExportResult.Error(e.message ?: "Unknown error")
                }
            }

            _state.update { it.copy(isExportingLogs = false, exportResult = result) }
        }
    }

    /**
     * Clears the export result from state.
     */
    fun clearExportResult() {
        _state.update { it.copy(exportResult = null) }
    }
}
