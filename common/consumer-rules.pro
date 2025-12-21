# Add project specific ProGuard rules here.
# Consumer rules for the common module
# These rules will be included when consuming this library

# Keep all AIDL generated classes
-keep class com.astrixforge.devicemasker.common.IDeviceMaskerService { *; }
-keep class com.astrixforge.devicemasker.common.IDeviceMaskerService$* { *; }

# Keep models with @Serializable
-keepattributes *Annotation*
-keepclassmembers class com.astrixforge.devicemasker.common.** {
    <init>(...);
    *;
}
