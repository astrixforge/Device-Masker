package com.astrixforge.devicemasker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes as constants. Using simple string routes to avoid sealed class initialization
 * issues on Android 16.
 *
 * Group-centric workflow (3-tab layout):
 * - HOME: Dashboard with module status, group selection, and quick actions
 * - GROUPS: List of spoof groups (click to open GroupSpoofing)
 * - SETTINGS: App settings and diagnostics
 */
object NavRoutes {
    const val HOME = "home"
    const val GROUPS = "groups"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"

    // Detail screens (not in bottom nav)
    const val GROUP_SPOOFING = "group_spoofing"

    /**
     * Creates the route for GroupSpoofingScreen with a group ID parameter. Usage:
     * navController.navigate(NavRoutes.groupSpoofingRoute(groupId))
     */
    fun groupSpoofingRoute(groupId: String): String = "$GROUP_SPOOFING/$groupId"

    /**
     * Route pattern for GroupSpoofingScreen (use in NavHost composable). Usage:
     * composable("${NavRoutes.GROUP_SPOOFING}/{groupId}") { ... }
     */
    const val GROUP_SPOOFING_PATTERN = "$GROUP_SPOOFING/{groupId}"
}

/** Navigation item data class for bottom navigation. */
data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Bottom navigation items (3 tabs for group-centric workflow). Order: Home → Groups → Settings
 *
 * Note: Global Spoof tab removed - each group now works independently with its own enable/disable
 * controls.
 */
val bottomNavItems: List<NavItem> =
    listOf(
        NavItem(
            route = NavRoutes.HOME,
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home,
        ),
        NavItem(
            route = NavRoutes.GROUPS,
            label = "Groups",
            selectedIcon = Icons.Filled.Groups,
            unselectedIcon = Icons.Outlined.Groups,
        ),
        NavItem(
            route = NavRoutes.SETTINGS,
            label = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
    )
