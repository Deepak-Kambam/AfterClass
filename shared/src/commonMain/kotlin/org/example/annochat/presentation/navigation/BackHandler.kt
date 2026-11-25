package org.example.annochat.presentation.navigation

import androidx.compose.runtime.Composable

/**
 * Platform-agnostic back button handler.
 * Intercepts hardware back button and predictive back gestures on Android,
 * and no-ops on platforms without system back buttons (iOS).
 */
@Composable
expect fun BackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)
