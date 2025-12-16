package com.astrixforge.devicemasker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation routes as constants. Using simple string routes to avoid sealed class initialization
 * issues on Android 16.
 */
object NavRoutes {
    const val HOME = "home"
    const val APPS = "apps"
    const val SPOOF = "spoof"
    const val PROFILES = "profiles"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"
}

/** Navigation item data class for bottom navigation. */
data class NavItem(
        val route: String,
        val label: String,
        val selectedIcon: ImageVector,
        val unselectedIcon: ImageVector
)

/**
 * All bottom navigation items (5 tabs as per PRD). Order: Home → Apps → Spoof → Profiles → Settings
 */
val bottomNavItems: List<NavItem> =
        listOf(
                NavItem(
                        route = NavRoutes.HOME,
                        label = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home
                ),
                NavItem(
                        route = NavRoutes.APPS,
                        label = "Apps",
                        selectedIcon = Icons.Filled.Apps,
                        unselectedIcon = Icons.Outlined.Apps
                ),
                NavItem(
                        route = NavRoutes.SPOOF,
                        label = "Spoof",
                        selectedIcon = Icons.Filled.Tune,
                        unselectedIcon = Icons.Outlined.Tune
                ),
                NavItem(
                        route = NavRoutes.PROFILES,
                        label = "Profiles",
                        selectedIcon = Icons.Filled.Person,
                        unselectedIcon = Icons.Outlined.Person
                ),
                NavItem(
                        route = NavRoutes.SETTINGS,
                        label = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings
                )
        )
