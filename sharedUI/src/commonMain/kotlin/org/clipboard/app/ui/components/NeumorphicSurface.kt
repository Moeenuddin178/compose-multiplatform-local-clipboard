package org.clipboard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight soft-neumorphic container without expensive blurs.
 * Uses dual outer shadows and optional inner shadow overlay for pressed state.
 * Respects current theme colors; no global tokens are changed.
 */
@Composable
fun NeumorphicSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    intensity: Float = 1f,
    pressed: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Shadow colors tuned for both light and dark themes
    val intensityState by rememberUpdatedState(newValue = intensity)
    val isDark = onSurface.luminance() > surface.luminance()
    val darkShadow = remember(surface, onSurface, intensityState, isDark) {
        if (isDark) Color.Black.copy(alpha = 0.30f * intensityState) else Color(0xFF000000).copy(alpha = 0.18f * intensityState)
    }
    val lightShadow = remember(surface, onSurface, intensityState, isDark) {
        if (isDark) Color.White.copy(alpha = 0.12f * intensityState) else Color.White.copy(alpha = 0.70f * intensityState)
    }

    val shape = RoundedCornerShape(cornerRadius)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    TrackPressed(interactionSource, isPressed)

    val effectivePressed = pressed || isPressed.value

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawNeumorphicOuterShadows(
                    shapeRadius = cornerRadius,
                    darkShadow = darkShadow,
                    lightShadow = lightShadow,
                    pressed = effectivePressed
                )
                if (effectivePressed) {
                    drawNeumorphicInnerShadow(
                        shapeRadius = cornerRadius,
                        light = lightShadow.copy(alpha = lightShadow.alpha * 0.6f),
                        dark = darkShadow.copy(alpha = darkShadow.alpha * 0.8f)
                    )
                }
            }
            .background(surface, shape)
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun TrackPressed(source: MutableInteractionSource, pressed: MutableState<Boolean>) {
    LaunchedEffect(source) {
        source.interactions.collect { interaction: Interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed.value = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed.value = false
                else -> {}
            }
        }
    }
}

private fun DrawScope.drawNeumorphicOuterShadows(
    shapeRadius: Dp,
    darkShadow: Color,
    lightShadow: Color,
    pressed: Boolean
) {
    val r = shapeRadius.toPx()
    val corner = CornerRadius(r, r)
    val rect = Rect(0f, 0f, size.width, size.height)
    val roundRect = RoundRect(rect, corner, corner, corner, corner)

    // Offsets are smaller when pressed to suggest concavity
    val spread = if (pressed) 2.dp.toPx() else 4.dp.toPx()
    val offset = if (pressed) 2.dp.toPx() else 6.dp.toPx()

    // Light top-left highlight
    inset(-spread, -spread) {
        drawRoundRect(
            color = lightShadow,
            topLeft = Offset(-offset, -offset),
            size = size.copy(width = size.width + offset, height = size.height + offset),
            cornerRadius = CornerRadius(r, r)
        )
    }

    // Dark bottom-right shadow
    inset(-spread, -spread) {
        drawRoundRect(
            color = darkShadow,
            topLeft = Offset(offset, offset),
            size = size.copy(width = size.width + offset, height = size.height + offset),
            cornerRadius = CornerRadius(r, r)
        )
    }

    // Subtle outline to keep edges readable in dark mode
    val outline = darkShadow.copy(alpha = (if (pressed) 0.18f else 0.10f) + 0.02f)
    val path = Path().apply { addRoundRect(roundRect) }
    clipPath(path) {
        drawRoundRect(color = outline, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
    }
}

private fun DrawScope.drawNeumorphicInnerShadow(
    shapeRadius: Dp,
    light: Color,
    dark: Color
) {
    val r = shapeRadius.toPx()
    val corner = CornerRadius(r, r)
    val rect = Rect(0f, 0f, size.width, size.height)
    val roundRect = RoundRect(rect, corner, corner, corner, corner)
    val path = Path().apply { addRoundRect(roundRect) }

    // Top-left inner highlight
    clipPath(path) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(light, Color.Transparent),
                start = Offset(0f, 0f),
                end = Offset(0f, size.height * 0.4f)
            ),
            blendMode = BlendMode.Softlight
        )
    }

    // Bottom-right inner shadow
    clipPath(path) {
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, dark),
                start = Offset(0f, size.height * 0.6f),
                end = Offset(0f, size.height)
            ),
            blendMode = BlendMode.Multiply
        )
    }
}


