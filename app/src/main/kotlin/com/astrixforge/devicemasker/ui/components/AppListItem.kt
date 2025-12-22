package com.astrixforge.devicemasker.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.data.models.InstalledApp
import com.astrixforge.devicemasker.ui.theme.DeviceMaskerTheme
import com.astrixforge.devicemasker.utils.ImageUtils

/**
 * Individual app item for app selection lists.
 *
 * Displays app icon, name, package name, and selection state.
 * Supports disabled state when app is assigned to another group.
 *
 * @param app The installed app info
 * @param isAssigned Whether the app is assigned to the current group
 * @param assignedToOtherGroupName If not null, the app is assigned to another group
 * @param onToggle Callback when the assignment state changes
 * @param modifier Optional modifier
 */
@Composable
fun AppListItem(
    app: InstalledApp,
    isAssigned: Boolean,
    assignedToOtherGroupName: String?,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isDisabled = assignedToOtherGroupName != null

    // Load real app icon from PackageManager
    val appIcon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }

    ExpressiveCard(
        onClick = { onToggle(!isAssigned) },
        modifier = modifier.alpha(if (isDisabled) 0.6f else 1f),
        isSelected = isAssigned,
        enabled = !isDisabled,
        shape = MaterialTheme.shapes.small,
        containerColor = MaterialTheme.colorScheme.surface,
        selectionColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Real app icon or fallback
            if (appIcon != null) {
                val bitmap = remember(appIcon) { ImageUtils.drawableToBitmap(appIcon) }
                if (bitmap != null) {
                    Image(
                        painter = BitmapPainter(bitmap.asImageBitmap()),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                } else {
                    AppIconFallback()
                }
            } else {
                AppIconFallback()
            }

            // App info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isDisabled) {
                        stringResource(
                            id = R.string.group_spoofing_assigned_to,
                            assignedToOtherGroupName ?: ""
                        )
                    } else {
                        app.packageName
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDisabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Checkbox or lock badge
            if (isDisabled) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = stringResource(id = R.string.group_spoofing_locked),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            } else {
                Checkbox(checked = isAssigned, onCheckedChange = onToggle)
            }
        }
    }
}

/**
 * Fallback icon when app icon cannot be loaded.
 */
@Composable
fun AppIconFallback(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Android,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppListItemPreview() {
    DeviceMaskerTheme {
        AppListItem(
            app = InstalledApp(
                packageName = "com.example.app",
                label = "Example App",
                isSystemApp = false,
            ),
            isAssigned = false,
            assignedToOtherGroupName = null,
            onToggle = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppListItemAssignedPreview() {
    DeviceMaskerTheme {
        AppListItem(
            app = InstalledApp(
                packageName = "com.example.app",
                label = "Example App",
                isSystemApp = false,
            ),
            isAssigned = true,
            assignedToOtherGroupName = null,
            onToggle = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun AppListItemLockedPreview() {
    DeviceMaskerTheme {
        AppListItem(
            app = InstalledApp(
                packageName = "com.example.app",
                label = "Example App",
                isSystemApp = false,
            ),
            isAssigned = false,
            assignedToOtherGroupName = "Work Group",
            onToggle = {},
        )
    }
}
