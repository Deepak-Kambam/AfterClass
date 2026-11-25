package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatIcons
import org.example.annochat.presentation.designsystem.AnonChatShapes
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import org.example.annochat.presentation.components.AnonChatPrimaryButton

/**
 * Clean empty state component with a solid icon orb.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = AnonChatIcons.Chat,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AnonChatSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Solid icon orb
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
                imageVector = icon,
                contentDescription = null,
                tint = AnonChatColors.Primary,
                modifier = Modifier.size(AnonChatSpacing.iconLarge)
            )
        }

        Spacer(Modifier.height(AnonChatSpacing.lg))

        Text(
            text = title,
            style = AnonChatTypography.RoomTitle,
            color = AnonChatColors.TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(AnonChatSpacing.xs))

        Text(
            text = message,
            style = AnonChatTypography.Caption,
            color = AnonChatColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AnonChatSpacing.md)
        )

        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(AnonChatSpacing.lg))

            AnonChatPrimaryButton(
                text = actionLabel,
                onClick = onAction
            )
        }
    }
}
