package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.data.model.CampusEnergy
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.navigation.BackHandler
import org.example.annochat.presentation.theme.LocalAppearanceSettings
import org.example.annochat.presentation.theme.LocalLiquidGlassEnabled

/**
 * Formats notification count according to AnnoChat specifications:
 * 0: null (no badge)
 * 1–99: exact string representation
 * 100+: "99+"
 */
fun formatNotificationBadge(count: Int): String? {
    return when {
        count <= 0 -> null
        count > 99 -> "99+"
        else -> count.toString()
    }
}

/**
 * Production-quality AnnoChat "Candy Menu" Launcher.
 *
 * Signature interaction:
 * - Resting: A compact, refined 3×3 grid of dots ("Candy Box") with subtle atmospheric breathing glow.
 *   When unread notifications exist, the center dot activates with the dynamic brand accent.
 * - Tap: Fluidly transforms into a vertical launch stack of secondary actions:
 *   1. Notifications (with reactive live unread badge)
 *   2. Create / Join Room (campus rooms creator/joiner)
 * - Coexists seamlessly above the floating CapsuleBottomNavBar.
 * - Outside tap and system back button dismiss the menu.
 */
@Composable
fun CandyMenu(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    onCreateJoinRoomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appearance = LocalAppearanceSettings.current
    val isLiquidGlass = LocalLiquidGlassEnabled.current
    val accentColor = MaterialTheme.colorScheme.primary
    val onAccentColor = MaterialTheme.colorScheme.onPrimary

    // Android back handler to close menu if expanded
    BackHandler(enabled = isExpanded) {
        onExpandedChange(false)
    }

    // Dynamic motion & energy awareness: disable or minimize glow for quiet campus energy or disabled animations
    val isReducedMotion = !appearance.animationsEnabled || appearance.campusEnergy == CampusEnergy.QUIET || !appearance.afterglowEnabled
    val infiniteTransition = rememberInfiniteTransition(label = "candyGlowTransition")

    val glowAlpha by if (!isReducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0.16f * appearance.glowIntensity,
            targetValue = 0.42f * appearance.glowIntensity,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (2800 / appearance.glowSpeed.coerceAtLeast(0.5f)).toInt(),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "candyGlowAlpha"
        )
    } else {
        remember(appearance.glowIntensity) {
            androidx.compose.runtime.mutableStateOf(0.18f * appearance.glowIntensity)
        }
    }

    val glowRadius by if (!isReducedMotion) {
        infiniteTransition.animateFloat(
            initialValue = 3.dp.value,
            targetValue = 9.dp.value,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (2800 / appearance.glowSpeed.coerceAtLeast(0.5f)).toInt(),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "candyGlowRadius"
        )
    } else {
        remember { androidx.compose.runtime.mutableStateOf(4.dp.value) }
    }

    // Anchor button morph animation
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "candyButtonRotation"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.04f else 1.0f,
        animationSpec = tween(
            durationMillis = 280,
            easing = FastOutSlowInEasing
        ),
        label = "candyButtonScale"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomEnd
    ) {
        // Fullscreen dismiss scrim when expanded (does not obstruct status or system bars)
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onExpandedChange(false)
                    }
            )
        }

        // Action Menu & Main Anchor Stack
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(bottom = 0.dp)
        ) {
            // Secondary Action Items Stack (visible when expanded)
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(240, easing = FastOutSlowInEasing)) { it / 3 } +
                        scaleIn(animationSpec = tween(240, easing = FastOutSlowInEasing), initialScale = 0.85f),
                exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                        slideOutVertically(tween(180, easing = FastOutSlowInEasing)) { it / 4 } +
                        scaleOut(animationSpec = tween(180, easing = FastOutSlowInEasing), targetScale = 0.85f)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    // Action 1: Notifications (Top)
                    CandyActionItem(
                        icon = AnonChatIcons.Bell,
                        title = "Notifications",
                        badgeText = formatNotificationBadge(unreadNotificationCount),
                        accentColor = accentColor,
                        onAccentColor = onAccentColor,
                        isLiquidGlass = isLiquidGlass,
                        onClick = {
                            onExpandedChange(false)
                            onNotificationsClick()
                        },
                        itemIndex = 0
                    )

                    // Action 2: Create / Join Room (Bottom)
                    CandyActionItem(
                        icon = AnonChatIcons.Plus,
                        title = "Create / Join",
                        badgeText = null,
                        accentColor = accentColor,
                        onAccentColor = onAccentColor,
                        isLiquidGlass = isLiquidGlass,
                        onClick = {
                            onExpandedChange(false)
                            onCreateJoinRoomClick()
                        },
                        itemIndex = 1
                    )
                }
            }

            // The 3×3 Candy Menu FAB Anchor Control
            val buttonBackground = if (isLiquidGlass) {
                Color(0xFF141416).copy(alpha = 0.86f)
            } else {
                AnonChatColors.SurfaceSecondary
            }

            val buttonBorderColor = if (isExpanded) {
                accentColor.copy(alpha = 0.65f)
            } else if (unreadNotificationCount > 0) {
                accentColor.copy(alpha = 0.45f)
            } else {
                AnonChatColors.Border.copy(alpha = 0.80f)
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .scale(buttonScale)
                    // Signature atmospheric outer glow
                    .drawBehind {
                        val effAlpha = if (isExpanded) (glowAlpha + 0.18f).coerceAtMost(0.70f) else glowAlpha
                        val glowPx = glowRadius.dp.toPx()
                        drawCircle(
                            color = accentColor.copy(alpha = effAlpha),
                            radius = size.minDimension / 2f + glowPx
                        )
                    }
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        spotColor = Color.Black.copy(alpha = 0.85f),
                        ambientColor = Color.Black.copy(alpha = 0.60f)
                    )
                    .clip(CircleShape)
                    .background(buttonBackground)
                    .border(1.2.dp, buttonBorderColor, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = accentColor),
                        onClick = { onExpandedChange(!isExpanded) }
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isExpanded) "Close quick actions" else "Open quick actions"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExpanded) {
                        // Dismiss Cross Icon
                        Icon(
                            imageVector = AnonChatIcons.Close,
                            contentDescription = null,
                            tint = AnonChatColors.TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        // Signature 3×3 Candy Box Dots Grid
                        CandyGridDots(
                            hasUnread = unreadNotificationCount > 0,
                            accentColor = accentColor,
                            defaultDotColor = AnonChatColors.TextPrimary.copy(alpha = 0.88f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3×3 Grid of circular dots.
 * Middle dot highlights with dynamic accent color when unread notifications are pending.
 */
@Composable
private fun CandyGridDots(
    hasUnread: Boolean,
    accentColor: Color,
    defaultDotColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val totalWidth = size.width
        val totalHeight = size.height
        val dotRadius = 1.9.dp.toPx()
        val activeDotRadius = 2.4.dp.toPx()

        val xs = listOf(totalWidth * 0.18f, totalWidth * 0.50f, totalWidth * 0.82f)
        val ys = listOf(totalHeight * 0.18f, totalHeight * 0.50f, totalHeight * 0.82f)

        for (r in 0..2) {
            for (c in 0..2) {
                val isCenter = (r == 1 && c == 1)
                val dotColor = if (isCenter && hasUnread) accentColor else defaultDotColor
                val radius = if (isCenter && hasUnread) activeDotRadius else dotRadius

                drawCircle(
                    color = dotColor,
                    radius = radius,
                    center = Offset(xs[c], ys[r])
                )

                // If center dot is active, draw a micro aura ring
                if (isCenter && hasUnread) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.35f),
                        radius = radius + 2.dp.toPx(),
                        center = Offset(xs[c], ys[r]),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Individual action item in the expanded Candy Menu stack.
 * Features rounded pill container, icon container, title typography, and reactive badge.
 */
@Composable
private fun CandyActionItem(
    icon: ImageVector,
    title: String,
    badgeText: String?,
    accentColor: Color,
    onAccentColor: Color,
    isLiquidGlass: Boolean,
    onClick: () -> Unit,
    itemIndex: Int,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isLiquidGlass) {
        Color(0xFF141416).copy(alpha = 0.88f)
    } else {
        AnonChatColors.SurfaceSecondary
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Action Capsule Pill
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = Color.Black.copy(alpha = 0.75f)
                )
                .clip(RoundedCornerShape(22.dp))
                .background(containerBg)
                .border(1.dp, AnonChatColors.Border.copy(alpha = 0.70f), RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = accentColor),
                    onClick = onClick
                )
                .padding(horizontal = 14.dp, vertical = 9.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = title
                }
        ) {
            // Crisp Icon container
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AnonChatColors.SurfaceTertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AnonChatColors.TextPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Title
            Text(
                text = title,
                color = AnonChatColors.TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )

            // Reactive Badge Pill (e.g. for Notifications)
            if (badgeText != null) {
                AnimatedContent(
                    targetState = badgeText,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.8f))
                    },
                    label = "badgeContentTransition"
                ) { targetBadge ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(accentColor)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetBadge,
                            color = onAccentColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
