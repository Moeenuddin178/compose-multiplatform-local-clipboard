package org.clipboard.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Check if the current screen is considered "small" (e.g., phone)
 * Platform-specific implementation - defaults to false in common code
 */
@Composable
expect fun isSmallScreen(): Boolean

/**
 * Get adaptive padding based on screen size
 * @return 12dp for small screens, 20dp for larger screens
 */
@Composable
fun adaptivePadding(): Dp {
    return if (isSmallScreen()) 12.dp else 20.dp
}

/**
 * Get adaptive spacing between elements based on screen size
 * @return 12dp for small screens, 20dp for larger screens
 */
@Composable
fun adaptiveSpacing(): Dp {
    return if (isSmallScreen()) 12.dp else 20.dp
}

/**
 * Get adaptive padding for cards based on screen size
 * @return 12dp for small screens, 20dp for larger screens
 */
@Composable
fun adaptiveCardPadding(): Dp {
    return if (isSmallScreen()) 12.dp else 20.dp
}

/**
 * Get adaptive padding for large empty states
 * @return 16dp for small screens, 32dp for larger screens
 */
@Composable
fun adaptiveLargePadding(): Dp {
    return if (isSmallScreen()) 16.dp else 32.dp
}

/**
 * Get adaptive font size for button text based on screen size
 * @return 13sp for small screens, 14sp for larger screens
 */
@Composable
fun adaptiveButtonTextSize(): TextUnit {
    return if (isSmallScreen()) 13.sp else 14.sp
}

/**
 * Get adaptive horizontal padding for buttons based on screen size
 * @return 12dp for small screens, 16dp for larger screens
 */
@Composable
fun adaptiveButtonPadding(): Dp {
    return if (isSmallScreen()) 12.dp else 16.dp
}

/**
 * Get adaptive spacing between icon and text in buttons based on screen size
 * @return 4dp for small screens, 8dp for larger screens
 */
@Composable
fun adaptiveIconSpacing(): Dp {
    return if (isSmallScreen()) 4.dp else 8.dp
}

