package com.astrixforge.devicemasker.ui.screens.settings

import com.astrixforge.devicemasker.ui.screens.ThemeMode

/** UI state for the Settings screen. */
data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledMode: Boolean = true,
    val dynamicColors: Boolean = true,
    val showThemeModeDialog: Boolean = false,
    // Log export state
    val isExportingLogs: Boolean = false,
    val exportResult: ExportResult? = null,
)

/** Result of a log export operation. */
sealed class ExportResult {
    data class Success(val filePath: String, val lineCount: Int) : ExportResult()

    data class Error(val message: String) : ExportResult()

    data object NoLogs : ExportResult()
}
