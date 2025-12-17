package com.astrixforge.devicemasker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive
import com.astrixforge.devicemasker.ui.theme.StatusWarning

/** Status state for the indicator. */
enum class StatusState {
    ACTIVE,
    INACTIVE,
    WARNING,
}

/**
 * Animated status indicator dot with optional label.
 *
 * Shows a pulsing dot that changes color based on status. Uses spring animations for smooth
 * transitions.
 *
 * @param status The current status state
 * @param modifier Optional modifier
 * @param size Size of the indicator dot
 * @param showLabel Whether to show a text label
 * @param labelText Optional custom label text
 */
@Composable
fun StatusIndicator(
    status: StatusState,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    showLabel: Boolean = false,
    labelText: String? = null,
) {
    val color by
        animateColorAsState(
            targetValue =
                when (status) {
                    StatusState.ACTIVE -> StatusActive
                    StatusState.INACTIVE -> StatusInactive
                    StatusState.WARNING -> StatusWarning
                },
            animationSpec = spring(),
            label = "statusColor",
        )

    val scale by
        animateFloatAsState(
            targetValue = if (status == StatusState.ACTIVE) 1.0f else 0.9f,
            animationSpec = AppMotion.DefaultSpring,
            label = "statusScale",
        )

    val label =
        labelText
            ?: when (status) {
                StatusState.ACTIVE -> "Active"
                StatusState.INACTIVE -> "Inactive"
                StatusState.WARNING -> "Warning"
            }

    if (showLabel) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = color, size = size, scale = scale)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    } else {
        StatusDot(color = color, size = size, scale = scale, modifier = modifier)
    }
}

/** Simple status dot without label. */
@Composable
private fun StatusDot(color: Color, size: Dp, scale: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size).scale(scale).background(color = color, shape = CircleShape))
}

/**
 * Compact status badge with background.
 *
 * @param status The current status state
 * @param modifier Optional modifier
 */
@Composable
fun StatusBadge(status: StatusState, modifier: Modifier = Modifier) {
    val backgroundColor by
        animateColorAsState(
            targetValue =
                when (status) {
                    StatusState.ACTIVE -> StatusActive.copy(alpha = 0.15f)
                    StatusState.INACTIVE -> StatusInactive.copy(alpha = 0.15f)
                    StatusState.WARNING -> StatusWarning.copy(alpha = 0.15f)
                },
            animationSpec = spring(),
            label = "badgeBgColor",
        )

    val textColor by
        animateColorAsState(
            targetValue =
                when (status) {
                    StatusState.ACTIVE -> StatusActive
                    StatusState.INACTIVE -> StatusInactive
                    StatusState.WARNING -> StatusWarning
                },
            animationSpec = spring(),
            label = "badgeTextColor",
        )

    val label =
        when (status) {
            StatusState.ACTIVE -> "Active"
            StatusState.INACTIVE -> "Inactive"
            StatusState.WARNING -> "Warning"
        }

    Box(
        modifier =
            modifier
                .background(color = backgroundColor, shape = MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor)
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StatusIndicatorActivePreview() {
    DeviceMaskerTheme { StatusIndicator(status = StatusState.ACTIVE, showLabel = true) }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StatusIndicatorInactivePreview() {
    DeviceMaskerTheme { StatusIndicator(status = StatusState.INACTIVE, showLabel = true) }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StatusBadgePreview() {
    DeviceMaskerTheme {
        Row {
            StatusBadge(status = StatusState.ACTIVE)
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = StatusState.INACTIVE)
            Spacer(modifier = Modifier.width(8.dp))
            StatusBadge(status = StatusState.WARNING)
        }
    }
}
