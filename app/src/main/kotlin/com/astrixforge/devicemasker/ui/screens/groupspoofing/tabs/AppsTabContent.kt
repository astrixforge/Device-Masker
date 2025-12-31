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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(FlowPreview::class)
@Composable
fun AppsTabContent(
    group: SpoofGroup?,
    allGroups: List<SpoofGroup>,
    installedApps: List<InstalledApp>,
    onAppToggle: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { debouncedQuery = it }
    }

    val filteredApps by remember(installedApps, group, debouncedQuery) {
        derivedStateOf {
            val query = debouncedQuery.lowercase()
            installedApps
                .asSequence()
                .filter { app ->
                    if (app.packageName == BuildConfig.APPLICATION_ID) return@filter false
                    if (app.isSystemApp) return@filter false
                    query.isEmpty() ||
                        app.label.lowercase().contains(query) ||
                        app.packageName.lowercase().contains(query)
                }
                .sortedWith(
                    compareByDescending<InstalledApp> {
                        group?.isAppAssigned(it.packageName) == true
                    }.thenBy { it.label.lowercase() }
                )
                .toList()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                text =
                    pluralStringResource(
                        id = R.plurals.group_spoofing_apps_assigned_stats,
                        count = filteredApps.size,
                        filteredApps.size,
                        group?.assignedAppCount() ?: 0,
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        when {
            installedApps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveLoadingIndicatorWithLabel(
                        label = stringResource(id = R.string.group_spoofing_loading_apps)
                    )
                }
            }
            filteredApps.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Search,
                    title = stringResource(id = R.string.group_spoofing_no_apps_found),
                    subtitle = stringResource(id = R.string.group_spoofing_adjust_search),
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(
                        items = filteredApps,
                        key = { it.packageName },
                        contentType = { "app_item" },
                    ) { app ->
                        val isAssignedToThisGroup = group?.isAppAssigned(app.packageName) == true
                        val assignedToOtherGroup =
                            allGroups
                                .firstOrNull { it.id != group?.id && it.isAppAssigned(app.packageName) }

                        AppListItem(
                            app = app,
                            isAssigned = isAssignedToThisGroup,
                            assignedToOtherGroupName = assignedToOtherGroup?.name,
                            onToggle = { checked -> onAppToggle(app, checked) },
                            modifier = Modifier.fillMaxWidth().animateItem(),
                        )
                    }

                    item(key = "bottom_spacer", contentType = "spacer") {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
