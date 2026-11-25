package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

/**
 * Clean, spacious, and consistent room list item.
 * Displays primary room name with intentional typography hierarchy
 * and standardized corner radiuses.
 */
@Composable
fun RoomListItem(
    room: PublicRoomDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roomIcon = getRoomIcon(room.icon, room.name)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AnonChatShapes.card)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.BorderSubtle, AnonChatShapes.card)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = AnonChatColors.Primary)
            ) { onClick() }
            .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Container — solid accent surface with standard medium corner radius
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(AnonChatShapes.medium)
                .background(AnonChatColors.BadgePrimaryBg)
                .border(1.dp, AnonChatColors.BadgePrimaryBorder, AnonChatShapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = roomIcon,
                contentDescription = null,
                tint = AnonChatColors.Primary,
                modifier = Modifier.size(AnonChatSpacing.iconMedium)
            )
        }

        Spacer(Modifier.width(AnonChatSpacing.md))

        // Room Name (Clean, prominent, uncluttered)
        Text(
            text = "#${room.name}",
            style = AnonChatTypography.RoomTitle,
            color = AnonChatColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        Spacer(Modifier.width(AnonChatSpacing.sm))

        // Navigation Chevron
        Icon(
            imageVector = AnonChatIcons.ChevronRight,
            contentDescription = "Enter room #${room.name}",
            tint = AnonChatColors.TextMuted,
            modifier = Modifier.size(AnonChatSpacing.iconMedium)
        )
    }
}

/**
 * Maps backend room icons to consistent design-system vector icons.
 */
fun getRoomIcon(iconStr: String?, roomName: String): ImageVector {
    val key = (iconStr ?: roomName).lowercase()
    return when {
        "terminal" in key || "cse" in key || "code" in key || "tech" in key || "dev" in key -> AnonChatIcons.Terminal
        "zap" in key || "elect" in key || "ece" in key || "chip" in key || "circuit" in key -> AnonChatIcons.Cpu
        "tool" in key || "mech" in key || "gear" in key || "wrench" in key || "eng" in key -> AnonChatIcons.Wrench
        "civil" in key || "build" in key || "arch" in key || "construct" in key -> AnonChatIcons.Building
        "book" in key || "faculty" in key || "prof" in key || "edu" in key || "grad" in key -> AnonChatIcons.GraduationCap
        "hostel" in key || "dorm" in key || "bed" in key || "living" in key || "moon" in key -> AnonChatIcons.Bed
        "globe" in key || "public" in key || "anon" in key || "world" in key || "campus" in key -> AnonChatIcons.Globe
        "lock" in key || "shield" in key || "sec" in key || "privacy" in key -> AnonChatIcons.Lock
        else -> AnonChatIcons.Chat
    }
}
