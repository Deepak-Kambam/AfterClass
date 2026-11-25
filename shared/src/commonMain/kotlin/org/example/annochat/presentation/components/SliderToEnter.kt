package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import kotlin.math.roundToInt

@Composable
fun SliderToEnter(
    text: String,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    onProgress: ((Float) -> Unit)? = null,
    isProcessing: Boolean = false,
    resetKey: Any? = null
) {
    var containerWidth by remember { mutableFloatStateOf(0f) }

    // Plain state driving thumb offset frame-by-frame
    var thumbOffset by remember { mutableFloatStateOf(0f) }

    // Animatable used for smooth spring-back or completing snap
    val springAnim = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    var isCompleted by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val trackHeight = 50.dp
    val thumbSize = 42.dp
    val thumbPadding = 4.dp
    val thumbTotalWidthPx = with(density) { (thumbSize + thumbPadding * 2).toPx() }

    // Reset internal slider state when processing completes or resets
    LaunchedEffect(isProcessing) {
        if (!isProcessing && isCompleted) {
            isCompleted = false
            thumbOffset = 0f
        }
    }

    // Explicit reset when navigation returns or room changes
    LaunchedEffect(resetKey) {
        if (resetKey != null) {
            isCompleted = false
            thumbOffset = 0f
        }
    }

    // Dynamic scale pop animation when thumb reaches the end and confirms
    val thumbScale by animateFloatAsState(
        targetValue = if (isCompleted) 1.08f else 1f,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        )
    )

    // NestedScrollConnection that consumes ALL horizontal scroll so HorizontalPager
    // cannot steal the gesture while the user is dragging the slider thumb.
    val horizontalScrollConsumer = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return Offset(available.x, 0f)
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                return Velocity(available.x, 0f)
            }
        }
    }

    val appearance = LocalAppearanceSettings.current
    val accentColor = Color(appearance.accent.colorHex)
    val isLight = (0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f
    val onAccentColor = if (isLight) Color.Black else Color.White

    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(CircleShape)
            .background(accentColor)
            .nestedScroll(horizontalScrollConsumer)   // ← blocks pager from stealing drag
            .onSizeChanged { containerWidth = it.width.toFloat() }
    ) {
        val maxDrag = (containerWidth - thumbTotalWidthPx).coerceAtLeast(1f)

        // Label starts strictly past the resting thumb (50dp) so it is never covered.
        // It fades out gracefully over the first 35% of the slide gesture.
        val textAlpha = (1f - (thumbOffset / (maxDrag * 0.35f))).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 50.dp, end = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = onAccentColor.copy(alpha = 0.85f),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alpha(textAlpha)
            )
        }

        // Draggable Thumb — contrasting circular disc on accent capsule track
        Box(
            modifier = Modifier
                .padding(thumbPadding)
                .size(thumbSize)
                .offset { IntOffset(thumbOffset.roundToInt(), 0) }
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(if (isLight) Color.Black else Color.White)
                .pointerInput(isCompleted, containerWidth) {
                    if (isCompleted || containerWidth <= 0f) return@pointerInput

                    val threshold = maxDrag * 0.75f

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        scope.launch { springAnim.stop() }
                        isAnimating = false

                        // ── DRAG LOOP ──────────────────────────────────────────
                        var isDragging = true
                        while (isDragging) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull()

                            if (pointer != null && pointer.pressed) {
                                thumbOffset = (thumbOffset + pointer.positionChange().x)
                                    .coerceIn(0f, maxDrag)

                                onProgress?.invoke((thumbOffset / maxDrag).coerceIn(0f, 1f))
                                pointer.consume()
                            } else {
                                isDragging = false
                            }
                        }

                        // ── RELEASE WITH SMOOTH END-OF-SLIDE ANIMATION ──────────
                        val releaseOffset = thumbOffset

                        if (releaseOffset >= threshold) {
                            // Smoothly animate remaining distance to maxDrag (zero jitter)
                            isAnimating = true
                            scope.launch {
                                springAnim.snapTo(releaseOffset)
                                springAnim.animateTo(
                                    targetValue = maxDrag,
                                    animationSpec = tween(durationMillis = 130, easing = FastOutSlowInEasing)
                                ) {
                                    thumbOffset = value
                                    onProgress?.invoke((value / maxDrag).coerceIn(0f, 1f))
                                }
                                thumbOffset = maxDrag
                                onProgress?.invoke(1f)
                                isCompleted = true
                                isAnimating = false

                                // Micro-pause to let user visually perceive the confirmed checkmark
                                delay(120)
                                onCompleted()

                                // Clean reset after navigation has engaged
                                delay(450)
                                thumbOffset = 0f
                                isCompleted = false
                            }
                        } else {
                            // Smoothly spring back to start position
                            isAnimating = true
                            scope.launch {
                                springAnim.snapTo(releaseOffset)
                                springAnim.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(
                                        durationMillis = 240,
                                        easing = FastOutSlowInEasing
                                    )
                                ) {
                                    thumbOffset = value
                                    onProgress?.invoke((value / maxDrag).coerceIn(0f, 1f))
                                }
                                thumbOffset = 0f
                                onProgress?.invoke(0f)
                                isAnimating = false
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Morph icon smoothly between ArrowRight and Checkmark upon completion
            AnimatedContent(
                targetState = isCompleted,
                transitionSpec = {
                    (scaleIn(tween(140, easing = FastOutSlowInEasing)) + fadeIn(tween(140, easing = FastOutSlowInEasing)))
                        .togetherWith(scaleOut(tween(100, easing = FastOutSlowInEasing)) + fadeOut(tween(100, easing = FastOutSlowInEasing)))
                }
            ) { completed ->
                Icon(
                    imageVector = if (completed) AnonChatIcons.Check else AnonChatIcons.ArrowRight,
                    contentDescription = if (completed) "Confirmed" else "Slide to enter",
                    tint = if (isLight) Color.White else Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
