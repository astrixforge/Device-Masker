package com.astrixforge.devicemasker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.astrixforge.devicemasker.ui.screens.ThemeMode

/**
 * AMOLED Dark Color Scheme.
 *
 * Uses pure black background for OLED displays to save battery and provide maximum contrast.
 */
private val AmoledDarkColorScheme =
    darkColorScheme(
        // Primary
        primary = PrimaryDark,
        onPrimary = Color.Black,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,

        // Secondary
        secondary = SecondaryDark,
        onSecondary = Color.Black,
        secondaryContainer = SecondaryContainer,
        onSecondaryContainer = Color(0xFFCCFFFF),

        // Tertiary
        tertiary = TertiaryDark,
        onTertiary = Color.Black,
        tertiaryContainer = TertiaryContainer,
        onTertiaryContainer = Color(0xFFEDE7F6),

        // Error
        error = ErrorDark,
        onError = Color.Black,
        errorContainer = ErrorContainer,
        onErrorContainer = Color(0xFFFFDAD6),

        // Background & Surface (AMOLED Black)
        background = AmoledBlack,
        onBackground = OnSurfaceDark,
        surface = AmoledBlack,
        onSurface = OnSurfaceDark,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = OnSurfaceVariantDark,

        // Surface containers
        surfaceContainer = AmoledSurfaceContainer,
        surfaceContainerHigh = AmoledSurfaceContainerHigh,
        surfaceContainerHighest = Color(0xFF2A2A2A),
        surfaceContainerLow = AmoledSurface,
        surfaceContainerLowest = AmoledBlack,

        // Other
        outline = OutlineDark,
        outlineVariant = OutlineVariantDark,
        inverseSurface = Color(0xFFE3E3E3),
        inverseOnSurface = Color(0xFF1A1A1A),
        inversePrimary = PrimaryLight,
        scrim = Color.Black,
    )

/** Light Color Scheme for users who prefer light mode. */
private val LightColorScheme =
    lightColorScheme(
        // Primary
        primary = PrimaryLight,
        onPrimary = Color.White,
        primaryContainer = PrimaryContainerLight,
        onPrimaryContainer = OnPrimaryContainerLight,

        // Secondary
        secondary = SecondaryLight,
        onSecondary = Color.White,
        secondaryContainer = SecondaryContainerLight,
        onSecondaryContainer = Color(0xFF00332C),

        // Tertiary
        tertiary = TertiaryLight,
        onTertiary = Color.White,
        tertiaryContainer = TertiaryContainerLight,
        onTertiaryContainer = Color(0xFF21005D),

        // Error
        error = ErrorLight,
        onError = Color.White,
        errorContainer = ErrorContainerLight,
        onErrorContainer = Color(0xFF410002),
    )

/**
 * Device Masker Theme composable with ThemeMode support.
 *
 * This is the recommended way to apply theming - pass the ThemeMode directly and let the theme
 * handle system dark mode detection internally.
 *
 * @param themeMode The theme mode (SYSTEM, LIGHT, DARK). Defaults to SYSTEM.
 * @param amoledBlack Whether to use AMOLED pure black when in dark mode. Defaults to true.
 * @param dynamicColor Whether to use dynamic colors on Android 12+. Defaults to true.
 * @param content The composable content to theme.
 */
@Composable
fun DeviceMaskerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoledBlack: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    // Determine dark theme based on mode
    // IMPORTANT: isSystemInDarkTheme() is evaluated inside this composable
    // to ensure it's in the correct composition context
    val darkTheme =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    DeviceMaskerThemeInternal(
        darkTheme = darkTheme,
        amoledBlack = amoledBlack,
        dynamicColor = dynamicColor,
        content = content,
    )
}

/**
 * Internal Device Masker Theme implementation.
 *
 * Use [DeviceMaskerTheme] with ThemeMode parameter instead for proper system theme detection.
 */
@Composable
private fun DeviceMaskerThemeInternal(
    darkTheme: Boolean,
    amoledBlack: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            // Dynamic colors on Android 12+
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) {
                    if (amoledBlack) {
                        // Use dynamic dark colors but override with AMOLED black
                        dynamicDarkColorScheme(context)
                            .copy(
                                background = AmoledBlack,
                                surface = AmoledBlack,
                                surfaceContainer = AmoledSurfaceContainer,
                                surfaceContainerLow = AmoledSurface,
                                surfaceContainerLowest = AmoledBlack,
                            )
                    } else {
                        // Regular dynamic dark colors
                        dynamicDarkColorScheme(context)
                    }
                } else {
                    dynamicLightColorScheme(context)
                }
            }

            // Custom dark themes
            darkTheme -> {
                if (amoledBlack) {
                    AmoledDarkColorScheme
                } else {
                    // Regular dark theme (not pure black) - using Material 3 default dark with
                    // our primaries
                    darkColorScheme(
                        // Primary
                        primary = PrimaryDark,
                        onPrimary = Color.Black,
                        primaryContainer = PrimaryContainer,
                        onPrimaryContainer = OnPrimaryContainer,
                        // Secondary
                        secondary = SecondaryDark,
                        onSecondary = Color.Black,
                        secondaryContainer = SecondaryContainer,
                        onSecondaryContainer = Color(0xFFCCFFFF),
                        // Tertiary
                        tertiary = TertiaryDark,
                        onTertiary = Color.Black,
                        tertiaryContainer = TertiaryContainer,
                        onTertiaryContainer = Color(0xFFEDE7F6),
                        // Error
                        error = ErrorDark,
                        onError = Color.Black,
                        errorContainer = ErrorContainer,
                        onErrorContainer = Color(0xFFFFDAD6),
                        // Background & Surface (dark gray, not pure black)
                        background = Color(0xFF121212),
                        onBackground = Color(0xFFE3E3E3),
                        surface = Color(0xFF121212),
                        onSurface = Color(0xFFE3E3E3),
                        surfaceVariant = Color(0xFF1E1E1E),
                        onSurfaceVariant = Color(0xFFC0C0C0),
                        // Surface containers
                        surfaceContainer = Color(0xFF1A1A1A),
                        surfaceContainerHigh = Color(0xFF242424),
                        surfaceContainerHighest = Color(0xFF2E2E2E),
                        surfaceContainerLow = Color(0xFF161616),
                        surfaceContainerLowest = Color(0xFF0E0E0E),
                        // Other
                        outline = OutlineDark,
                        outlineVariant = OutlineVariantDark,
                        inverseSurface = Color(0xFFE3E3E3),
                        inverseOnSurface = Color(0xFF1A1A1A),
                        inversePrimary = PrimaryLight,
                        scrim = Color.Black,
                    )
                }
            }

            // Custom light theme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
