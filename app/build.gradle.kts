plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Use JVM 17 for better Android/Xposed compatibility
kotlin {
    jvmToolchain(17)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-Xwarning-level=DEPRECATION:disabled",
        )
    }
}

android {
    namespace = "com.astrixforge.devicemasker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.astrixforge.devicemasker"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true  // Enable AIDL for IPC with xposed module
    }

    // CRITICAL: Prevent synthetic lambda classes that cause ClassNotFoundException in Xposed
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        // Ensure all module classes are in the primary dex
        dex { useLegacyPackaging = true }
    }

    // ═══════════════════════════════════════════════════════════
    // LINT CONFIGURATION
    // ═══════════════════════════════════════════════════════════
    lint {
        // Enable cross-module analysis to prevent false positives
        // in library modules (:common, :xposed)
        checkDependencies = true
        
        // Treat lint errors as warnings in debug builds
        abortOnError = false
        
        // Generate HTML report for review
        htmlReport = true
        
        // Baseline file to track known issues (optional)
        // baseline = file("lint-baseline.xml")
    }
}

dependencies {
    // ═══════════════════════════════════════════════════════════
    // HMA-OSS ARCHITECTURE MODULES
    // ═══════════════════════════════════════════════════════════
    implementation(project(":common"))  // Shared models and AIDL
    implementation(project(":xposed"))  // Hook logic - bundled in APK

    // ═══════════════════════════════════════════════════════════
    // CORE ANDROID
    // ═══════════════════════════════════════════════════════════
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ═══════════════════════════════════════════════════════════
    // JETPACK COMPOSE
    // ═══════════════════════════════════════════════════════════
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.material3.window.size)
    implementation(libs.graphics.shapes)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // ═══════════════════════════════════════════════════════════
    // ACTIVITY & LIFECYCLE
    // ═══════════════════════════════════════════════════════════
    implementation(libs.activity.compose)
    implementation(libs.bundles.lifecycle)

    // ═══════════════════════════════════════════════════════════
    // NAVIGATION
    // ═══════════════════════════════════════════════════════════
    implementation(libs.navigation.compose)

    // ═══════════════════════════════════════════════════════════
    // YUKIHOOKAPI (Modern Kotlin Hook Framework)
    // ═══════════════════════════════════════════════════════════
    implementation(libs.yukihookapi.api)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)
    implementation(libs.hiddenapibypass)
    ksp(libs.yukihookapi.ksp.xposed)
    compileOnly(libs.xposed.api) // Provided at runtime by Xposed framework

    // ═══════════════════════════════════════════════════════════
    // DATA STORAGE
    // ═══════════════════════════════════════════════════════════
    implementation(libs.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    // ═══════════════════════════════════════════════════════════
    // COROUTINES
    // ═══════════════════════════════════════════════════════════
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ═══════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════
    implementation(libs.timber)
    implementation(libs.coil.compose)

    // ═══════════════════════════════════════════════════════════
    // TESTING
    // ═══════════════════════════════════════════════════════════
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
