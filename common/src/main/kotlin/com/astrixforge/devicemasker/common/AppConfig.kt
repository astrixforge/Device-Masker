package com.astrixforge.devicemasker.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for a specific app.
 *
 * This determines which group (if any) is assigned to an app,
 * and whether spoofing is enabled for that app.
 *
 * @property packageName The app's package name
 * @property groupId ID of the assigned group (null = use default)
 * @property isEnabled Whether spoofing is enabled for this app
 */
@Serializable
data class AppConfig(
    val packageName: String,
    @SerialName("profileId")
    val groupId: String? = null,
    val isEnabled: Boolean = true,
) {
    /** Creates a copy with a new group assignment. */
    fun withGroup(groupId: String?): AppConfig {
        return copy(groupId = groupId)
    }

    /** Creates a copy with updated enabled state. */
    fun withEnabled(enabled: Boolean): AppConfig {
        return copy(isEnabled = enabled)
    }
}
