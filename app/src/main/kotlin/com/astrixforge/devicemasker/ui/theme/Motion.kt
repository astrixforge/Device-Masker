package com.astrixforge.devicemasker.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * DeviceMasker Motion specifications.
 *
 * Uses spring-based animations as recommended by Material 3 Expressive. Spring animations feel more
 * natural and responsive than duration-based animations.
 */
object AppMotion {

    // ═══════════════════════════════════════════════════════════
    // Default Spring - For most transitions
    // ═══════════════════════════════════════════════════════════
    val DefaultSpring =
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

    val DefaultSpringDp =
        spring<Dp>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    val DefaultSpringOffset =
        spring<IntOffset>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        )

    // ═══════════════════════════════════════════════════════════
    // Fast Spring - For quick feedback (toggles, buttons)
    // ═══════════════════════════════════════════════════════════
    val FastSpring =
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        )

    val FastSpringDp =
        spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    // ═══════════════════════════════════════════════════════════
    // Bouncy Spring - For playful interactions
    // ═══════════════════════════════════════════════════════════
    val BouncySpring =
        spring<Float>(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)

    val BouncySpringDp =
        spring<Dp>(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)

    // ═══════════════════════════════════════════════════════════
    // Gentle Spring - For subtle, slow animations
    // ═══════════════════════════════════════════════════════════
    val GentleSpring =
        spring<Float>(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessVeryLow,
        )

    val GentleSpringDp =
        spring<Dp>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow)

    // ═══════════════════════════════════════════════════════════
    // Snappy Spring - For instant feedback
    // ═══════════════════════════════════════════════════════════
    val SnappySpring =
        spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)

    val SnappySpringDp =
        spring<Dp>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh)
}
