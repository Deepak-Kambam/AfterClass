package org.example.annochat.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.presentation.theme.RoomVisualRegistry
import org.jetbrains.compose.resources.painterResource

/**
 * Full-screen ambient atmospheric background synchronized with the carousel swipe state.
 *
 * Implements continuous, GPU-accelerated interpolation between room ambient color palettes
 * and atmospheric image textures, ensuring smooth transitions both forward and backward
 * without abrupt snaps or frame drops.
 */
@Composable
fun AmbientCarouselBackground(
    rooms: List<PublicRoomDto>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    if (rooms.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF040406)))
        return
    }

    val pageCount = rooms.size
    val scrollPosition = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (pageCount - 1).toFloat())
    val fromIndex = scrollPosition.toInt().coerceIn(0, pageCount - 1)
    val toIndex = (fromIndex + 1).coerceIn(0, pageCount - 1)
    val rawFraction = (scrollPosition - fromIndex).coerceIn(0f, 1f)
    val easedFraction = FastOutSlowInEasing.transform(rawFraction)

    val fromRoom = rooms[fromIndex]
    val toRoom = rooms[toIndex]

    val fromPalette = RoomVisualRegistry.getPalette(fromRoom)
    val toPalette = RoomVisualRegistry.getPalette(toRoom)
    val currentPalette = RoomVisualRegistry.interpolatePalettes(fromPalette, toPalette, rawFraction)

    val fromImage = RoomVisualRegistry.getImage(fromRoom)
    val toImage = RoomVisualRegistry.getImage(toRoom)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF030305))) {
        // Subtle background texture cross-fade (diffused atmospheric presence)
        if (fromIndex == toIndex || fromImage == toImage) {
            Image(
                painter = painterResource(fromImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.18f }
            )
        } else {
            Image(
                painter = painterResource(fromImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = (1f - easedFraction) * 0.18f }
            )
            Image(
                painter = painterResource(toImage),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = easedFraction * 0.18f }
            )
        }

        // Dark diffusion scrim to preserve crisp contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF030305).copy(alpha = 0.38f))
        )

        // Continuous Ambient Glow Field (Hardware-accelerated radial shader passes)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Broad upper ambient glow (illuminating header & card top)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentPalette.primaryGlow.copy(alpha = 0.95f),
                        currentPalette.primaryGlow.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.20f),
                    radius = w * 1.30f
                )
            )

            // 2. Focused center ambient glow directly behind active carousel card
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentPalette.secondaryGlow.copy(alpha = 0.95f),
                        currentPalette.primaryGlow.copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.48f),
                    radius = w * 1.10f
                )
            )

            // 3. Lower ambient wash providing color continuity across the screen
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        currentPalette.primaryGlow.copy(alpha = 0.50f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.78f),
                    radius = w * 0.95f
                )
            )

            // 4. Smooth vertical contrast vignette
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to Color(0xFF030305).copy(alpha = 0.22f),
                    0.40f to Color.Transparent,
                    0.78f to Color(0xFF020204).copy(alpha = 0.45f),
                    1.0f to Color(0xFF000000).copy(alpha = 0.90f)
                )
            )
        }
    }
}
