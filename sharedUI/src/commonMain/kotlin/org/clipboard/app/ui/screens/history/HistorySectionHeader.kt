package org.clipboard.app.ui.screens.history

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import org.clipboard.app.ui.theme.adaptiveSpacing

@Composable
fun HistorySectionHeader(title: String) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveSpacing() / 2, vertical = adaptiveSpacing() / 4)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(adaptiveSpacing() / 2))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind {
                    drawRect(color = dividerColor)
                }
        )
    }
}


