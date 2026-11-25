package org.example.annochat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

sealed interface Screen {
    object Splash : Screen
    data class NoInternet(val reason: String? = null) : Screen
    object Onboarding : Screen
    object Rooms : Screen
    data class Chat(val roomId: String, val roomName: String) : Screen
    object Appearance : Screen
}


enum class NavDirection {
    Forward,
    Backward
}

class Navigator(initialScreen: Screen = Screen.Splash) {
    private val backStack = mutableStateListOf<Screen>(initialScreen)
    var lastDirection by mutableStateOf(NavDirection.Forward)
        private set

    val currentScreen: Screen
        get() = backStack.lastOrNull() ?: Screen.Rooms

    val canPopBack: Boolean
        get() = backStack.size > 1

    fun navigateTo(screen: Screen) {
        lastDirection = NavDirection.Forward
        backStack.add(screen)
    }

    fun popBack(): Boolean {
        return if (backStack.size > 1) {
            lastDirection = NavDirection.Backward
            backStack.removeAt(backStack.size - 1)
            true
        } else {
            false
        }
    }

    fun replaceWith(screen: Screen) {
        lastDirection = NavDirection.Forward
        backStack.clear()
        backStack.add(screen)
    }
}

@Composable
fun rememberNavigator(initialScreen: Screen = Screen.Rooms): Navigator {
    return remember { Navigator(initialScreen) }
}
