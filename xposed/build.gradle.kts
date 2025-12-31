plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

// Use JVM 17 for Android/Xposed compatibility
kotlin {
    jvmToolchain(17)
    compilerOptions { freeCompilerArgs.addAll("-Xwarning-level=DEPRECATION:disabled") }
}

android {
    namespace = "com.astrixforge.devicemasker.xposed"
    compileSdk = rootProject.ext.get("compileSdk") as Int

    defaultConfig {
        minSdk = rootProject.ext.get("minSdk") as Int
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    buildFeatures { buildConfig = true }

    // Include assets in the library (for xposed_init)
    sourceSets { getByName("main") { assets.srcDirs("src/main/assets") } }
}

dependencies {
    // Common module for shared models and AIDL
    implementation(project(":common"))

    // YukiHookAPI - Modern Kotlin Hook Framework (API only, no KSP for library)
    implementation(libs.yukihookapi.api)

    // KavaRef - Reflection API (required for YukiHookAPI 1.3.x)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.extension)

    // Hidden API Bypass (replaces FreeReflection)
    implementation(libs.hiddenapibypass)

    // Xposed API (provided at runtime by framework)
    compileOnly(libs.xposed.api)

    // Coroutines for async operations
    implementation(libs.kotlinx.coroutines.core)

    // Serialization for config parsing
    implementation(libs.kotlinx.serialization.json)
}
