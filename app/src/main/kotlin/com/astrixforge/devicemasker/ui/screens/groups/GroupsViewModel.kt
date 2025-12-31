package com.astrixforge.devicemasker.ui.screens.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.astrixforge.devicemasker.data.models.SpoofGroup
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Groups list screen.
 *
 * Manages group list state and CRUD operations.
 *
 * @param repository The SpoofRepository for data access
 */
class GroupsViewModel(private val repository: SpoofRepository) : ViewModel() {

    private val _state = MutableStateFlow(GroupsState())
    val state: StateFlow<GroupsState> = _state.asStateFlow()

    init {
        // Collect groups
        viewModelScope.launch {
            repository.getAllGroups().collect { groups ->
                _state.update { it.copy(groups = groups, isLoading = false) }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            delay(1000) // Simulate refresh
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun hideCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }

    fun hideEditDialog() {
        _state.update { it.copy(showEditDialog = null) }
    }

    fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = null) }
    }

    fun createGroup(name: String, description: String) {
        viewModelScope.launch { repository.createGroup(name, description) }
        hideCreateDialog()
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch { repository.deleteGroup(groupId) }
        hideDeleteDialog()
    }

    fun setDefaultGroup(groupId: String) {
        viewModelScope.launch { repository.setDefaultGroup(groupId) }
    }

    fun setGroupEnabled(groupId: String, enabled: Boolean) {
        viewModelScope.launch { repository.setGroupEnabled(groupId, enabled) }
    }

    /**
     * Exports groups and returns data via callback. The screen handles file writing via
     * ActivityResultContract.
     */
    fun exportGroups(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val jsonData = repository.exportGroups()
            onResult(jsonData)
        }
    }

    /**
     * Imports groups from JSON string directly. The screen handles file reading via
     * ActivityResultContract.
     */
    fun importGroups(jsonData: String) {
        viewModelScope.launch { repository.importGroups(jsonData) }
    }

    /** Updates an existing group. */
    fun updateGroup(group: SpoofGroup) {
        viewModelScope.launch { repository.updateGroup(group) }
    }
}
