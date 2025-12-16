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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.SpoofDataStore
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.navigation.BottomNavBar
import com.astrixforge.devicemasker.ui.navigation.NavRoutes
import com.astrixforge.devicemasker.ui.screens.AppSelectionScreen
import com.astrixforge.devicemasker.ui.screens.DiagnosticsScreen
import com.astrixforge.devicemasker.ui.screens.HomeScreen
import com.astrixforge.devicemasker.ui.screens.ProfileScreen
import com.astrixforge.devicemasker.ui.screens.SettingsScreen
import com.astrixforge.devicemasker.ui.screens.SpoofSettingsScreen
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
            val dataStore = remember { SpoofDataStore(applicationContext) }
            val repository = remember { SpoofRepository.getInstance(applicationContext) }

            // Collect theme settings from DataStore
            val darkMode by dataStore.darkMode.collectAsState(initial = true)
            val amoledMode by dataStore.amoledMode.collectAsState(initial = true)
            val dynamicColors by dataStore.dynamicColors.collectAsState(initial = true)
            val debugLogging by dataStore.debugLogging.collectAsState(initial = false)

            // Use dark theme if darkMode is enabled
            val darkTheme = darkMode

            // Update edge-to-edge styling when theme changes
            val activity = this@MainActivity
            DisposableEffect(darkTheme) {
                activity.enableEdgeToEdge(
                        statusBarStyle =
                                if (darkTheme) {
                                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                } else {
                                    SystemBarStyle.light(
                                            android.graphics.Color.TRANSPARENT,
                                            android.graphics.Color.TRANSPARENT
                                    )
                                },
                        navigationBarStyle =
                                if (darkTheme) {
                                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                                } else {
                                    SystemBarStyle.light(
                                            android.graphics.Color.TRANSPARENT,
                                            android.graphics.Color.TRANSPARENT
                                    )
                                }
                )
                onDispose {}
            }

            DeviceMaskerTheme(
                    darkTheme = darkTheme,
                    amoledBlack = amoledMode,
                    dynamicColor = dynamicColors
            ) {
                DeviceMaskerMainApp(
                        repository = repository,
                        dataStore = dataStore,
                        darkMode = darkMode,
                        amoledMode = amoledMode,
                        dynamicColors = dynamicColors,
                        debugLogging = debugLogging
                )
            }
        }
    }
}

/** Main app composable with navigation. */
@Composable
fun DeviceMaskerMainApp(
        repository: SpoofRepository,
        dataStore: SpoofDataStore,
        darkMode: Boolean,
        amoledMode: Boolean,
        dynamicColors: Boolean,
        debugLogging: Boolean,
        navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.HOME
    val scope = rememberCoroutineScope()

    Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
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
                        }
                )
            }
    ) { innerPadding ->
        NavHost(
                navController = navController,
                startDestination = NavRoutes.HOME,
                modifier = Modifier.padding(innerPadding),
                enterTransition = {
                    fadeIn(animationSpec = spring()) +
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = AppMotion.DefaultSpringOffset
                            )
                },
                exitTransition = {
                    fadeOut(animationSpec = spring()) +
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                    animationSpec = AppMotion.DefaultSpringOffset
                            )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = spring()) +
                            slideIntoContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = AppMotion.DefaultSpringOffset
                            )
                },
                popExitTransition = {
                    fadeOut(animationSpec = spring()) +
                            slideOutOfContainer(
                                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                                    animationSpec = AppMotion.DefaultSpringOffset
                            )
                }
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                        repository = repository,
                        onNavigateToSpoof = { navController.navigate(NavRoutes.SPOOF) },
                        onRegenerateAll = {
                            // Will be connected to ViewModel
                            Timber.d("Regenerate all values requested")
                        }
                )
            }

            composable(NavRoutes.SPOOF) {
                SpoofSettingsScreen(
                        repository = repository,
                        onEditValue = { type, value ->
                            // Will open edit dialog
                            Timber.d("Edit $type: $value")
                        }
                )
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                        darkMode = darkMode,
                        amoledDarkMode = amoledMode,
                        dynamicColors = dynamicColors,
                        debugLogging = debugLogging,
                        onDarkModeChange = { enabled ->
                            Timber.d("Dark mode changed: $enabled")
                            scope.launch { dataStore.setDarkMode(enabled) }
                        },
                        onAmoledDarkModeChange = { enabled ->
                            Timber.d("AMOLED mode changed: $enabled")
                            scope.launch { dataStore.setAmoledMode(enabled) }
                        },
                        onDynamicColorChange = { enabled ->
                            Timber.d("Dynamic colors changed: $enabled")
                            scope.launch { dataStore.setDynamicColors(enabled) }
                        },
                        onDebugLogChange = { enabled ->
                            Timber.d("Debug logging changed: $enabled")
                            scope.launch { dataStore.setDebugLogging(enabled) }
                        },
                        onNavigateToDiagnostics = { navController.navigate(NavRoutes.DIAGNOSTICS) }
                )
            }

            composable(NavRoutes.APPS) {
                AppSelectionScreen(
                        repository = repository,
                        onAppClick = { app -> Timber.d("App clicked: ${app.packageName}") }
                )
            }

            composable(NavRoutes.PROFILES) {
                ProfileScreen(
                        repository = repository,
                        onProfileClick = { profile -> Timber.d("Profile clicked: ${profile.name}") }
                )
            }

            composable(NavRoutes.DIAGNOSTICS) {
                DiagnosticsScreen(
                        repository = repository,
                        onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
