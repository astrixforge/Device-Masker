package com.astrixforge.devicemasker.ui.screens.home

import com.astrixforge.devicemasker.data.models.SpoofGroup

/**
 * UI state for the Home screen.
 *
 * Represents all the state needed to render the home screen, collected from the repository flows by
 * the ViewModel.
 */
data class HomeState(
    val isLoading: Boolean = true,
    val isXposedActive: Boolean = false,
    val isModuleEnabled: Boolean = false,
    val groups: List<SpoofGroup> = emptyList(),
    val selectedGroup: SpoofGroup? = null,
    val enabledAppsCount: Int = 0,
    val maskedIdentifiersCount: Int = 0,
)
