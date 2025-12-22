package com.astrixforge.devicemasker.ui.screens.groupspoofing.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.ui.components.expressive.CompactExpressiveIconButton
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.IndependentSpoofItem

/**
 * Special content layout for Location category.
 *
 * - Timezone + Locale: Single card, single switch. Regenerate button updates both.
 * - Latitude/Longitude: Each has its own Switch + Regenerate (fully independent)
 *
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationCategoryContent(
    group: SpoofGroup?,
    onToggle: (SpoofType, Boolean) -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    onRegenerateLocation: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val timezoneEnabled = group?.isTypeEnabled(SpoofType.TIMEZONE) ?: false
    val timezoneValue = group?.getValue(SpoofType.TIMEZONE) ?: ""
    val localeValue = group?.getValue(SpoofType.LOCALE) ?: ""
    val latEnabled = group?.isTypeEnabled(SpoofType.LOCATION_LATITUDE) ?: false
    val latValue = group?.getValue(SpoofType.LOCATION_LATITUDE) ?: ""
    val longEnabled = group?.isTypeEnabled(SpoofType.LOCATION_LONGITUDE) ?: false
    val longValue = group?.getValue(SpoofType.LOCATION_LONGITUDE) ?: ""

    // 1. Timezone + Locale combined card - one switch controls both
    ExpressiveCard(
        onClick = { /* Combined setting feedback */ },
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header with switch - controls both Timezone AND Locale
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = SpoofType.TIMEZONE.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                ExpressiveSwitch(
                    checked = timezoneEnabled,
                    onCheckedChange = { enabled ->
                        // Toggle timezone AND locale together
                        onToggle(SpoofType.TIMEZONE, enabled)
                        onToggle(SpoofType.LOCALE, enabled)
                    }
                )
            }

            if (timezoneEnabled) {
                // Timezone value row - long press to copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timezoneValue.ifEmpty { stringResource(id = R.string.group_spoofing_not_set) },
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { if (timezoneValue.isNotEmpty()) onCopy(timezoneValue) }
                            ),
                    )

                    // Regenerate Timezone + Locale together from same country
                    CompactExpressiveIconButton(
                        onClick = onRegenerateLocation,
                        icon = Icons.Filled.Refresh,
                        contentDescription = stringResource(id = R.string.action_regenerate),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                // Locale section with its own header
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = SpoofType.LOCALE.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = localeValue.ifEmpty { stringResource(id = R.string.group_spoofing_not_set) },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.combinedClickable(
                        onClick = { },
                        onLongClick = { if (localeValue.isNotEmpty()) onCopy(localeValue) }
                    ),
                )
            }
        }
    }

    // 2. Latitude - independent with own switch and regenerate
    IndependentSpoofItem(
        type = SpoofType.LOCATION_LATITUDE,
        value = latValue,
        isEnabled = latEnabled,
        onToggle = { enabled -> onToggle(SpoofType.LOCATION_LATITUDE, enabled) },
        onRegenerate = { onRegenerate(SpoofType.LOCATION_LATITUDE) },
        onCopy = { onCopy(latValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // 3. Longitude - independent with own switch and regenerate
    IndependentSpoofItem(
        type = SpoofType.LOCATION_LONGITUDE,
        value = longValue,
        isEnabled = longEnabled,
        onToggle = { enabled -> onToggle(SpoofType.LOCATION_LONGITUDE, enabled) },
        onRegenerate = { onRegenerate(SpoofType.LOCATION_LONGITUDE) },
        onCopy = { onCopy(longValue) },
        modifier = Modifier.fillMaxWidth()
    )
}
