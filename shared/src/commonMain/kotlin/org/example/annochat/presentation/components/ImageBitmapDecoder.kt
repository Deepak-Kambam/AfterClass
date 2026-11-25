package org.example.annochat.presentation.components

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes raw image bytes (JPEG, PNG, WEBP, etc.) into a Compose ImageBitmap.
 * Returns null if the byte array is invalid or unsupported.
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
