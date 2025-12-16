package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.components.AppListItem
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import kotlinx.coroutines.launch

/** Filter options for the app list. */
enum class AppFilter {
    ALL,
    ENABLED_ONLY
}

/**
 * Screen for selecting apps to enable spoofing.
 *
 * Features:
 * - Searchable app list
 * - Filter by user/system apps
 * - Select all / clear all actions
 * - Shows spoofing status per app
 *
 * @param repository The SpoofRepository for data access
 * @param onAppClick Callback when an app is clicked for details
 * @param modifier Optional modifier
 */
@Composable
fun AppSelectionScreen(
        repository: SpoofRepository,
        onAppClick: (InstalledApp) -> Unit,
        modifier: Modifier = Modifier
) {
    val apps by repository.getInstalledApps().collectAsState(initial = emptyList())
    val enabledPackages by repository.getEnabledPackages().collectAsState(initial = emptySet())
    val isLoading = apps.isEmpty()
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(AppFilter.ALL) }

    // Filter apps: exclude system apps and DeviceMasker, then apply search/filter
    val filteredApps =
            remember(apps, searchQuery, activeFilter, enabledPackages) {
                apps.filter { app ->
                    // Exclude system apps (this module only spoofs user apps)
                    if (app.isSystemApp) return@filter false

                    // Exclude DeviceMasker itself
                    if (app.packageName == "com.astrixforge.devicemasker") return@filter false

                    val matchesSearch =
                            if (searchQuery.isBlank()) {
                                true
                            } else {
                                app.label.contains(searchQuery, ignoreCase = true) ||
                                        app.packageName.contains(searchQuery, ignoreCase = true)
                            }

                    val matchesFilter =
                            when (activeFilter) {
                                AppFilter.ALL -> true
                                AppFilter.ENABLED_ONLY -> app.packageName in enabledPackages
                            }

                    matchesSearch && matchesFilter
                }
            }

    AppSelectionContent(
            apps = filteredApps,
            enabledPackages = enabledPackages,
            isLoading = isLoading,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            activeFilter = activeFilter,
            onFilterChange = { activeFilter = it },
            onAppSelectionChange = { app, isSelected ->
                scope.launch { repository.setAppEnabled(app.packageName, isSelected) }
            },
            onAppClick = onAppClick,
            onSelectAll = {
                scope.launch {
                    filteredApps.forEach { app -> repository.setAppEnabled(app.packageName, true) }
                }
            },
            onClearAll = {
                scope.launch {
                    filteredApps.forEach { app -> repository.setAppEnabled(app.packageName, false) }
                }
            },
            modifier = modifier
    )
}

/** Stateless content for AppSelectionScreen. */
@Composable
fun AppSelectionContent(
        apps: List<InstalledApp>,
        enabledPackages: Set<String>,
        isLoading: Boolean,
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        activeFilter: AppFilter,
        onFilterChange: (AppFilter) -> Unit,
        onAppSelectionChange: (InstalledApp, Boolean) -> Unit,
        onAppClick: (InstalledApp) -> Unit,
        onSelectAll: () -> Unit,
        onClearAll: () -> Unit,
        modifier: Modifier = Modifier
) {
    LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                    ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with title and actions
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Select Apps",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Row {
                    IconButton(onClick = onSelectAll) {
                        Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onClearAll) {
                        Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear All",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                                visible = searchQuery.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut()
                        ) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
            )
        }

        // Filter Chips
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                        label = "All Apps",
                        icon = Icons.Outlined.Apps,
                        selected = activeFilter == AppFilter.ALL,
                        onClick = { onFilterChange(AppFilter.ALL) }
                )
                FilterChipItem(
                        label = "Enabled",
                        icon = Icons.Default.CheckCircle,
                        selected = activeFilter == AppFilter.ENABLED_ONLY,
                        onClick = { onFilterChange(AppFilter.ENABLED_ONLY) }
                )
            }
        }

        // Stats Row
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                        text = "${apps.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                        text = "${enabledPackages.size} enabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Loading State
        if (isLoading) {
            item {
                Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "Loading apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else if (apps.isEmpty()) {
            // Empty State
            item {
                Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                imageVector = Icons.Outlined.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                                text = "No apps found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { onSearchQueryChange("") }) {
                                Text("Clear search")
                            }
                        }
                    }
                }
            }
        } else {
            // App List
            items(items = apps, key = { it.packageName }) { app ->
                val isSelected = app.packageName in enabledPackages
                AppListItem(
                        app = app,
                        isSelected = isSelected,
                        onSelectionChange = { selected -> onAppSelectionChange(app, selected) },
                        onClick = { onAppClick(app) }
                )
            }
        }
    }
}

/** Filter chip for app filtering. */
@Composable
private fun FilterChipItem(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit
) {
    FilterChip(
            selected = selected,
            onClick = onClick,
            label = { Text(label) },
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            }
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppSelectionContentPreview() {
    DeviceMaskerTheme {
        AppSelectionContent(
                apps =
                        listOf(
                                InstalledApp("com.example.app1", "Example App 1"),
                                InstalledApp("com.example.app2", "Example App 2"),
                                InstalledApp(
                                        "com.android.settings",
                                        "Settings",
                                        isSystemApp = true
                                ),
                        ),
                enabledPackages = setOf("com.example.app1"),
                isLoading = false,
                searchQuery = "",
                onSearchQueryChange = {},
                activeFilter = AppFilter.ALL,
                onFilterChange = {},
                onAppSelectionChange = { _, _ -> },
                onAppClick = {},
                onSelectAll = {},
                onClearAll = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppSelectionEmptyPreview() {
    DeviceMaskerTheme {
        AppSelectionContent(
                apps = emptyList(),
                enabledPackages = emptySet(),
                isLoading = false,
                searchQuery = "nonexistent",
                onSearchQueryChange = {},
                activeFilter = AppFilter.ALL,
                onFilterChange = {},
                onAppSelectionChange = { _, _ -> },
                onAppClick = {},
                onSelectAll = {},
                onClearAll = {}
        )
    }
}
