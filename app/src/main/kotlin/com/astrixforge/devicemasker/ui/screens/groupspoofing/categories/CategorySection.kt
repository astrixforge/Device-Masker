package com.astrixforge.devicemasker.ui.screens.groupspoofing.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier
import com.astrixforge.devicemasker.ui.components.IconCircle
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.components.expressive.animatedRoundedCornerShape
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.CorrelatedSpoofItem
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.IndependentSpoofItem
import com.astrixforge.devicemasker.ui.screens.groupspoofing.model.UIDisplayCategory
import com.astrixforge.devicemasker.ui.theme.AppMotion

/** Category section for group spoof values - organized by correlation groups. */
@Composable
fun CategorySection(
    category: UIDisplayCategory,
    group: SpoofGroup?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    onRegenerateCategory: () -> Unit,
    onRegenerateLocation: () -> Unit,
    onToggle: (SpoofType, Boolean) -> Unit,
    onCarrierChange: (Carrier) -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationAngle by
        animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            animationSpec = AppMotion.FastSpring,
            label = "expandRotation",
        )

    val categoryShape = animatedRoundedCornerShape(targetRadius = if (isExpanded) 24.dp else 16.dp)

    // For correlated categories, check if ANY type is enabled
    val isCategoryEnabled =
        if (category.isCorrelated) {
            category.types.any { group?.isTypeEnabled(it) ?: false }
        } else {
            false // Not used for independent categories
        }

    ExpressiveCard(
        onClick = { onToggleExpand() },
        modifier = modifier.animateContentSize(animationSpec = spring()),
        shape = categoryShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            // Header - simplified, only icon + title + expand arrow
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    IconCircle(
                        icon = category.icon,
                        containerColor = category.color.copy(alpha = 0.15f),
                        iconColor = category.color,
                        iconSize = 22.dp,
                    )
                    Text(
                        text = stringResource(id = category.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription =
                        if (isExpanded) {
                            stringResource(id = R.string.action_collapse)
                        } else {
                            stringResource(id = R.string.action_expand)
                        },
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // For correlated categories: show switch + regenerate at top of expanded area
                    if (category.isCorrelated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExpressiveSwitch(
                                checked = isCategoryEnabled,
                                onCheckedChange = { enabled ->
                                    // Toggle ALL types in this category together
                                    category.types.forEach { type -> onToggle(type, enabled) }
                                },
                            )

                            if (isCategoryEnabled) {
                                CompactExpressiveIconButton(
                                    onClick = onRegenerateCategory,
                                    icon = Icons.Filled.Refresh,
                                    contentDescription =
                                        stringResource(id = R.string.action_regenerate_all),
                                    tint = category.color,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Handle special categories
                    when (category) {
                        UIDisplayCategory.SIM_CARD -> {
                            SIMCardCategoryContent(
                                group = group,
                                onToggle = onToggle,
                                onRegenerate = onRegenerate,
                                onCarrierChange = onCarrierChange,
                                onCopy = onCopy,
                            )
                        }

                        UIDisplayCategory.LOCATION -> {
                            LocationCategoryContent(
                                group = group,
                                onToggle = onToggle,
                                onRegenerate = onRegenerate,
                                onRegenerateLocation = onRegenerateLocation,
                                onCopy = onCopy,
                            )
                        }

                        UIDisplayCategory.DEVICE_HARDWARE -> {
                            DeviceHardwareCategoryContent(
                                group = group,
                                onToggle = onToggle,
                                onRegenerate = onRegenerate,
                                onRegenerateCategory = onRegenerateCategory,
                                onCopy = onCopy,
                            )
                        }

                        else -> {
                            // Standard handling for other categories (Network, Advertising)
                            category.types.forEach { type ->
                                val isGroupEnabled = group?.isTypeEnabled(type) ?: false
                                val rawValue = group?.getValue(type) ?: ""

                                // For DEVICE_PROFILE, show the preset name instead of ID
                                val displayValue =
                                    if (type == SpoofType.DEVICE_PROFILE) {
                                        DeviceProfilePreset.findById(rawValue)?.name ?: rawValue
                                    } else {
                                        rawValue
                                    }

                                if (category.isCorrelated) {
                                    // Correlated: display-only items (no individual controls)
                                    CorrelatedSpoofItem(
                                        type = type,
                                        value = displayValue,
                                        isEnabled = isCategoryEnabled,
                                        onCopy = { onCopy(displayValue) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    // Independent: items with individual controls
                                    IndependentSpoofItem(
                                        type = type,
                                        value = displayValue,
                                        isEnabled = isGroupEnabled,
                                        onToggle = { enabled -> onToggle(type, enabled) },
                                        onRegenerate = { onRegenerate(type) },
                                        onCopy = { onCopy(displayValue) },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
