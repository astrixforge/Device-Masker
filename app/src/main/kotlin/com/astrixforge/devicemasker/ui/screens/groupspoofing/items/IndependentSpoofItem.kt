package com.astrixforge.devicemasker.ui.screens.groupspoofing.items

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch

/**
 * Independent spoof item with individual controls.
 *
 * Has its own switch and regenerate button since values don't need to sync. Long-press on value to
 * copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IndependentSpoofItem(
    type: SpoofType,
    value: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpressiveCard(
        onClick = { /* Item click feedback */ },
        onLongClick = { if (isEnabled && value.isNotEmpty()) onCopy() },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row with switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                ExpressiveSwitch(checked = isEnabled, onCheckedChange = { onToggle(it) })
            }

            // Value and regenerate (only when enabled)
            if (isEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Value text
                    Text(
                        text =
                            value.ifEmpty { stringResource(id = R.string.group_spoofing_not_set) },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    // Regenerate button (with expressive feedback)
                    CompactExpressiveIconButton(
                        onClick = onRegenerate,
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.action_regenerate),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
