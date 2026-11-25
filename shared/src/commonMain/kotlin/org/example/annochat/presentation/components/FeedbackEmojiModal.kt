package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.presentation.designsystem.AnonChatIcons

/**
 * Reaction state configuration mapped across the continuous 0.0 .. 1.0 slider range.
 */
data class FeedbackReaction(
    val label: String,
    val emoji: String,
    val fullText: String,
    val rating: Int
)

fun getFeedbackReaction(progress: Float): FeedbackReaction {
    return when {
        progress < 0.20f -> FeedbackReaction("Ugh! Bad", "😡", "Ugh! Bad 😡", 1)
        progress < 0.40f -> FeedbackReaction("Meh! Poor", "😕", "Meh! Poor 😕", 2)
        progress < 0.65f -> FeedbackReaction("Hmm Satisfied", "😇", "Hmm Satisfied 😇", 3)
        progress < 0.85f -> FeedbackReaction("Good! Happy", "😊", "Good! Happy 😊", 4)
        else -> FeedbackReaction("Loved It!", "😍👍", "Loved It! 😍👍", 5)
    }
}

/**
 * Senior-level interactive feedback modal featuring:
 * - Radiant amber-gold card presentation
 * - Large 3D rendered emoji with continuous, smooth expression morphing
 * - Smooth step-free slider with custom tactile thumb
 * - Speech bubble showing the current reaction
 * - "Done 👍" submission button with async repository integration
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackEmojiModal(
    repository: ChatRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Slider value (continuous 0.0f .. 1.0f, default to satisfied at 0.5f)
    var sliderValue by remember { mutableStateOf(0.5f) }
    val animatedProgress by animateFloatAsState(
        targetValue = sliderValue,
        animationSpec = tween(
            durationMillis = 200,
            easing = FastOutSlowInEasing
        ),
        label = "emojiProgress"
    )

    var showNoteField by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentReaction = remember(sliderValue) { getFeedbackReaction(sliderValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Main yellow/amber card container
            Box(
                modifier = modifier
                    .fillMaxWidth(0.90f)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = Color(0xFFFFB300).copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFC618),
                                Color(0xFFFFB700),
                                Color(0xFFFFA000)
                            )
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* prevent backdrop dismissal on dialog card tap */ }
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                // Close button (X) at top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF1E1E1E),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Title: "How was your Experience?"
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF1C1C1E), fontWeight = FontWeight.Bold)) {
                                append("How was\nyour ")
                            }
                            withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.ExtraBold)) {
                                append("Experience?")
                            }
                        },
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(20.dp))

                    // ── Speech Bubble with downward arrow pointer ─────────────────
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = currentReaction,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                                        expandVertically()) togetherWith
                                        (fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing)) + shrinkVertically())
                            },
                            label = "reactionBubble"
                        ) { reaction ->
                            Box(
                                modifier = Modifier
                                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = reaction.fullText,
                                    color = Color(0xFF1E1E1E),
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Bubble pointer triangle pointing down
                        Canvas(modifier = Modifier.size(width = 12.dp, height = 6.dp)) {
                            val path = Path().apply {
                                moveTo(0f, 0f)
                                lineTo(size.width, 0f)
                                lineTo(size.width / 2f, size.height)
                                close()
                            }
                            drawPath(path = path, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // ── Interactive 3D Morphing Emoji ─────────────────────────────
                    FeedbackEmojiFace(
                        progress = animatedProgress,
                        modifier = Modifier.size(175.dp)
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Smooth Step-free Slider with Custom Tactile Thumb ──────────
                    Slider(
                        value = sliderValue,
                        onValueChange = { newVal ->
                            val oldRating = getFeedbackReaction(sliderValue).rating
                            sliderValue = newVal
                            val newRating = getFeedbackReaction(newVal).rating
                            if (oldRating != newRating) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = Color.Black.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E1E1E))
                                )
                            }
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = Color.White.copy(alpha = 0.75f),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier.height(3.dp),
                                drawStopIndicator = null
                            )
                        }
                    )

                    Spacer(Modifier.height(16.dp))

                    // ── Optional Note Field ───────────────────────────────────────
                    AnimatedVisibility(
                        visible = showNoteField,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            TextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = {
                                    Text(
                                        text = "Tell us what you loved or how we can improve…",
                                        fontSize = 13.sp,
                                        color = Color.Black.copy(alpha = 0.50f)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.90f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.70f),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color(0xFF1E1E1E),
                                    unfocusedTextColor = Color(0xFF1E1E1E)
                                )
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    if (!showNoteField) {
                        Text(
                            text = "Add note (optional)",
                            color = Color(0xFF1E1E1E).copy(alpha = 0.75f),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { showNoteField = true }
                                .padding(vertical = 4.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    // ── "Done 👍" Action Button ────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF18181A))
                            .clickable(enabled = !isSubmitting && !isSubmitted) {
                                scope.launch {
                                    isSubmitting = true
                                    errorMessage = null
                                    val reaction = getFeedbackReaction(sliderValue)
                                    val fullMessage = if (noteText.isNotBlank()) {
                                        "${reaction.fullText} • ${noteText.trim()}"
                                    } else {
                                        reaction.fullText
                                    }
                                    val res = repository.submitFeedback(
                                        category = "app_experience",
                                        message = fullMessage,
                                        rating = reaction.rating
                                    )
                                    isSubmitting = false
                                    if (res.isSuccess) {
                                        isSubmitted = true
                                        delay(650)
                                        onDismiss()
                                    } else {
                                        errorMessage = "Failed to send feedback. Please try again."
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (isSubmitted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = AnonChatIcons.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Thank You!",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                text = "Done 👍",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity 3D Emoji Canvas with continuous transition:
 * - 0.0 .. 0.35: Anxious / nervous emoji (wide asymmetrical white eyes, pupils, slanted worried eyebrows, squiggly mouth)
 * - 0.35 .. 0.65: Hmm Satisfied (closed smiling curved eyes ⌒ ⌒, blushed rosy cheeks, open joyful mouth with pink tongue)
 * - 0.65 .. 1.00: Loved It! (vibrant 3D glossy red heart eyes scaling up with outward tilt, blushing cheeks, wide open smile with tongue)
 */
@Composable
fun FeedbackEmojiFace(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = size.minDimension / 2f * 0.88f

        // ── 1. Soft Ambient Floating Shadow ──────────────────────────────────
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x38000000), Color.Transparent),
                center = Offset(cx, cy + radius * 0.88f),
                radius = radius * 0.90f
            ),
            topLeft = Offset(cx - radius * 0.80f, cy + radius * 0.72f),
            size = Size(radius * 1.60f, radius * 0.32f)
        )

        // ── 2. 3D Yellow Sphere Shading ──────────────────────────────────────
        val sphereBrush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF9B5),
                Color(0xFFFFD719),
                Color(0xFFFFB700),
                Color(0xFFE88A00)
            ),
            center = Offset(cx - radius * 0.28f, cy - radius * 0.32f),
            radius = radius * 1.25f
        )
        drawCircle(
            brush = sphereBrush,
            radius = radius,
            center = Offset(cx, cy)
        )

        // Top-left Specular Glossy Glint
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.50f), Color.Transparent),
                center = Offset(cx - radius * 0.32f, cy - radius * 0.38f),
                radius = radius * 0.45f
            ),
            radius = radius * 0.45f,
            center = Offset(cx - radius * 0.32f, cy - radius * 0.38f)
        )

        // ── 3. Rosy Blush Cheeks (fade in for progress > 0.25) ────────────────
        val blushAlpha = ((progress - 0.25f) / 0.35f).coerceIn(0f, 0.75f)
        if (blushAlpha > 0f) {
            val leftCheekCenter = Offset(cx - radius * 0.56f, cy + radius * 0.12f)
            val rightCheekCenter = Offset(cx + radius * 0.56f, cy + radius * 0.12f)
            val cheekRadius = radius * 0.28f
            val cheekBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF3366).copy(alpha = blushAlpha),
                    Color(0xFFFF3366).copy(alpha = blushAlpha * 0.35f),
                    Color.Transparent
                ),
                radius = cheekRadius
            )
            drawCircle(brush = cheekBrush, radius = cheekRadius, center = leftCheekCenter)
            drawCircle(brush = cheekBrush, radius = cheekRadius, center = rightCheekCenter)
        }

        // ── 4. Eyes ──────────────────────────────────────────────────────────
        // A. Anxious open eyes (progress < 0.45f)
        if (progress < 0.45f) {
            val openEyeScaleY = ((0.45f - progress) / 0.45f).coerceIn(0f, 1f)
            val eyeAlpha = ((0.45f - progress) / 0.15f).coerceIn(0f, 1f)

            // Sclera (White eye ovals)
            val leftEyeCenter = Offset(cx - radius * 0.32f, cy - radius * 0.15f)
            val rightEyeCenter = Offset(cx + radius * 0.32f, cy - radius * 0.12f)
            val leftEyeRx = radius * 0.20f
            val leftEyeRy = radius * 0.24f * openEyeScaleY
            val rightEyeRx = radius * 0.22f
            val rightEyeRy = radius * 0.26f * openEyeScaleY

            if (leftEyeRy > 1f && rightEyeRy > 1f) {
                // White of eyes
                drawOval(
                    color = Color.White.copy(alpha = eyeAlpha),
                    topLeft = Offset(leftEyeCenter.x - leftEyeRx, leftEyeCenter.y - leftEyeRy),
                    size = Size(leftEyeRx * 2f, leftEyeRy * 2f)
                )
                drawOval(
                    color = Color.White.copy(alpha = eyeAlpha),
                    topLeft = Offset(rightEyeCenter.x - rightEyeRx, rightEyeCenter.y - rightEyeRy),
                    size = Size(rightEyeRx * 2f, rightEyeRy * 2f)
                )

                // Dark Pupils
                val pupilRadius = radius * 0.09f * openEyeScaleY
                drawCircle(
                    color = Color(0xFF1E1E1E).copy(alpha = eyeAlpha),
                    radius = pupilRadius,
                    center = Offset(leftEyeCenter.x + radius * 0.02f, leftEyeCenter.y + radius * 0.02f)
                )
                drawCircle(
                    color = Color(0xFF1E1E1E).copy(alpha = eyeAlpha),
                    radius = pupilRadius * 1.05f,
                    center = Offset(rightEyeCenter.x - radius * 0.02f, rightEyeCenter.y)
                )

                // Tiny white reflection dots
                drawCircle(
                    color = Color.White.copy(alpha = eyeAlpha),
                    radius = pupilRadius * 0.35f,
                    center = Offset(leftEyeCenter.x + radius * 0.04f, leftEyeCenter.y - radius * 0.02f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = eyeAlpha),
                    radius = pupilRadius * 0.35f,
                    center = Offset(rightEyeCenter.x, rightEyeCenter.y - radius * 0.03f)
                )
            }

            // Worried slanted eyebrows
            val browAlpha = ((0.35f - progress) / 0.25f).coerceIn(0f, 1f)
            if (browAlpha > 0f) {
                val browStroke = Stroke(width = radius * 0.055f, cap = StrokeCap.Round)
                // Left eyebrow slanted up toward center: /
                val leftBrow = Path().apply {
                    moveTo(leftEyeCenter.x - radius * 0.16f, leftEyeCenter.y - radius * 0.28f)
                    lineTo(leftEyeCenter.x + radius * 0.14f, leftEyeCenter.y - radius * 0.38f)
                }
                drawPath(path = leftBrow, color = Color(0xFF1E1E1E).copy(alpha = browAlpha), style = browStroke)

                // Right eyebrow slanted up toward center: \
                val rightBrow = Path().apply {
                    moveTo(rightEyeCenter.x - radius * 0.14f, rightEyeCenter.y - radius * 0.38f)
                    lineTo(rightEyeCenter.x + radius * 0.16f, rightEyeCenter.y - radius * 0.28f)
                }
                drawPath(path = rightBrow, color = Color(0xFF1E1E1E).copy(alpha = browAlpha), style = browStroke)
            }
        }

        // B. Closed smiling happy eyes ⌒ ⌒ (0.32f <= progress <= 0.75f)
        if (progress in 0.32f..0.75f) {
            val closedEyeAlpha = when {
                progress < 0.45f -> (progress - 0.32f) / 0.13f
                progress > 0.65f -> (0.75f - progress) / 0.10f
                else -> 1f
            }.coerceIn(0f, 1f)

            if (closedEyeAlpha > 0f) {
                val stroke = Stroke(width = radius * 0.085f, cap = StrokeCap.Round)
                val eyeColor = Color(0xFF1C1C1E).copy(alpha = closedEyeAlpha)

                // Left smiling arc ⌒
                val leftArc = Path().apply {
                    moveTo(cx - radius * 0.50f, cy - radius * 0.08f)
                    quadraticTo(
                        cx - radius * 0.32f, cy - radius * 0.32f,
                        cx - radius * 0.14f, cy - radius * 0.08f
                    )
                }
                drawPath(path = leftArc, color = eyeColor, style = stroke)

                // Right smiling arc ⌒
                val rightArc = Path().apply {
                    moveTo(cx + radius * 0.14f, cy - radius * 0.08f)
                    quadraticTo(
                        cx + radius * 0.32f, cy - radius * 0.32f,
                        cx + radius * 0.50f, cy - radius * 0.08f
                    )
                }
                drawPath(path = rightArc, color = eyeColor, style = stroke)
            }
        }

        // C. 3D Heart Eyes ❤️ ❤️ (progress > 0.60f)
        if (progress > 0.60f) {
            val heartScale = ((progress - 0.60f) / 0.35f).coerceIn(0f, 1.15f)
            if (heartScale > 0f) {
                val heartSize = radius * 0.70f * heartScale
                val leftHeartCenter = Offset(cx - radius * 0.32f, cy - radius * 0.14f)
                val rightHeartCenter = Offset(cx + radius * 0.32f, cy - radius * 0.14f)

                val heartBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF5252),
                        Color(0xFFE53935),
                        Color(0xFFB71C1C)
                    )
                )

                drawHeartShape(
                    drawScope = this,
                    center = leftHeartCenter,
                    size = heartSize,
                    brush = heartBrush,
                    rotationDegrees = -12f
                )

                drawHeartShape(
                    drawScope = this,
                    center = rightHeartCenter,
                    size = heartSize,
                    brush = heartBrush,
                    rotationDegrees = 12f
                )
            }
        }

        // ── 5. Mouth ─────────────────────────────────────────────────────────
        // A. Wavy squiggly mouth (progress <= 0.38f)
        if (progress <= 0.38f) {
            val waveIntensity = ((0.38f - progress) / 0.38f).coerceIn(0f, 1f)
            val mouthY = cy + radius * 0.36f
            val mouthHalfW = radius * 0.34f

            val squigglyPath = Path().apply {
                moveTo(cx - mouthHalfW, mouthY)
                cubicTo(
                    cx - mouthHalfW * 0.6f, mouthY - radius * 0.10f * waveIntensity,
                    cx - mouthHalfW * 0.2f, mouthY + radius * 0.10f * waveIntensity,
                    cx, mouthY
                )
                cubicTo(
                    cx + mouthHalfW * 0.2f, mouthY - radius * 0.10f * waveIntensity,
                    cx + mouthHalfW * 0.6f, mouthY + radius * 0.10f * waveIntensity,
                    cx + mouthHalfW, mouthY
                )
            }
            drawPath(
                path = squigglyPath,
                color = Color(0xFF1E1E1E),
                style = Stroke(width = radius * 0.085f, cap = StrokeCap.Round)
            )
        } else {
            // B. Wide open joyful crescent mouth with tongue (progress > 0.38f)
            val openFraction = ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)
            val mouthWidth = radius * (0.62f + 0.26f * openFraction)
            val mouthHeight = radius * (0.30f + 0.26f * openFraction)
            val mouthTopY = cy + radius * (0.24f - 0.05f * openFraction)

            val mouthPath = Path().apply {
                moveTo(cx - mouthWidth / 2f, mouthTopY)
                quadraticTo(cx, mouthTopY + mouthHeight * 0.10f, cx + mouthWidth / 2f, mouthTopY)
                quadraticTo(cx, mouthTopY + mouthHeight, cx - mouthWidth / 2f, mouthTopY)
                close()
            }

            // Dark inner mouth cavity
            drawPath(path = mouthPath, color = Color(0xFF221111), style = Fill)

            // Cute pink tongue
            val tongueW = mouthWidth * 0.62f
            val tongueH = mouthHeight * 0.72f
            val tonguePath = Path().apply {
                moveTo(cx - tongueW / 2f, mouthTopY + mouthHeight * 0.45f)
                quadraticTo(
                    cx, mouthTopY + mouthHeight * 1.05f,
                    cx + tongueW / 2f, mouthTopY + mouthHeight * 0.45f
                )
                close()
            }
            drawPath(path = tonguePath, color = Color(0xFFFF5E7E), style = Fill)

            // Outline stroke of the open mouth
            drawPath(
                path = mouthPath,
                color = Color(0xFF1E1E1E),
                style = Stroke(width = radius * 0.055f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Helper to draw a sleek, plump 3D heart with glint reflection.
 */
private fun drawHeartShape(
    drawScope: DrawScope,
    center: Offset,
    size: Float,
    brush: Brush,
    rotationDegrees: Float
) {
    drawScope.rotate(rotationDegrees, pivot = center) {
        val w = size
        val h = size
        val path = Path().apply {
            moveTo(center.x, center.y + h * 0.42f)
            // Left lobe
            cubicTo(
                center.x - w * 0.55f, center.y + h * 0.15f,
                center.x - w * 0.60f, center.y - h * 0.35f,
                center.x - w * 0.25f, center.y - h * 0.42f
            )
            cubicTo(
                center.x - w * 0.08f, center.y - h * 0.42f,
                center.x, center.y - h * 0.25f,
                center.x, center.y - h * 0.15f
            )
            // Right lobe
            cubicTo(
                center.x, center.y - h * 0.25f,
                center.x + w * 0.08f, center.y - h * 0.42f,
                center.x + w * 0.25f, center.y - h * 0.42f
            )
            cubicTo(
                center.x + w * 0.60f, center.y - h * 0.35f,
                center.x + w * 0.55f, center.y + h * 0.15f,
                center.x, center.y + h * 0.42f
            )
            close()
        }
        drawPath(path = path, brush = brush, style = Fill)

        // Glossy highlight on left lobe
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = size * 0.10f,
            center = Offset(center.x - size * 0.22f, center.y - size * 0.25f)
        )
    }
}
