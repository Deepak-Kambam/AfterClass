package org.example.annochat.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlinproject.shared.generated.resources.Res
import kotlinproject.shared.generated.resources.afterclass_anim_a
import kotlinproject.shared.generated.resources.afterclass_anim_arc
import kotlinproject.shared.generated.resources.afterclass_anim_c
import kotlinproject.shared.generated.resources.afterclass_anim_glow
import kotlinproject.shared.generated.resources.afterclass_anim_wordmark
import kotlinproject.shared.generated.resources.afterclass_logo
import kotlinx.coroutines.async
import org.example.annochat.data.repository.ChatRepository
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Premium cinematic splash animation for AfterClass, strictly adhering to the 9-frame storyboard:
 *
 * 0.00–0.30s — DARKNESS & AMBIENT IGNITION (Screen deep black, ambient hotspot gathers energy)
 * 0.30–1.00s — ARC FLOW SWEEP (Light streak traces semi-circular arc from left to right with bloom)
 * 0.80–1.55s — C FLOW REVEAL ("C" behind "A" + 3 dots progressively revealed from left to right)
 * 1.35–2.25s — A INRUSH REVEAL (Dominant ribbon "A" rushes in over C with specular ridge glint)
 * 2.10–2.65s — COMPLETE SYMBOL (A + C + 3 dots + arc complete, slanted specular sheen sweeps emblem)
 * 2.50–3.10s — WORDMARK REVEAL (Horizontal light beam rushes across "AfterClass" from left to right)
 * 3.00–3.40s — SETTLE (Light exits to right, ambient floor glow settles under wordmark)
 * 3.40–4.20s — FINAL HOLD (Complete square logo holds proudly and cleanly with current height)
 * 4.20s       — AUTO-TRANSITION to Onboarding / Rooms (or No Internet on network failure)
 */
