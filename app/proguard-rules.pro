# Device Masker ProGuard Rules
# ========================================

# ═══════════════════════════════════════════════════════════
# GENERAL ANDROID RULES
# ═══════════════════════════════════════════════════════════

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ═══════════════════════════════════════════════════════════
# YUKIHOOKAPI - Keep everything needed for hooks
# ═══════════════════════════════════════════════════════════

-keep class com.highcapable.yukihookapi.** { *; }
-keep interface com.highcapable.yukihookapi.** { *; }
-keepclassmembers class com.highcapable.yukihookapi.** { *; }
-dontwarn com.highcapable.yukihookapi.**

# ═══════════════════════════════════════════════════════════
# KAVAREF - Reflection library used by YukiHookAPI
# ═══════════════════════════════════════════════════════════

-keep class com.highcapable.kavaref.** { *; }
-keep interface com.highcapable.kavaref.** { *; }
-keepclassmembers class com.highcapable.kavaref.** { *; }
-dontwarn com.highcapable.kavaref.**

# Missing class stubs that R8 complains about
-dontwarn java.lang.reflect.AnnotatedType
-dontwarn java.lang.reflect.AnnotatedElement
-dontwarn java.lang.invoke.MethodHandles$Lookup

# ═══════════════════════════════════════════════════════════
# XPOSED/LSPOSED - Keep framework classes
# ═══════════════════════════════════════════════════════════

-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }
-keep class io.github.lsposed.** { *; }
-dontwarn de.robv.android.xposed.**
-dontwarn io.github.lsposed.**

# ═══════════════════════════════════════════════════════════
# DEVICE MASKER MODULE - Keep hook entry points
# ═══════════════════════════════════════════════════════════

# Keep our hook entry and all hookers
-keep class com.astrixforge.devicemasker.hook.** { *; }
-keep class com.astrixforge.devicemasker.hook.hooker.** { *; }

# Keep data models (for serialization)
-keep class com.astrixforge.devicemasker.data.models.** { *; }

# Keep generators
-keep class com.astrixforge.devicemasker.data.generators.** { *; }

# ═══════════════════════════════════════════════════════════
# KOTLIN SERIALIZATION
# ═══════════════════════════════════════════════════════════

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-keepclasseswithmembers class ** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ═══════════════════════════════════════════════════════════
# KOTLIN COROUTINES
# ═══════════════════════════════════════════════════════════

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ═══════════════════════════════════════════════════════════
# DATASTORE
# ═══════════════════════════════════════════════════════════

-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# ═══════════════════════════════════════════════════════════
# COMPOSE - Needed for release builds
# ═══════════════════════════════════════════════════════════

-dontwarn androidx.compose.**

# ═══════════════════════════════════════════════════════════
# TIMBER LOGGING
# ═══════════════════════════════════════════════════════════

-dontwarn org.jetbrains.annotations.**

# ═══════════════════════════════════════════════════════════
# HIDDEN API BYPASS
# ═══════════════════════════════════════════════════════════

-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**