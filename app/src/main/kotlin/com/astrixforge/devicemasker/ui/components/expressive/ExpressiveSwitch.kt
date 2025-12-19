package com.astrixforge.devicemasker.ui.components.expressive

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

// ═══════════════════════════════════════════════════════════════════════════════
// M3 Switch Dimensions (from Material Design 3 spec)
// ═══════════════════════════════════════════════════════════════════════════════

private object SwitchDimensions {
    // Track dimensions
    val TrackWidth = 52.dp
    val TrackHeight = 32.dp
    val TrackCornerRadius = 16.dp

    // Thumb dimensions
    val ThumbSizeUnchecked = 16.dp
    val ThumbSizeChecked = 24.dp
    val ThumbSizePressed = 28.dp
    val ThumbPadding = 4.dp

    // Icon dimensions
    val IconSize = 16.dp

    // Border
    val BorderWidth = 2.dp
}

/**
 * Material 3 Expressive Switch with spring-animated thumb transitions.
 *
 * A fully custom switch implementation that follows M3 visual specs while providing
 * expressive spring-based animations for all state changes:
 *
 * - **Thumb position**: Bouncy spring animation when toggling
 * - **Thumb size**: Grows when checked, shrinks when unchecked, expands when pressed
 * - **Colors**: Smooth spring transitions for track and thumb colors
 * - **Icon**: Animated check/close icon with scaling effect
 *
 * ## M3 Expressive Features:
 * - Spring physics for natural feel (using AppMotion.Spatial.Snappy)
 * - Thumb size morphing on state change (M3 spec)
 * - Press feedback with thumb expansion
 * - Subtle scale animation on toggle
 *
 * ## Theme Integration:
 * - Uses MaterialTheme.colorScheme.primary for checked track color
 * - Supports dynamic colors on Android 12+
 * - Adapts to light/dark theme automatically
 *
 * @param checked Current checked state
 * @param onCheckedChange Callback when state changes. If null, switch is read-only
 * @param modifier Optional modifier
 * @param enabled Whether the switch is interactive
 * @param showThumbIcon Whether to show check/close icon in thumb
 *
 * @sample ExpressiveSwitchPreview
 */
@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showThumbIcon: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Theme-aware colors
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val checkedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
    val uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant

    // ═══════════════════════════════════════════════════════════════════════════
    // SPRING ANIMATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    // Thumb position - bouncy spring for expressive movement
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            SwitchDimensions.TrackWidth - SwitchDimensions.ThumbSizeChecked - SwitchDimensions.ThumbPadding
        } else {
            SwitchDimensions.ThumbPadding
        },
        animationSpec = AppMotion.Spatial.SnappyDp,
        label = "thumbOffset",
    )

    // Thumb size - M3 spec: grows when checked, shrinks when unchecked
    val targetThumbSize = when {
        isPressed -> SwitchDimensions.ThumbSizePressed
        checked -> SwitchDimensions.ThumbSizeChecked
        else -> SwitchDimensions.ThumbSizeUnchecked
    }
    val thumbSize by animateDpAsState(
        targetValue = targetThumbSize,
        animationSpec = AppMotion.Spatial.SnappyDp,
        label = "thumbSize",
    )

    // Track color - smooth transition using theme colors
    val trackColor by animateColorAsState(
        targetValue = if (enabled) {
            if (checked) activeColor else inactiveColor
        } else {
            if (checked) activeColor.copy(alpha = 0.38f) else inactiveColor.copy(alpha = 0.12f)
        },
        animationSpec = AppMotion.Effect.Color,
        label = "trackColor",
    )

    // Thumb color - white when checked, outline when unchecked
    val thumbColor by animateColorAsState(
        targetValue = if (enabled) {
            if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
        } else {
            if (checked) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.38f)
        },
        animationSpec = AppMotion.Effect.Color,
        label = "thumbColor",
    )

    // Border color (only visible when unchecked)
    val borderColor by animateColorAsState(
        targetValue = if (enabled) {
            if (checked) Color.Transparent else MaterialTheme.colorScheme.outline
        } else {
            if (checked) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        },
        animationSpec = AppMotion.Effect.Color,
        label = "borderColor",
    )

    // Icon color - theme-aware
    val iconColor by animateColorAsState(
        targetValue = if (enabled) {
            if (checked) checkedIconColor else uncheckedIconColor
        } else {
            if (checked) checkedIconColor.copy(alpha = 0.38f) else uncheckedIconColor.copy(alpha = 0.38f)
        },
        animationSpec = AppMotion.Effect.Color,
        label = "iconColor",
    )

    // Icon scale - subtle grow/shrink on toggle
    val iconScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.85f,
        animationSpec = AppMotion.Spatial.Snappy,
        label = "iconScale",
    )

    // Overall switch scale - micro-interaction on press
    val switchScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = AppMotion.Spatial.Snappy,
        label = "switchScale",
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // COMPOSABLE LAYOUT
    // ═══════════════════════════════════════════════════════════════════════════

    Box(
        modifier = modifier
            .scale(switchScale)
            .size(width = SwitchDimensions.TrackWidth, height = SwitchDimensions.TrackHeight)
            .clip(RoundedCornerShape(SwitchDimensions.TrackCornerRadius))
            .background(trackColor)
            .border(
                width = SwitchDimensions.BorderWidth,
                color = borderColor,
                shape = RoundedCornerShape(SwitchDimensions.TrackCornerRadius),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                onClick = { onCheckedChange?.invoke(!checked) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 0.dp)
                .size(thumbSize)
                .background(color = thumbColor, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Icon inside thumb
            if (showThumbIcon && thumbSize >= SwitchDimensions.ThumbSizeChecked) {
                Icon(
                    imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = null,
                    modifier = Modifier
                        .size(SwitchDimensions.IconSize)
                        .scale(iconScale),
                    tint = iconColor,
                )
            }
        }
    }
}

