package com.astrixforge.devicemasker.ui.screens.settings

import com.astrixforge.devicemasker.ui.screens.ThemeMode

/**
 * UI state for the Settings screen.
 *
 * Note: SettingsScreen is already stateless - this state is passed from MainActivity.
 * The ViewModel pattern here is optional since settings state comes from SettingsDataStore.
 */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledMode: Boolean = true,
    val dynamicColors: Boolean = true,
    val debugLogging: Boolean = false,
    val showThemeModeDialog: Boolean = false,
    // Log export state
    val isExportingLogs: Boolean = false,
    val exportResult: ExportResult? = null
)

/**
 * Result of a log export operation.
 */
sealed class ExportResult {
    data class Success(val filePath: String, val logCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
    data object NoLogs : ExportResult()
}

