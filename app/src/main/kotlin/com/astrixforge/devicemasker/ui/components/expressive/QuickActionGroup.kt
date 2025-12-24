package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Quick Action Button data class.
 *
 * @param label Button label text
 * @param icon Optional leading icon
 * @param onClick Click handler
 * @param enabled Whether the button is enabled
 */
data class QuickAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
    val enabled: Boolean = true
)

/**
 * Material 3 Expressive Button Group for quick actions.
 *
 * Uses the new M3 ButtonGroup component with spring animations
 * for a cohesive group of related actions.
 *
 * @param actions List of quick actions to display
 * @param modifier Modifier for the button group
 *
 * Usage:
 * ```kotlin
 * QuickActionGroup(
 *     actions = listOf(
 *         QuickAction("Configure", Icons.Outlined.Fingerprint) { onConfigure() },
 *         QuickAction("Regenerate", Icons.Filled.Refresh) { onRegenerate() }
 *     )
 * )
 * ```
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickActionGroup(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
    ) {
        actions.forEach { action ->
            ToggleButton(
                checked = false,
                onCheckedChange = { action.onClick() },
                enabled = action.enabled,
                modifier = Modifier.weight(1f),
            ) {
                if (action.icon != null) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(text = action.label)
            }
        }
    }
}

/**
 * Simple two-button quick action row (fallback for non-experimental use).
 *
 * Uses FilledTonalButton for consistency when ButtonGroup is not available.
 *
 * @param primaryAction Primary action (left button)
 * @param secondaryAction Secondary action (right button)
 * @param modifier Modifier for the row
 */
@Composable
fun QuickActionRow(
    primaryAction: QuickAction,
    secondaryAction: QuickAction,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = primaryAction.onClick,
            enabled = primaryAction.enabled,
            modifier = Modifier.weight(1f)
        ) {
            if (primaryAction.icon != null) {
                Icon(primaryAction.icon, null)
                Spacer(Modifier.width(8.dp))
            }
            Text(primaryAction.label)
        }

        FilledTonalButton(
            onClick = secondaryAction.onClick,
            enabled = secondaryAction.enabled,
            modifier = Modifier.weight(1f)
        ) {
            if (secondaryAction.icon != null) {
                Icon(secondaryAction.icon, null)
                Spacer(Modifier.width(8.dp))
            }
            Text(secondaryAction.label)
        }
    }
}

/**
 * Single-select button group for mode selection.
 *
 * @param options List of option labels
 * @param selectedIndex Currently selected index
 * @param onSelectionChange Callback when selection changes
 * @param modifier Modifier for the button group
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SelectionButtonGroup(
    options: List<String>,
    selectedIndex: Int,
    onSelectionChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = selectedIndex == index,
                onCheckedChange = { onSelectionChange(index) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = option)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QuickActionGroupPreview() {
    DeviceMaskerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            QuickActionGroup(
                actions = listOf(
                    QuickAction(
                        label = "Configure",
                        icon = Icons.Outlined.Fingerprint,
                        onClick = {}
                    ),
                    QuickAction(
                        label = "Regenerate",
                        icon = Icons.Filled.Refresh,
                        onClick = {}
                    )
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun QuickActionRowPreview() {
    DeviceMaskerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            QuickActionRow(
                primaryAction = QuickAction(
                    label = "Configure",
                    icon = Icons.Outlined.Fingerprint,
                    onClick = {}
                ),
                secondaryAction = QuickAction(
                    label = "Regenerate",
                    icon = Icons.Filled.Refresh,
                    onClick = {}
                )
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun SelectionButtonGroupPreview() {
    DeviceMaskerTheme {
        var selectedIndex by remember { mutableIntStateOf(0) }
        Box(modifier = Modifier.padding(16.dp)) {
            SelectionButtonGroup(
                options = listOf("Day", "Week", "Month"),
                selectedIndex = selectedIndex,
                onSelectionChange = { selectedIndex = it }
            )
        }
    }
}
