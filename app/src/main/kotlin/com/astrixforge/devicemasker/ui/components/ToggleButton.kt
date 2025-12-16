package com.astrixforge.devicemasker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive

/**
 * Custom animated toggle button with spring physics.
 *
 * A more expressive toggle compared to the standard Switch,
 * using spring-based animations for a bouncy, playful feel.
 *
 * @param checked Whether the toggle is on
 * @param onCheckedChange Callback when toggle state changes
 * @param modifier Optional modifier
 * @param enabled Whether the toggle is interactive
 * @param activeColor Color when toggle is on
 * @param inactiveColor Color when toggle is off
 * @param thumbColor Color of the toggle thumb
 */
@Composable
fun ToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = StatusActive,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    thumbColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val trackWidth = 52.dp
    val trackHeight = 28.dp
    val thumbSize = 22.dp
    val thumbPadding = 3.dp

    // Animate the thumb position
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            trackWidth - thumbSize - (thumbPadding * 2)
        } else {
            0.dp
        },
        animationSpec = AppMotion.BouncySpringDp,
        label = "thumbOffset"
    )

    // Animate the track color
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else inactiveColor,
        animationSpec = spring(),
        label = "trackColor"
    )

    // Animate the thumb color
    val currentThumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else thumbColor.copy(alpha = 0.8f),
        animationSpec = spring(),
        label = "thumbColor"
    )

    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor.copy(alpha = alpha))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .background(
                    color = currentThumbColor.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
    }
}

/**
 * Large toggle button variant for hero sections.
 *
 * Use in prominent places like the home screen status card.
 */
@Composable
fun LargeToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackWidth = 72.dp
    val trackHeight = 38.dp
    val thumbSize = 30.dp
    val thumbPadding = 4.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            trackWidth - thumbSize - (thumbPadding * 2)
        } else {
            0.dp
        },
        animationSpec = AppMotion.BouncySpringDp,
        label = "largeThumbOffset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            StatusActive
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        animationSpec = spring(),
        label = "largeTrackColor"
    )

    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .clip(RoundedCornerShape(50))
            .background(trackColor.copy(alpha = alpha))
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onCheckedChange(!checked)
            }
            .padding(thumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .background(
                    color = Color.White.copy(alpha = alpha),
                    shape = CircleShape
                )
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ToggleButtonOffPreview() {
    DeviceMaskerTheme {
        ToggleButton(
            checked = false,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun ToggleButtonOnPreview() {
    DeviceMaskerTheme {
        ToggleButton(
            checked = true,
            onCheckedChange = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun LargeToggleButtonPreview() {
    DeviceMaskerTheme {
        LargeToggleButton(
            checked = true,
            onCheckedChange = {}
        )
    }
}
