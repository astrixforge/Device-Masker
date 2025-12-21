package com.astrixforge.devicemasker.xposed

import com.astrixforge.devicemasker.common.Utils

/**
 * Safe logging helper for the xposed module.
 *
 * Provides logging functions that:
 * 1. Avoid recursion (important in hooks)
 * 2. Log to both YLog and service buffer
 * 3. Handle exceptions gracefully
 */
object Logcat {

    private const val TAG = "DeviceMasker"

    // ThreadLocal to prevent logging recursion
    private val isLogging = ThreadLocal<Boolean>()

    /**
     * Checks if we're already in a logging call (prevents recursion).
     */
    private fun isRecursing(): Boolean {
        return isLogging.get() == true
    }

    /**
     * Logs a debug message.
     */
    fun logD(message: String, tag: String = TAG) {
        log(Utils.LogLevel.DEBUG, tag, message)
    }

    /**
     * Logs an info message.
     */
    fun logI(message: String, tag: String = TAG) {
        log(Utils.LogLevel.INFO, tag, message)
    }

    /**
     * Logs a warning message.
     */
    fun logW(message: String, tag: String = TAG) {
        log(Utils.LogLevel.WARN, tag, message)
    }

    /**
     * Logs an error message.
     */
    fun logE(message: String, tag: String = TAG) {
        log(Utils.LogLevel.ERROR, tag, message)
    }

    /**
     * Logs an error with exception.
     */
    fun logE(message: String, throwable: Throwable, tag: String = TAG) {
        log(Utils.LogLevel.ERROR, tag, "$message: ${throwable.message}")
    }

    /**
     * Core logging function with recursion guard.
     */
    private fun log(level: Int, tag: String, message: String) {
        // Prevent recursion
        if (isRecursing()) return

        isLogging.set(true)
        try {
            // Log to Android logcat
            when (level) {
                Utils.LogLevel.VERBOSE -> android.util.Log.v(tag, message)
                Utils.LogLevel.DEBUG -> android.util.Log.d(tag, message)
                Utils.LogLevel.INFO -> android.util.Log.i(tag, message)
                Utils.LogLevel.WARN -> android.util.Log.w(tag, message)
                Utils.LogLevel.ERROR -> android.util.Log.e(tag, message)
            }

            // Also log to service buffer for UI access
            runCatching {
                DeviceMaskerService.instance?.log(level, tag, message)
            }
        } finally {
            isLogging.set(false)
        }
    }
}
