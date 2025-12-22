package com.astrixforge.devicemasker.ui.screens.groupspoofing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.common.CorrelationGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Group Spoofing screen.
 *
 * Manages group spoofing state and spoof value operations.
 *
 * @param repository The SpoofRepository for data access
 * @param groupId The ID of the group to display
 */
class GroupSpoofingViewModel(
    private val repository: SpoofRepository,
    private val groupId: String
) : ViewModel() {

    private val _state = MutableStateFlow(GroupSpoofingState())
    val state: StateFlow<GroupSpoofingState> = _state.asStateFlow()

    init {
        // Collect groups
        viewModelScope.launch {
            repository.groups.collect { groups ->
                val group = groups.find { it.id == groupId }
                _state.update { it.copy(
                    groups = groups,
                    group = group,
                    isLoading = false
                ) }
            }
        }

        // Collect installed apps
        viewModelScope.launch {
            repository.appScopeRepository.getInstalledAppsFlow().collect { apps ->
                _state.update { it.copy(installedApps = apps) }
            }
        }
    }

    fun setSelectedTab(tab: Int) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun regenerateValue(type: SpoofType) {
        val group = state.value.group ?: return
        viewModelScope.launch {
            val correlationGroup = type.correlationGroup
            
            // For SIM values: use regenerateSIMValueOnly to keep same carrier
            val newValue = when (correlationGroup) {
                CorrelationGroup.SIM_CARD -> repository.regenerateSIMValueOnly(type)
                else -> {
                    // For non-SIM correlated values, reset cache first
                    if (correlationGroup != CorrelationGroup.NONE) {
                        repository.resetCorrelationGroup(correlationGroup)
                    }
                    repository.generateValue(type)
                }
            }
            
            val updated = group.withValue(type, newValue)
            repository.updateGroup(updated)
        }
    }

    fun regenerateCategory(types: List<SpoofType>, isCorrelated: Boolean) {
        val group = state.value.group ?: return
        viewModelScope.launch {
            // Reset the cache for this correlation group first
            if (isCorrelated) {
                val correlationGroup = types.firstOrNull()?.correlationGroup
                if (correlationGroup != null) {
                    repository.resetCorrelationGroup(correlationGroup)
                }
            }
            
            // Now regenerate all types in this category
            var updatedGroup = group
            types.forEach { type ->
                val newValue = repository.generateValue(type)
                updatedGroup = updatedGroup.withValue(type, newValue)
            }
            repository.updateGroup(updatedGroup)
        }
    }

    fun toggleSpoofType(type: SpoofType, enabled: Boolean) {
        val group = state.value.group ?: return
        viewModelScope.launch {
            val identifier = group.getIdentifier(type) ?: DeviceIdentifier.createDefault(type)
            val updated = group.withIdentifier(identifier.copy(isEnabled = enabled))
            repository.updateGroup(updated)
        }
    }

    fun regenerateLocation() {
        val group = state.value.group ?: return
        viewModelScope.launch {
            repository.regenerateLocationValues(group.id)
        }
    }

    fun updateCarrier(carrier: Carrier) {
        val group = state.value.group ?: return
        viewModelScope.launch {
            repository.updateGroupWithCarrier(group.id, carrier)
        }
    }

    fun addAppToGroup(packageName: String) {
        viewModelScope.launch {
            repository.addAppToGroup(groupId, packageName)
        }
    }

    fun removeAppFromGroup(packageName: String) {
        viewModelScope.launch {
            repository.removeAppFromGroup(groupId, packageName)
        }
    }
}
