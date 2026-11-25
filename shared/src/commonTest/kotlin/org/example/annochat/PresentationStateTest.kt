package org.example.annochat

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.navigation.Navigator
import org.example.annochat.presentation.navigation.Screen
import org.example.annochat.presentation.viewmodel.ChatViewModel
import org.example.annochat.presentation.viewmodel.RoomsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresentationStateTest {

    @Test
    fun testNavigatorState() {
        val navigator = Navigator(Screen.Rooms)
        assertEquals(Screen.Rooms, navigator.currentScreen)

        navigator.navigateTo(Screen.Chat("cse", "CSE Stream"))
        assertTrue(navigator.currentScreen is Screen.Chat)
        val chat = navigator.currentScreen as Screen.Chat
        assertEquals("cse", chat.roomId)
        assertEquals("CSE Stream", chat.roomName)

        val popped = navigator.popBack()
        assertTrue(popped)
        assertEquals(Screen.Rooms, navigator.currentScreen)

        // Popping root should return false
        assertFalse(navigator.popBack())
        assertEquals(Screen.Rooms, navigator.currentScreen)
    }

    @Test
    fun testTimestampFormatting() {
        val iso = "2026-09-07T14:35:12.123456"
        val formatted = org.example.annochat.presentation.components.formatTimestamp(iso)
        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}")), "Formatted time should match HH:mm pattern, was: $formatted")
    }

    @Test
    fun testAnimalEmojiLookup() {
        assertEquals("🐆", org.example.annochat.presentation.components.getAnimalEmoji("Snow Leopard"))
        assertEquals("🦫", org.example.annochat.presentation.components.getAnimalEmoji("Capybara"))
        assertEquals("🦊", org.example.annochat.presentation.components.getAnimalEmoji("Arctic Fox"))
        assertEquals("🐼", org.example.annochat.presentation.components.getAnimalEmoji("Giant Panda"))
    }
}

