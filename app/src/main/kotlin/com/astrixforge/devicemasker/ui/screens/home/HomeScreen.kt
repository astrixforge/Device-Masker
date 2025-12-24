package com.astrixforge.devicemasker.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.SpoofGroup
import com.astrixforge.devicemasker.ui.components.IconCircle
import com.astrixforge.devicemasker.ui.components.StatCard
import com.astrixforge.devicemasker.ui.components.expressive.AnimatedLoadingOverlay
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveLoadingIndicatorWithLabel
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.components.expressive.QuickAction
import com.astrixforge.devicemasker.ui.components.expressive.QuickActionGroup
import com.astrixforge.devicemasker.ui.components.expressive.animatedRoundedCornerShape
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive

/**
 * Home screen displaying module status and quick stats.
 *
 * Uses MVVM pattern with HomeViewModel for state management.
 *
 * Shows:
 * - Module active/inactive status with animated indicator
 * - Group dropdown selector
 * - Current group summary with protected apps count
 * - Quick stats (protected apps, masked identifiers)
 * - Quick actions
 *
 * @param viewModel The HomeViewModel for state and actions
 * @param onNavigateToSpoof Callback to navigate to spoof screen
 * @param onRegenerateAll Callback to regenerate all values
 * @param onNavigateToGroup Callback to navigate to group spoofing screen with selected group ID
 * @param modifier Optional modifier
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSpoof: () -> Unit,
    onRegenerateAll: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToGroup: ((String) -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreenContent(
        isXposedActive = state.isXposedActive,
        isModuleEnabled = state.isModuleEnabled,
        groups = state.groups,
        selectedGroup = state.selectedGroup,
        onGroupSelected = { group ->
            viewModel.selectGroup(group.id)
        },
        enabledAppsCount = state.enabledAppsCount,
        maskedIdentifiersCount = state.maskedIdentifiersCount,
        onModuleEnabledChange = { enabled ->
            viewModel.setModuleEnabled(enabled)
        },
        onNavigateToSpoof = {
            // Navigate to the selected group's spoofing screen
            val selectedGroup = state.selectedGroup
            if (onNavigateToGroup != null && selectedGroup != null) {
                onNavigateToGroup(selectedGroup.id)
            } else {
                onNavigateToSpoof()
            }
        },
        onRegenerateAll = {
            viewModel.regenerateAll {
                onRegenerateAll()
            }
        },
        isLoading = state.isLoading,
        modifier = modifier,
    )
}

/** Stateless content for HomeScreen - enables preview and testing. */
@Composable
fun HomeScreenContent(
    isXposedActive: Boolean,
    isModuleEnabled: Boolean,
    groups: List<SpoofGroup>,
    selectedGroup: SpoofGroup?,
    onGroupSelected: (SpoofGroup) -> Unit,
    enabledAppsCount: Int,
    maskedIdentifiersCount: Int,
    onModuleEnabledChange: (Boolean) -> Unit,
    onNavigateToSpoof: () -> Unit,
    onRegenerateAll: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Box(modifier = modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(if (isLoading) 0f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Status Card - Hero Section
            StatusCard(
                isXposedActive = isXposedActive,
                isModuleEnabled = isModuleEnabled,
                onModuleEnabledChange = onModuleEnabledChange,
                modifier = Modifier.fillMaxWidth()
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
                    label = stringResource(id = R.string.home_protected_apps_label),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Outlined.Fingerprint,
                    value = maskedIdentifiersCount.toString(),
                    label = stringResource(id = R.string.home_masked_ids_label),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Group Selector Card with Dropdown
            GroupSelectorCard(
                groups = groups,
                selectedGroup = selectedGroup,
                onGroupSelected = onGroupSelected,
                onClick = onNavigateToSpoof,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions
            QuickActionsSection(
                onNavigateToSpoof = onNavigateToSpoof,
                onRegenerateAll = onRegenerateAll,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        AnimatedLoadingOverlay(isLoading = isLoading) {
            ExpressiveLoadingIndicatorWithLabel(label = "Loading Dashboard...")
        }
    }
}

/** Hero status card showing module activation status. */
@Composable
private fun StatusCard(
    isXposedActive: Boolean,
    isModuleEnabled: Boolean,
    onModuleEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor by
    animateColorAsState(
        targetValue = if (isXposedActive && isModuleEnabled) StatusActive else StatusInactive,
        animationSpec = AppMotion.Effect.Color,
        label = "statusColor",
    )

    val scale by
    animateFloatAsState(
        targetValue = if (isXposedActive && isModuleEnabled) 1f else 0.95f,
        animationSpec = AppMotion.Spatial.Expressive,
        label = "cardScale",
    )

    val statusShape = animatedRoundedCornerShape(
        targetRadius = if (isXposedActive && isModuleEnabled) 28.dp else 16.dp,
        label = "statusCardMorph"
    )

    Card(
        modifier = modifier.scale(scale),
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = statusShape,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
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
                        Modifier
                            .size(80.dp)
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
                    text = stringResource(id = R.string.app_name),
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
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text =
                            when {
                                !isXposedActive -> stringResource(id = R.string.home_module_not_injected)
                                isModuleEnabled -> stringResource(id = R.string.home_protection_active)
                                else -> stringResource(id = R.string.home_protection_disabled)
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
                            text = stringResource(id = R.string.home_enable_instruction),
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
                            text = stringResource(id = R.string.home_module_enabled_label),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        ExpressiveSwitch(
                            checked = isModuleEnabled,
                            onCheckedChange = onModuleEnabledChange,
                        )
                    }
                }
            }
        }
    }
}

/** Group selector card with dropdown menu. */
@Composable
private fun GroupSelectorCard(
    groups: List<SpoofGroup>,
    selectedGroup: SpoofGroup?,
    onGroupSelected: (SpoofGroup) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val rotationAngle by
    animateFloatAsState(
        targetValue = if (dropdownExpanded) 180f else 0f,
        animationSpec = AppMotion.FastSpring,
        label = "dropdownRotation",
    )

    ExpressiveCard(
        onClick = { dropdownExpanded = true },
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconCircle(
                    icon = Icons.Filled.Groups,
                    size = 48.dp,
                    iconSize = 24.dp,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.home_active_group_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedGroup?.name
                                ?: stringResource(id = R.string.home_no_group),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (selectedGroup?.isEnabled == false) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.home_group_disabled_tag),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select Group",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(rotationAngle),
                    )
                    CompactExpressiveIconButton(
                        onClick = onClick,
                        icon = Icons.Outlined.Visibility,
                        contentDescription = "View Group",
                    )
                }
            }

            // Dropdown menu for group selection
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f),
            ) {
                if (groups.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.home_no_groups_available)) },
                        onClick = { dropdownExpanded = false },
                        enabled = false,
                    )
                } else {
                    groups.forEach { group ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column {
                                        Text(
                                            text = group.name,
                                            fontWeight =
                                                if (group.id == selectedGroup?.id)
                                                    FontWeight.Bold
                                                else FontWeight.Normal,
                                        )
                                        Text(
                                            text = pluralStringResource(
                                                id = R.plurals.home_apps_count,
                                                count = group.assignedAppCount(),
                                                group.assignedAppCount()
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (!group.isEnabled) {
                                            Text(
                                                text = stringResource(id = R.string.home_group_disabled_tag).trim(
                                                    '(',
                                                    ')'
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                        if (group.id == selectedGroup?.id) {
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
                                onGroupSelected(group)
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
private fun QuickActionsSection(
    onNavigateToSpoof: () -> Unit,
    onRegenerateAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(id = R.string.home_quick_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Material 3 Expressive Button Group
        QuickActionGroup(
            actions = listOf(
                QuickAction(
                    label = stringResource(id = R.string.home_action_configure),
                    icon = Icons.Outlined.Fingerprint,
                    onClick = onNavigateToSpoof
                ),
                QuickAction(
                    label = stringResource(id = R.string.action_regenerate),
                    icon = Icons.Filled.Refresh,
                    onClick = onRegenerateAll
                )
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun HomeScreenContentPreview() {
    DeviceMaskerTheme {
        HomeScreenContent(
            isXposedActive = true,
            isModuleEnabled = true,
            groups =
                listOf(
                    SpoofGroup.createDefaultGroup(),
                    SpoofGroup.createNew("Work Group"),
                    SpoofGroup.createNew("Gaming"),
                ),
            selectedGroup = SpoofGroup.createDefaultGroup(),
            onGroupSelected = {},
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
            groups = emptyList(),
            selectedGroup = null,
            onGroupSelected = {},
            enabledAppsCount = 0,
            maskedIdentifiersCount = 0,
            onModuleEnabledChange = {},
            onNavigateToSpoof = {},
            onRegenerateAll = {},
        )
    }
}
