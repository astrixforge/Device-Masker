// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

// Shared configuration for all modules
ext {
    set("compileSdk", 36)
    set("minSdk", 26)
    set("targetSdk", 36)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktfmt("0.54").kotlinlangStyle()
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        ktfmt("0.54").kotlinlangStyle()
    }
}
