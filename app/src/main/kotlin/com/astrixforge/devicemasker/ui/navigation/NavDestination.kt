package com.astrixforge.devicemasker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes as constants. Using simple string routes to avoid sealed class initialization
 * issues on Android 16.
 *
 * Profile-centric workflow (3-tab layout):
 * - HOME: Dashboard with module status, profile selection, and quick actions
 * - PROFILES: List of spoof profiles (click to open ProfileDetail)
 * - SETTINGS: App settings and diagnostics
 */
object NavRoutes {
    const val HOME = "home"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"

    // Detail screens (not in bottom nav)
    const val PROFILE_DETAIL = "profile_detail"

    /**
     * Creates the route for ProfileDetailScreen with a profile ID parameter. Usage:
     * navController.navigate(NavRoutes.profileDetailRoute(profileId))
     */
    fun profileDetailRoute(profileId: String): String = "$PROFILE_DETAIL/$profileId"

    /**
     * Route pattern for ProfileDetailScreen (use in NavHost composable). Usage:
     * composable("${NavRoutes.PROFILE_DETAIL}/{profileId}") { ... }
     */
    const val PROFILE_DETAIL_PATTERN = "$PROFILE_DETAIL/{profileId}"
}

/** Navigation item data class for bottom navigation. */
data class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/**
 * Bottom navigation items (3 tabs for profile-centric workflow). Order: Home → Profiles → Settings
 *
 * Note: Global Spoof tab removed - each profile now works independently with its own enable/disable
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
            route = NavRoutes.PROFILES,
            label = "Profiles",
            selectedIcon = Icons.Filled.Person,
            unselectedIcon = Icons.Outlined.Person,
        ),
        NavItem(
            route = NavRoutes.SETTINGS,
            label = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
        ),
    )
