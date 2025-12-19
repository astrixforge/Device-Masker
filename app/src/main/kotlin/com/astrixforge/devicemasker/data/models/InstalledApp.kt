package com.astrixforge.devicemasker.data.models

import kotlinx.serialization.Serializable

/**
 * Represents an installed app for display in the selection UI.
 *
 * @property packageName The app's package name
 * @property label The app's display name
 * @property isSystemApp Whether this is a system app
 * @property versionName The app's version string
 */
@Serializable
data class InstalledApp(
    val packageName: String,
    val label: String,
    val isSystemApp: Boolean = false,
    val versionName: String = "",
)
