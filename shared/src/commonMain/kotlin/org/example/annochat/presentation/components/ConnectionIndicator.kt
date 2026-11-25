package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

@Composable
fun ConnectionStatusBadge(
    connectionState: ConnectionState,
    onlineCount: Int,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusText) = when (connectionState.status) {
        ConnectionStatus.CONNECTED -> AnonChatColors.Success to if (onlineCount > 0) "$onlineCount online" else "Connected"
        ConnectionStatus.CONNECTING -> AnonChatColors.Warning to "Connecting"
        ConnectionStatus.RECONNECTING -> AnonChatColors.Warning to "Reconnecting"
        ConnectionStatus.ERROR -> AnonChatColors.Error to "Offline"
        ConnectionStatus.DISCONNECTED -> AnonChatColors.Offline to "Disconnected"
    }

    Row(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, AnonChatShapes.pill)
            .padding(horizontal = AnonChatSpacing.sm, vertical = AnonChatSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnonChatSpacing.xs)
    ) {
        // Battery-conscious live presence dot with 7.5s ambient pulse
        LivePresenceDot(
            size = 6.5.dp,
            color = statusColor,
            isPulsing = connectionState.status == ConnectionStatus.CONNECTED
        )

        AnimatedContent(
            targetState = statusText,
            transitionSpec = {
                (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 3 })
                    .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
            },
            label = "connectionStatusText"
        ) { text ->
            Text(
                text = text,
                style = AnonChatTypography.Badge,
                color = AnonChatColors.TextPrimary
            )
        }
    }
}

@Composable
fun ConnectionWarningBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val isVisible = connectionState.status != ConnectionStatus.CONNECTED

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val icon = when (connectionState.status) {
            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> AnonChatIcons.Refresh
            ConnectionStatus.ERROR, ConnectionStatus.DISCONNECTED -> AnonChatIcons.WifiOff
            ConnectionStatus.CONNECTED -> AnonChatIcons.Check
        }
        val message = when (connectionState.status) {
            ConnectionStatus.CONNECTING -> "Connecting to AnonChat network…"
            ConnectionStatus.RECONNECTING -> "Connection interrupted. Reconnecting…"
            ConnectionStatus.ERROR -> connectionState.errorMessage ?: "Chat server unreachable."
            ConnectionStatus.DISCONNECTED -> "Disconnected from server."
            ConnectionStatus.CONNECTED -> ""
        }
        val accentColor = when (connectionState.status) {
            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> AnonChatColors.Warning
            ConnectionStatus.ERROR -> AnonChatColors.Error
            ConnectionStatus.DISCONNECTED -> AnonChatColors.Offline
            ConnectionStatus.CONNECTED -> AnonChatColors.Success
        }
        val bannerBg = when (connectionState.status) {
            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> AnonChatColors.BadgeWarningBg
            ConnectionStatus.ERROR -> AnonChatColors.BadgeErrorBg
            ConnectionStatus.DISCONNECTED -> AnonChatColors.SurfaceSecondary
            ConnectionStatus.CONNECTED -> AnonChatColors.BadgeSuccessBg
        }
        val bannerBorder = when (connectionState.status) {
            ConnectionStatus.CONNECTING, ConnectionStatus.RECONNECTING -> AnonChatColors.BadgeWarningBorder
            ConnectionStatus.ERROR -> AnonChatColors.BadgeErrorBorder
            ConnectionStatus.DISCONNECTED -> AnonChatColors.Border
            ConnectionStatus.CONNECTED -> AnonChatColors.BadgeSuccessBorder
        }

        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.xs)
                .clip(AnonChatShapes.medium)
                .background(bannerBg)
                .border(1.dp, bannerBorder, AnonChatShapes.medium)
                .padding(horizontal = AnonChatSpacing.md, vertical = AnonChatSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(AnonChatSpacing.iconSmall)
            )
            Spacer(Modifier.width(AnonChatSpacing.sm))
            Text(
                text = message,
                style = AnonChatTypography.Caption,
                color = AnonChatColors.TextPrimary
            )
        }
    }
}
