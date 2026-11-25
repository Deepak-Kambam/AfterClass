package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Deprecated / No-op: Glassmorphism and liquid morphism have been removed.
 * AnonChat uses 100% solid colors throughout the UI.
 */
@Composable
fun LiquidGlassToggle(
    isLiquidGlassEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Intentionally empty: no glassmorphism toggle in solid color theme
}
