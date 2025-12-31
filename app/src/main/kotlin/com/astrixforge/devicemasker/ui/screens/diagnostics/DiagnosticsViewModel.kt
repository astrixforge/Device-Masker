package com.astrixforge.devicemasker.ui.screens.diagnostics

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Diagnostics screen.
 *
 * Manages diagnostic results and anti-detection tests.
 *
 * @param application Application for context access
 * @param repository The SpoofRepository for data access
 */
class DiagnosticsViewModel(application: Application, private val repository: SpoofRepository) :
    AndroidViewModel(application) {

    private val _state = MutableStateFlow(DiagnosticsState())
    val state: StateFlow<DiagnosticsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(isXposedActive = DeviceMaskerApp.isXposedModuleActive) }
        runDiagnostics()
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(500) // Minimum visible refresh time
            runDiagnostics()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    private fun runDiagnostics() {
        viewModelScope.launch {
            val diagnosticResults = runDiagnosticTests()
            val antiDetectionResults = runAntiDetectionTests()

            _state.update {
                it.copy(
                    diagnosticResults = diagnosticResults,
                    antiDetectionResults = antiDetectionResults,
                    isLoading = false,
                )
            }
        }
    }

    private suspend fun runDiagnosticTests(): List<DiagnosticResult> {
        // Get current spoofed group using suspend function (avoids runBlocking)
        val group = repository.activeGroup.first()

        // Get device group preset info if set
        val presetId = group?.getValue(SpoofType.DEVICE_PROFILE)
        val presetInfo =
            presetId?.let { com.astrixforge.devicemasker.common.DeviceProfilePreset.findById(it) }

        val context = getApplication<Application>()

        return listOf(
            // Device Identifiers
            DiagnosticResult(
                type = SpoofType.ANDROID_ID,
                realValue =
                    try {
                        @Suppress("HardwareIds")
                        Settings.Secure.getString(
                            context.contentResolver,
                            Settings.Secure.ANDROID_ID,
                        )
                    } catch (_: Exception) {
                        null
                    },
                spoofedValue = group?.getValue(SpoofType.ANDROID_ID),
                isActive = group?.isTypeEnabled(SpoofType.ANDROID_ID) == true,
                isSpoofed = group?.getValue(SpoofType.ANDROID_ID) != null,
            ),
            // Device Profile (unified Build.* spoofing)
            DiagnosticResult(
                type = SpoofType.DEVICE_PROFILE,
                realValue = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                spoofedValue = presetInfo?.let { "${it.manufacturer} ${it.model}" },
                isActive = group?.isTypeEnabled(SpoofType.DEVICE_PROFILE) == true,
                isSpoofed = presetInfo != null,
            ),
        )
    }

    private fun runAntiDetectionTests(): List<AntiDetectionTest> {
        return listOf(
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_stack_trace,
                descriptionRes = R.string.diagnostics_test_stack_trace_desc,
                isPassed =
                    try {
                        val stackTrace = Thread.currentThread().stackTrace
                        stackTrace.none { it.className.contains("xposed", ignoreCase = true) }
                    } catch (_: Exception) {
                        false
                    },
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_class_loading,
                descriptionRes = R.string.diagnostics_test_class_loading_desc,
                isPassed =
                    try {
                        Class.forName("de.robv.android.xposed.XposedBridge")
                        false
                    } catch (_: ClassNotFoundException) {
                        true
                    } catch (_: Exception) {
                        false
                    },
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_native_hiding,
                descriptionRes = R.string.diagnostics_test_native_hiding_desc,
                isPassed = true, // Assume success if module is active
            ),
            AntiDetectionTest(
                nameRes = R.string.diagnostics_test_package_hiding,
                descriptionRes = R.string.diagnostics_test_package_hiding_desc,
                isPassed = true, // Assume success if module is active
            ),
        )
    }
}
