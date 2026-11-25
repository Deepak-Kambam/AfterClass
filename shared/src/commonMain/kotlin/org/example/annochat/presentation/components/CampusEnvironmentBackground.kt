package org.example.annochat.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.bg_space
import org.example.annochat.data.model.CampusEnvironment
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import org.jetbrains.compose.resources.painterResource
import kotlin.math.cos
import kotlin.math.sin

/**
 * Branded Dynamic Campus Environment Background for AfterClass.
 *
 * Renders atmospheric background gradients and ambient effects tailored to the active
 * Campus Environment preset ([CampusEnvironment]), reactive to [environmentIntensity],
 * [orbitEnabled], and [ambientEffectsEnabled].
 */
@Composable
fun CampusEnvironmentBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val settings = LocalAppearanceSettings.current
    val isAnimated = settings.animationsEnabled

    val (orbitX, orbitY, shimmerPhase) = if (isAnimated && (settings.orbitEnabled || settings.ambientEffectsEnabled)) {
        val transition = rememberInfiniteTransition(label = "campusEnvMotion")
        val orbitAngle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 30000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "orbitAngle"
        )
        val shimmer by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 6000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )
        val rad = (orbitAngle * (kotlin.math.PI / 180.0)).toFloat()
        Triple(cos(rad), sin(rad), shimmer)
    } else {
        Triple(0f, 0f, 0.5f)
    }

    val intensity = settings.environmentIntensity.coerceIn(0.1f, 1.0f)
    val accentColor = Color(settings.accent.colorHex)

    // Base background styling by environment
    val baseBgColor = when (settings.environment) {
        CampusEnvironment.CAMPUS_CORE -> Color(0xFF0A0A0C)
        CampusEnvironment.LIBRARY_LIGHTS -> Color(0xFF14100C)
        CampusEnvironment.HOSTEL_NIGHTS -> Color(0xFF100C16)
        CampusEnvironment.STARGAZER -> Color(0xFF070512)
    }

    val glowColor1 = when (settings.environment) {
        CampusEnvironment.CAMPUS_CORE -> accentColor.copy(alpha = 0.15f * intensity)
        CampusEnvironment.LIBRARY_LIGHTS -> Color(0xFFFFB703).copy(alpha = 0.18f * intensity)
        CampusEnvironment.HOSTEL_NIGHTS -> Color(0xFFFF6E40).copy(alpha = 0.16f * intensity)
        CampusEnvironment.STARGAZER -> Color(0xFFB388FF).copy(alpha = 0.22f * intensity)
    }

    val glowColor2 = when (settings.environment) {
        CampusEnvironment.CAMPUS_CORE -> Color(0xFF1A1A22).copy(alpha = 0.40f * intensity)
        CampusEnvironment.LIBRARY_LIGHTS -> Color(0xFF3D2614).copy(alpha = 0.35f * intensity)
        CampusEnvironment.HOSTEL_NIGHTS -> Color(0xFF2A1438).copy(alpha = 0.35f * intensity)
        CampusEnvironment.STARGAZER -> Color(0xFF1A0F33).copy(alpha = 0.45f * intensity)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // Authentic orbit background image rendered as default base layer
        Image(
            painter = painterResource(Res.drawable.bg_space),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Ambient Canvas Lighting & Environment Atmosphere Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val orbitShiftX = if (settings.orbitEnabled) orbitX * 80f else 0f
            val orbitShiftY = if (settings.orbitEnabled) orbitY * 60f else 0f

            // Primary diffuse radial atmospheric glow (top right / center)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor1, Color.Transparent),
                    center = Offset(w * 0.75f + orbitShiftX, h * 0.20f + orbitShiftY),
                    radius = (w * 0.95f)
                ),
                radius = w * 0.95f,
                center = Offset(w * 0.75f + orbitShiftX, h * 0.20f + orbitShiftY)
            )

            // Secondary atmospheric glow (bottom left)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor2, Color.Transparent),
                    center = Offset(w * 0.20f - orbitShiftX, h * 0.80f - orbitShiftY),
                    radius = (w * 1.05f)
                ),
                radius = w * 1.05f,
                center = Offset(w * 0.20f - orbitShiftX, h * 0.80f - orbitShiftY)
            )

            // Ambient Stargazer / Shimmer dust motes
            if (settings.ambientEffectsEnabled && (settings.environment == CampusEnvironment.STARGAZER || intensity > 0.4f)) {
                val baseAlpha = if (settings.environment == CampusEnvironment.STARGAZER) 0.60f else 0.35f
                val moteAlpha = baseAlpha * intensity * (0.5f + 0.5f * shimmerPhase)
                val moteColor = Color.White.copy(alpha = moteAlpha)

                val seedPoints = listOf(
                    Triple(Offset(w * 0.12f, h * 0.08f), 1.5f, 0.8f),
                    Triple(Offset(w * 0.35f, h * 0.05f), 2.0f, 1.0f),
                    Triple(Offset(w * 0.78f, h * 0.12f), 2.5f, 0.9f),
                    Triple(Offset(w * 0.92f, h * 0.18f), 1.5f, 0.7f),
                    Triple(Offset(w * 0.22f, h * 0.28f), 1.8f, 0.6f),
                    Triple(Offset(w * 0.65f, h * 0.35f), 2.2f, 1.0f),
                    Triple(Offset(w * 0.08f, h * 0.52f), 1.6f, 0.7f),
                    Triple(Offset(w * 0.85f, h * 0.58f), 2.0f, 0.8f),
                    Triple(Offset(w * 0.45f, h * 0.72f), 1.8f, 0.9f),
                    Triple(Offset(w * 0.15f, h * 0.82f), 2.2f, 0.7f),
                    Triple(Offset(w * 0.75f, h * 0.88f), 1.5f, 0.8f)
                )

                for ((point, radius, scale) in seedPoints) {
                    drawCircle(
                        color = moteColor.copy(alpha = (moteAlpha * scale).coerceIn(0f, 1f)),
                        radius = radius,
                        center = point
                    )
                }
            }
        }

        content()
    }
}

