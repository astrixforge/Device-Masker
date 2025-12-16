package com.astrixforge.devicemasker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Spoof settings screen for configuring individual spoof values.
 *
 * Displays all spoof types organized by category with options to:
 * - View current values
 * - Regenerate values
 * - Edit values
 * - Copy values to clipboard
 * - Enable/disable individual spoof types
 *
 * @param repository The SpoofRepository for data access
 * @param onEditValue Callback when user wants to edit a value
 * @param modifier Optional modifier
 */
@Composable
fun SpoofSettingsScreen(
    repository: SpoofRepository,
    onEditValue: (SpoofType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProfile by repository.activeProfile.collectAsState(initial = null)

    SpoofSettingsContent(
        profile = activeProfile,
        onRegenerate = { type ->
            // Regenerate via repository (will be connected to ViewModel)
        },
        onToggle = { type, enabled ->
            // Toggle via repository
        },
        onEditValue = onEditValue,
        modifier = modifier
    )
}

/**
 * Stateless content for SpoofSettingsScreen.
 */
@Composable
fun SpoofSettingsContent(
    profile: SpoofProfile?,
    onRegenerate: (SpoofType) -> Unit,
    onToggle: (SpoofType, Boolean) -> Unit,
    onEditValue: (SpoofType, String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Spoof Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        // Categories
        SpoofCategory.entries.forEach { category ->
            item(key = category.name) {
                CategorySection(
                    category = category,
                    profile = profile,
                    onRegenerate = onRegenerate,
                    onToggle = onToggle,
                    onEditValue = onEditValue
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Expandable category section.
 */
@Composable
private fun CategorySection(
    category: SpoofCategory,
    profile: SpoofProfile?,
    onRegenerate: (SpoofType) -> Unit,
    onToggle: (SpoofType, Boolean) -> Unit,
    onEditValue: (SpoofType, String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 180f,
        animationSpec = AppMotion.FastSpring,
        label = "expandRotation"
    )

    val categoryIcon = when (category) {
        SpoofCategory.DEVICE -> Icons.Outlined.Devices
        SpoofCategory.NETWORK -> Icons.Outlined.Wifi
        SpoofCategory.ADVERTISING -> Icons.Outlined.TrackChanges
        SpoofCategory.SYSTEM -> Icons.Outlined.Settings
        SpoofCategory.LOCATION -> Icons.Outlined.LocationOn
    }

    val categoryColor = when (category) {
        SpoofCategory.DEVICE -> Color(0xFF00BCD4)
        SpoofCategory.NETWORK -> Color(0xFF4CAF50)
        SpoofCategory.ADVERTISING -> Color(0xFFFF9800)
        SpoofCategory.SYSTEM -> Color(0xFF9C27B0)
        SpoofCategory.LOCATION -> Color(0xFFE91E63)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${SpoofType.byCategory(category).size} identifiers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ExpandLess,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Spoof Type Items
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpoofType.byCategory(category).forEach { type ->
                        val identifier = profile?.getIdentifier(type)
                        SpoofValueItem(
                            type = type,
                            identifier = identifier,
                            onRegenerate = { onRegenerate(type) },
                            onToggle = { enabled -> onToggle(type, enabled) },
                            onEdit = { value -> onEditValue(type, value) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual spoof value item with controls.
 */
@Composable
private fun SpoofValueItem(
    type: SpoofType,
    identifier: DeviceIdentifier?,
    onRegenerate: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val value = identifier?.value ?: "Not set"
    val isEnabled = identifier?.isEnabled ?: true

    val alpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.5f,
        animationSpec = AppMotion.FastSpring,
        label = "itemAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = type.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (identifier?.value != null) maskValue(type, value) else "—",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(value)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onRegenerate,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Regenerate",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { onEdit(value) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Masks sensitive values for display.
 */
private fun maskValue(type: SpoofType, value: String): String {
    if (value.length < 8) return value

    return when (type) {
        SpoofType.IMEI, SpoofType.MEID, SpoofType.IMSI, SpoofType.ICCID -> {
            value.take(4) + "****" + value.takeLast(4)
        }
        SpoofType.PHONE_NUMBER -> {
            value.take(3) + "****" + value.takeLast(4)
        }
        SpoofType.WIFI_MAC, SpoofType.BLUETOOTH_MAC, SpoofType.WIFI_BSSID -> {
            val parts = value.split(":")
            if (parts.size == 6) {
                "${parts[0]}:${parts[1]}:**:**:**:${parts[5]}"
            } else {
                value.take(5) + "****" + value.takeLast(5)
            }
        }
        else -> {
            if (value.length > 16) {
                value.take(6) + "..." + value.takeLast(6)
            } else {
                value
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SpoofSettingsContentPreview() {
    val sampleProfile = SpoofProfile.createDefaultProfile()

    DeviceMaskerTheme {
        SpoofSettingsContent(
            profile = sampleProfile,
            onRegenerate = {},
            onToggle = { _, _ -> },
            onEditValue = { _, _ -> }
        )
    }
}
