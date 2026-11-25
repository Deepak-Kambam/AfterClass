package org.example.annochat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController

object IosDeepLinkState {
    var deepLinkUri by mutableStateOf<String?>(null)

    fun onOpenUrl(url: String) {
        deepLinkUri = url
    }
}

fun MainViewController() = ComposeUIViewController {
    startKotzillaMonitoring()
    App(deepLinkUri = IosDeepLinkState.deepLinkUri)
}