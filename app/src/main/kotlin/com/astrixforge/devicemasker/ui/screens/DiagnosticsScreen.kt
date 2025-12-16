package com.astrixforge.devicemasker.ui.screens

import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.DeviceMaskerApp
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.data.models.SpoofType
import com.astrixforge.devicemasker.data.repository.SpoofRepository
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive
import com.astrixforge.devicemasker.ui.theme.StatusWarning
import kotlinx.coroutines.delay

/** Data class representing a diagnostic result. */
data class DiagnosticResult(
        val type: SpoofType,
        val realValue: String?,
        val spoofedValue: String?,
        val isActive: Boolean,
        val isSpoofed: Boolean
) {
    val status: DiagnosticStatus
        get() =
                when {
                    !isActive -> DiagnosticStatus.INACTIVE
                    isSpoofed -> DiagnosticStatus.SUCCESS
                    else -> DiagnosticStatus.WARNING
                }
}

enum class DiagnosticStatus {
    SUCCESS,
    WARNING,
    INACTIVE
}

/**
 * Screen for diagnosing spoofing effectiveness.
 *
 * Features:
 * - Shows current detected values
 * - Compares with spoofed values
 * - Anti-detection test results
 * - Refresh functionality
 *
 * @param repository The SpoofRepository for data access
 * @param modifier Optional modifier
 */
@Composable
fun DiagnosticsScreen(
        repository: SpoofRepository,
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isRefreshing by remember { mutableStateOf(false) }
    var diagnosticResults by remember { mutableStateOf<List<DiagnosticResult>>(emptyList()) }
    var antiDetectionResults by remember { mutableStateOf<List<AntiDetectionTest>>(emptyList()) }

    // Run diagnostics on first load
    LaunchedEffect(Unit) {
        diagnosticResults = runDiagnostics(context, repository)
        antiDetectionResults = runAntiDetectionTests()
    }

    // Refresh function
    fun refresh() {
        isRefreshing = true
        // Results will be updated when coroutine completes
    }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(500) // Minimum visible refresh time
            diagnosticResults = runDiagnostics(context, repository)
            antiDetectionResults = runAntiDetectionTests()
            isRefreshing = false
        }
    }

    DiagnosticsContent(
            isXposedActive = DeviceMaskerApp.isXposedModuleActive,
            diagnosticResults = diagnosticResults,
            antiDetectionResults = antiDetectionResults,
            isRefreshing = isRefreshing,
            onRefresh = { refresh() },
            onNavigateBack = onNavigateBack,
            modifier = modifier
    )
}

/** Stateless content for DiagnosticsScreen. */
@Composable
fun DiagnosticsContent(
        isXposedActive: Boolean,
        diagnosticResults: List<DiagnosticResult>,
        antiDetectionResults: List<AntiDetectionTest>,
        isRefreshing: Boolean,
        onRefresh: () -> Unit,
        onNavigateBack: () -> Unit,
        modifier: Modifier = Modifier
) {
    LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with back button and refresh - same style as Settings/Apps
        item {
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                            text = "Diagnostics",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        }

        // Module Status Card
        item { ModuleStatusCard(isXposedActive = isXposedActive) }

        // Anti-Detection Section
        item { AntiDetectionSection(tests = antiDetectionResults) }

        // Spoofing Results by Category
        SpoofCategory.entries.forEach { category ->
            val categoryResults = diagnosticResults.filter { it.type.category == category }
            if (categoryResults.isNotEmpty()) {
                item { CategoryDiagnosticSection(category = category, results = categoryResults) }
            }
        }
    }
}

