package org.example.annochat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.ActiveBroadcastResponse
import org.example.annochat.data.model.AdminBroadcastRequest
import org.example.annochat.data.model.AdminBroadcastResponse
import org.example.annochat.data.model.BroadcastSeverity
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.SystemBroadcast
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.repository.BroadcastRepository
import org.example.annochat.data.repository.BroadcastRepositoryImpl
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.network.ApiClient
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.WebSocketClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BroadcastRepositoryTest {

    private class FakeChatRepo : ChatRepository {
        val _incomingEvents = MutableSharedFlow<IncomingEvent>(replay = 1, extraBufferCapacity = 64)
        override val incomingEvents: SharedFlow<IncomingEvent> = _incomingEvents.asSharedFlow()

        val _connectionState = MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
        override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

        var currentRoom: String = "main"
        override fun getCurrentRoomId(): String = currentRoom
        override fun setCurrentRoomId(roomId: String) { currentRoom = roomId }

        override val myIdentity: StateFlow<UserIdentityDto?> = MutableStateFlow(null)
        override val onlineCount: StateFlow<Int> = MutableStateFlow(1)
        override val publicRooms = MutableStateFlow(emptyList<org.example.annochat.data.model.PublicRoomDto>())
        override val userRooms = MutableStateFlow(emptyList<org.example.annochat.data.model.RoomDto>())
        override val onlineUsers = MutableStateFlow(emptyList<org.example.annochat.data.model.OnlineUserDto>())
        override val hasLoadedOnlineUsers = MutableStateFlow(true)
        override val messages = MutableStateFlow(emptyList<org.example.annochat.data.model.MessageDto>())

        override fun getSessionId(): String = "sess_test123"
        override fun connect() {}
        override fun disconnect() {}
        override suspend fun loadPublicRooms() = Result.success(emptyList<org.example.annochat.data.model.PublicRoomDto>())
        override suspend fun loadUserRooms() = Result.success(emptyList<org.example.annochat.data.model.RoomDto>())
        override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean) = Result.success(org.example.annochat.data.model.RoomDto(roomId = "r1", name = name))
        override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?) = Result.success(emptyList<org.example.annochat.data.model.MessageDto>())
        override suspend fun loadOnlineUsers() = Result.success(emptyList<org.example.annochat.data.model.OnlineUserDto>())
        override suspend fun sendMessage(content: String, roomId: String, replyTo: Long?, imagePath: String?, imageThumb: String?, imageName: String?, imageSize: Long?, imageDims: String?, clientMsgId: String?) = true
        override suspend fun sendTyping() = true
        override suspend fun toggleReaction(postNum: Long, emoji: String) = true
        override suspend fun requestPrivateChat(targetPublicId: String) = true
        override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean) = true
        override suspend fun leaveRoom(roomId: String) = true
        override suspend fun addUserToRoom(roomId: String, targetPublicId: String) = true
        override suspend fun getStickersAndGifs() = Result.failure<org.example.annochat.data.model.StickersAndGifsResponse>(Exception("not mocked"))
        override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String) = Result.failure<org.example.annochat.data.model.UploadResponse>(Exception("not mocked"))
        override suspend fun submitFeedback(category: String, message: String, rating: Int) = Result.success(org.example.annochat.data.model.FeedbackResponse(status = "ok"))
        override fun syncWidgets() {}
        override suspend fun leaveAllRooms() = Result.success(emptyList<String>())
        override suspend fun getMediaBytes(urlOrPath: String) = Result.success(ByteArray(0))
        override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String = relativeOrAbsoluteUrl
    }

    private class MockWebSocketClient : WebSocketClient(NetworkConfig.AndroidEmulator) {
        val testConnState = MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
        override val connectionState: StateFlow<ConnectionState> = testConnState.asStateFlow()

        val testInEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
        override val incomingEvents: SharedFlow<IncomingEvent> = testInEvents.asSharedFlow()

        fun setStatus(status: ConnectionStatus) {
            testConnState.value = ConnectionState(status)
        }
    }

    private class MockApiClient : ApiClient(NetworkConfig.AndroidEmulator) {
        var activeBroadcastResult: Result<ActiveBroadcastResponse> = Result.success(ActiveBroadcastResponse(status = "ok", broadcast = null))

        fun setActiveBroadcast(bc: SystemBroadcast?) {
            activeBroadcastResult = Result.success(ActiveBroadcastResponse(status = "ok", broadcast = bc))
        }

        fun setFailure(e: Exception) {
            activeBroadcastResult = Result.failure(e)
        }

        override suspend fun getActiveBroadcast(): Result<ActiveBroadcastResponse> {
            return activeBroadcastResult
        }
    }

    @Test
    fun testIncomingEventSystemBroadcastParsing() {
        val rawJson = """
            {
                "type": "system_broadcast",
                "action": "system_broadcast",
                "broadcast_id": "bc_9988",
                "message": "Campus Wi-Fi maintenance at midnight.",
                "severity": "WARNING",
                "room_id": "",
                "duration": 45,
                "created_at": "2026-09-23T12:00:00Z"
            }
        """.trimIndent()

        val event = IncomingEvent.fromJson(rawJson)
        assertTrue(event is IncomingEvent.SystemBroadcast)
        val bc = event.broadcast
        assertEquals("bc_9988", bc.broadcastId)
        assertEquals("Campus Wi-Fi maintenance at midnight.", bc.message)
        assertEquals(BroadcastSeverity.WARNING, bc.severity)
        assertEquals("", bc.roomId)
        assertEquals(45, bc.duration)
        assertEquals("2026-09-23T12:00:00Z", bc.createdAt)
        assertTrue(bc.isGlobal)
    }

    @Test
    fun testMalformedWebSocketEventSafeHandling() {
        val malformedJson = """{"type": "system_broadcast", "corrupted": true"""
        val event = IncomingEvent.fromJson(malformedJson)
        assertTrue(event is IncomingEvent.Unknown)

        val unhandledTypeJson = """{"type": "unrecognized_future_event", "payload": {}}"""
        val unhandledEvent = IncomingEvent.fromJson(unhandledTypeJson)
        assertTrue(unhandledEvent is IncomingEvent.Unknown)
    }

    @Test
    fun testReceiveInfoWarningEmergencyBroadcasts() = runBlocking {
        val chatRepo = FakeChatRepo()
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        val sessionStorage = InMemorySessionStorage()

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        // 1. INFO
        val infoBc = SystemBroadcast(
            broadcastId = "bc_info_1",
            message = "Library hours extended",
            severity = BroadcastSeverity.INFO,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(infoBc))
        delay(50)
        assertEquals("bc_info_1", repository.activeBroadcast.value?.broadcastId)
        assertEquals(BroadcastSeverity.INFO, repository.activeBroadcast.value?.severity)

        // 2. WARNING
        val warnBc = SystemBroadcast(
            broadcastId = "bc_warn_2",
            message = "Server restart in 10 minutes",
            severity = BroadcastSeverity.WARNING,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(warnBc))
        delay(50)
        assertEquals("bc_warn_2", repository.activeBroadcast.value?.broadcastId)
        assertEquals(BroadcastSeverity.WARNING, repository.activeBroadcast.value?.severity)

        // 3. EMERGENCY
        val emergBc = SystemBroadcast(
            broadcastId = "bc_emerg_3",
            message = "Immediate campus evacuation test",
            severity = BroadcastSeverity.EMERGENCY,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(emergBc))
        delay(50)
        assertEquals("bc_emerg_3", repository.activeBroadcast.value?.broadcastId)
        assertEquals(BroadcastSeverity.EMERGENCY, repository.activeBroadcast.value?.severity)
    }

    @Test
    fun testBroadcastWithDurationAndZeroDuration() = runBlocking {
        val chatRepo = FakeChatRepo()
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        val sessionStorage = InMemorySessionStorage()

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        // Broadcast with 1 second duration
        val shortBc = SystemBroadcast(
            broadcastId = "bc_short",
            message = "Flash announcement",
            severity = BroadcastSeverity.INFO,
            duration = 1
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(shortBc))
        delay(50)
        assertEquals("bc_short", repository.activeBroadcast.value?.broadcastId)

        // After duration expires, banner should automatically clear
        delay(1200)
        assertNull(repository.activeBroadcast.value)

        // Broadcast with duration = 0 (indefinite)
        val persistentBc = SystemBroadcast(
            broadcastId = "bc_persist",
            message = "Permanent rule notice",
            severity = BroadcastSeverity.INFO,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(persistentBc))
        delay(50)
        assertEquals("bc_persist", repository.activeBroadcast.value?.broadcastId)

        delay(1200)
        // Should STILL be active!
        assertEquals("bc_persist", repository.activeBroadcast.value?.broadcastId)
    }

    @Test
    fun testManualDismissalAndDeduplication() = runBlocking {
        val chatRepo = FakeChatRepo()
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        val sessionStorage = InMemorySessionStorage()

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        val bc = SystemBroadcast(
            broadcastId = "bc_dismiss_me",
            message = "Notice to dismiss",
            severity = BroadcastSeverity.INFO,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(bc))
        delay(50)
        assertEquals("bc_dismiss_me", repository.activeBroadcast.value?.broadcastId)

        // Manual dismiss
        repository.dismissBroadcast("bc_dismiss_me")
        assertNull(repository.activeBroadcast.value)
        assertTrue(sessionStorage.isBroadcastDismissed("bc_dismiss_me"))

        // Duplicate event arriving again should be ignored
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(bc))
        delay(50)
        assertNull(repository.activeBroadcast.value)
    }

    @Test
    fun testRoomSpecificBroadcastFiltering() = runBlocking {
        val chatRepo = FakeChatRepo()
        chatRepo.currentRoom = "gaming_room"
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        val sessionStorage = InMemorySessionStorage()

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        // 1. Broadcast specifically for study_hub: should NOT be visible while in gaming_room
        val studyBc = SystemBroadcast(
            broadcastId = "bc_study",
            message = "Quiet hours starting in study hub",
            severity = BroadcastSeverity.INFO,
            roomId = "study_hub",
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(studyBc))
        delay(50)
        assertNull(repository.activeBroadcast.value)

        // 2. User navigates into study_hub: banner should appear!
        chatRepo.currentRoom = "study_hub"
        repository.onRoomChanged("study_hub")
        delay(50)
        assertEquals("bc_study", repository.activeBroadcast.value?.broadcastId)

        // 3. User navigates back out to main: banner should hide
        chatRepo.currentRoom = "main"
        repository.onRoomChanged("main")
        delay(50)
        assertNull(repository.activeBroadcast.value)

        // 4. Global broadcast arrives: should be visible in main and everywhere!
        val globalBc = SystemBroadcast(
            broadcastId = "bc_global",
            message = "Campus all-hands today",
            severity = BroadcastSeverity.WARNING,
            roomId = null,
            duration = 0
        )
        chatRepo._incomingEvents.emit(IncomingEvent.SystemBroadcast(globalBc))
        delay(50)
        assertEquals("bc_global", repository.activeBroadcast.value?.broadcastId)

        // Changing room doesn't hide global broadcast
        chatRepo.currentRoom = "random_room"
        repository.onRoomChanged("random_room")
        delay(50)
        assertEquals("bc_global", repository.activeBroadcast.value?.broadcastId)
    }

    @Test
    fun testStartupAndReconnectFallbackSync() = runBlocking {
        val chatRepo = FakeChatRepo()
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        val sessionStorage = InMemorySessionStorage()

        val serverBc = SystemBroadcast(
            broadcastId = "bc_from_rest",
            message = "Active broadcast synced from REST",
            severity = BroadcastSeverity.INFO,
            duration = 0
        )
        apiClient.setActiveBroadcast(serverBc)

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        // Startup fallback should sync active broadcast
        repository.refreshActiveBroadcast()
        delay(50)
        assertEquals("bc_from_rest", repository.activeBroadcast.value?.broadcastId)

        // Dismiss it
        repository.dismissBroadcast("bc_from_rest")
        assertNull(repository.activeBroadcast.value)

        // Reconnect should not resurrect dismissed broadcast
        repository.refreshActiveBroadcast()
        delay(50)
        assertNull(repository.activeBroadcast.value)
    }

    @Test
    fun testRestFailureDoesNotCrash() = runBlocking {
        val chatRepo = FakeChatRepo()
        val wsClient = MockWebSocketClient()
        val apiClient = MockApiClient()
        apiClient.setFailure(Exception("Connection refused"))
        val sessionStorage = InMemorySessionStorage()

        val repository = BroadcastRepositoryImpl(
            apiClient = apiClient,
            webSocketClient = wsClient,
            chatRepository = chatRepo,
            sessionStorage = sessionStorage
        )

        // Should complete safely without throwing
        repository.refreshActiveBroadcast()
        delay(50)
        assertNull(repository.activeBroadcast.value)
    }
}
