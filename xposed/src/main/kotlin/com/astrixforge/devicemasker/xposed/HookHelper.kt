package com.astrixforge.devicemasker.xposed

import android.os.Build
import com.highcapable.yukihookapi.hook.param.PackageParam

/**
 * Hook Helper Extensions - Provides safe and cleaner hook utilities.
 *
 * Features:
 * - SDK version-aware hooking
 * - Input validation (IMEI, MAC, Android ID)
 * - Safe execution wrappers
 *
 * Note: For method hooking, use the standard YukiHookAPI pattern:
 * ```kotlin
 * runCatching {
 *     method { name = "methodName" }.hook { }
 * }
 * ```
 */

// ═══════════════════════════════════════════════════════════
// SAFE CLASS HOOKING
// ═══════════════════════════════════════════════════════════

/** Safely hooks a class, returning false if class doesn't exist. */
inline fun PackageParam.hookClassSafe(
    className: String,
    tag: String = "HookHelper",
    block: Class<*>.() -> Unit,
): Boolean {
    return try {
        val clazz = className.toClassOrNull()
        if (clazz != null) {
            clazz.block()
            true
        } else {
            DualLog.debug(tag, "Class not found: $className")
            false
        }
    } catch (e: Exception) {
        DualLog.warn(tag, "Failed to hook class: $className", e)
        false
    }
}

// ═══════════════════════════════════════════════════════════
// SDK VERSION UTILITIES
// ═══════════════════════════════════════════════════════════

/** SDK version constants for cleaner code. */
object SdkVersions {
    const val LOLLIPOP = Build.VERSION_CODES.LOLLIPOP // 21
    const val MARSHMALLOW = Build.VERSION_CODES.M // 23
    const val NOUGAT = Build.VERSION_CODES.N // 24
    const val OREO = Build.VERSION_CODES.O // 26
    const val PIE = Build.VERSION_CODES.P // 28
    const val Q = Build.VERSION_CODES.Q // 29 (Android 10)
    const val R = Build.VERSION_CODES.R // 30 (Android 11)
    const val S = Build.VERSION_CODES.S // 31 (Android 12)
    const val TIRAMISU = Build.VERSION_CODES.TIRAMISU // 33 (Android 13)
    const val UPSIDE_DOWN_CAKE = 34 // Android 14
    const val VANILLA_ICE_CREAM = 35 // Android 15
    const val BAKLAVA = 36 // Android 16
}

/** Checks if current SDK is at least the specified version. */
fun isAtLeastSdk(version: Int): Boolean = Build.VERSION.SDK_INT >= version

/** Checks if current SDK is at most the specified version. */
fun isAtMostSdk(version: Int): Boolean = Build.VERSION.SDK_INT <= version

/** Checks if SDK is in range [min, max]. */
fun isSdkInRange(min: Int, max: Int): Boolean = Build.VERSION.SDK_INT in min..max

/** Hooks only if the SDK version matches. */
inline fun PackageParam.hookIfSdk(minSdk: Int, maxSdk: Int? = null, block: () -> Unit): Boolean {
    val currentSdk = Build.VERSION.SDK_INT
    return if (currentSdk >= minSdk && (maxSdk == null || currentSdk <= maxSdk)) {
        block()
        true
    } else {
        false
    }
}

/** Legacy API hook wrapper - executes only on Android 9 and below. */
inline fun PackageParam.hookLegacyApi(tag: String = "LegacyHook", block: () -> Unit) {
    if (isAtMostSdk(SdkVersions.PIE)) {
        try {
            block()
            DualLog.debug(tag, "Legacy hook applied for SDK ${Build.VERSION.SDK_INT}")
        } catch (e: Exception) {
            DualLog.warn(tag, "Legacy hook failed", e)
        }
    }
}

/** Modern API hook wrapper - executes only on Android 10+. */
inline fun PackageParam.hookModernApi(tag: String = "ModernHook", block: () -> Unit) {
    if (isAtLeastSdk(SdkVersions.Q)) {
        try {
            block()
            DualLog.debug(tag, "Modern hook applied for SDK ${Build.VERSION.SDK_INT}")
        } catch (e: Exception) {
            DualLog.warn(tag, "Modern hook failed", e)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// INPUT VALIDATION
// ═══════════════════════════════════════════════════════════

/** Validates IMEI format (15 digits, Luhn-valid). */
fun validateImei(imei: String): Boolean {
    if (imei.length != 15 || !imei.all { it.isDigit() }) {
        return false
    }
    // Luhn check
    var sum = 0
    for (i in imei.indices) {
        var digit = imei[i].digitToInt()
        if (i % 2 == 1) {
            digit *= 2
            if (digit > 9) digit -= 9
        }
        sum += digit
    }
    return sum % 10 == 0
}

/** Validates MAC address format (XX:XX:XX:XX:XX:XX). */
fun validateMac(mac: String): Boolean {
    val regex = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
    return regex.matches(mac)
}

/** Validates Android ID format (16 hex characters). */
fun validateAndroidId(id: String): Boolean {
    val regex = Regex("^[0-9a-f]{16}$")
    return regex.matches(id.lowercase())
}

/** Validates IMSI format (15 digits). */
fun validateImsi(imsi: String): Boolean {
    return imsi.length == 15 && imsi.all { it.isDigit() }
}

/** Validates ICCID format (19-20 digits). */
fun validateIccid(iccid: String): Boolean {
    return iccid.length in 19..20 && iccid.all { it.isDigit() }
}
