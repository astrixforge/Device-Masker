package com.astrixforge.devicemasker.common

import kotlinx.serialization.Serializable

/**
 * Configuration for a specific app.
 *
 * This determines which profile (if any) is assigned to an app,
 * and whether spoofing is enabled for that app.
 *
 * @property packageName The app's package name
 * @property profileId ID of the assigned profile (null = use default)
 * @property isEnabled Whether spoofing is enabled for this app
 */
@Serializable
data class AppConfig(
    val packageName: String,
    val profileId: String? = null,
    val isEnabled: Boolean = true,
) {
    /** Creates a copy with a new profile assignment. */
    fun withProfile(profileId: String?): AppConfig {
        return copy(profileId = profileId)
    }

    /** Creates a copy with updated enabled state. */
    fun withEnabled(enabled: Boolean): AppConfig {
        return copy(isEnabled = enabled)
    }
}
