package com.astrixforge.devicemasker.ui.screens.diagnostics

import com.astrixforge.devicemasker.data.models.SpoofType

/** UI state for the Diagnostics screen. */
data class DiagnosticsState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isXposedActive: Boolean = false,
    val diagnosticResults: List<DiagnosticResult> = emptyList(),
    val antiDetectionResults: List<AntiDetectionTest> = emptyList(),
)

/** Data class representing a diagnostic result. */
data class DiagnosticResult(
    val type: SpoofType,
    val realValue: String?,
    val spoofedValue: String?,
    val isActive: Boolean,
    val isSpoofed: Boolean,
) {
    val status: DiagnosticStatus
        get() =
            when {
                !isActive -> DiagnosticStatus.INACTIVE
                isSpoofed -> DiagnosticStatus.SUCCESS
                else -> DiagnosticStatus.WARNING
            }
}

enum class DiagnosticStatus {
    SUCCESS,
    WARNING,
    INACTIVE,
}

/** Anti-detection test result. */
data class AntiDetectionTest(val nameRes: Int, val descriptionRes: Int, val isPassed: Boolean)
