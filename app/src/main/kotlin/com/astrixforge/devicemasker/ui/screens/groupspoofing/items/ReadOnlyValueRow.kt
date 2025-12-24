package com.astrixforge.devicemasker.ui.screens.groupspoofing.items

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Read-only value row for locked/derived values.
 * No switch, no regenerate - just label, value, and long-press to copy.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReadOnlyValueRow(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp) // Align with first line of monospace text
        )
        Text(
            text = value.ifEmpty { "—" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
                .combinedClickable(
                    onClick = { },
                    onLongClick = { if (value.isNotEmpty()) onCopy() }
                ),
            textAlign = TextAlign.End,
        )
    }
}
