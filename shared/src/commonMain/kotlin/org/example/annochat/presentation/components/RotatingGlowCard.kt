package org.example.annochat.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.theme.LocalAppearanceSettings

/**
 * Premium Card with an optional slow, ambient rotating glow border.
 *
 * Characteristics:
 * - Dynamic energy highlight flowing smoothly around the perimeter in the active accent color.
 * - Reactive to [glowIntensity] and [glowSpeed] from Appearance Settings.
 * - Card and content remain perfectly static and crisp — only the border gradient highlight sweeps.
 * - When [enabled] is false or animations are disabled: renders static default border with zero overhead.
 */
@Composable
fun RotatingGlowCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glowColor: Color? = null,
    glowIntensity: Float? = null,
    glowSpeed: Float? = null,
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    backgroundColor: Color = AnonChatColors.SurfacePrimary,
    staticBorderColor: Color = AnonChatColors.Border,
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val settings = LocalAppearanceSettings.current
    val isEffectivelyEnabled = enabled && settings.afterglowEnabled && settings.animationsEnabled

    val effectiveGlowColor = glowColor ?: Color(settings.accent.colorHex)
    val effectiveIntensity = (glowIntensity ?: settings.glowIntensity).coerceIn(0.1f, 1.0f)
    val effectiveSpeed = (glowSpeed ?: settings.glowSpeed).coerceIn(0.2f, 3.0f)

    if (isEffectivelyEnabled) {
        val durationMillis = (12000f / effectiveSpeed).toInt().coerceIn(2000, 30000)
        val infiniteTransition = rememberInfiniteTransition(label = "glowBorderLoop")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "borderRotation"
        )

        val glowBrush = Brush.sweepGradient(
            colors = listOf(
                Color.Transparent,
                effectiveGlowColor.copy(alpha = 0.05f * effectiveIntensity),
                effectiveGlowColor.copy(alpha = 0.35f * effectiveIntensity),
                effectiveGlowColor.copy(alpha = 0.95f * effectiveIntensity),
                effectiveGlowColor.copy(alpha = 0.35f * effectiveIntensity),
                effectiveGlowColor.copy(alpha = 0.05f * effectiveIntensity),
                Color.Transparent
            )
        )

        Box(
            modifier = modifier
                .clip(shape)
                .background(backgroundColor)
                .drawWithContent {
                    drawContent()

                    val strokeWidthPx = borderWidth.toPx()
                    val radiusPx = cornerRadius.toPx()
                    val halfStroke = strokeWidthPx / 2f
                    val sizeMinusStroke = Size(
                        size.width - strokeWidthPx,
                        size.height - strokeWidthPx
                    )
                    val topLeft = Offset(halfStroke, halfStroke)

                    // Draw base subtle border
                    drawRoundRect(
                        color = staticBorderColor,
                        topLeft = topLeft,
                        size = sizeMinusStroke,
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(width = strokeWidthPx)
                    )

                    // Draw ambient sweeping highlight along the border
                    rotate(degrees = rotationAngle, pivot = center) {
                        drawRoundRect(
                            brush = glowBrush,
                            topLeft = topLeft,
                            size = sizeMinusStroke,
                            cornerRadius = CornerRadius(radiusPx, radiusPx),
                            style = Stroke(width = strokeWidthPx * 1.4f)
                        )
                    }
                },
            content = content
        )
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .background(backgroundColor)
                .border(borderWidth, staticBorderColor, shape),
            content = content
        )
    }
}

