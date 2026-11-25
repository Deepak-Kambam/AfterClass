package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Spacer
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography

@Composable
fun AnonIdentityChip(
    identity: UserIdentityDto?,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (identity == null) {
        return
    }

    Row(
        modifier = modifier
            .clip(AnonChatShapes.pill)
            .background(AnonChatColors.SurfaceSecondary)
            .border(1.dp, AnonChatColors.Border, AnonChatShapes.pill)
            .padding(
                horizontal = if (compact) 6.dp else AnonChatSpacing.sm,
                vertical = if (compact) 4.dp else AnonChatSpacing.xs
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 20.dp else AnonChatSpacing.avatarSmall)
                .clip(CircleShape)
                .background(AnonChatColors.BadgePrimaryBg)
                .border(1.dp, AnonChatColors.BadgePrimaryBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AnonChatIcons.Paw,
                contentDescription = null,
                tint = AnonChatColors.Primary,
                modifier = Modifier.size(if (compact) 11.dp else 13.dp)
            )
        }

        val displayName = if (compact) {
            val tag = identity.publicId.takeLast(4)
            "#$tag"
        } else {
            identity.anonName
        }

        Text(
            text = displayName,
            style = AnonChatTypography.Badge,
            color = AnonChatColors.TextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        if (identity.isHost) {
            Text(
                text = "HOST",
                style = AnonChatTypography.Caption,
                fontSize = 9.sp,
                color = AnonChatColors.TextMuted
            )
        }
    }
}

/**
 * Returns an appropriate animal emoji based on the backend animal name.
 */
fun getAnimalEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "cat" in lower || "cheetah" in lower || "leopard" in lower || "lion" in lower || "tiger" in lower || "jaguar" in lower || "panther" in lower || "lynx" in lower -> "🐆"
        "fox" in lower || "wolf" in lower || "coyote" in lower || "jackal" in lower || "dingo" in lower -> "🦊"
        "bear" in lower || "panda" in lower -> "🐼"
        "owl" in lower || "eagle" in lower || "falcon" in lower || "hawk" in lower || "osprey" in lower || "raven" in lower -> "🦅"
        "penguin" in lower -> "🐧"
        "whale" in lower || "orca" in lower || "dolphin" in lower -> "🐬"
        "shark" in lower -> "🦈"
        "octopus" in lower || "cuttlefish" in lower -> "🐙"
        "frog" in lower || "axolotl" in lower -> "🐸"
        "chameleon" in lower || "gecko" in lower || "lizard" in lower || "dragon" in lower || "turtle" in lower || "crocodile" in lower || "alligator" in lower -> "🦎"
        "elephant" in lower -> "🐘"
        "rhino" in lower || "hippo" in lower -> "🦏"
        "zebra" in lower || "giraffe" in lower || "camel" in lower || "llama" in lower || "alpaca" in lower -> "🦒"
        "deer" in lower || "moose" in lower || "elk" in lower || "reindeer" in lower || "ibex" in lower || "bison" in lower -> "🦌"
        "monkey" in lower || "gorilla" in lower || "chimpanzee" in lower || "orangutan" in lower || "lemur" in lower -> "🐒"
        "koala" in lower || "kangaroo" in lower || "wombat" in lower || "quokka" in lower -> "🐨"
        "capybara" in lower || "beaver" in lower || "otter" in lower || "badger" in lower || "hedgehog" in lower || "platypus" in lower -> "🦫"
        else -> "🐾"
    }
}