/**
 * Switch with an inline label that's fully clickable.
 *
 * Use this for settings items or list items where the entire row
 * should toggle the switch state.
 *
 * @param checked Current checked state
 * @param onCheckedChange Callback when state changes
 * @param label Text label displayed next to the switch
 * @param modifier Optional modifier
 * @param enabled Whether the switch is interactive
 * @param showThumbIcon Whether to show check/close icon in thumb
 * @param labelFirst If true, label appears before switch (default: true)
 */
@Composable
fun ExpressiveSwitchWithLabel(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showThumbIcon: Boolean = true,
    labelFirst: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
                onClick = { onCheckedChange(!checked) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (labelFirst) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        ExpressiveSwitch(
            checked = checked,
            onCheckedChange = null, // Handled by Row click
            enabled = enabled,
            showThumbIcon = showThumbIcon,
        )

        if (!labelFirst) {
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212, name = "Unchecked & Checked")
@Composable
private fun ExpressiveSwitchPreview() {
    DeviceMaskerTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ExpressiveSwitch(checked = false, onCheckedChange = {})
            Spacer(modifier = Modifier.width(24.dp))
            ExpressiveSwitch(checked = true, onCheckedChange = {})
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, name = "Without Icon")
@Composable
private fun ExpressiveSwitchNoIconPreview() {
    DeviceMaskerTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ExpressiveSwitch(checked = false, onCheckedChange = {}, showThumbIcon = false)
            Spacer(modifier = Modifier.width(24.dp))
            ExpressiveSwitch(checked = true, onCheckedChange = {}, showThumbIcon = false)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, name = "Disabled States")
@Composable
private fun ExpressiveSwitchDisabledPreview() {
    DeviceMaskerTheme {
        Row(modifier = Modifier.padding(16.dp)) {
            ExpressiveSwitch(checked = false, onCheckedChange = {}, enabled = false)
            Spacer(modifier = Modifier.width(24.dp))
            ExpressiveSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212, name = "With Label")
@Composable
private fun ExpressiveSwitchWithLabelPreview() {
    DeviceMaskerTheme {
        ExpressiveSwitchWithLabel(
            checked = true,
            onCheckedChange = {},
            label = "Enable Feature",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Light Theme")
@Composable
private fun ExpressiveSwitchLightPreview() {
    DeviceMaskerTheme(themeMode = com.astrixforge.devicemasker.ui.screens.ThemeMode.LIGHT) {
        Row(modifier = Modifier.padding(16.dp)) {
            ExpressiveSwitch(checked = false, onCheckedChange = {})
            Spacer(modifier = Modifier.width(24.dp))
            ExpressiveSwitch(checked = true, onCheckedChange = {})
        }
    }
}
