package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Solid, clean identity card displayed on the Rooms dashboard.
 * No radial glow, no glassmorphism — bold and legible.
 */
@Composable
fun IdentityCard(
    identity: UserIdentityDto?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(AnonChatShapes.container)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, AnonChatShapes.container)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AnonChatSpacing.lg)
        ) {
            // Top Row: Section Label + Security Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANONYMOUS IDENTITY",
                    style = AnonChatTypography.SectionHeader,
                    color = AnonChatColors.Primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(AnonChatShapes.pill)
                        .background(AnonChatColors.BadgeSuccessBg)
                        .border(1.dp, AnonChatColors.BadgeSuccessBorder, AnonChatShapes.pill)
                        .padding(horizontal = AnonChatSpacing.sm, vertical = AnonChatSpacing.xxs)
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Lock,
                        contentDescription = null,
                        tint = AnonChatColors.Success,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(Modifier.width(AnonChatSpacing.xs))
                    Text(
                        text = "Encrypted",
                        style = AnonChatTypography.Caption,
                        color = AnonChatColors.Success,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(AnonChatSpacing.md))

            // Main Row: Avatar + Handle + Subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Solid avatar orb — primary solid
                Box(
                    modifier = Modifier
                        .size(AnonChatSpacing.avatarLarge)
                        .clip(CircleShape)
                        .background(AnonChatColors.BadgePrimaryBg)
                        .border(
                            width = 1.dp,
                            color = AnonChatColors.BadgePrimaryBorder,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AnonChatIcons.Paw,
                        contentDescription = null,
                        tint = AnonChatColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(AnonChatSpacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = identity?.anonName ?: "Ephemeral Persona",
                            style = AnonChatTypography.Label,
                            color = AnonChatColors.TextPrimary
                        )

                        if (identity?.isHost == true) {
                            Spacer(Modifier.width(AnonChatSpacing.xs))
                            Box(
                                modifier = Modifier
                                .clip(AnonChatShapes.badge)
                                .background(AnonChatColors.BadgeWarningBg)
                                .border(1.dp, AnonChatColors.BadgeWarningBorder, AnonChatShapes.badge)
                                .padding(horizontal = AnonChatSpacing.xs, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HOST",
                                    style = AnonChatTypography.Caption,
                                    color = AnonChatColors.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(AnonChatSpacing.xxs))

                    val subtitleText = when {
                        identity == null -> "Establishing secure socket session…"
                        identity.isHost -> "Host administration mode active"
                        !identity.sciName.isNullOrBlank() -> "${identity.sciName} • Your identity is private"
                        else -> "Your identity is private & untracked"
                    }

                    Text(
                        text = subtitleText,
                        style = AnonChatTypography.Caption,
                        color = if (identity != null) AnonChatColors.TextSecondary else AnonChatColors.TextMuted
                    )
                }
            }
        }
    }
}

/**
 * Maps an animal name or anon identifier to an emoji representation.
 */
fun getAnimalEmoji(name: String?): String {
    if (name.isNullOrBlank()) return "👤"
    val lower = name.lowercase()
    return when {
        "wolf" in lower -> "🐺"
        "fox" in lower -> "🦊"
        "bear" in lower -> "🐻"
        "tiger" in lower -> "🐯"
        "lion" in lower -> "🦁"
        "eagle" in lower -> "🦅"
        "owl" in lower -> "🦉"
        "hawk" in lower -> "🦅"
        "falcon" in lower -> "🦅"
        "panther" in lower -> "🐆"
        "leopard" in lower -> "🐆"
        "jaguar" in lower -> "🐆"
        "lynx" in lower -> "🐱"
        "badger" in lower -> "🦡"
        "otter" in lower -> "🦦"
        "beaver" in lower -> "🦫"
        "deer" in lower -> "🦌"
        "elk" in lower -> "🦌"
        "moose" in lower -> "🫎"
        "bison" in lower -> "🦬"
        "whale" in lower -> "🐋"
        "dolphin" in lower -> "🐬"
        "shark" in lower -> "🦈"
        "cobra" in lower -> "🐍"
        "viper" in lower -> "🐍"
        "python" in lower -> "🐍"
        "raven" in lower -> "🐦‍⬛"
        "crow" in lower -> "🐦‍⬛"
        else -> "🎭"
    }
}
