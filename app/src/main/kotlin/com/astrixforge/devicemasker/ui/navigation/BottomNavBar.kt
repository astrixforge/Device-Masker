package com.astrixforge.devicemasker.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.ui.theme.AppMotion

/**
 * Bottom navigation bar for DeviceMasker.
 *
 * Uses Material 3 NavigationBar with animated icons and labels. Supports spring-based animations
 * for smooth transitions.
 *
 * @param currentRoute Current active route string
 * @param onNavigate Callback when user selects a route
 * @param modifier Optional modifier
 */
@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
    ) {
        bottomNavItems.forEach { item ->
            BottomNavItem(
                item = item,
                isSelected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
            )
        }
    }
}

/** Individual navigation bar item. */
@Composable
private fun RowScope.BottomNavItem(item: NavItem, isSelected: Boolean, onClick: () -> Unit) {
    // Animate icon scale for selection
    val scale by
    animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = AppMotion.Spatial.Standard,
        label = "iconScale_${item.route}",
    )

    NavigationBarItem(
        selected = isSelected,
        onClick = onClick,
        icon = {
            AnimatedNavIcon(item = item, isSelected = isSelected, modifier = Modifier.scale(scale))
        },
        label = { Text(text = item.label, style = MaterialTheme.typography.labelMedium) },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.secondary, // M3 1.4.0+ expressive nav
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
    )
}

/** Animated icon that transitions between selected and unselected states. */
@Composable
private fun AnimatedNavIcon(item: NavItem, isSelected: Boolean, modifier: Modifier = Modifier) {
    val iconColor by
    animateColorAsState(
        targetValue =
            if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = AppMotion.Effect.Color,
        label = "iconColor",
    )

    Icon(
        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
        contentDescription = item.label,
        modifier = modifier.size(24.dp),
        tint = iconColor,
    )
}
