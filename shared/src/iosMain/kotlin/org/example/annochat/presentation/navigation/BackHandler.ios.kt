package org.example.annochat.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iOS handles gestures natively and through on-screen back navigation controls.
}
