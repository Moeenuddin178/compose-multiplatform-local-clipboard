package org.clipboard.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@Composable
actual fun isSmallScreen(): Boolean {
    // Get the window size using available compose infrastructure
    // iOS devices are typically small screens (iPhone width ~390dp)
    // We'll assume iOS is a small screen for now
    // TODO: Implement proper window size detection if available in Compose Multiplatform iOS
    return true
}

