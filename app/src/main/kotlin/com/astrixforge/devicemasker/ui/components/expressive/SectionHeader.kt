package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive

/**
 * Collapsible section header with Material 3 Expressive animations.
 *
 * Features:
 * - Optional leading icon with primary color
 * - Title with optional subtitle/count
 * - Spring-animated expand icon rotation
 * - Clickable for expand/collapse toggle
 *
 * Use this component for section headers in lists:
 * - Diagnostic category headers
 * - Settings group headers
 * - Group section headers
 *
 * @param title Section title
 * @param modifier Modifier for the header
 * @param icon Optional leading icon
 * @param count Optional count text (e.g., "3/5", "4 items")
 * @param countColor Color for the count text
 * @param isExpanded Whether the section is expanded
 * @param onExpandChange Callback when expand toggle is clicked (null = not expandable)
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    count: String? = null,
    countColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    isExpanded: Boolean = true,
    onExpandChange: ((Boolean) -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Spring-animated rotation for expand icon
    val iconRotation by
        animateFloatAsState(
            targetValue = if (isExpanded) 180f else 0f,
            animationSpec = AppMotion.Spatial.Standard,
            label = "headerExpandRotation",
        )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (onExpandChange != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onExpandChange(!isExpanded) },
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            // Optional leading icon
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Title and optional count
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                if (count != null) {
                    Text(
                        text = count,
                        style = MaterialTheme.typography.bodySmall,
                        color = countColor,
                    )
                }
            }
        }

        // Expand/collapse icon (only shown if expandable)
        if (onExpandChange != null) {
            Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(iconRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SectionHeaderPreview() {
    DeviceMaskerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Anti-Detection",
                icon = Icons.Outlined.Security,
                count = "4/5 passed",
                countColor = StatusActive,
                onExpandChange = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SectionHeaderCollapsedPreview() {
    DeviceMaskerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Device Identifiers",
                count = "8 items",
                isExpanded = false,
                onExpandChange = {},
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SectionHeaderNoExpandPreview() {
    DeviceMaskerTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(
                title = "Static Section",
                icon = Icons.Outlined.Security,
                count = "No toggle",
                onExpandChange = null,
            )
        }
    }
}
