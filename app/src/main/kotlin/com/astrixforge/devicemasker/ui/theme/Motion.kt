package com.astrixforge.devicemasker.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * DeviceMasker Motion specifications.
 *
 * Uses spring-based animations as recommended by Material 3 Expressive. Spring animations feel more
 * natural and responsive than duration-based animations.
 *
 * ## Material 3 Expressive Motion Categories:
 * - **Spatial**: For position, size, rotation, scale - these CAN overshoot target values
 * - **Effect**: For color, opacity, blur - these should NOT overshoot
 *
 * Use [Spatial] springs for interactive elements that need expressive feedback. Use [Effect]
 * springs for smooth visual transitions without bounce.
 */
object AppMotion {

    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║                    MATERIAL 3 EXPRESSIVE SPRINGS                           ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝

    /**
     * Spatial springs for animating position, size, rotation, and scale. These springs CAN
     * overshoot their target values for expressive feedback.
     */
    object Spatial {
        /**
         * Expressive spring for hero moments and prominent interactions. High bounce, low
         * stiffness - creates noticeable overshoot. Use for: Button presses, card selection, FAB
         * animations.
         */
        val Expressive: SpringSpec<Float> =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy, // 0.5 - moderate bounce
                stiffness = Spring.StiffnessLow, // 200 - slow, expressive
            )

        /**
         * Standard spring for typical interactions. Subtle bounce, medium stiffness - minimal
         * overshoot. Use for: Navigation transitions, list item animations.
         */
        val Standard: SpringSpec<Float> =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy, // 0.75 - slight bounce
                stiffness = Spring.StiffnessMediumLow, // 400 - moderate speed
            )

        val StandardIntSize: SpringSpec<IntSize> =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )

        /**
         * Snappy spring for quick feedback. Minimal bounce, high stiffness - fast response. Use
         * for: Toggle switches, quick icon animations.
         */
        val Snappy: SpringSpec<Float> =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy, // 0.75 - slight bounce
                stiffness = Spring.StiffnessMedium, // 1500 - fast
            )

        val SnappyDp: SpringSpec<Dp> =
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    }

    /**
     * Effect springs for animating color, opacity, and blur. These springs should NOT overshoot
     * their target values.
     */
    object Effect {
        /**
         * Smooth spring for color transitions. No bounce, medium stiffness - smooth color change.
         * Use for: Background color, icon tint, status colors.
         */
        val Color: SpringSpec<Color> =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // 1.0 - no bounce
                stiffness = Spring.StiffnessMediumLow, // 400 - smooth transition
            )

        /**
         * Smooth spring for opacity/alpha transitions. No bounce, medium stiffness - smooth fade.
         * Use for: Fade in/out, alpha changes, visibility.
         */
        val Alpha: SpringSpec<Float> =
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // 1.0 - no bounce
                stiffness = Spring.StiffnessMedium, // 1500 - responsive
            )
    }

    // ╔═══════════════════════════════════════════════════════════════════════════╗
    // ║                         LEGACY SPRINGS (Deprecated)                        ║
    // ║              Use Spatial.* or Effect.* for new code                        ║
    // ╚═══════════════════════════════════════════════════════════════════════════╝

    @Deprecated(
        message = "Use Spatial.StandardOffset for position animations",
        replaceWith = ReplaceWith("Spatial.StandardOffset"),
    )
    val DefaultSpringOffset: SpringSpec<IntOffset> =
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)

    // Fast Spring - For quick feedback
    @Deprecated(
        message = "Use Spatial.Snappy for quick scale or Effect.Quick for colors",
        replaceWith = ReplaceWith("Spatial.Snappy"),
    )
    val FastSpring: SpringSpec<Float> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)

    @Deprecated(
        message = "Use Spatial.ExpressiveDp for bouncy size animations",
        replaceWith = ReplaceWith("Spatial.ExpressiveDp"),
    )
    val BouncySpringDp: SpringSpec<Dp> =
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessLow)
}
