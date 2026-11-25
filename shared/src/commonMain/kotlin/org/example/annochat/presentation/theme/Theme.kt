package org.example.annochat.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import org.example.annochat.data.model.AppearanceSettings
import org.example.annochat.presentation.designsystem.AnonChatColors
import org.example.annochat.presentation.designsystem.AnonChatShapes

val LocalLiquidGlassEnabled = compositionLocalOf { false }
val LocalAppearanceSettings = compositionLocalOf { AppearanceSettings() }

private val AppShapes = Shapes(
    extraSmall = AnonChatShapes.badge,
    small = AnonChatShapes.small,
    medium = AnonChatShapes.medium,
    large = AnonChatShapes.large,
    extraLarge = AnonChatShapes.composer
)

@Composable
fun AnonChatTheme(
    liquidGlassEnabled: Boolean = false,
    appearanceSettings: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit
) {
    val accentColor = Color(appearanceSettings.accent.colorHex)
    val accentSecondary = Color(appearanceSettings.accent.secondaryHex)

    val onPrimaryColor = if ((0.2126f * accentColor.red + 0.7152f * accentColor.green + 0.0722f * accentColor.blue) > 0.45f) Color.Black else Color.White
    val onSecondaryColor = if ((0.2126f * accentSecondary.red + 0.7152f * accentSecondary.green + 0.0722f * accentSecondary.blue) > 0.45f) Color.Black else Color.White

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = onPrimaryColor,
        primaryContainer = accentSecondary.copy(alpha = 0.25f),
        onPrimaryContainer = onPrimaryColor,
        secondary = accentSecondary,
        onSecondary = onSecondaryColor,
        surface = AnonChatColors.Surface,
        onSurface = AnonChatColors.TextPrimary,
        surfaceVariant = AnonChatColors.SurfaceElevated,
        onSurfaceVariant = AnonChatColors.TextSecondary,
        background = AnonChatColors.Background,
        onBackground = AnonChatColors.TextPrimary,
        outline = AnonChatColors.Border,
        error = AnonChatColors.Error,
        onError = AnonChatColors.OnPrimary
    )

    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * appearanceSettings.textSize.scaleMultiplier
    )

    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides customDensity,
        LocalLiquidGlassEnabled provides liquidGlassEnabled,
        LocalAppearanceSettings provides appearanceSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            content = content
        )
    }
}


