package com.astrixforge.devicemasker.ui.screens.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.service.LogExportResult
import com.astrixforge.devicemasker.service.LogManager
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Settings screen.
 *
 * Manages theme settings and log export functionality.
 *
 * @param application Application for context access
 * @param settingsStore The SettingsDataStore for persistence
 */
class SettingsViewModel(application: Application, private val settingsStore: SettingsDataStore) :
    AndroidViewModel(application) {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        // Collect theme mode
        viewModelScope.launch {
            settingsStore.themeMode.collect { mode -> _state.update { it.copy(themeMode = mode) } }
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
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsStore.setThemeMode(mode) }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setAmoledMode(enabled) }
    }

    fun setDynamicColors(enabled: Boolean) {
        viewModelScope.launch { settingsStore.setDynamicColors(enabled) }
    }

    // ═══════════════════════════════════════════════════════════
    // EXPORT LOGS (In-Memory YLog Data)
    // ═══════════════════════════════════════════════════════════

    /** Exports YLog in-memory data to a custom URI location (file picker). */
    fun exportLogsToUri(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isExportingLogs = true, exportResult = null) }

            val result =
                withContext(Dispatchers.IO) {
                    LogManager.exportLogsToUri(getApplication(), uri).toExportResult()
                }

            _state.update { it.copy(isExportingLogs = false, exportResult = result) }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // UTILITY FUNCTIONS
    // ═══════════════════════════════════════════════════════════

    fun clearExportResult() {
        _state.update { it.copy(exportResult = null) }
    }

    /** Generate filename for Export Logs file picker */
    fun generateLogFileName(): String = LogManager.generateLogFileName()

    private fun LogExportResult.toExportResult(): ExportResult {
        return when (this) {
            is LogExportResult.Success -> ExportResult.Success(filePath, lineCount)
            is LogExportResult.Error -> ExportResult.Error(message)
        }
    }
}
