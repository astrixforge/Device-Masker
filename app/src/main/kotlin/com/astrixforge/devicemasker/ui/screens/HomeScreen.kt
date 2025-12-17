package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive
import kotlinx.coroutines.launch

/**
 * Home screen displaying module status and quick stats.
 *
 * Shows:
 * - Module active/inactive status with animated indicator
 * - Profile dropdown selector
 * - Current profile summary with protected apps count
 * - Quick stats (protected apps, masked identifiers)
 * - Quick actions
 *
 * @param repository The SpoofRepository for data access
 * @param onNavigateToProfile Callback to navigate to profile detail screen with selected profile ID
 * @param modifier Optional modifier
 */
@Composable
fun HomeScreen(
    repository: SpoofRepository,
    onNavigateToSpoof: () -> Unit,
    onRegenerateAll: () -> Unit,
    onNavigateToProfile: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val profiles by repository.profiles.collectAsState(initial = emptyList())
    val dashboardState by
        repository.dashboardState.collectAsState(
            initial =
                SpoofRepository.DashboardState(
                    isModuleEnabled = false,
                    activeProfile = null,
                    enabledAppCount = 0,
                    profileCount = 0,
                )
        )
    val scope = rememberCoroutineScope()

    // Track selected profile for the dropdown
    var selectedProfileId by
        remember(dashboardState.activeProfile) { mutableStateOf(dashboardState.activeProfile?.id) }
    val selectedProfile =
        profiles.find { it.id == selectedProfileId } ?: dashboardState.activeProfile

    // Calculate protected apps count based on selected profile
    val protectedAppsCount =
        if (selectedProfile?.isEnabled == true) {
            selectedProfile.assignedAppCount()
        } else {
            0
        }

    HomeScreenContent(
        isXposedActive = DeviceMaskerApp.isXposedModuleActive,
        isModuleEnabled = dashboardState.isModuleEnabled,
        profiles = profiles,
        selectedProfile = selectedProfile,
        onProfileSelected = { profile ->
            selectedProfileId = profile.id
            scope.launch { repository.setActiveProfile(profile.id) }
        },
        enabledAppsCount = protectedAppsCount,
        maskedIdentifiersCount = selectedProfile?.enabledCount() ?: 0,
        onModuleEnabledChange = { enabled ->
            scope.launch { repository.setModuleEnabled(enabled) }
        },
        onNavigateToSpoof = {
            // Navigate to the selected profile's detail screen
            if (onNavigateToProfile != null && selectedProfile != null) {
                onNavigateToProfile(selectedProfile.id)
            } else {
                onNavigateToSpoof()
            }
        },
        onRegenerateAll = {
            // Regenerate values for selected profile only
            scope.launch {
                selectedProfile?.let { profile -> repository.setActiveProfile(profile.id) }
                // Then regenerate using existing method
                onRegenerateAll()
            }
        },
        modifier = modifier,
    )
}

