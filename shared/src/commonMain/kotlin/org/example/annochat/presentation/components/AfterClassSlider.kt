package org.example.annochat.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.theme.LocalAppearanceSettings

/**
 * Premium sleek slider tailored for AfterClass.
 *
 * Replaces the bulky M3 default slider with a refined 6dp slim track and a tactile 20dp thumb.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterClassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    activeColor: Color? = null,
    enabled: Boolean = true
) {
    val appearance = LocalAppearanceSettings.current
    val effectiveActiveColor = activeColor ?: Color(appearance.accent.colorHex)
    val isWhite = effectiveActiveColor == Color.White || effectiveActiveColor.value == 0xFFFFFFFFUL || effectiveActiveColor.value == 0xFFFFFFFFL.toULong()

    val trackActive = if (isWhite) Color.White else effectiveActiveColor
    val thumbBg = if (isWhite) Color.White else effectiveActiveColor

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier.height(36.dp),
        thumb = {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .shadow(elevation = 3.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(thumbBg)
                    .border(
                        width = 1.dp,
                        color = if (isWhite) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = SliderDefaults.colors(
                    activeTrackColor = trackActive,
                    inactiveTrackColor = AnonChatColors.SurfaceTertiary
                ),
                modifier = Modifier.height(6.dp),
                drawStopIndicator = null
            )
        }
    )
}
