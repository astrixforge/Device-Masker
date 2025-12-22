package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Expandable section with Material 3 Expressive spring animations.
 *
 * Features:
 * - Spring-based expand/collapse content animation
 * - Spring-animated expand icon rotation
 * - Consistent card styling with app theme
 * - Optional header icon and count badge
 *
 * Use this component for collapsible sections:
 * - Diagnostic categories
 * - Settings groups
 * - Group detail sections
 *
 * @param title Section title
 * @param modifier Modifier for the card
 * @param isExpanded Whether the section is expanded
 * @param onExpandChange Callback when expand state changes
 * @param icon Optional leading icon
 * @param count Optional count badge (e.g., "3/5 passed")
 * @param content Expandable content using ColumnScope
 */
@Composable
fun AnimatedSection(
    title: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = true,
    onExpandChange: (Boolean) -> Unit = {},
    icon: ImageVector? = null,
    count: String? = null,
    countColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    // Spring-animated rotation for expand icon
    val iconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = AppMotion.Spatial.Standard,
        label = "expandIconRotation"
    )

    ExpressiveCard(
        onClick = { onExpandChange(!isExpanded) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Optional leading icon
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Optional count badge
                        if (count != null) {
                            Text(
                                text = count,
                                style = MaterialTheme.typography.bodySmall,
                                color = countColor
                            )
                        }
                    }
                }

                // Expand/collapse indicator with animated icon
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.padding(12.dp).rotate(iconRotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Animated content with spring physics
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = AppMotion.Spatial.StandardIntSize),
                exit = shrinkVertically(animationSpec = AppMotion.Spatial.StandardIntSize),
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
                    ),
                    content = content
                )
            }
        }
    }
}

/**
 * Stateful variant of AnimatedSection that manages its own expand state.
 * Use this when you don't need external control over the expand state.
 */
@Composable
fun AnimatedSectionStateful(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = true,
    icon: ImageVector? = null,
    count: String? = null,
    countColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    AnimatedSection(
        title = title,
        modifier = modifier,
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
        icon = icon,
        count = count,
        countColor = countColor,
        content = content
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AnimatedSectionExpandedPreview() {
    DeviceMaskerTheme {
        AnimatedSectionStateful(
            title = "Device Identifiers",
            count = "4 items",
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Content goes here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AnimatedSectionCollapsedPreview() {
    DeviceMaskerTheme {
        AnimatedSection(
            title = "Network Identifiers",
            isExpanded = false,
            count = "2 items",
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Content")
        }
    }
}
