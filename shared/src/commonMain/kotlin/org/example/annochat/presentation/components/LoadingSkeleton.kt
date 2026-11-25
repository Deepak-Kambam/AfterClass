package org.example.annochat.presentation.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing

/**
 * Solid-colour pulse skeleton — no gradient, no Brush.
 * The colour alternates between SkeletonBase and SkeletonHighlight.
 */
@Composable
fun rememberSkeletonColor(): Color {
    val transition = rememberInfiniteTransition()
    val color by transition.animateColor(
        initialValue = AnonChatColors.SkeletonBase,
        targetValue  = AnonChatColors.SkeletonHighlight,
        animationSpec = infiniteRepeatable(
            animation   = tween(durationMillis = 900),
            repeatMode  = RepeatMode.Reverse
        )
    )
    return color
}

@Composable
fun RoomItemSkeleton(
    modifier: Modifier = Modifier,
    color: Color = rememberSkeletonColor()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AnonChatShapes.card)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.BorderSubtle, AnonChatShapes.card)
            .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AnonChatSpacing.avatarMedium)
                .clip(AnonChatShapes.medium)
                .background(color)
        )

        Spacer(Modifier.width(AnonChatSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(AnonChatShapes.badge)
                    .background(color)
            )
            Spacer(Modifier.height(AnonChatSpacing.xs))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(11.dp)
                    .clip(AnonChatShapes.badge)
                    .background(color)
            )
        }
    }
}

@Composable
fun IdentityChipSkeleton(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    color: Color = rememberSkeletonColor()
) {
    Row(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.BorderSubtle, AnonChatShapes.pill)
            .padding(
                horizontal = if (compact) 6.dp else AnonChatSpacing.sm,
                vertical = if (compact) 4.dp else AnonChatSpacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 20.dp else AnonChatSpacing.avatarSmall)
                .clip(CircleShape)
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(if (compact) 40.dp else 72.dp)
                .height(if (compact) 10.dp else 12.dp)
                .clip(AnonChatShapes.badge)
                .background(color)
        )
    }
}

@Composable
fun IdentityCardSkeleton(
    modifier: Modifier = Modifier,
    color: Color = rememberSkeletonColor()
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AnonChatShapes.container)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.BorderSubtle, AnonChatShapes.container)
            .padding(AnonChatSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(AnonChatSpacing.avatarLarge)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(AnonChatSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .clip(AnonChatShapes.badge)
                    .background(color)
            )
            Spacer(Modifier.height(AnonChatSpacing.xs))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(12.dp)
                    .clip(AnonChatShapes.badge)
                    .background(color)
            )
        }
    }
}

@Composable
fun MessageSkeleton(
    isOwn: Boolean = false,
    modifier: Modifier = Modifier,
    color: Color = rememberSkeletonColor()
) {
    val shape = if (isOwn) AnonChatShapes.bubbleOwn else AnonChatShapes.bubblePeer

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .width(if (isOwn) 200.dp else 260.dp)
                .height(56.dp)
                .clip(shape)
                .background(color)
                .border(1.dp, AnonChatColors.BorderSubtle, shape)
        )
    }
}
