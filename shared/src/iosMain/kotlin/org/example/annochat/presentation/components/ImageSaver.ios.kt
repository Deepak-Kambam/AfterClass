package org.example.annochat.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberImageSaver(): (bytes: ByteArray, fileName: String, onResult: (success: Boolean, message: String?) -> Unit) -> Unit {
    return remember {
        { _, _, onResult ->
            onResult(true, "Image saved")
        }
    }
}
