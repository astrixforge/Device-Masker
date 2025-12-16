package com.astrixforge.devicemasker.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.BuildConfig
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme

/**
 * Settings screen for app preferences.
 *
 * Provides options for:
 * - Theme settings (dark mode, AMOLED dark mode, dynamic colors)
 * - Debug options
 * - About information
 *
 * @param darkMode Whether dark mode is enabled
 * @param amoledDarkMode Whether AMOLED dark mode is enabled (pure black)
 * @param dynamicColors Whether dynamic colors are enabled
 * @param debugLogging Whether debug logging is enabled
 * @param onDarkModeChange Callback when dark mode preference changes
 * @param onAmoledDarkModeChange Callback when AMOLED dark mode preference changes
 * @param onDynamicColorChange Callback when dynamic color preference changes
 * @param onDebugLogChange Callback when debug logging preference changes
 * @param onNavigateToDiagnostics Callback to navigate to diagnostics screen
 * @param modifier Optional modifier
 */
@Composable
fun SettingsScreen(
        darkMode: Boolean = true,
        amoledDarkMode: Boolean = true,
        dynamicColors: Boolean = true,
        debugLogging: Boolean = false,
        onDarkModeChange: (Boolean) -> Unit = {},
        onAmoledDarkModeChange: (Boolean) -> Unit = {},
        onDynamicColorChange: (Boolean) -> Unit = {},
        onDebugLogChange: (Boolean) -> Unit = {},
        onNavigateToDiagnostics: () -> Unit = {},
        modifier: Modifier = Modifier
) {
        var currentDarkMode by remember { mutableStateOf(darkMode) }
        var currentAmoledDarkMode by remember { mutableStateOf(amoledDarkMode) }
        var currentDynamicColors by remember { mutableStateOf(dynamicColors) }
        var currentDebugLogging by remember { mutableStateOf(debugLogging) }

        LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                // Header
                item {
                        Text(
                                text = "Settings",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                        )
                }

                // Appearance Section
                item {
                        SettingsSection(title = "Appearance") {
                                // Dark Mode Toggle
                                SettingsSwitchItem(
                                        icon =
                                                if (currentDarkMode) Icons.Outlined.DarkMode
                                                else Icons.Outlined.LightMode,
                                        title = "Dark Mode",
                                        description =
                                                if (currentDarkMode) "Dark theme enabled"
                                                else "Light theme enabled",
                                        checked = currentDarkMode,
                                        onCheckedChange = {
                                                currentDarkMode = it
                                                onDarkModeChange(it)
                                                // If dark mode is disabled, also disable AMOLED
                                                if (!it) {
                                                        currentAmoledDarkMode = false
                                                        onAmoledDarkModeChange(false)
                                                }
                                        }
                                )

                                // AMOLED Dark Mode (only visible when dark mode is enabled)
                                AnimatedVisibility(
                                        visible = currentDarkMode,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                ) {
                                        Column {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                SettingsSwitchItem(
                                                        icon = Icons.Outlined.Contrast,
                                                        title = "AMOLED Dark Mode",
                                                        description =
                                                                "Pure black background for OLED displays",
                                                        checked = currentAmoledDarkMode,
                                                        onCheckedChange = {
                                                                currentAmoledDarkMode = it
                                                                onAmoledDarkModeChange(it)
                                                        }
                                                )
                                        }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        SettingsSwitchItem(
                                                icon = Icons.Outlined.Palette,
                                                title = "Dynamic Colors",
                                                description =
                                                        "Use Material You colors from your wallpaper",
                                                checked = currentDynamicColors,
                                                onCheckedChange = {
                                                        currentDynamicColors = it
                                                        onDynamicColorChange(it)
                                                }
                                        )
                                }
                        }
                }

                // Advanced Section
                item {
                        SettingsSection(title = "Advanced") {
                                SettingsSwitchItem(
                                        icon = Icons.Outlined.BugReport,
                                        title = "Debug Logging",
                                        description = "Enable verbose logging for troubleshooting",
                                        checked = currentDebugLogging,
                                        onCheckedChange = {
                                                currentDebugLogging = it
                                                onDebugLogChange(it)
                                        }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingsClickableItem(
                                        icon = Icons.Outlined.Shield,
                                        title = "Diagnostics",
                                        description =
                                                "Test spoofing effectiveness and anti-detection",
                                        onClick = onNavigateToDiagnostics
                                )
                        }
                }

                // About Section
                item {
                        SettingsSection(title = "About") {
                                SettingsInfoItem(
                                        icon = Icons.Outlined.Info,
                                        title = "Version",
                                        value =
                                                "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingsInfoItem(
                                        icon = Icons.Outlined.Code,
                                        title = "Build Type",
                                        value = if (BuildConfig.DEBUG) "Debug" else "Release"
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SettingsClickableItem(
                                        icon = Icons.Outlined.Tune,
                                        title = "Module Info",
                                        description = "YukiHookAPI 1.3.1 • LSPosed Module",
                                        onClick = { /* Open module info */}
                                )
                        }
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(24.dp)) }
        }
}

/** Settings section with title and content. */
@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                )

                ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                                CardDefaults.elevatedCardColors(
                                        containerColor =
                                                MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                        shape = MaterialTheme.shapes.large
                ) { Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) { content() } }
        }
}

/** Settings item with switch toggle. */
@Composable
private fun SettingsSwitchItem(
        icon: ImageVector,
        title: String,
        description: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                        )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
}

/** Settings item with read-only info. */
@Composable
private fun SettingsInfoItem(icon: ImageVector, title: String, value: String) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                        )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
        }
}

/** Settings item with click action. */
@Composable
private fun SettingsClickableItem(
        icon: ImageVector,
        title: String,
        description: String,
        onClick: () -> Unit
) {
        Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
        ) {
                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                        )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                        Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }

                Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SettingsScreenPreview() {
        DeviceMaskerTheme { SettingsScreen() }
}
