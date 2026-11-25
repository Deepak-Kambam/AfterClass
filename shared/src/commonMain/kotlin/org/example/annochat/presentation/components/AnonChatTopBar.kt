package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatSpacing
import org.example.annochat.presentation.designsystem.AnonChatTypography
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Frosted Glass / Glassmorphic Top App Bar.
 *
 * Uses a translucent dark glass surface with subtle frosted sheen and specular bottom highlight,
 * visually integrating with the atmospheric background while maintaining crisp readability,
 * modern AfterClass rounded styling, and high performance.
 */
@Composable
fun AnonChatTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // Translucent dark glass background with vertical depth gradient
    val glassBackground = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xCC111217), // 80% opacity dark slate glass
                Color(0xB80E0F14), // 72% opacity dark glass
                Color(0xC40B0C10)  // 77% opacity dark glass
            )
        )
    }

    // Frosted specular sheen overlay (subtle light falloff from top)
    val frostedSheen = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(glassBackground)
            .background(frostedSheen)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (navigationIcon != null && onNavigationClick != null) {
                    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .size(AnonChatSpacing.touchTargetMin)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = ripple(bounded = true, color = AnonChatColors.Primary)
                            ) { onNavigationClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = navigationIcon,
                            contentDescription = "Navigate back",
                            tint = AnonChatColors.TextPrimary,
                            modifier = Modifier.size(AnonChatSpacing.iconLarge)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .then(
                            if (onTitleClick != null) {
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onTitleClick() }
                            } else Modifier
                        )
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = (-0.4).sp,
                        color = AnonChatColors.TextPrimary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = AnonChatTypography.Caption,
                            fontSize = 12.5.sp,
                            color = AnonChatColors.TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AnonChatSpacing.sm)
            ) {
                actions()
            }
        }

        // Frosted glass specular bottom border / highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    )
                )
        )
    }
}