@Composable
fun SplashScreen(
    repository: ChatRepository,
    onStartupSuccess: () -> Unit,
    onStartupFailure: (reason: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // Master animation clock from 0f to 4200f (milliseconds)
    val animMillis = remember { Animatable(0f) }

    // Load component layers (1024x1024 resolution)
    val arcPainter = painterResource(Res.drawable.afterclass_anim_arc)
    val cPainter = painterResource(Res.drawable.afterclass_anim_c)
    val aPainter = painterResource(Res.drawable.afterclass_anim_a)
    val wordmarkPainter = painterResource(Res.drawable.afterclass_anim_wordmark)
    val glowPainter = painterResource(Res.drawable.afterclass_anim_glow)
    val fullLogoPainter = painterResource(Res.drawable.afterclass_logo)

    LaunchedEffect(Unit) {
        val animDuration = 4200
        val animJob = async {
            animMillis.animateTo(
                targetValue = 4200f,
                animationSpec = tween(durationMillis = animDuration, easing = LinearEasing)
            )
        }

        // 2. Concurrently check backend reachability
        val checkJob = async {
            repository.connect()
            repository.loadPublicRooms()
        }

        // Await completion of choreography & network reachability
        animJob.await()
        val result = checkJob.await()

        if (result.isSuccess) {
            onStartupSuccess()
        } else {
            val errorMsg = result.exceptionOrNull()?.message
            onStartupFailure(errorMsg)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val currentMs = animMillis.value
        // Exact 460dp square canvas maintaining current height without horizontal compression
        val canvasSize = 460.dp

        Canvas(
            modifier = Modifier
                .wrapContentSize(align = Alignment.Center, unbounded = true)
                .requiredSize(canvasSize)
        ) {
            // Guarantee exact 1:1 square dimensions pinned to height
            val squareDim = size.height
            val squareSize = Size(squareDim, squareDim)
            val offsetX = (size.width - squareDim) / 2f

            withTransform({
                translate(left = offsetX, top = 0f)
            }) {
                val w = squareDim
                val h = squareDim

                if (currentMs >= 3400f) {
                    // ── Phase 8: Final Hold (3400ms - 4200ms) ────────────────────────
                    // 800ms solid hold of the complete master logo in its exact 1:1 square geometry
                    with(fullLogoPainter) { draw(squareSize, alpha = 1f) }
                } else if (currentMs < 300f) {
                    // ── Phase 1: Darkness & Ambient Warmup (0ms - 300ms) ─────────────
                    val ambientAlpha = (currentMs / 300f).coerceIn(0f, 1f) * 0.50f
                    val startX = 0.22f * w
                    val startY = 0.44f * h
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = ambientAlpha),
                                Color.White.copy(alpha = ambientAlpha * 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(startX, startY),
                            radius = 36.dp.toPx()
                        ),
                        radius = 36.dp.toPx(),
                        center = Offset(startX, startY),
                        blendMode = BlendMode.Screen
                    )
                } else {
                    // ── Layer 1: Floor Glow (Settle Phase: 3000ms - 3400ms) ──────────
                    if (currentMs >= 3000f) {
                        val glowProgress = ((currentMs - 3000f) / 400f).coerceIn(0f, 1f)
                        val glowAlpha = FastOutSlowInEasing.transform(glowProgress) * 0.85f
                        with(glowPainter) { draw(squareSize, alpha = glowAlpha) }

                        // Smoothly blend into full master logo towards 3400ms for seamless final hold transition
                        if (currentMs >= 3200f) {
                            val blendAlpha = ((currentMs - 3200f) / 200f).coerceIn(0f, 1f)
                            with(fullLogoPainter) { draw(squareSize, alpha = blendAlpha) }
                        }
                    }

                    // ── Layer 2: Arc Reveal (300ms - 1000ms, holds thereafter) ───────
                    // Geometric arc center and radii inside the logo canvas
                    val arcCx = 0.5078f * w
                    val arcCy = 0.3730f * h
                    val arcRx = 0.3008f * w
                    val arcRy = 0.2393f * h

                    val arcProgress = ((currentMs - 300f) / 700f).coerceIn(0f, 1f)
                    if (arcProgress > 0f) {
                        if (arcProgress >= 1f) {
                            // Fully revealed arc
                            with(arcPainter) { draw(squareSize, alpha = 1f) }
                        } else {
                            // Tracing arc along counter-clockwise sweep from 195° down through 90° to -15°
                            val sweepProgress = FastOutSlowInEasing.transform(arcProgress)
                            val startAngle = 195f
                            val sweepAngle = -sweepProgress * 210f
                            val currentAngleDeg = startAngle + sweepAngle

                            // Sector clip for progressive drawing
                            val arcClipPath = Path().apply {
                                moveTo(arcCx, arcCy)
                                arcTo(
                                    rect = Rect(
                                        arcCx - arcRx - 30f,
                                        arcCy - arcRy - 30f,
                                        arcCx + arcRx + 30f,
                                        arcCy + arcRy + 30f
                                    ),
                                    startAngleDegrees = startAngle,
                                    sweepAngleDegrees = sweepAngle,
                                    forceMoveTo = false
                                )
                                close()
                            }

                            clipPath(arcClipPath) {
                                with(arcPainter) { draw(squareSize, alpha = 1f) }
                            }

                            // Head flare position along the arc curve
                            val rad = currentAngleDeg * (PI.toFloat() / 180f)
                            val headX = arcCx + arcRx * cos(rad)
                            val headY = arcCy - arcRy * sin(rad)
                            val tangentAngle = currentAngleDeg - 90f

                            drawCinematicFlare(
                                center = Offset(headX, headY),
                                flareRadius = 36.dp.toPx(),
                                streakLength = 64.dp.toPx(),
                                alpha = 1f,
                                rotationDegrees = tangentAngle
                            )
                        }
                    }

                    // ── Layer 3: C Symbol + 3 Dots (800ms - 1550ms) ──────────────────
                    val cProgress = ((currentMs - 800f) / 750f).coerceIn(0f, 1f)
                    if (cProgress > 0f) {
                        if (cProgress >= 1f) {
                            with(cPainter) { draw(squareSize, alpha = 1f) }
                        } else {
                            val easedC = FastOutSlowInEasing.transform(cProgress)
                            val cStartX = 0.50f * w
                            val cEndX = 0.84f * w
                            val revealX = cStartX + easedC * (cEndX - cStartX)

                            clipRect(left = 0f, top = 0f, right = revealX, bottom = h) {
                                with(cPainter) { draw(squareSize, alpha = 1f) }
                            }

                            // Soft specular sheen riding on C reveal frontier
                            drawCinematicFlare(
                                center = Offset(revealX, 0.46f * h),
                                flareRadius = 26.dp.toPx(),
                                streakLength = 40.dp.toPx(),
                                alpha = 0.8f * (1f - easedC * 0.25f),
                                rotationDegrees = 90f
                            )
                        }
                    }

                    // ── Layer 4: Dominant Ribbon A Inrush (1350ms - 2250ms) ──────────
                    val aProgress = ((currentMs - 1350f) / 900f).coerceIn(0f, 1f)
                    if (aProgress > 0f) {
                        if (aProgress >= 1f) {
                            with(aPainter) { draw(squareSize, alpha = 1f) }
                        } else {
                            val easedA = FastOutSlowInEasing.transform(aProgress)
                            // Diagonal sweep uncovering ribbon "A" from bottom-left to upper-right
                            val sweepMinX = 0.18f * w
                            val sweepMaxX = 0.65f * w
                            val currentFrontierX = sweepMinX + easedA * (sweepMaxX - sweepMinX)

                            val aClipPath = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(currentFrontierX + 0.12f * w, 0f)
                                lineTo(currentFrontierX - 0.12f * w, h)
                                lineTo(0f, h)
                                close()
                            }

                            clipPath(aClipPath) {
                                with(aPainter) { draw(squareSize, alpha = 1f) }
                            }

                            // Specular highlight traveling along ribbon ridge with inrush energy
                            val ridgeY = 0.62f * h - easedA * 0.32f * h
                            drawCinematicFlare(
                                center = Offset(currentFrontierX, ridgeY),
                                flareRadius = 34.dp.toPx(),
                                streakLength = 58.dp.toPx(),
                                alpha = 1f,
                                rotationDegrees = 45f
                            )
                        }
                    }

                    // ── Layer 5: Complete Symbol Specular Sheen (2100ms - 2650ms) ────
                    if (currentMs in 2100f..2650f) {
                        val sheenProgress = ((currentMs - 2100f) / 550f).coerceIn(0f, 1f)
                        val sheenX = 0.20f * w + sheenProgress * 0.65f * w
                        val sheenAlpha = sin(sheenProgress * PI.toFloat()) * 0.80f

                        // Slanted specular gleam passing across completed AC emblem
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = sheenAlpha),
                                    Color.Transparent
                                ),
                                start = Offset(sheenX - 30f, 0.20f * h),
                                end = Offset(sheenX + 30f, 0.62f * h)
                            ),
                            start = Offset(sheenX + 0.08f * w, 0.18f * h),
                            end = Offset(sheenX - 0.08f * w, 0.64f * h),
                            strokeWidth = 22.dp.toPx(),
                            blendMode = BlendMode.Screen
                        )
                    }

                    // ── Layer 6: Wordmark "AfterClass" (2500ms - 3100ms) ─────────────
                    val wmProgress = ((currentMs - 2500f) / 600f).coerceIn(0f, 1f)
                    if (wmProgress > 0f) {
                        if (wmProgress >= 1f) {
                            with(wordmarkPainter) { draw(squareSize, alpha = 1f) }
                        } else {
                            val easedWm = FastOutSlowInEasing.transform(wmProgress)
                            val wmStartX = 0.22f * w
                            val wmEndX = 0.80f * w
                            val revealWmX = wmStartX + easedWm * (wmEndX - wmStartX)
                            val wmY = 0.686f * h

                            clipRect(left = 0f, top = 0f, right = revealWmX, bottom = h) {
                                with(wordmarkPainter) { draw(squareSize, alpha = 1f) }
                            }

                            // Bright horizontal traveling light streak at wordmark frontier
                            drawCinematicFlare(
                                center = Offset(revealWmX, wmY),
                                flareRadius = 32.dp.toPx(),
                                streakLength = 80.dp.toPx(),
                                alpha = 1f,
                                rotationDegrees = 0f
                            )
                        }
                    }

                    // ── Light Exit Trail (3000ms - 3400ms) ───────────────────────────
                    if (currentMs in 3000f..3400f) {
                        val exitProgress = ((currentMs - 3000f) / 400f).coerceIn(0f, 1f)
                        val exitX = 0.80f * w + exitProgress * 0.25f * w
                        val exitAlpha = (1f - exitProgress) * 0.75f
                        drawCinematicFlare(
                            center = Offset(exitX, 0.686f * h),
                            flareRadius = 22.dp.toPx(),
                            streakLength = 48.dp.toPx() * (1f - exitProgress),
                            alpha = exitAlpha,
                            rotationDegrees = 0f
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a GPU-accelerated cinematic flare with:
 * 1. Soft radial bloom halo
 * 2. High-intensity specular core
 * 3. Rotatable lens streak along tangent of motion
 */
private fun DrawScope.drawCinematicFlare(
    center: Offset,
    flareRadius: Float,
    streakLength: Float,
    alpha: Float,
    rotationDegrees: Float
) {
    if (alpha <= 0.01f) return

    // 1. Radial bloom halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha * 0.85f),
                Color.White.copy(alpha = alpha * 0.35f),
                Color.White.copy(alpha = alpha * 0.08f),
                Color.Transparent
            ),
            center = center,
            radius = flareRadius
        ),
        radius = flareRadius,
        center = center,
        blendMode = BlendMode.Screen
    )

    // 2. High-intensity hotspot core
    drawCircle(
        color = Color.White.copy(alpha = alpha),
        radius = flareRadius * 0.16f,
        center = center,
        blendMode = BlendMode.Screen
    )

    // 3. Directional lens streak
    withTransform({
        rotate(degrees = rotationDegrees, pivot = center)
    }) {
        val streakWidth = streakLength * 2f
        val streakHeight = 3.5f.dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = alpha * 0.4f),
                    Color.White.copy(alpha = alpha * 0.95f),
                    Color.White.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                startX = center.x - streakLength,
                endX = center.x + streakLength
            ),
            topLeft = Offset(center.x - streakLength, center.y - streakHeight / 2f),
            size = Size(streakWidth, streakHeight),
            blendMode = BlendMode.Screen
        )
    }
}

