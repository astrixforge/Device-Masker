package com.astrixforge.devicemasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.SpoofProfile
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card displaying a spoof profile in a list.
 *
 * Shows profile name, summary, creation date, app count, enable/disable switch, and action buttons.
 * Highlights the default profile with a star indicator.
 *
 * @param profile The profile to display
 * @param isEnabled Whether spoofing is enabled for this profile
 * @param appCount Number of apps assigned to this profile
 * @param onClick Callback when the card is clicked
 * @param onEdit Callback when edit is requested
 * @param onDelete Callback when delete is requested
 * @param onSetDefault Callback to set as default profile
 * @param onEnableChange Callback when enable/disable switch is toggled
 * @param modifier Optional modifier
 */
@Composable
fun ProfileCard(
        profile: SpoofProfile,
        isEnabled: Boolean = profile.isEnabled,
        appCount: Int = profile.assignedAppCount(),
        onClick: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onSetDefault: () -> Unit,
        onEnableChange: (Boolean) -> Unit = {},
        modifier: Modifier = Modifier,
) {
    val contentAlpha = if (isEnabled) 1f else 0.5f

    ElevatedCard(
            modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp).alpha(contentAlpha)) {
            // Profile Info Row with Switch
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                ) {
                    // Profile Icon
                    Box(
                            modifier =
                                    Modifier.size(40.dp)
                                            .background(
                                                    color =
                                                            MaterialTheme.colorScheme.primary.copy(
                                                                    alpha = 0.15f
                                                            ),
                                                    shape = CircleShape,
                                            ),
                            contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                            )

                            if (profile.isDefault) {
                                Spacer(modifier = Modifier.width(8.dp))
                                DefaultBadge()
                            }
                        }

                        if (profile.description.isNotBlank()) {
                            Text(
                                    text = profile.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                // Enable/Disable Switch
                Switch(
                        checked = isEnabled,
                        onCheckedChange = onEnableChange,
                        colors =
                                SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor =
                                                MaterialTheme.colorScheme.primaryContainer,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor =
                                                MaterialTheme.colorScheme.surfaceVariant,
                                ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                            text =
                                    if (appCount > 0)
                                            "$appCount app${if (appCount != 1) "s" else ""}"
                                    else "No apps",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                    if (appCount > 0) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                            text = "Created ${formatDate(profile.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                // Action Buttons
                Row {
                    if (!profile.isDefault) {
                        IconButton(onClick = onSetDefault, modifier = Modifier.size(36.dp)) {
                            Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Set as Default",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                        )
                    }

                    if (!profile.isDefault) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Badge indicating the default profile. */
@Composable
private fun DefaultBadge(modifier: Modifier = Modifier) {
    Box(
            modifier =
                    modifier.background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small,
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
                text = "Default",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** Compact profile card for selection lists. */
@Composable
fun CompactProfileCard(
        profile: SpoofProfile,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
) {
    ElevatedCard(
            modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(
                                                alpha = 0.3f
                                        )
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    }
                    ),
            shape = MaterialTheme.shapes.medium,
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = profile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                        text = profile.summary(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (profile.isDefault) {
                DefaultBadge()
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = StatusActive,
                        modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** Formats a timestamp for display. */
private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileCardDefaultPreview() {
    DeviceMaskerTheme {
        ProfileCard(
                profile = SpoofProfile.createDefaultProfile(),
                onClick = {},
                onEdit = {},
                onDelete = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ProfileCardCustomPreview() {
    DeviceMaskerTheme {
        ProfileCard(
                profile = SpoofProfile.createNew("Samsung Galaxy S24"),
                onClick = {},
                onEdit = {},
                onDelete = {},
                onSetDefault = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CompactProfileCardPreview() {
    DeviceMaskerTheme {
        CompactProfileCard(
                profile = SpoofProfile.createDefaultProfile(),
                isSelected = true,
                onClick = {},
        )
    }
}
