package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import org.example.annochat.getCurrentLocalHour
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatShapes

/**
 * Root atmospheric background with subtle, time-aware dark atmosphere.
 * Preserves the solid obsidian base, applying only a very faint time-of-day gradient tint
 * (morning softness, evening dusk depth, night cosmic obsidian).
 * Completely preserves readability and dark-cosmic color constraints.
 */
@Composable
fun AtmosphericBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val currentHour = remember {
        try {
            getCurrentLocalHour()
        } catch (_: Throwable) {
            12
        }
    }

    val timeAtmosphereBrush = remember(currentHour) {
        when (currentHour) {
            // Morning (6:00 - 11:59): Subtle softer top ambient atmosphere
            in 6..11 -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF16161D).copy(alpha = 0.55f),
                    Color(0xFF0F0F13).copy(alpha = 0.20f),
                    Color.Transparent
                )
            )
            // Afternoon & Evening (12:00 - 19:59): Deep dusk obsidian
            in 12..19 -> Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF111116).copy(alpha = 0.35f),
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.40f)
                )
            )
            // Night & Late Night (20:00 - 5:59): Cosmic obsidian depth with midnight vignette
            else -> Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0D0F18).copy(alpha = 0.45f),
                    Color.Black.copy(alpha = 0.70f)
                ),
                radius = 1400f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AnonChatColors.BaseBackground)
    ) {
        // Subtle time-aware ambient overlay (preserves base background and dark theme)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(timeAtmosphereBrush)
        )
        content()
    }
}

/**
 * Solid-surface container. Replaces the old glass translucent surface.
 * Kept as GlassSurface for naming compatibility but renders opaque solid.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = AnonChatShapes.card,
    backgroundColor: Color = AnonChatColors.SurfacePrimary,
    borderColor: Color = AnonChatColors.Border,
    borderWidth: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    val clickModifier = if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = AnonChatColors.Primary),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = borderWidth.dp,
                color = borderColor,
                shape = shape
            )
            .then(clickModifier),
        content = content
    )
}

/**
 * Interactive card with ripple.
 */
@Composable
fun GlassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AnonChatShapes.card,
    backgroundColor: Color = AnonChatColors.SurfaceSecondary,
    borderColor: Color = AnonChatColors.Border,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AnonChatColors.Primary),
                enabled = enabled,
                onClick = onClick
            ),
        content = content
    )
}
