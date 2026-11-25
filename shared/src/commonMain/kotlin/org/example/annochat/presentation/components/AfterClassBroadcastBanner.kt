package org.example.annochat.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.example.annochat.data.model.BroadcastSeverity
import org.example.annochat.data.model.SystemBroadcast
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Production-quality AfterClass Campus Broadcast Banner.
 *
 * Dedicated system-level notification component honoring the strictly monochromatic,
 * dark, atmospheric AfterClass design language:
 * - INFO: Subtle obsidian glass surface with hairline border and radio beacon icon.
 * - WARNING: Refined elevated dark card with prominent light-grey border highlight.
 * - EMERGENCY: High-impact pure white border, elevated shadow, and bold emergency badge.
 *
 * Features:
 * - Dynamic countdown progress bar when duration > 0.
 * - Room context tag for room-specific broadcasts.
 * - Accessible 48dp dismissal touch target.
 * - Screen-reader semantic descriptions.
 * - Fluid entrance and exit transitions.
 */
@Composable
fun AfterClassBroadcastBanner(
    broadcast: SystemBroadcast,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isReducedMotion: Boolean = false
) {
    val durationSeconds = broadcast.duration
    var progress by remember(broadcast.broadcastId, durationSeconds) {
        mutableFloatStateOf(if (durationSeconds > 0) 1.0f else 0.0f)
    }

    if (durationSeconds > 0) {
        LaunchedEffect(broadcast.broadcastId, durationSeconds) {
            val totalSteps = durationSeconds * 20 // 50ms interval
            for (step in 0..totalSteps) {
                progress = 1.0f - (step.toFloat() / totalSteps.toFloat())
                delay(50L)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 60, easing = LinearEasing),
        label = "BroadcastTimerProgress"
    )

    // Visual attributes based on severity (100% solid opaque obsidian - no transparency, no borders)
    val (surfaceColor, badgeText, badgeBg, badgeTextColor) = when (broadcast.severity) {
        BroadcastSeverity.EMERGENCY -> SeverityStyle(
            surface = Color(0xFF141416),
            badgeText = "EMERGENCY BROADCAST",
            badgeBg = Color.White,
            badgeTextColor = Color.Black
        )
        BroadcastSeverity.WARNING -> SeverityStyle(
            surface = Color(0xFF18181A),
            badgeText = "CAMPUS NOTICE",
            badgeBg = Color(0xFF2C2C30),
            badgeTextColor = Color.White
        )
        BroadcastSeverity.INFO -> SeverityStyle(
            surface = Color(0xFF161618),
            badgeText = "SYSTEM ANNOUNCEMENT",
            badgeBg = Color(0xFF242428),
            badgeTextColor = AnonChatColors.TextSecondary
        )
    }

    val a11yDescription = buildString {
        append("System Announcement: ")
        append(broadcast.severity.name)
        append(". ")
        append(broadcast.message)
        if (!broadcast.roomId.isNullOrBlank()) {
            append(". Room: ")
            append(broadcast.roomId)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
            }
    ) {
        // High-contrast, borderless, fully opaque dialog box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(20.dp))
                .background(surfaceColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Header row: Icon + Severity Badge + Room Tag + Close Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System Broadcast Icon (Atmospheric beacon or megaphone)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (broadcast.severity == BroadcastSeverity.EMERGENCY) {
                                AnonChatIcons.BroadcastBeacon
                            } else {
                                AnonChatIcons.Megaphone
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Severity Pill Badge (borderless solid pill)
                    Box(
                        modifier = Modifier
                            .clip(AnonChatShapes.pill)
                            .background(badgeBg)
                            .padding(horizontal = 9.dp, vertical = 3.5.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeTextColor,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Optional Room Scope Tag (e.g. #dev-hub)
                    if (!broadcast.roomId.isNullOrBlank() && !broadcast.isGlobal) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(AnonChatShapes.pill)
                                .background(Color(0xFF242428))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#${broadcast.roomId.trim().removePrefix("#")}",
                                color = AnonChatColors.TextMuted,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Manual Dismiss Button (>= 48dp accessible touch target, borderless)
                    Box(
                        modifier = Modifier
                            .size(AnonChatSpacing.touchTargetMin)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClickLabel = "Dismiss system announcement"
                            ) {
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = AnonChatIcons.Close,
                                contentDescription = "Dismiss",
                                tint = AnonChatColors.TextSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Broadcast Message Body (Crisp, focused typography)
                Text(
                    text = broadcast.message,
                    color = AnonChatColors.TextPrimary,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    fontWeight = if (broadcast.severity == BroadcastSeverity.EMERGENCY) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                // Optional Duration Countdown Bar
                if (durationSeconds > 0) {
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction = animatedProgress.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(
                                    if (broadcast.severity == BroadcastSeverity.EMERGENCY) Color.White else Color(0xFFCCCCCC)
                                )
                        )
                    }
                }
            }
        }
    }
}

private data class SeverityStyle(
    val surface: Color,
    val badgeText: String,
    val badgeBg: Color,
    val badgeTextColor: Color
)
