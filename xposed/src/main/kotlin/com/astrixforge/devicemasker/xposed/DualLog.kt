package com.astrixforge.devicemasker.xposed

import com.highcapable.yukihookapi.hook.log.YLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Dual Logger - Logs to both YukiHookAPI (YLog) and internal buffer.
 *
 * This ensures logs are visible in:
 * 1. LSPosed Manager (via YLog → Logcat)
 * 2. Export Logs feature (via internal buffer → AIDL)
 *
 * Usage:
 * ```kotlin
 * DualLog.debug("MyHooker", "Hook applied successfully")
 * DualLog.info("MyHooker", "Started hooks for: com.example.app")
 * DualLog.error("MyHooker", "Failed to hook", exception)
 * ```
 */
@Suppress("unused") // Logging utility - all methods are API
object DualLog {

    private const val MAX_LOGS = 1000
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /** Internal log buffer for diagnostics. */
    val logBuffer: CopyOnWriteArrayList<String> = CopyOnWriteArrayList()

    // ═══════════════════════════════════════════════════════════
    // LOGGING METHODS
    // ═══════════════════════════════════════════════════════════

    /** Debug level log - for detailed debugging info. */
    fun debug(tag: String, message: String) {
        YLog.debug("[$tag] $message")
        addToBuffer("D", tag, message)
    }

    /** Info level log - for important events. */
    fun info(tag: String, message: String) {
        YLog.info("[$tag] $message")
        addToBuffer("I", tag, message)
    }

    /** Warning level log - for recoverable issues. */
    fun warn(tag: String, message: String) {
        YLog.warn("[$tag] $message")
        addToBuffer("W", tag, message)
    }

    /** Warning level log with exception. */
    fun warn(tag: String, message: String, throwable: Throwable) {
        YLog.warn("[$tag] $message: ${throwable.message}")
        addToBuffer("W", tag, "$message: ${throwable.message}")
    }

    /** Error level log - for failures. */
    fun error(tag: String, message: String) {
        YLog.error("[$tag] $message")
        addToBuffer("E", tag, message)
    }

    /** Error level log with exception. */
    fun error(tag: String, message: String, throwable: Throwable) {
        YLog.error("[$tag] $message", throwable)
        addToBuffer("E", tag, "$message: ${throwable.message}")
        // Also log stack trace
        throwable.stackTrace.take(5).forEach { element -> addToBuffer("E", tag, "  at $element") }
    }

    // ═══════════════════════════════════════════════════════════
    // BUFFER MANAGEMENT
    // ═══════════════════════════════════════════════════════════

    /** Gets all logs as array (for AIDL). */
    fun getLogs(): Array<String> = logBuffer.toTypedArray()

    /** Clears all logs. */
    fun clearLogs() {
        logBuffer.clear()
    }

    /** Gets log count. */
    fun getLogCount(): Int = logBuffer.size

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════

    private fun addToBuffer(level: String, tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val entry = "[$timestamp][$level][$tag] $message"

        logBuffer.add(entry)

        // Trim buffer if too large
        while (logBuffer.size > MAX_LOGS) {
            logBuffer.removeAt(0)
        }
    }
}

/**
 * Hook Metrics - Tracks hook registration success/failure rates.
 *
 * Usage:
 * ```kotlin
 * method { name = "getImei" }.optional().hook {
 *     after {
 *         HookMetrics.recordSuccess("DeviceHooker", "getImei")
 *         result = spoofedValue
 *     }
 * }
 * ```
 */
object HookMetrics {
    private val successCount = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private val failureCount = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun recordSuccess(hookerName: String, methodName: String) {
        val key = "$hookerName.$methodName"
        successCount[key] = successCount.getOrDefault(key, 0) + 1
    }

    fun recordFailure(hookerName: String, methodName: String) {
        val key = "$hookerName.$methodName"
        failureCount[key] = failureCount.getOrDefault(key, 0) + 1
    }

    fun getSuccessCount(): Int = successCount.values.sum()

    fun getFailureCount(): Int = failureCount.values.sum()

    fun getSummary(): String = buildString {
        appendLine("=== Hook Metrics ===")
        appendLine("Success: ${getSuccessCount()} calls")
        appendLine("Failures: ${getFailureCount()} calls")
        if (failureCount.isNotEmpty()) {
            appendLine("Failed methods:")
            failureCount.forEach { (method, count) -> appendLine("  - $method: $count") }
        }
    }

    fun dumpToLog() {
        DualLog.info("HookMetrics", getSummary())
    }
}
