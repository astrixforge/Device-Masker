package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * ElevatedCard with Material 3 Expressive enhancements.
 *
 * Features:
 * - Spring-animated scale on press (0.98x) with bouncy return
 * - Spring-animated scale on selection (1.02x)
 * - Smooth color transition for selected state (primaryContainer at 30% alpha)
 * - Consistent styling with app theme (surfaceContainerHigh, shapes.large)
 *
 * Use this component for any clickable card that needs expressive feedback:
 * - Profile cards
 * - Stat cards
 * - Selection cards in lists
 *
 * @param onClick Click handler
 * @param modifier Modifier for the card
 * @param isSelected Whether the card is in selected state (affects color and scale)
 * @param enabled Whether the card is clickable
 * @param content Card content using ColumnScope
 */
@Composable
fun ExpressiveCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spatial spring for scale (CAN overshoot)
    val scale by animateFloatAsState(
        targetValue = when {
            !enabled -> 1f
            isPressed -> 0.98f
            isSelected -> 1.02f
            else -> 1f
        },
        animationSpec = AppMotion.Spatial.Expressive,
        label = "expressiveCardScale"
    )

    // Effect spring for color (NO overshoot)
    val containerColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = AppMotion.Effect.Color,
        label = "expressiveCardColor"
    )

    ElevatedCard(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Scale provides visual feedback
                enabled = enabled,
                onClick = onClick
            ),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        shape = shape,
    ) {
        Column(content = content)
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ExpressiveCardPreview() {
    DeviceMaskerTheme {
        ExpressiveCard(
            onClick = {},
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Expressive Card",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ExpressiveCardSelectedPreview() {
    DeviceMaskerTheme {
        ExpressiveCard(
            onClick = {},
            isSelected = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Selected Card",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
