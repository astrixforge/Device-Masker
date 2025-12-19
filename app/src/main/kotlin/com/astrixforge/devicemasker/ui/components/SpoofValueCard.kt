package com.astrixforge.devicemasker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.data.models.DeviceIdentifier
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveIconButton
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Card displaying a single spoof value with controls.
 *
 * Shows the identifier label, current value, and action buttons for regenerating, editing, and
 * copying the value.
 *
 * @param identifier The device identifier to display
 * @param onRegenerate Callback to regenerate the value
 * @param onEdit Callback to edit the value
 * @param onCopy Callback to copy the value to clipboard
 * @param onToggle Callback to enable/disable this spoof
 * @param modifier Optional modifier
 * @param showActions Whether to show action buttons
 * @param maskValue Whether to mask the displayed value
 */
@Composable
fun SpoofValueCard(
    identifier: DeviceIdentifier,
    onRegenerate: () -> Unit,
    onEdit: (String) -> Unit,
    onCopy: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    maskValue: Boolean = false,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = identifier.type.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = identifier.type.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                ExpressiveSwitch(checked = identifier.isEnabled, onCheckedChange = onToggle)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Value Display
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = MaterialTheme.shapes.small,
                        )
                        .padding(12.dp)
            ) {
                Text(
                    text =
                        if (maskValue) {
                            maskValueString(identifier.type, identifier.value)
                        } else {
                            identifier.value ?: "Not set"
                        },
                    style =
                        MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color =
                        if (identifier.value != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Actions Row
            if (showActions) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    CompactExpressiveIconButton(
                        onClick = onRegenerate,
                        icon = Icons.Default.Refresh,
                        contentDescription = "Regenerate",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    CompactExpressiveIconButton(
                        onClick = { onEdit(identifier.value ?: "") },
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit",
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    CompactExpressiveIconButton(
                        onClick = onCopy,
                        icon = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                    )
                }
            }
        }
    }
}

/** Compact version of SpoofValueCard for dense lists. */
@Composable
fun CompactSpoofValueCard(
    label: String,
    value: String?,
    onRegenerate: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value ?: "Not set",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color =
                    if (value != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        ExpressiveIconButton(
            onClick = onRegenerate,
            icon = Icons.Default.Refresh,
            contentDescription = "Regenerate",
            enabled = enabled,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}



/** Masks a value for display based on type. */
private fun maskValueString(type: SpoofType, value: String?): String {
    if (value == null) return "Not set"

    return when (type) {
        SpoofType.IMEI,
        SpoofType.IMSI,
        SpoofType.MEID -> {
            if (value.length > 6) {
                value.take(3) + "***" + value.takeLast(3)
            } else {
                "***"
            }
        }
        SpoofType.SERIAL -> {
            if (value.length > 4) {
                "***" + value.takeLast(4)
            } else {
                "***"
            }
        }
        SpoofType.ANDROID_ID,
        SpoofType.GSF_ID,
        SpoofType.ADVERTISING_ID -> {
            if (value.length > 8) {
                value.take(4) + "***" + value.takeLast(4)
            } else {
                "***"
            }
        }
        SpoofType.WIFI_MAC,
        SpoofType.BLUETOOTH_MAC -> {
            val parts = value.split(":")
            if (parts.size == 6) {
                "${parts[0]}:${parts[1]}:**:**:**:${parts[5]}"
            } else {
                "**:**:**:**:**:**"
            }
        }
        else -> value
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SpoofValueCardPreview() {
    DeviceMaskerTheme {
        SpoofValueCard(
            identifier =
                DeviceIdentifier(
                    type = SpoofType.IMEI,
                    value = "358673091234567",
                    isEnabled = true,
                ),
            onRegenerate = {},
            onEdit = {},
            onCopy = {},
            onToggle = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SpoofValueCardMaskedPreview() {
    DeviceMaskerTheme {
        SpoofValueCard(
            identifier =
                DeviceIdentifier(
                    type = SpoofType.WIFI_MAC,
                    value = "A4:83:E7:12:34:56",
                    isEnabled = true,
                ),
            onRegenerate = {},
            onEdit = {},
            onCopy = {},
            onToggle = {},
            maskValue = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CompactSpoofValueCardPreview() {
    DeviceMaskerTheme {
        CompactSpoofValueCard(label = "IMEI", value = "358673091234567", onRegenerate = {})
    }
}
