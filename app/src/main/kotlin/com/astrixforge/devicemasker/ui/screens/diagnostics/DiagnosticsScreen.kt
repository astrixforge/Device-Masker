package com.astrixforge.devicemasker.ui.screens.diagnostics

import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.astrixforge.devicemasker.R
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.astrixforge.devicemasker.data.models.SpoofCategory
import com.astrixforge.devicemasker.ui.components.expressive.AnimatedSection
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressivePullToRefresh
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive
import com.astrixforge.devicemasker.ui.theme.StatusWarning
import com.astrixforge.devicemasker.data.models.SpoofType

/**
 * Screen for diagnosing spoofing effectiveness.
 *
 * Features:
 * - Shows current detected values
 * - Compares with spoofed values
 * - Anti-detection test results
 * - Refresh functionality
 *
 * Uses MVVM architecture with ViewModel for state management.
 *
 * @param viewModel The DiagnosticsViewModel for state management
 * @param onNavigateBack Callback to navigate back
 * @param modifier Optional modifier
 */
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DiagnosticsContent(
        isXposedActive = state.isXposedActive,
        diagnosticResults = state.diagnosticResults,
        antiDetectionResults = state.antiDetectionResults,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refresh() },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
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
    modifier: Modifier = Modifier,
) {
    // Use the reusable ExpressivePullToRefresh component
    ExpressivePullToRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header with back button - refresh is now pull-to-refresh
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.diagnostics_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(id = R.string.diagnostics_pull_refresh),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
}

/** Card showing module activation status. */
@Composable
private fun ModuleStatusCard(isXposedActive: Boolean) {
    ExpressiveCard(
        onClick = { /* Status card click */ },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier.size(48.dp)
                        .background(
                            color = if (isXposedActive) StatusActive else StatusInactive,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isXposedActive) stringResource(id = R.string.module_active) else stringResource(id = R.string.module_inactive),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isXposedActive) StatusActive else StatusInactive,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        if (isXposedActive) {
                            stringResource(id = R.string.diagnostics_module_active_desc)
                        } else {
                            stringResource(id = R.string.diagnostics_module_inactive_desc)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Section showing anti-detection test results.
 * Uses AnimatedSection for spring-based expand/collapse animation.
 */
@Composable
private fun AntiDetectionSection(tests: List<AntiDetectionTest>) {
    var isExpanded by remember { mutableStateOf(true) }
    val passedCount = tests.count { it.isPassed }

    AnimatedSection(
        title = stringResource(id = R.string.diagnostics_anti_detection),
        icon = Icons.Outlined.Security,
        count = pluralStringResource(id = R.plurals.diagnostics_tests_passed, count = passedCount, passedCount, tests.size),
        countColor = if (passedCount == tests.size) StatusActive else StatusWarning,
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
    ) {
        tests.forEach { test ->
            AntiDetectionTestItem(test = test)
            if (test != tests.last()) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        shape = CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (test.isPassed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = stringResource(id = test.nameRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(id = test.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Section showing diagnostic results for a category.
 * Uses AnimatedSection for spring-based expand/collapse animation.
 */
@Composable
private fun CategoryDiagnosticSection(category: SpoofCategory, results: List<DiagnosticResult>) {
    var isExpanded by remember { mutableStateOf(true) }

    AnimatedSection(
        title = category.displayName,
        count = pluralStringResource(id = R.plurals.diagnostics_items_count, count = results.size, results.size),
        isExpanded = isExpanded,
        onExpandChange = { isExpanded = it },
    ) {
        results.forEach { result ->
            DiagnosticResultItem(result = result)
            if (result != results.last()) {
                Spacer(modifier = Modifier.height(12.dp))
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
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = result.type.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            StatusBadge(status = result.status)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Value comparison
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ValueColumn(labelRes = R.string.diagnostics_real_label, value = result.realValue, modifier = Modifier.weight(1f))
            ValueColumn(
                labelRes = R.string.diagnostics_spoofed_label,
                value = result.spoofedValue,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Status badge for diagnostic result. */
@Composable
private fun StatusBadge(status: DiagnosticStatus) {
    val (color, textRes) =
        when (status) {
            DiagnosticStatus.SUCCESS -> StatusActive to R.string.diagnostics_hook_success
            DiagnosticStatus.WARNING -> StatusWarning to R.string.diagnostics_hook_failure
            DiagnosticStatus.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant to R.string.diagnostics_hook_inactive
        }

    Box(
        modifier =
            Modifier.background(
                    color = color.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = stringResource(id = textRes), style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Column showing a value with label. */
@Composable
private fun ValueColumn(labelRes: Int, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(id = labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value ?: stringResource(id = R.string.diagnostics_unknown),
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color =
                if (value != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            maxLines = 1,
        )
    }
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
                        isSpoofed = true,
                    ),
                    DiagnosticResult(
                        type = SpoofType.DEVICE_PROFILE,
                        realValue = "Google Pixel 9",
                        spoofedValue = "Samsung Galaxy S24",
                        isActive = true,
                        isSpoofed = true,
                    ),
                ),
            antiDetectionResults =
                listOf(
                    AntiDetectionTest(R.string.diagnostics_test_stack_trace, R.string.diagnostics_test_stack_trace_desc, true),
                    AntiDetectionTest(R.string.diagnostics_test_class_loading, R.string.diagnostics_test_class_loading_desc, true),
                    AntiDetectionTest(R.string.diagnostics_test_native_hiding, R.string.diagnostics_test_native_hiding_desc, false),
                ),
            isRefreshing = false,
            onRefresh = {},
            onNavigateBack = {},
        )
    }
}
