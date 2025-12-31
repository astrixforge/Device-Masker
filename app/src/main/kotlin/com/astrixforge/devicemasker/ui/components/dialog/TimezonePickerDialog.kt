package com.astrixforge.devicemasker.ui.components.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.util.TimeZone

/**
 * Data class representing a timezone entry for the picker.
 */
data class TimezoneEntry(
    val id: String,
    val displayName: String,
    val offset: String,
    val region: String,
) {
    companion object {
        /** All available timezones grouped and sorted. */
        val ALL: List<TimezoneEntry> by lazy {
            TimeZone.getAvailableIDs()
                .filter { it.contains("/") } // Only region-based timezones
                .map { id ->
                    val tz = TimeZone.getTimeZone(id)
                    val offsetMinutes = tz.rawOffset / 60000
                    val hours = offsetMinutes / 60
                    val minutes = kotlin.math.abs(offsetMinutes % 60)
                    val offsetStr = String.format(
                        "GMT%s%02d:%02d",
                        if (hours >= 0) "+" else "",
                        hours,
                        minutes
                    )
                    val region = id.substringBefore("/")
                    val city = id.substringAfter("/").replace("_", " ")
                    TimezoneEntry(
                        id = id,
                        displayName = city,
                        offset = offsetStr,
                        region = region,
                    )
                }
                .sortedWith(compareBy({ it.region }, { it.displayName }))
        }

        /** Search timezones by query (matches id, display name, or offset). */
        fun search(query: String): List<TimezoneEntry> {
            if (query.isBlank()) return ALL
            val lowerQuery = query.lowercase()
            return ALL.filter { tz ->
                tz.id.lowercase().contains(lowerQuery) ||
                    tz.displayName.lowercase().contains(lowerQuery) ||
                    tz.offset.lowercase().contains(lowerQuery) ||
                    tz.region.lowercase().contains(lowerQuery)
            }
        }
    }
}

/**
 * Searchable dialog for selecting a timezone.
 *
 * @param selectedTimezone Currently selected timezone ID (e.g., "America/New_York")
 * @param onTimezoneSelected Called when a timezone is selected
 * @param onDismiss Called when dialog is dismissed
 */
@Composable
fun TimezonePickerDialog(
    selectedTimezone: String?,
    onTimezoneSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery) { TimezoneEntry.search(searchQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select Timezone", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search timezones...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                    },
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timezone list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(filteredTimezones, key = { it.id }) { timezone ->
                        val isSelected = timezone.id == selectedTimezone

                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { onTimezoneSelected(timezone.id) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = timezone.displayName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight =
                                            if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = timezone.offset,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = timezone.region,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (timezone != filteredTimezones.lastOrNull()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        },
    )
}