/** Stateless content for HomeScreen - enables preview and testing. */
@Composable
fun HomeScreenContent(
    isXposedActive: Boolean,
    isModuleEnabled: Boolean,
    profiles: List<SpoofProfile>,
    selectedProfile: SpoofProfile?,
    onProfileSelected: (SpoofProfile) -> Unit,
    enabledAppsCount: Int,
    maskedIdentifiersCount: Int,
    onModuleEnabledChange: (Boolean) -> Unit,
    onNavigateToSpoof: () -> Unit,
    onRegenerateAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status Card - Hero Section
        StatusCard(
            isXposedActive = isXposedActive,
            isModuleEnabled = isModuleEnabled,
            onModuleEnabledChange = onModuleEnabledChange,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = Icons.Outlined.Apps,
                value = enabledAppsCount.toString(),
                label = "Protected Apps",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Outlined.Fingerprint,
                value = maskedIdentifiersCount.toString(),
                label = "Masked IDs",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Selector Card with Dropdown
        ProfileSelectorCard(
            profiles = profiles,
            selectedProfile = selectedProfile,
            onProfileSelected = onProfileSelected,
            onClick = onNavigateToSpoof,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions
        QuickActionsSection(
            onNavigateToSpoof = onNavigateToSpoof,
            onRegenerateAll = onRegenerateAll,
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Hero status card showing module activation status. */
@Composable
private fun StatusCard(
    isXposedActive: Boolean,
    isModuleEnabled: Boolean,
    onModuleEnabledChange: (Boolean) -> Unit,
) {
    val statusColor by
        animateColorAsState(
            targetValue = if (isXposedActive && isModuleEnabled) StatusActive else StatusInactive,
            animationSpec = spring(),
            label = "statusColor",
        )

    val scale by
        animateFloatAsState(
            targetValue = if (isXposedActive && isModuleEnabled) 1f else 0.95f,
            animationSpec = AppMotion.BouncySpring,
            label = "cardScale",
        )

    Card(
        modifier = Modifier.fillMaxWidth().scale(scale),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(statusColor.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
                    .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Shield Icon with Status
                Box(
                    modifier =
                        Modifier.size(80.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = statusColor,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Status Text
                Text(
                    text = "DeviceMasker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when {
                                !isXposedActive -> "Module Not Injected"
                                isModuleEnabled -> "Protection Active"
                                else -> "Protection Disabled"
                            },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // LSPosed Status Indicator
                if (!isXposedActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedCard(
                        colors =
                            CardDefaults.outlinedCardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            )
                    ) {
                        Text(
                            text = "⚠️ Enable in LSPosed Manager and reboot",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }

                // Enable/Disable Toggle
                if (isXposedActive) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Module Enabled",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Switch(
                            checked = isModuleEnabled,
                            onCheckedChange = onModuleEnabledChange,
                            thumbContent = {
                                Icon(
                                    imageVector =
                                        if (isModuleEnabled) Icons.Filled.Check
                                        else Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/** Small stat card for displaying counts. */
@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Profile selector card with dropdown menu. */
@Composable
private fun ProfileSelectorCard(
    profiles: List<SpoofProfile>,
    selectedProfile: SpoofProfile?,
    onProfileSelected: (SpoofProfile) -> Unit,
    onClick: () -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val rotationAngle by
        animateFloatAsState(
            targetValue = if (dropdownExpanded) 180f else 0f,
            animationSpec = AppMotion.FastSpring,
            label = "dropdownRotation",
        )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column {
            Row(
                modifier =
                    Modifier.fillMaxWidth().clickable { dropdownExpanded = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier.size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Active Profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedProfile?.name ?: "No Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedProfile?.isEnabled == false) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(Disabled)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (selectedProfile != null) {
                        Text(
                            text = "${selectedProfile.assignedAppCount()} apps assigned",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rotationAngle),
                    )
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = "View Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { onClick() },
                    )
                }
            }

            // Dropdown menu for profile selection
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                if (profiles.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No profiles available") },
                        onClick = { dropdownExpanded = false },
                        enabled = false,
                    )
                } else {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        Text(
                                            text = profile.name,
                                            fontWeight =
                                                if (profile.id == selectedProfile?.id)
                                                    FontWeight.Bold
                                                else FontWeight.Normal,
                                        )
                                        Text(
                                            text = "${profile.assignedAppCount()} apps",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!profile.isEnabled) {
                                            Text(
                                                text = "Disabled",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                        if (profile.id == selectedProfile?.id) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                onProfileSelected(profile)
                                dropdownExpanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Quick actions section. */
@Composable
private fun QuickActionsSection(onNavigateToSpoof: () -> Unit, onRegenerateAll: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(onClick = onNavigateToSpoof, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configure")
            }

            FilledTonalButton(onClick = onRegenerateAll, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Regenerate All")
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HomeScreenContentPreview() {
    DeviceMaskerTheme {
        HomeScreenContent(
            isXposedActive = true,
            isModuleEnabled = true,
            profiles =
                listOf(
                    SpoofProfile.createDefaultProfile(),
                    SpoofProfile.createNew("Work Profile"),
                    SpoofProfile.createNew("Gaming"),
                ),
            selectedProfile = SpoofProfile.createDefaultProfile(),
            onProfileSelected = {},
            enabledAppsCount = 12,
            maskedIdentifiersCount = 24,
            onModuleEnabledChange = {},
            onNavigateToSpoof = {},
            onRegenerateAll = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HomeScreenInactivePreview() {
    DeviceMaskerTheme {
        HomeScreenContent(
            isXposedActive = false,
            isModuleEnabled = false,
            profiles = emptyList(),
            selectedProfile = null,
            onProfileSelected = {},
            enabledAppsCount = 0,
            maskedIdentifiersCount = 0,
            onModuleEnabledChange = {},
            onNavigateToSpoof = {},
            onRegenerateAll = {},
        )
    }
}
