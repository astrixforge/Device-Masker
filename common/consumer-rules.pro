# Add project specific ProGuard rules here.
# Consumer rules for the common module
# These rules will be included when consuming this library

# ═══════════════════════════════════════════════════════════════════════════════
# ANNOTATIONS - Required for kotlinx.serialization to work at runtime
# ═══════════════════════════════════════════════════════════════════════════════
# Keep all annotations (original rule maintained for compatibility)
-keepattributes *Annotation*

# More specific annotation attributes for optimal preservation
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault

# Signature is needed for generic type preservation
-keepattributes Signature

# InnerClasses is needed for proper nested class handling
-keepattributes InnerClasses

# Exceptions attribute for proper exception handling
-keepattributes Exceptions

# ═══════════════════════════════════════════════════════════════════════════════
# KOTLINX SERIALIZATION
# ═══════════════════════════════════════════════════════════════════════════════
# Keep @Serializable classes and their serializers
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    <init>(...);
    *;
}

# Keep serializer companion objects
-keepclassmembers class com.astrixforge.devicemasker.common.**$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializer classes
-keep class com.astrixforge.devicemasker.common.**$$serializer { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# ENUMS
# ═══════════════════════════════════════════════════════════════════════════════
# Keep enum values and valueOf methods (used by serialization and hooks)
-keepclassmembers enum com.astrixforge.devicemasker.common.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    <fields>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# DATA CLASSES
# ═══════════════════════════════════════════════════════════════════════════════
# Keep data class component methods for destructuring
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    public ** component*();
    public ** copy(...);
}

# ═══════════════════════════════════════════════════════════════════════════════
# KOTLIN SPECIFICS
# ═══════════════════════════════════════════════════════════════════════════════
# Keep Kotlin Metadata for reflection if needed
-keepattributes *KotlinMetadata*

# Don't warn about Kotlin internal classes
-dontwarn kotlin.reflect.jvm.internal.**
