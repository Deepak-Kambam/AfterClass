package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatShapes

/**
 * High-efficiency, battery-conscious live presence status dot.
 * Emits a subtle, gentle ambient aura pulse once every 7.5 seconds (idle 85% of the time).
 */
@Composable
fun LivePresenceDot(
    modifier: Modifier = Modifier,
    size: Dp = 6.dp,
    color: Color = AnonChatColors.Success,
    isPulsing: Boolean = true
) {
    val appearance = org.example.annochat.presentation.theme.LocalAppearanceSettings.current
    val shouldPulse = isPulsing && appearance.animationsEnabled && appearance.campusEnergy != org.example.annochat.data.model.CampusEnergy.QUIET

    if (!shouldPulse) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "presencePulse")
    
    // Very gentle ring aura that expands and dissolves every 7.5 seconds
    val auraScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 7500
                1.0f at 0 using FastOutSlowInEasing
                2.1f at 1300 using FastOutSlowInEasing
                2.1f at 7500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "auraScale"
    )

    val auraAlpha by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 7500
                0.50f at 0 using FastOutSlowInEasing
                0.0f at 1300
                0.0f at 7500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "auraAlpha"
    )

    Box(
        modifier = modifier.size(size * 2.2f),
        contentAlignment = Alignment.Center
    ) {
        // Ambient soft pulse aura (only visible during the 1.3s pulse window)
        if (auraAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(auraScale)
                    .clip(CircleShape)
                    .background(color.copy(alpha = auraAlpha))
            )
        }

        // Core solid presence dot
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

/**
 * Live presence pill badge showing real-time online count with smooth numeric transitions
 * and subtle presence pulse.
 */
@Composable
fun LivePresenceBadge(
    onlineCount: Int,
    connectionState: ConnectionState? = null,
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.dp
) {
    val isConnected = connectionState == null || connectionState.status == ConnectionStatus.CONNECTED
    val isReconnecting = connectionState?.status == ConnectionStatus.CONNECTING ||
            connectionState?.status == ConnectionStatus.RECONNECTING
    val dotColor = when {
        isReconnecting -> AnonChatColors.Warning
        isConnected -> AnonChatColors.Success
        else -> AnonChatColors.Offline
    }

    Box(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(AnonChatColors.SurfacePrimary)
            .border(1.dp, AnonChatColors.Border, AnonChatShapes.pill)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LivePresenceDot(
                size = dotSize,
                color = dotColor,
                isPulsing = isConnected
            )

            Spacer(Modifier.width(6.dp))

            AnimatedContent(
                targetState = onlineCount,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) 1 else -1
                    (slideInVertically(tween(260, easing = FastOutSlowInEasing)) { slideDirection * (it / 2) } + fadeIn(tween(220)))
                        .togetherWith(slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -slideDirection * (it / 2) } + fadeOut(tween(180)))
                },
                label = "onlineCountRoll"
            ) { count ->
                val label = when {
                    isReconnecting -> "Reconnecting"
                    !isConnected -> "Offline"
                    count > 0 -> "$count online"
                    else -> "Connected"
                }
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = AnonChatColors.TextPrimary,
                    letterSpacing = (-0.1).sp
                )
            }
        }
    }
}
