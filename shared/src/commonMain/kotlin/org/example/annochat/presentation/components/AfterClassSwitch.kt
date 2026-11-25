package org.example.annochat.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.theme.LocalAppearanceSettings

/**
 * Premium custom toggle switch for AfterClass.
 *
 * Ensures sharp, tactile contrast across all theme accents (including Moonlight White).
 * Completely replaces Material3 Switch to avoid solid white blob rendering bugs.
 */
@Composable
fun AfterClassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color? = null,
    enabled: Boolean = true
) {
    val appearance = LocalAppearanceSettings.current
    val effectiveActiveColor = activeColor ?: Color(appearance.accent.colorHex)

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 23.dp else 3.dp,
        animationSpec = tween(
            durationMillis = 240,
            easing = FastOutSlowInEasing
        ),
        label = "switchThumbOffset"
    )

    val isWhiteAccent = effectiveActiveColor == Color.White || effectiveActiveColor.value == 0xFFFFFFFFUL || effectiveActiveColor.value == 0xFFFFFFFFL.toULong()

    val trackBg by animateColorAsState(
        targetValue = if (checked) {
            if (isWhiteAccent) Color.White else effectiveActiveColor
        } else {
            AnonChatColors.SurfaceTertiary
        },
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "switchTrackBg"
    )

    val trackBorder by animateColorAsState(
        targetValue = if (checked) {
            if (isWhiteAccent) Color(0xFFD4D4D8) else effectiveActiveColor.copy(alpha = 0.6f)
        } else {
            AnonChatColors.Border
        },
        label = "switchTrackBorder"
    )

    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            if (isWhiteAccent) Color(0xFF141416) else Color.White
        } else {
            Color(0xFFA1A1AA)
        },
        label = "switchThumbColor"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackBg)
            .border(
                width = 1.dp,
                color = trackBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(thumbColor)
                .border(
                    width = 0.5.dp,
                    color = if (checked && isWhiteAccent) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        )
    }
}
