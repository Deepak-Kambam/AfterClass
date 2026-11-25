package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable

/**
 * Provides a platform-specific function to save image bytes to the device photo library or downloads.
 * Returns a lambda: (bytes: ByteArray, fileName: String, onResult: (success: Boolean, message: String?) -> Unit) -> Unit
 */
@Composable
expect fun rememberImageSaver(): (bytes: ByteArray, fileName: String, onResult: (success: Boolean, message: String?) -> Unit) -> Unit
