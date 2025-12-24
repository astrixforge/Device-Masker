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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrixforge.devicemasker.common.DeviceProfilePreset
import com.astrixforge.devicemasker.common.SpoofGroup
import com.astrixforge.devicemasker.common.SpoofType
import com.astrixforge.devicemasker.ui.components.expressive.ExpressiveCard
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.IndependentSpoofItem
import com.astrixforge.devicemasker.ui.screens.groupspoofing.items.ReadOnlyValueRow

/**
 * Content layout for Device Hardware category.
 *
 * All 3 items are fully independent with their own Switch + Regenerate:
 * - Device Profile (shows preset name)
 * - IMEI
 * - Serial
 *
 * Long-press on values to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceHardwareCategoryContent(
    group: SpoofGroup?,
    onToggle: (SpoofType, Boolean) -> Unit,
    onRegenerate: (SpoofType) -> Unit,
    @Suppress("UNUSED_PARAMETER") // Parameter kept for interface compatibility
    onRegenerateCategory: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val deviceProfileEnabled = group?.isTypeEnabled(SpoofType.DEVICE_PROFILE) ?: false
    val deviceProfileRawValue = group?.getValue(SpoofType.DEVICE_PROFILE) ?: ""
    val deviceProfileDisplayValue =
        DeviceProfilePreset.findById(deviceProfileRawValue)?.name ?: deviceProfileRawValue
    val imeiEnabled = group?.isTypeEnabled(SpoofType.IMEI) ?: false
    val imeiValue = group?.getValue(SpoofType.IMEI) ?: ""
    val serialEnabled = group?.isTypeEnabled(SpoofType.SERIAL) ?: false
    val serialValue = group?.getValue(SpoofType.SERIAL) ?: ""

    // 1. Device Profile - independent (shows preset name instead of ID)
    val currentPreset = DeviceProfilePreset.findById(deviceProfileRawValue)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IndependentSpoofItem(
            type = SpoofType.DEVICE_PROFILE,
            value = deviceProfileDisplayValue,
            isEnabled = deviceProfileEnabled,
            onToggle = { enabled -> onToggle(SpoofType.DEVICE_PROFILE, enabled) },
            onRegenerate = { onRegenerate(SpoofType.DEVICE_PROFILE) },
            onCopy = { onCopy(deviceProfileDisplayValue) },
            modifier = Modifier.fillMaxWidth()
        )

        // Collapsible Device Info
        AnimatedVisibility(
            visible = deviceProfileEnabled && currentPreset != null,
            enter = expandVertically(animationSpec = spring()) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring()) + fadeOut(),
        ) {
            ExpressiveCard(
                onClick = { /* Info action feedback */ },
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Device Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    currentPreset?.let { preset ->
                        ReadOnlyValueRow(
                            label = "Manufacturer",
                            value = preset.manufacturer,
                            onCopy = { onCopy(preset.manufacturer) })
                        ReadOnlyValueRow(
                            label = "Brand",
                            value = preset.brand,
                            onCopy = { onCopy(preset.brand) })
                        ReadOnlyValueRow(
                            label = "Model",
                            value = preset.model,
                            onCopy = { onCopy(preset.model) })
                        ReadOnlyValueRow(
                            label = "Device",
                            value = preset.device,
                            onCopy = { onCopy(preset.device) })
                        ReadOnlyValueRow(
                            label = "Product",
                            value = preset.product,
                            onCopy = { onCopy(preset.product) })
                        ReadOnlyValueRow(
                            label = "Board",
                            value = preset.board,
                            onCopy = { onCopy(preset.board) })
                        ReadOnlyValueRow(
                            label = "Fingerprint",
                            value = preset.fingerprint,
                            onCopy = { onCopy(preset.fingerprint) })
                        if (preset.securityPatch.isNotBlank()) {
                            ReadOnlyValueRow(
                                label = "Security Patch",
                                value = preset.securityPatch,
                                onCopy = { onCopy(preset.securityPatch) })
                        }
                    }
                }
            }
        }
    }

    // 2. IMEI - independent
    IndependentSpoofItem(
        type = SpoofType.IMEI,
        value = imeiValue,
        isEnabled = imeiEnabled,
        onToggle = { enabled -> onToggle(SpoofType.IMEI, enabled) },
        onRegenerate = { onRegenerate(SpoofType.IMEI) },
        onCopy = { onCopy(imeiValue) },
        modifier = Modifier.fillMaxWidth()
    )

    // 3. Serial - independent
    IndependentSpoofItem(
        type = SpoofType.SERIAL,
        value = serialValue,
        isEnabled = serialEnabled,
        onToggle = { enabled -> onToggle(SpoofType.SERIAL, enabled) },
        onRegenerate = { onRegenerate(SpoofType.SERIAL) },
        onCopy = { onCopy(serialValue) },
        modifier = Modifier.fillMaxWidth()
    )
}
