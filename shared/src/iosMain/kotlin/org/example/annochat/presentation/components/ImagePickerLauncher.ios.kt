package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImageItem>) -> Unit
): () -> Unit {
    return {}
}

@Composable
actual fun RegisterKeyboardContentReceiver(
    onMediaReceived: (PickedImageItem) -> Unit
) {
    // No-op on iOS
}
