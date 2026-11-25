package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable

/**
 * Data holder for an image picked from the system gallery or inserted via keyboard.
 */
data class PickedImageItem(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val uriString: String? = null
)

/**
 * Platform launcher for picking one or multiple images from device gallery / media storage.
 * Returns a lambda that triggers the picker UI.
 */
@Composable
expect fun rememberImagePickerLauncher(
    onImagesPicked: (List<PickedImageItem>) -> Unit
): () -> Unit

/**
 * Registers platform-level content receiver for Android keyboard GIF/sticker insertion and clipboard paste.
 */
@Composable
expect fun RegisterKeyboardContentReceiver(
    onMediaReceived: (PickedImageItem) -> Unit
)
