package com.astrixforge.devicemasker.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.SettingsDataStore
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.navigation.BottomNavBar
import com.astrixforge.devicemasker.ui.navigation.NavRoutes
import com.astrixforge.devicemasker.ui.screens.DiagnosticsScreen
import com.astrixforge.devicemasker.ui.screens.HomeScreen
import com.astrixforge.devicemasker.ui.screens.ProfileDetailScreen
import com.astrixforge.devicemasker.ui.screens.ProfileScreen
import com.astrixforge.devicemasker.ui.screens.SettingsScreen
import com.astrixforge.devicemasker.ui.screens.ThemeMode
import com.astrixforge.devicemasker.ui.theme.AppMotion
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Main Activity for DeviceMasker.
 *
 * Uses Jetpack Compose with Material 3 for the entire UI with edge-to-edge display. Features:
 * - Bottom navigation with animated transitions
 * - Theme settings persistence (AMOLED, Dynamic Colors)
 * - Spring-based animations for smooth navigation
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initial edge-to-edge setup
        enableEdgeToEdge()

        Timber.d("MainActivity created, module active: ${DeviceMaskerApp.isXposedModuleActive}")

        setContent {
            val settingsStore = remember { SettingsDataStore(applicationContext) }
            val repository = remember { SpoofRepository.getInstance(applicationContext) }

            // Collect theme settings from SettingsDataStore
            val themeMode by settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val amoledMode by settingsStore.amoledMode.collectAsState(initial = true)
            val dynamicColors by settingsStore.dynamicColors.collectAsState(initial = true)
            val debugLogging by settingsStore.debugLogging.collectAsState(initial = false)

            // Determine actual dark state for edge-to-edge styling
            // isSystemInDarkTheme() is evaluated here in composition context
            val isSystemDark = isSystemInDarkTheme()
            val isDarkModeActive =
                    when (themeMode) {
                        ThemeMode.SYSTEM -> isSystemDark
                        ThemeMode.LIGHT -> false
                        ThemeMode.DARK -> true
                    }

            // Update edge-to-edge styling when theme changes
            val activity = this@MainActivity
            DisposableEffect(isDarkModeActive) {
                activity.enableEdgeToEdge(
                        statusBarStyle =
                                if (isDarkModeActive) {
                                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                } else {
                                    SystemBarStyle.light(
                                            android.graphics.Color.TRANSPARENT,
                                            android.graphics.Color.TRANSPARENT,
                                    )
                                },
                        navigationBarStyle =
                                if (isDarkModeActive) {
                                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                } else {
                                    SystemBarStyle.light(
                                            android.graphics.Color.TRANSPARENT,
                                            android.graphics.Color.TRANSPARENT,
                                    )
                                },
                )
                onDispose {}
            }

            DeviceMaskerTheme(
                    themeMode = themeMode,
                    amoledBlack = amoledMode,
                    dynamicColor = dynamicColors,
            ) {
                DeviceMaskerMainApp(
                        repository = repository,
                        settingsStore = settingsStore,
                        themeMode = themeMode,
                        amoledMode = amoledMode,
                        dynamicColors = dynamicColors,
                        debugLogging = debugLogging,
                )
            }
        }
    }
}

/** Main app composable with navigation. */
@Composable
fun DeviceMaskerMainApp(
        repository: SpoofRepository,
        settingsStore: SettingsDataStore,
        themeMode: ThemeMode,
        amoledMode: Boolean,
        dynamicColors: Boolean,
        debugLogging: Boolean,
        navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.HOME
    val scope = rememberCoroutineScope()

    // Hide bottom nav on profile detail and diagnostics screens for a cleaner focused experience
    val showBottomBar = !currentRoute.startsWith(NavRoutes.PROFILE_DETAIL) && 
                        currentRoute != NavRoutes.DIAGNOSTICS

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    // Pop up to the start destination to avoid stacking
                                    popUpTo(NavRoutes.HOME) { saveState = true }
                                    // Avoid multiple copies of the same destination
                                    launchSingleTop = true
                                    // Restore state when re-selecting a previously selected item
                                    restoreState = true
                                }
                            },
                    )
                }
            },
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = spring()) +
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = AppMotion.DefaultSpringOffset,
                            )
                },
                exitTransition = {
                    fadeOut(animationSpec = spring()) +
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = AppMotion.DefaultSpringOffset,
                            )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = spring()) +
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = AppMotion.DefaultSpringOffset,
                            )
                },
                popExitTransition = {
                    fadeOut(animationSpec = spring()) +
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = AppMotion.DefaultSpringOffset,
                            )
                },
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                        repository = repository,
                        onNavigateToSpoof = { navController.navigate(NavRoutes.PROFILES) },
                        onRegenerateAll = {
                            // Will be connected to ViewModel
                            Timber.d("Regenerate all values requested")
                        },
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                        themeMode = themeMode,
                        amoledDarkMode = amoledMode,
                        dynamicColors = dynamicColors,
                        debugLogging = debugLogging,
                        onThemeModeChange = { mode ->
                            Timber.d("Theme mode changed: $mode")
                            scope.launch { settingsStore.setThemeMode(mode) }
                        },
                        onAmoledDarkModeChange = { enabled ->
                            Timber.d("AMOLED mode changed: $enabled")
                            scope.launch { settingsStore.setAmoledMode(enabled) }
                        },
                        onDynamicColorChange = { enabled ->
                            Timber.d("Dynamic colors changed: $enabled")
                            scope.launch { settingsStore.setDynamicColors(enabled) }
                        },
                        onDebugLogChange = { enabled ->
                            Timber.d("Debug logging changed: $enabled")
                            scope.launch { settingsStore.setDebugLogging(enabled) }
                        },
                        onNavigateToDiagnostics = { navController.navigate(NavRoutes.DIAGNOSTICS) },
                )
            }

            // Profile Detail Screen - Per-profile spoof values and app assignment
            composable(
                    route = NavRoutes.PROFILE_DETAIL_PATTERN,
                    arguments = listOf(navArgument("profileId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val profileId =
                        backStackEntry.arguments?.getString("profileId") ?: return@composable
                ProfileDetailScreen(
                        profileId = profileId,
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(NavRoutes.PROFILES) {
                ProfileScreen(
                        repository = repository,
                        onProfileClick = { profile ->
                            navController.navigate(NavRoutes.profileDetailRoute(profile.id))
                        },
                )
            }

            composable(NavRoutes.DIAGNOSTICS) {
                DiagnosticsScreen(
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
