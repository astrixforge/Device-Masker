package com.astrixforge.devicemasker.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 *
 * Manages theme and debug settings via SettingsDataStore.
 *
 * @param settingsStore The SettingsDataStore for persistence
 */
class SettingsViewModel(
    private val settingsStore: SettingsDataStore
) : ViewModel() {

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

    fun showThemeModeDialog() {
        _state.update { it.copy(showThemeModeDialog = true) }
    }

    fun hideThemeModeDialog() {
        _state.update { it.copy(showThemeModeDialog = false) }
    }
}
