package org.example.annochat.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Premium subtle animated typing indicator.
 * Displays 3 pulsing dots with staggered phases inside a translucent glass pill.
 */
import androidx.compose.ui.graphics.Color

/**
 * Premium subtle animated typing indicator.
 * Displays 3 pulsing dots with staggered phases inside a translucent glass pill.
 */
@Composable
fun TypingIndicator(
    typingUsers: Collection<String>,
    modifier: Modifier = Modifier
) {
    if (typingUsers.isEmpty()) return

    val typingText = when (typingUsers.size) {
        1 -> "${typingUsers.first()} is typing"
        2 -> {
            val list = typingUsers.toList()
            "${list[0]} and ${list[1]} are typing"
        }
        else -> "${typingUsers.size} people are typing"
    }

    Row(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(Color(0xE6141418))
            .border(1.dp, Color.White.copy(alpha = 0.14f), AnonChatShapes.pill)
            .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated 3 pulsing dots
        PulsingDots(modifier = Modifier.padding(end = AnonChatSpacing.sm))

        Text(
            text = "$typingText…",
            style = AnonChatTypography.Caption,
            color = AnonChatColors.TextPrimary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PulsingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sequentialTypingDots")

    // Lead status dot: gentle breathing
    val leadAlpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leadDotAlpha"
    )
    val leadScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "leadDotScale"
    )

    // Three sequential bouncing follower dots
    val dot1Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Offset"
    )
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1Alpha"
    )

    val dot2Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Offset"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 120, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2Alpha"
    )

    val dot3Offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = -3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Offset"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, delayMillis = 240, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3Alpha"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ● Lead dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .graphicsLayer {
                    alpha = leadAlpha
                    scaleX = leadScale
                    scaleY = leadScale
                }
                .clip(CircleShape)
                .background(AnonChatColors.Primary)
        )

        // • • • Staggered wave follower dots
        FollowerDot(offsetY = dot1Offset, alpha = dot1Alpha)
        FollowerDot(offsetY = dot2Offset, alpha = dot2Alpha)
        FollowerDot(offsetY = dot3Offset, alpha = dot3Alpha)
    }
}

@Composable
private fun FollowerDot(offsetY: Float, alpha: Float) {
    Box(
        modifier = Modifier
            .size(3.5.dp)
            .graphicsLayer {
                translationY = offsetY
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(AnonChatColors.Primary.copy(alpha = 0.85f))
    )
}

