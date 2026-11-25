package org.example.annochat.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.annochat.presentation.designsystem.AnonChatSpacing

/**
 * A large, transparent header used across the main screens (Chats, Contacts, Settings, Profile)
 * to blend seamlessly with the global space background.
 */
@Composable
fun AnonChatLargeHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    contentBelowSubtitle: @Composable (() -> Unit)? = null,
    rightContent: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AnonChatSpacing.lg, vertical = AnonChatSpacing.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left: Page Titles
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(end = AnonChatSpacing.sm)
        ) {
            Text(
                text = title,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            if (contentBelowSubtitle != null) {
                Spacer(Modifier.height(AnonChatSpacing.sm))
                contentBelowSubtitle()
            }
        }

        // Right: Custom Controls (e.g., Online Badge, Refresh button)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnonChatSpacing.xs),
            modifier = Modifier.padding(top = 8.dp)
        ) {
            rightContent()
        }
    }
}
