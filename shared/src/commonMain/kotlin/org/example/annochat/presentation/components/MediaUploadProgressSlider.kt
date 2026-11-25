package org.example.annochat.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import kotlin.math.roundToInt

/**
 * Progress indicator modeled identically after [SliderToEnter]:
 * - 50dp height capsule track matching SliderToEnter.
 * - Base track is white, with text rendered in high-contrast black.
 * - As progress advances (0% -> 100%), solid black fill expands from the left.
 * - A 42dp black circular button (thumb) leads the progress across the track.
 * - The text (percentage) dynamically splits color:
 *   Over the black fill, text is rendered in pure WHITE.
 *   Over the unfilled white track, text is rendered in pure BLACK.
 */
@Composable
fun MediaUploadProgressSlider(
    progress: Float, // 0f .. 1f
    modifier: Modifier = Modifier,
    status: String? = null
) {
    val density = LocalDensity.current
    var containerWidthPx by remember { mutableFloatStateOf(0f) }

    val trackHeight = 50.dp
    val thumbSize = 42.dp
    val thumbPadding = 4.dp
    val thumbSizePx = with(density) { thumbSize.toPx() }
    val thumbPaddingPx = with(density) { thumbPadding.toPx() }
    val thumbTotalWidthPx = thumbSizePx + thumbPaddingPx * 2

    // Smooth progress interpolation
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "mediaUploadProgress"
    )

    val percentInt = (animatedProgress * 100).roundToInt().coerceIn(0, 100)
    val displayText = status ?: "Sending $percentInt%"

    Box(
        modifier = modifier
            .height(trackHeight)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0x22000000), CircleShape)
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
    ) {
        val maxThumbTravelPx = (containerWidthPx - thumbTotalWidthPx).coerceAtLeast(0f)
        val thumbOffsetPx = maxThumbTravelPx * animatedProgress
        // The black fill extends up to the center/trailing edge of the thumb
        val fillWidthPx = if (containerWidthPx > 0f) {
            (thumbOffsetPx + thumbSizePx / 2f + thumbPaddingPx)
                .coerceIn(0f, containerWidthPx)
        } else 0f
        val fillWidthDp = with(density) { fillWidthPx.toDp() }
        val containerWidthDp = with(density) { containerWidthPx.toDp() }

        // ─────────────────────────────────────────────────────────────
        // 1. BASE LAYER (White Track -> Black Text)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                color = Color.Black,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // ─────────────────────────────────────────────────────────────
        // 2. BLACK PROGRESS FILL
        // ─────────────────────────────────────────────────────────────
        if (fillWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(fillWidthDp)
                    .clip(RectangleShape)
                    .background(Color(0xFF0F0F0F))
            )
        }

        // ─────────────────────────────────────────────────────────────
        // 3. INVERTED WHITE TEXT (Clipped precisely to fillWidth)
        // ─────────────────────────────────────────────────────────────
        if (fillWidthPx > 0f && containerWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(fillWidthDp)
                    .clipToBounds()
            ) {
                // Fixed full-width container centered to exactly align text with Layer 1
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(containerWidthDp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = Color.White,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────────────
        // 4. MOVING BLACK THUMB BUTTON (Matching SliderToEnter thumb)
        // ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .padding(thumbPadding)
                .offset { IntOffset(thumbOffsetPx.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color(0xFF0F0F0F))
                .border(1.dp, Color(0x33FFFFFF), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (animatedProgress >= 0.99f) {
                Icon(
                    imageVector = AnonChatIcons.Check,
                    contentDescription = "Sent",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = AnonChatIcons.Send,
                    contentDescription = "Uploading",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
