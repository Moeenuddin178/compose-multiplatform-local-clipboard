package org.clipboard.app.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.clipboard.app.database.entities.ClipboardHistoryEntity
import org.clipboard.app.ui.components.NeumorphicSurface
import org.clipboard.app.ui.theme.isSmallScreen

@Composable
fun HistoryListItem(
    item: ClipboardHistoryEntity,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val rowHeight = if (isSmallScreen()) 56.dp else 64.dp

    NeumorphicSurface(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .semantics {
                contentDescription = item.text.take(60) + 
                    (if (item.text.length > 60) "…" else "") + 
                    ", from " + item.sourceDeviceName
                role = Role.Button
            },
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Leading concave avatar/icon well
            NeumorphicSurface(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                cornerRadius = 18.dp,
                pressed = true // concave look
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.text.take(100) + if (item.text.length > 100) "..." else "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.sourceDeviceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Open details" }
            ) {
                Text("›", style = MaterialTheme.typography.titleMedium)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Delete item" }
            ) {
                Text("🗑️", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}


