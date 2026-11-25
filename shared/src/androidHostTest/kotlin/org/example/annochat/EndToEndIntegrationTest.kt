package org.example.annochat

import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.OutgoingEvent
import org.example.annochat.data.repository.ChatRepositoryImpl
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.network.ApiClient
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.WebSocketClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EndToEndIntegrationTest {

    private val localConfig = NetworkConfig(host = "127.0.0.1", port = 8000, isSecure = false)

    private fun isBackendAvailable(): Boolean {
        return try {
            val url = java.net.URI("http://127.0.0.1:8000/api/public_rooms").toURL()
            val conn = url.openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 1000
            conn.readTimeout = 1000
            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (_: Exception) {
            false
        }
    }

    @Test
    fun testRestEndpointsWithLiveBackend() = runBlocking {
        if (!isBackendAvailable()) {
            println("INFO: Skipping testRestEndpointsWithLiveBackend because local backend is offline at 127.0.0.1:8000")
            return@runBlocking
        }

        val apiClient = ApiClient(localConfig)
        val sessionId = "sess_integration_test_1"

        // 1. GET /api/public_rooms
        val publicRoomsResult = apiClient.getPublicRooms()
        assertTrue(publicRoomsResult.isSuccess, "Failed to fetch public rooms: ${publicRoomsResult.exceptionOrNull()}")
        val publicRooms = publicRoomsResult.getOrThrow()
        assertTrue(publicRooms.isNotEmpty())
        assertTrue(publicRooms.any { it.id == "main" })

        // 2. GET /api/rooms
        val userRoomsResult = apiClient.getUserRooms(sessionId)
        assertTrue(userRoomsResult.isSuccess, "Failed to fetch user rooms")

        // 3. GET /api/messages
        val messagesResult = apiClient.getMessages(roomId = "main", limit = 10, sessionId = sessionId)
        assertTrue(messagesResult.isSuccess, "Failed to fetch messages")

        // 4. GET /api/online_users
        val onlineUsersResult = apiClient.getOnlineUsers()
        assertTrue(onlineUsersResult.isSuccess, "Failed to fetch online users")

        // 5. GET /api/stickers_and_gifs
        val stickersResult = apiClient.getStickersAndGifs()
        assertTrue(stickersResult.isSuccess, "Failed to fetch stickers and gifs")
        assertTrue(stickersResult.getOrThrow().stickers.isNotEmpty())
    }

    @Test
    fun testWebSocketAndTwoClientRealtimeMessaging() = runBlocking {
        if (!isBackendAvailable()) {
            println("INFO: Skipping testWebSocketAndTwoClientRealtimeMessaging because local backend is offline at 127.0.0.1:8000")
            return@runBlocking
        }

        val sessionA = "sess_test_client_a"
        val sessionB = "sess_test_client_b"


        val wsClientA = WebSocketClient(localConfig)
        val repoA = ChatRepositoryImpl(
            apiClient = ApiClient(localConfig),
            webSocketClient = wsClientA,
            sessionStorage = InMemorySessionStorage(sessionA)
        )

        val wsClientB = WebSocketClient(localConfig)
        val repoB = ChatRepositoryImpl(
            apiClient = ApiClient(localConfig),
            webSocketClient = wsClientB,
            sessionStorage = InMemorySessionStorage(sessionB)
        )

        // Connect Client A
        repoA.connect()
        withTimeout(10000) {
            repoA.connectionState.filter { it.status == ConnectionStatus.CONNECTED }.first()
        }

        // Verify Client A receives my_identity
        val identityA = withTimeout(10000) {
            repoA.myIdentity.filter { it != null }.first()!!
        }
        assertNotNull(identityA.publicId)
        assertNotNull(identityA.anonName)
        assertTrue(identityA.avatarUrl.startsWith("/api/animal/"))

        // Connect Client B
        repoB.connect()
        withTimeout(10000) {
            repoB.connectionState.filter { it.status == ConnectionStatus.CONNECTED }.first()
        }

        val identityB = withTimeout(10000) {
            repoB.myIdentity.filter { it != null }.first()!!
        }
        assertNotNull(identityB.publicId)

        // Verify online count >= 2
        val onlineCount = withTimeout(10000) {
            repoA.onlineCount.filter { it >= 2 }.first()
        }
        assertTrue(onlineCount >= 2)

        // Ping / Pong test
        wsClientA.send(OutgoingEvent.Ping(time = 12345.0))
        val pongEvent = withTimeout(10000) {
            wsClientA.incomingEvents.filter { it is IncomingEvent.Pong }.first() as IncomingEvent.Pong
        }
        assertEquals(12345.0, pongEvent.time)

        // Client A sends typing -> Client B receives typing
        repoA.sendTyping()
        val typingEvent = withTimeout(10000) {
            repoB.incomingEvents.filter { it is IncomingEvent.Typing }.first() as IncomingEvent.Typing
        }
        assertNotNull(typingEvent.animalName)

        // Client A posts message -> Client B receives new_message in real time!
        val testContent = "Integration test message ${System.currentTimeMillis()}"
        val sent = repoA.sendMessage(content = testContent, roomId = "main")
        assertTrue(sent, "Client A failed to send message")

        val receivedMsgEvent = withTimeout(10000) {
            repoB.incomingEvents.filter {
                it is IncomingEvent.NewMessage && it.message.content == testContent
            }.first() as IncomingEvent.NewMessage
        }
        assertEquals(testContent, receivedMsgEvent.message.content)
        assertEquals("main", receivedMsgEvent.message.roomId)
        assertEquals(identityA.anonName, receivedMsgEvent.message.anonName)

        // Client B reacts to post -> Client A receives reaction_update
        val postNum = receivedMsgEvent.message.postNum
        repoB.toggleReaction(postNum, "🔥")

        val reactionEvent = withTimeout(10000) {
            repoA.incomingEvents.filter {
                it is IncomingEvent.ReactionUpdate && it.postNum == postNum
            }.first() as IncomingEvent.ReactionUpdate
        }
        assertEquals(postNum, reactionEvent.postNum)
        assertEquals(1, reactionEvent.reactions["🔥"])

        // Cleanup
        repoA.disconnect()
        repoB.disconnect()
    }
}
