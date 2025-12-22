package com.astrixforge.devicemasker.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * Manages the UI state by collecting from repository flows and
 * provides action methods for UI events.
 *
 * @param repository The SpoofRepository for data access
 */
class HomeViewModel(
    private val repository: SpoofRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        // Set initial Xposed status
        _state.update { it.copy(isXposedActive = DeviceMaskerApp.isXposedModuleActive) }

        // Collect groups
        viewModelScope.launch {
            repository.groups.collect { groups ->
                _state.update { currentState ->
                    val selectedGroup = currentState.selectedGroup
                        ?: groups.find { it.isDefault }
                        ?: groups.firstOrNull()
                    
                    currentState.copy(
                        groups = groups,
                        selectedGroup = selectedGroup,
                        maskedIdentifiersCount = selectedGroup?.enabledCount() ?: 0,
                        enabledAppsCount = if (selectedGroup?.isEnabled == true) {
                            selectedGroup.assignedAppCount()
                        } else 0,
                        isLoading = false
                    )
                }
            }
        }

        // Collect dashboard state
        viewModelScope.launch {
            repository.dashboardState.collect { dashboard ->
                _state.update { currentState ->
                    // If we have an active group from dashboard and no selection yet, use it
                    val selectedGroup = currentState.selectedGroup
                        ?: dashboard.activeGroup
                    
                    currentState.copy(
                        isModuleEnabled = dashboard.isModuleEnabled,
                        selectedGroup = selectedGroup,
                        enabledAppsCount = if (selectedGroup?.isEnabled == true) {
                            selectedGroup.assignedAppCount()
                        } else 0,
                        maskedIdentifiersCount = selectedGroup?.enabledCount() ?: 0
                    )
                }
            }
        }
    }

    /**
     * Toggle module enabled/disabled state.
     */
    fun setModuleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setModuleEnabled(enabled)
        }
    }

    /**
     * Select a group from the dropdown.
     */
    fun selectGroup(groupId: String) {
        viewModelScope.launch {
            repository.setActiveGroup(groupId)
            // Update local state immediately for responsiveness
            _state.update { currentState ->
                val group = currentState.groups.find { it.id == groupId }
                currentState.copy(
                    selectedGroup = group,
                    enabledAppsCount = if (group?.isEnabled == true) {
                        group.assignedAppCount()
                    } else 0,
                    maskedIdentifiersCount = group?.enabledCount() ?: 0
                )
            }
        }
    }

    /**
     * Regenerate all values for the selected group.
     * 
     * @param onComplete Callback invoked after regeneration starts
     */
    fun regenerateAll(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            state.value.selectedGroup?.let { group ->
                repository.setActiveGroup(group.id)
            }
            onComplete()
        }
    }
}
