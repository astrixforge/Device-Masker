# Add project specific ProGuard rules here.
# Consumer rules for the xposed module
# These rules will be included when consuming this library

# Keep YukiHookAPI entry point and generated init class
-keep class com.astrixforge.devicemasker.xposed.XposedHookLoader { *; }
-keep class com.astrixforge.devicemasker.xposed.**_YukiHookXposedInit { *; }

# Keep all hookers
-keep class com.astrixforge.devicemasker.xposed.hooker.** { *; }

# Keep service implementation
-keep class com.astrixforge.devicemasker.xposed.DeviceMaskerService { *; }

# Keep preference helpers
-keep class com.astrixforge.devicemasker.xposed.PrefsHelper { *; }
-keep class com.astrixforge.devicemasker.xposed.PrefsKeys { *; }
-keep class com.astrixforge.devicemasker.xposed.DualLog { *; }

# Keep all classes for Xposed framework
-keep class * extends de.robv.android.xposed.IXposedHookLoadPackage { *; }
-keep class * extends de.robv.android.xposed.IXposedHookZygoteInit { *; }

