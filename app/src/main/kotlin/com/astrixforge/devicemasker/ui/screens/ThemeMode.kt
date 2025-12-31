package com.astrixforge.devicemasker.ui.screens

import com.astrixforge.devicemasker.R

/**
 * Theme mode options for the app.
 *
 * Defines the available theme modes that users can select.
 */
enum class ThemeMode(val displayNameRes: Int) {
    SYSTEM(R.string.settings_theme_system),
    LIGHT(R.string.settings_theme_light),
    DARK(R.string.settings_theme_dark),
}
