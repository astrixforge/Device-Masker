package com.astrixforge.devicemasker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DeviceMasker shapes following Material 3 guidelines.
 */
val AppShapes = Shapes(
    // Extra small - Chips, small buttons
    extraSmall = RoundedCornerShape(4.dp),
    
    // Small - Text fields, list items
    small = RoundedCornerShape(8.dp),
    
    // Medium - Cards, dialogs
    medium = RoundedCornerShape(12.dp),
    
    // Large - Bottom sheets, navigation drawers
    large = RoundedCornerShape(16.dp),
    
    // Extra large - Full-screen components
    extraLarge = RoundedCornerShape(28.dp)
)