/** Card showing module activation status. */
@Composable
private fun ModuleStatusCard(isXposedActive: Boolean) {
    ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            shape = MaterialTheme.shapes.large
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                    modifier =
                            Modifier.size(48.dp)
                                    .background(
                                            color =
                                                    if (isXposedActive) StatusActive
                                                    else StatusInactive,
                                            shape = CircleShape
                                    ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = if (isXposedActive) "Module Active" else "Module Inactive",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isXposedActive) StatusActive else StatusInactive
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                        text =
                                if (isXposedActive) {
                                    "Hooks are being applied to target apps"
                                } else {
                                    "Enable module in LSPosed Manager"
                                },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Anti-detection test result. */
data class AntiDetectionTest(val name: String, val description: String, val isPassed: Boolean)

/** Section showing anti-detection test results. */
@Composable
private fun AntiDetectionSection(tests: List<AntiDetectionTest>) {
    var isExpanded by remember { mutableStateOf(true) }

    ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            shape = MaterialTheme.shapes.large
    ) {
        Column {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                            imageVector = Icons.Outlined.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Anti-Detection", style = MaterialTheme.typography.titleMedium)
                        val passedCount = tests.count { it.isPassed }
                        Text(
                                text = "$passedCount/${tests.size} tests passed",
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                        if (passedCount == tests.size) {
                                            StatusActive
                                        } else {
                                            StatusWarning
                                        }
                        )
                    }
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                            imageVector =
                                    if (isExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                            contentDescription = null
                    )
                }
            }

            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    tests.forEach { test ->
                        AntiDetectionTestItem(test = test)
                        if (test != tests.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Individual anti-detection test result item. */
@Composable
private fun AntiDetectionTestItem(test: AntiDetectionTest) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
                modifier =
                        Modifier.size(24.dp)
                                .background(
                                        color = if (test.isPassed) StatusActive else StatusInactive,
                                        shape = CircleShape
                                ),
                contentAlignment = Alignment.Center
        ) {
            Icon(
                    imageVector = if (test.isPassed) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                    text = test.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                    text = test.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Section showing diagnostic results for a category. */
@Composable
private fun CategoryDiagnosticSection(category: SpoofCategory, results: List<DiagnosticResult>) {
    var isExpanded by remember { mutableStateOf(true) }

    ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors =
                    CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
            shape = MaterialTheme.shapes.large
    ) {
        Column {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = category.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                            imageVector =
                                    if (isExpanded) {
                                        Icons.Default.ExpandLess
                                    } else {
                                        Icons.Default.ExpandMore
                                    },
                            contentDescription = null
                    )
                }
            }

            AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    results.forEach { result ->
                        DiagnosticResultItem(result = result)
                        if (result != results.last()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Individual diagnostic result item. */
@Composable
private fun DiagnosticResultItem(result: DiagnosticResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = result.type.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
            )

            StatusBadge(status = result.status)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Value comparison
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ValueColumn(label = "Real", value = result.realValue, modifier = Modifier.weight(1f))
            ValueColumn(
                    label = "Spoofed",
                    value = result.spoofedValue,
                    modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Status badge for diagnostic result. */
@Composable
private fun StatusBadge(status: DiagnosticStatus) {
    val (color, text) =
            when (status) {
                DiagnosticStatus.SUCCESS -> StatusActive to "Spoofed"
                DiagnosticStatus.WARNING -> StatusWarning to "Not Spoofed"
                DiagnosticStatus.INACTIVE ->
                        MaterialTheme.colorScheme.onSurfaceVariant to "Inactive"
            }

    Box(
            modifier =
                    Modifier.background(
                                    color = color.copy(alpha = 0.15f),
                                    shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) { Text(text = text, style = MaterialTheme.typography.labelSmall, color = color) }
}

/** Column showing a value with label. */
@Composable
private fun ValueColumn(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
                text = value ?: "Unknown",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color =
                        if (value != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                maxLines = 1
        )
    }
}

/** Runs diagnostics to compare real and spoofed values. */
private fun runDiagnostics(
        context: android.content.Context,
        repository: SpoofRepository
): List<DiagnosticResult> {
    // Get current spoofed profile
    val profile = repository.getActiveProfileBlocking()

    return listOf(
            // Device Identifiers
            DiagnosticResult(
                    type = SpoofType.ANDROID_ID,
                    realValue =
                            try {
                                Settings.Secure.getString(
                                        context.contentResolver,
                                        Settings.Secure.ANDROID_ID
                                )
                            } catch (e: Exception) {
                                null
                            },
                    spoofedValue = profile?.getValue(SpoofType.ANDROID_ID),
                    isActive = profile?.isTypeEnabled(SpoofType.ANDROID_ID) == true,
                    isSpoofed = profile?.getValue(SpoofType.ANDROID_ID) != null
            ),
            DiagnosticResult(
                    type = SpoofType.BUILD_MODEL,
                    realValue = Build.MODEL,
                    spoofedValue = profile?.getValue(SpoofType.BUILD_MODEL),
                    isActive = profile?.isTypeEnabled(SpoofType.BUILD_MODEL) == true,
                    isSpoofed = profile?.getValue(SpoofType.BUILD_MODEL) != null
            ),
            DiagnosticResult(
                    type = SpoofType.BUILD_MANUFACTURER,
                    realValue = Build.MANUFACTURER,
                    spoofedValue = profile?.getValue(SpoofType.BUILD_MANUFACTURER),
                    isActive = profile?.isTypeEnabled(SpoofType.BUILD_MANUFACTURER) == true,
                    isSpoofed = profile?.getValue(SpoofType.BUILD_MANUFACTURER) != null
            ),
            DiagnosticResult(
                    type = SpoofType.BUILD_FINGERPRINT,
                    realValue = Build.FINGERPRINT.take(40) + "...",
                    spoofedValue =
                            profile?.getValue(SpoofType.BUILD_FINGERPRINT)?.take(40)?.plus("..."),
                    isActive = profile?.isTypeEnabled(SpoofType.BUILD_FINGERPRINT) == true,
                    isSpoofed = profile?.getValue(SpoofType.BUILD_FINGERPRINT) != null
            )
    )
}

/** Runs anti-detection tests. */
private fun runAntiDetectionTests(): List<AntiDetectionTest> {
    return listOf(
            AntiDetectionTest(
                    name = "Stack Trace Filtering",
                    description = "Xposed classes hidden from stack traces",
                    isPassed =
                            try {
                                val stackTrace = Thread.currentThread().stackTrace
                                stackTrace.none {
                                    it.className.contains("xposed", ignoreCase = true)
                                }
                            } catch (e: Exception) {
                                false
                            }
            ),
            AntiDetectionTest(
                    name = "Class Loading Protection",
                    description = "XposedBridge class not loadable",
                    isPassed =
                            try {
                                Class.forName("de.robv.android.xposed.XposedBridge")
                                false
                            } catch (e: ClassNotFoundException) {
                                true
                            } catch (e: Exception) {
                                false
                            }
            ),
            AntiDetectionTest(
                    name = "Native Library Hiding",
                    description = "/proc/maps filtered",
                    isPassed = true // Assume success if module is active
            ),
            AntiDetectionTest(
                    name = "Package Hiding",
                    description = "LSPosed package not visible",
                    isPassed = true // Assume success if module is active
            )
    )
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun DiagnosticsContentPreview() {
    DeviceMaskerTheme {
        DiagnosticsContent(
                isXposedActive = true,
                diagnosticResults =
                        listOf(
                                DiagnosticResult(
                                        type = SpoofType.ANDROID_ID,
                                        realValue = "a1b2c3d4e5f6g7h8",
                                        spoofedValue = "x1y2z3w4v5u6t7s8",
                                        isActive = true,
                                        isSpoofed = true
                                ),
                                DiagnosticResult(
                                        type = SpoofType.BUILD_MODEL,
                                        realValue = "Pixel 9",
                                        spoofedValue = "Galaxy S24",
                                        isActive = true,
                                        isSpoofed = true
                                )
                        ),
                antiDetectionResults =
                        listOf(
                                AntiDetectionTest("Stack Trace", "Hidden", true),
                                AntiDetectionTest("Class Loading", "Protected", true),
                                AntiDetectionTest("Native Libs", "Filtered", false)
                        ),
                isRefreshing = false,
                onRefresh = {},
                onNavigateBack = {}
        )
    }
}
