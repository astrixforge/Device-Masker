package com.astrixforge.devicemasker.ui.screens.groupspoofing.model

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.common.models.Carrier

/**
 * Data class to bundle spoof-related callbacks and reduce parameter drilling.
 *
 * Instead of passing 4+ callbacks through multiple layers of composables,
 * we bundle them into a single object that can be passed down.
 */
data class SpoofCallbacks(
    /** Regenerate a single spoof type value */
    val onRegenerate: (SpoofType) -> Unit,

    /** Toggle a spoof type on/off */
    val onToggle: (SpoofType, Boolean) -> Unit,

    /** Change the carrier selection */
    val onCarrierChange: (Carrier) -> Unit,

    /** Copy a value to clipboard */
    val onCopy: (String) -> Unit,
) {
    companion object {
        /** Empty callbacks for preview purposes */
        val Empty = SpoofCallbacks(
            onRegenerate = {},
            onToggle = { _, _ -> },
            onCarrierChange = {},
            onCopy = {},
        )
    }
}
