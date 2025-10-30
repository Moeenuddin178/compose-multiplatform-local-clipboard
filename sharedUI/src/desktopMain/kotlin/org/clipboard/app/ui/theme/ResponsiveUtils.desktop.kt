package org.clipboard.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

@Composable
actual fun isSmallScreen(): Boolean {
    // Desktop screens are typically large
    return false
}

