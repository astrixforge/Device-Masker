package com.astrixforge.devicemasker.ui.screens.groupspoofing.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.R
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.ui.components.dialog.TimezonePickerDialog
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveOutlinedCard
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveSwitch
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.IndependentSpoofItem

/**
 * Content layout for Location category - mirrors SIM Card design pattern exactly.
 *
 * UI Structure:
 * 1. Location Selection Card - "Choose Location" switch, Timezone picker, Locale display
 * 2. Latitude/Longitude - Each has its own Switch + Regenerate (fully independent)
 *
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationCategoryContent(
    group: SpoofGroup?,
    onToggle: (SpoofType, Boolean) -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    @Suppress("UNUSED_PARAMETER") // Parameter kept for interface compatibility
    onRegenerateLocation: () -> Unit,
    onTimezoneSelected: (String) -> Unit,
    onCopy: (String) -> Unit,
) {
    var showTimezoneDialog by remember { mutableStateOf(false) }

    val timezoneEnabled = group?.isTypeEnabled(SpoofType.TIMEZONE) ?: false
    val timezoneValue = group?.getValue(SpoofType.TIMEZONE) ?: ""
    val localeValue = group?.getValue(SpoofType.LOCALE) ?: ""
    val latEnabled = group?.isTypeEnabled(SpoofType.LOCATION_LATITUDE) ?: false
    val latValue = group?.getValue(SpoofType.LOCATION_LATITUDE) ?: ""
    val longEnabled = group?.isTypeEnabled(SpoofType.LOCATION_LONGITUDE) ?: false
    val longValue = group?.getValue(SpoofType.LOCATION_LONGITUDE) ?: ""

    // Timezone Picker Dialog
    if (showTimezoneDialog) {
        TimezonePickerDialog(
            selectedTimezone = timezoneValue,
            onTimezoneSelected = { timezone ->
                onTimezoneSelected(timezone)
                showTimezoneDialog = false
            },
            onDismiss = { showTimezoneDialog = false },
        )
    }

    // ═══════════════════════════════════════════════════════════
    // 1. LOCATION SELECTION CARD (100% same as SIM Card carrier selection)
    // ═══════════════════════════════════════════════════════════
    ExpressiveCard(
        onClick = { /* Selection action feedback */ },
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Choose Location Switch (same as Choose Sim)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.label_choose_location),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                ExpressiveSwitch(
                    checked = timezoneEnabled,
                    onCheckedChange = { enabled ->
                        // Toggle timezone AND locale together
                        onToggle(SpoofType.TIMEZONE, enabled)
                        onToggle(SpoofType.LOCALE, enabled)
                    },
                )
            }

            // Collapsible content for Timezone and Locale
            AnimatedVisibility(
                visible = timezoneEnabled,
                enter = expandVertically(animationSpec = spring()) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring()) + fadeOut(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Timezone picker row (same as Country picker row)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Timezone", style = MaterialTheme.typography.bodyMedium)

                        // Timezone button - opens dialog (same width as Country button: 200.dp)
                        ExpressiveOutlinedCard(
                            enabled = timezoneEnabled,
                            onClick = { showTimezoneDialog = true },
                            onLongClick = { if (timezoneValue.isNotEmpty()) onCopy(timezoneValue) },
                            modifier = Modifier.width(200.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Centered: Timezone display
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = timezoneValue.ifEmpty {
                                            stringResource(id = R.string.group_spoofing_not_set)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                // Right arrow indicator
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Locale row (same as Carrier row, but read-only)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Locale", style = MaterialTheme.typography.bodyMedium)

                        // Locale display (same style as Carrier dropdown, but disabled/read-only)
                        ExpressiveOutlinedCard(
                            enabled = false, // Read-only
                            onClick = { /* No action - read only */ },
                            onLongClick = { if (localeValue.isNotEmpty()) onCopy(localeValue) },
                            modifier = Modifier.width(200.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = localeValue.ifEmpty {
                                        stringResource(id = R.string.group_spoofing_not_set)
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 2. LATITUDE - independent with own switch and regenerate
    // ═══════════════════════════════════════════════════════════
    IndependentSpoofItem(
        type = SpoofType.LOCATION_LATITUDE,
        value = latValue,
        isEnabled = latEnabled,
        onToggle = { enabled -> onToggle(SpoofType.LOCATION_LATITUDE, enabled) },
        onRegenerate = { onRegenerate(SpoofType.LOCATION_LATITUDE) },
        onCopy = { onCopy(latValue) },
        modifier = Modifier.fillMaxWidth(),
    )

    // ═══════════════════════════════════════════════════════════
    // 3. LONGITUDE - independent with own switch and regenerate
    // ═══════════════════════════════════════════════════════════
    IndependentSpoofItem(
        type = SpoofType.LOCATION_LONGITUDE,
        value = longValue,
        isEnabled = longEnabled,
        onToggle = { enabled -> onToggle(SpoofType.LOCATION_LONGITUDE, enabled) },
        onRegenerate = { onRegenerate(SpoofType.LOCATION_LONGITUDE) },
        onCopy = { onCopy(longValue) },
        modifier = Modifier.fillMaxWidth(),
    )
}
