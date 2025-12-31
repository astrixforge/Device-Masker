package com.astrixforge.devicemasker.service

import android.content.Context
import android.net.Uri
import android.os.Build
import com.astrixforge.devicemasker.BuildConfig
import com.highcapable.yukihookapi.hook.log.YLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Log Manager - Comprehensive log export with industry-standard formatting.
 *
 * Exports YLog in-memory data (app process logs) via SAF file picker.
 */
object LogManager {

    private const val LOG_FILE_PREFIX = "devicemasker_logs_"
    private const val LOG_FILE_EXTENSION = ".log"

    // Industry-standard log separators
    private const val HEADER_LINE =
        "════════════════════════════════════════════════════════════════════════════════"
    private const val SECTION_LINE =
        "────────────────────────────────────────────────────────────────────────────────"
    private const val SUBSECTION_LINE =
        "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"

    // ═══════════════════════════════════════════════════════════════════
    // EXPORT LOGS (In-Memory YLog Data)
    // ═══════════════════════════════════════════════════════════════════

    /** Exports YLog in-memory data to a custom URI location. */
    fun exportLogsToUri(context: Context, uri: Uri): LogExportResult {
        return try {
            val logContent = buildInMemoryLogContent(context)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(logContent.toByteArray())
            } ?: return LogExportResult.Error("Failed to open file for writing")

            LogExportResult.Success(uri.lastPathSegment ?: "logs.log", logContent.lines().size)
        } catch (e: Exception) {
            LogExportResult.Error("Export failed: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // IN-MEMORY LOG CONTENT BUILDER
    // ═══════════════════════════════════════════════════════════════════

    private fun buildInMemoryLogContent(context: Context): String {
        val builder = StringBuilder()
        val exportTime = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
        dateFormat.timeZone = TimeZone.getDefault()

        // Header
        builder.appendLine(HEADER_LINE)
        builder.appendLine("              DEVICE MASKER - IN-MEMORY LOG EXPORT")
        builder.appendLine("                    YukiHookAPI YLog Data")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()

        // Metadata
        appendMetadata(builder, context, "IN-MEMORY LOGS", dateFormat, exportTime)

        // Log Content
        builder.appendLine()
        builder.appendLine("[LOG ENTRIES]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine()
        builder.appendLine("Source: YukiHookAPI YLog.inMemoryData")
        builder.appendLine("Scope: Device Masker app process only")
        builder.appendLine(SUBSECTION_LINE)
        builder.appendLine()

        try {
            val ylogData = YLog.inMemoryData
            if (ylogData.isNotEmpty()) {
                builder.appendLine("Total Entries: ${ylogData.size}")
                builder.appendLine()
                builder.appendLine("TIMESTAMP                    | LVL | MESSAGE")
                builder.appendLine(SECTION_LINE)

                ylogData.forEach { entry ->
                    val level = parseLogLevel(entry.priority.toString())
                    builder.appendLine("${entry.time.padEnd(28)} | $level | ${entry.msg}")

                    entry.throwable?.let { t ->
                        builder.appendLine(
                            "                             |     | Exception: ${t.javaClass.simpleName}: ${t.message}"
                        )
                        t.stackTrace.take(5).forEach { se ->
                            builder.appendLine("                             |     |   at $se")
                        }
                    }
                }
            } else {
                builder.appendLine("(No in-memory logs available)")
                builder.appendLine()
                builder.appendLine("Possible reasons:")
                builder.appendLine("  • App was just launched (no operations performed yet)")
                builder.appendLine("  • Memory was cleared")
            }
        } catch (e: Exception) {
            builder.appendLine("(Failed to read in-memory logs: ${e.message})")
        }

        // Footer
        appendFooter(builder, dateFormat)

        return builder.toString()
    }

    // ═══════════════════════════════════════════════════════════════════
    // SHARED CONTENT BUILDERS
    // ═══════════════════════════════════════════════════════════════════

    private fun appendMetadata(
        builder: StringBuilder,
        context: Context,
        logType: String,
        dateFormat: SimpleDateFormat,
        exportTime: Date,
    ) {
        builder.appendLine("[EXPORT METADATA]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Export Type      : $logType")
        builder.appendLine("Export Time      : ${dateFormat.format(exportTime)}")
        builder.appendLine("Timezone         : ${TimeZone.getDefault().id}")
        builder.appendLine("Log Format       : Device Masker Debug Log v1.0")
        builder.appendLine()

        builder.appendLine("[APPLICATION INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("App Name         : Device Masker")
        builder.appendLine("Package          : ${context.packageName}")
        builder.appendLine(
            "Version          : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        )
        builder.appendLine(
            "Build Type       : ${if (BuildConfig.DEBUG) "DEBUG" else "RELEASE (Beta)"}"
        )
        builder.appendLine()

        builder.appendLine("[DEVICE INFO]")
        builder.appendLine(SECTION_LINE)
        builder.appendLine("Manufacturer     : ${Build.MANUFACTURER}")
        builder.appendLine("Model            : ${Build.MODEL}")
        builder.appendLine("Device           : ${Build.DEVICE}")
        builder.appendLine(
            "Android          : ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )
        builder.appendLine("Security Patch   : ${Build.VERSION.SECURITY_PATCH}")
        builder.appendLine("Build            : ${Build.DISPLAY}")
    }

    private fun appendFooter(builder: StringBuilder, dateFormat: SimpleDateFormat) {
        builder.appendLine()
        builder.appendLine(HEADER_LINE)
        builder.appendLine("                         END OF LOG EXPORT")
        builder.appendLine(HEADER_LINE)
        builder.appendLine()
        builder.appendLine("For support, include this log file in your bug report.")
        builder.appendLine("GitHub: https://github.com/AstrixForge/DeviceMasker")
        builder.appendLine()
        builder.appendLine("Export completed: ${dateFormat.format(Date())}")
    }

    private fun parseLogLevel(priorityStr: String): String {
        return when {
            priorityStr.contains("2") || priorityStr.contains("VERBOSE", true) -> "VRB"
            priorityStr.contains("3") || priorityStr.contains("DEBUG", true) -> "DBG"
            priorityStr.contains("4") || priorityStr.contains("INFO", true) -> "INF"
            priorityStr.contains("5") || priorityStr.contains("WARN", true) -> "WRN"
            priorityStr.contains("6") || priorityStr.contains("ERROR", true) -> "ERR"
            else -> "UNK"
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════════════════

    fun getLogCount(): Int =
        try {
            YLog.inMemoryData.size
        } catch (_: Exception) {
            0
        }

    fun generateLogFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "$LOG_FILE_PREFIX$timestamp$LOG_FILE_EXTENSION"
    }
}

/** Result of log export operation. */
sealed class LogExportResult {
    data class Success(val filePath: String, val lineCount: Int) : LogExportResult()

    data class Error(val message: String) : LogExportResult()
}
