package org.clipboard.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import org.clipboard.app.ui.theme.adaptiveSpacing

@Composable
fun SectionHeader(title: String) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = adaptiveSpacing() / 2)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(modifier = Modifier.height(adaptiveSpacing() / 3))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .drawBehind { drawRect(color = dividerColor) }
        )
    }
}



