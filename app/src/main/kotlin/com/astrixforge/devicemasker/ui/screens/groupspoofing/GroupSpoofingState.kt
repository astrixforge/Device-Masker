package com.astrixforge.devicemasker.ui.screens.groupspoofing

import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.models.InstalledApp

/**
 * UI state for the Group Spoofing screen.
 */
data class GroupSpoofingState(
    val isLoading: Boolean = true,
    val group: SpoofGroup? = null,
    val groups: List<SpoofGroup> = emptyList(),
    val installedApps: List<InstalledApp> = emptyList(),
    val selectedTab: Int = 0
)
