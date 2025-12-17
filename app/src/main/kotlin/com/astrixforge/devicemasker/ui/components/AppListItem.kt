package com.astrixforge.devicemasker.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.ui.theme.StatusActive
import com.astrixforge.devicemasker.ui.theme.StatusInactive

/**
 * List item displaying an installed app with selection checkbox.
 *
 * Shows app icon, name, package name, and spoofing status. Used in AppSelectionScreen for
 * enabling/disabling per-app spoofing.
 *
 * @param app The installed app data
 * @param isSelected Whether the app is selected for spoofing
 * @param onSelectionChange Callback when selection changes
 * @param onClick Callback when the item is clicked (for details)
 * @param icon Optional app icon drawable
 * @param modifier Optional modifier
 */
@Composable
fun AppListItem(
    app: InstalledApp,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    icon: Drawable? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Checkbox
            Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)

            Spacer(modifier = Modifier.width(8.dp))

            // App Icon
            AppIconImage(icon = icon, modifier = Modifier.size(48.dp))

            Spacer(modifier = Modifier.width(16.dp))

            // App Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SystemAppBadge()
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (app.isConfigured) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = app.config?.summary() ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (app.isSpoofEnabled) {
                                StatusActive
                            } else {
                                StatusInactive
                            },
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Indicator
            StatusIndicator(
                status = if (isSelected) StatusState.ACTIVE else StatusState.INACTIVE,
                size = 10.dp,
            )
        }
    }
}

/** Compact app list item for dense layouts. */
@Composable
fun CompactAppListItem(
    app: InstalledApp,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    icon: Drawable? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onSelectionChange(!isSelected) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = isSelected, onCheckedChange = onSelectionChange)

        Spacer(modifier = Modifier.width(8.dp))

        AppIconImage(icon = icon, modifier = Modifier.size(36.dp))

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (app.isSystemApp) {
            SystemAppBadge()
        }
    }
}

/** Displays app icon or fallback. */
@Composable
private fun AppIconImage(icon: Drawable?, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

/** Badge indicating a system app. */
@Composable
private fun SystemAppBadge(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "System",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppListItemSelectedPreview() {
    DeviceMaskerTheme {
        AppListItem(
            app =
                InstalledApp(
                    packageName = "com.example.app",
                    label = "Example App",
                    isSystemApp = false,
                    versionName = "1.0.0",
                ),
            isSelected = true,
            onSelectionChange = {},
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppListItemSystemPreview() {
    DeviceMaskerTheme {
        AppListItem(
            app =
                InstalledApp(
                    packageName = "com.android.settings",
                    label = "Settings",
                    isSystemApp = true,
                    versionName = "14.0",
                ),
            isSelected = false,
            onSelectionChange = {},
            onClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun CompactAppListItemPreview() {
    DeviceMaskerTheme {
        CompactAppListItem(
            app = InstalledApp(packageName = "com.example.app", label = "Example App"),
            isSelected = true,
            onSelectionChange = {},
        )
    }
}
