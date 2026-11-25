package org.example.annochat

import androidx.compose.runtime.Composable

/**
 * Composable side-effect that enables sensor-based screen rotation (e.g. landscape / portrait)
 * while active, and automatically restores portrait lock when dismissed / disposed.
 */
@Composable
expect fun AllowScreenRotationWhileActive()
