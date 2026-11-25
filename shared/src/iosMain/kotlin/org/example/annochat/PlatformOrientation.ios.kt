package org.example.annochat

import androidx.compose.runtime.Composable

@Composable
actual fun AllowScreenRotationWhileActive() {
    // On iOS, orientation is managed by root ViewController and Info.plist configurations.
}
