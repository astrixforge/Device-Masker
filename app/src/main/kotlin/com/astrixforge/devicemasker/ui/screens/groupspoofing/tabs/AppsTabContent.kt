package com.astrixforge.devicemasker.ui.screens.groupspoofing.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.components.EmptyState
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicatorWithLabel

/**
 * Apps tab content for group spoofing screen.
 *
 * Shows a searchable list of installed apps with toggles
 * to assign/unassign them from the current group.
 */
@Composable
fun AppsTabContent(
    group: SpoofGroup?,
    allGroups: List<SpoofGroup>,
    installedApps: List<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }

    // Filter apps - exclude system apps and own app
    // Sort with assigned apps first for better UX
    val filteredApps = remember(installedApps, searchQuery, group) {
        installedApps
            .filter { app ->
                // Always exclude our own app and system apps
                if (app.packageName == BuildConfig.APPLICATION_ID) {
                    return@filter false
                }
                if (app.isSystemApp) {
                    return@filter false
                }
                // Match search query
                searchQuery.isEmpty() ||
                        app.label.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
            // Sort: assigned apps first, then alphabetically
            .sortedWith(
                compareByDescending<InstalledApp> {
                    group?.isAppAssigned(it.packageName) == true
                }.thenBy { it.label.lowercase() }
            )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Search and filter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(stringResource(id = R.string.group_spoofing_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            Text(
                text = pluralStringResource(
                    id = R.plurals.group_spoofing_apps_assigned_stats,
                    count = filteredApps.size,
                    filteredApps.size,
                    group?.assignedAppCount() ?: 0
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // App list with empty state
        if (installedApps.isEmpty()) {
            // Loading state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ExpressiveLoadingIndicatorWithLabel(
                    label = stringResource(id = R.string.group_spoofing_loading_apps)
                )
            }
        } else if (filteredApps.isEmpty()) {
            // Empty state for search results
            EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(id = R.string.group_spoofing_no_apps_found),
                subtitle = stringResource(id = R.string.group_spoofing_adjust_search),
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    count = filteredApps.size,
                    key = { filteredApps[it].packageName }
                ) { index ->
                    val app = filteredApps[index]
                    val isAssignedToThisGroup = group?.isAppAssigned(app.packageName) == true
                    val assignedToOtherGroup = allGroups
                        .filter { it.id != group?.id }
                        .find { it.isAppAssigned(app.packageName) }

                    AppListItem(
                        app = app,
                        isAssigned = isAssignedToThisGroup,
                        assignedToOtherGroupName = assignedToOtherGroup?.name,
                        onToggle = { checked -> onAppToggle(app, checked) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}
