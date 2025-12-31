package com.astrixforge.devicemasker.xposed.hooker

import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.xposed.DualLog
import com.astrixforge.devicemasker.xposed.HookMetrics
import com.astrixforge.devicemasker.xposed.PrefsHelper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker

/**
 * Base hooker with common functionality shared across all spoof hookers.
 *
 * Provides:
 * - Centralized spoof value retrieval via PrefsHelper
 * - Consistent logging with DualLog
 * - Metrics recording with HookMetrics
 * - Common utility functions
 *
 * Usage:
 * ```kotlin
 * object MyHooker : BaseSpoofHooker("MyHooker") {
 *     override fun onHook() {
 *         logStart()
 *         // Hook logic here
 *         recordSuccess()
 *     }
 * }
 * ```
 */
abstract class BaseSpoofHooker(protected val tag: String) : YukiBaseHooker() {

    /**
     * Gets a spoof value from preferences, with fallback generation.
     *
     * @param type The type of value to spoof
     * @param fallback Generator function if no configured value exists
     * @return The configured spoof value or generated fallback
     */
    protected fun getSpoofValue(type: SpoofType, fallback: () -> String): String {
        return PrefsHelper.getSpoofValue(prefs, packageName, type, fallback)
    }

    /** Logs the start of hook registration for this package. */
    protected fun logStart() {
        DualLog.debug(tag, "Starting hooks for: $packageName")
    }

    /** Records successful hook initialization in metrics. */
    protected fun recordSuccess() {
        HookMetrics.recordSuccess(tag, "initialization")
    }

    /**
     * Records a hook failure in metrics.
     *
     * @param methodName Name of the method that failed to hook
     */
    protected fun recordFailure(methodName: String) {
        HookMetrics.recordFailure(tag, methodName)
    }

    /** Logs a debug message with the hooker's tag. */
    protected fun logDebug(message: String) {
        DualLog.debug(tag, message)
    }

    /** Logs a warning message with the hooker's tag. */
    protected fun logWarn(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            DualLog.warn(tag, message, throwable)
        } else {
            DualLog.warn(tag, message)
        }
    }
}
