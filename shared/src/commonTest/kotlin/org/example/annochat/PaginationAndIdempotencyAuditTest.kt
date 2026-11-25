package org.example.annochat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.*
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.viewmodel.ChatViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FakeAuditChatRepository : ChatRepository {
    val _connectionState = MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val _myIdentity = MutableStateFlow<UserIdentityDto?>(
        UserIdentityDto(publicId = "my_pid_123", anonName = "Snow Leopard #123", avatarUrl = "")
    )
    override val myIdentity: StateFlow<UserIdentityDto?> = _myIdentity.asStateFlow()

    private val _onlineCount = MutableStateFlow(5)
    override val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    private val _publicRooms = MutableStateFlow<List<PublicRoomDto>>(emptyList())
    override val publicRooms: StateFlow<List<PublicRoomDto>> = _publicRooms.asStateFlow()

    private val _userRooms = MutableStateFlow<List<RoomDto>>(emptyList())
    override val userRooms: StateFlow<List<RoomDto>> = _userRooms.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<OnlineUserDto>>(emptyList())
    override val onlineUsers: StateFlow<List<OnlineUserDto>> = _onlineUsers.asStateFlow()

    private val _hasLoadedOnlineUsers = MutableStateFlow(true)
    override val hasLoadedOnlineUsers: StateFlow<Boolean> = _hasLoadedOnlineUsers.asStateFlow()

    val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    override val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    val _incomingEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    override val incomingEvents: SharedFlow<IncomingEvent> = _incomingEvents.asSharedFlow()

    var activeRoomId: String = "main"
    var lastRequestedBeforePostNum: Long? = null
    var olderMessagesToReturn: List<MessageDto> = emptyList()

    override fun getSessionId(): String = "sess_fake_123"
    override fun connect() {}
    override fun disconnect() {}
    override fun setCurrentRoomId(roomId: String) { activeRoomId = roomId }
    override fun getCurrentRoomId(): String = activeRoomId

    override suspend fun loadPublicRooms(): Result<List<PublicRoomDto>> = Result.success(emptyList())
    override suspend fun loadUserRooms(): Result<List<RoomDto>> = Result.success(emptyList())
    override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean): Result<RoomDto> = Result.success(RoomDto(roomId = "fake_room", name = name))

    override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?): Result<List<MessageDto>> {
        lastRequestedBeforePostNum = beforePostNum
        val list = if (beforePostNum != null) olderMessagesToReturn else _messages.value
        if (beforePostNum != null) {
            _messages.update { existing ->
                (list + existing)
                    .distinctBy { it.clientMsgId?.takeIf { id -> id.isNotBlank() } ?: it.postNum.toString() }
                    .sortedWith(compareBy({ it.isPinned == 0 }, { it.postNum }))
            }
        } else {
            _messages.value = list
        }
        return Result.success(list)
    }

    override suspend fun loadOnlineUsers(): Result<List<OnlineUserDto>> = Result.success(emptyList())

    var sentMessages = mutableListOf<OutgoingEvent.PostMessage>()
    override suspend fun sendMessage(
        content: String,
        roomId: String,
        replyTo: Long?,
        imagePath: String?,
        imageThumb: String?,
        imageName: String?,
        imageSize: Long?,
        imageDims: String?,
        clientMsgId: String?
    ): Boolean {
        sentMessages.add(
            OutgoingEvent.PostMessage(
                content = content,
                roomId = roomId,
                replyTo = replyTo,
                clientMsgId = clientMsgId
            )
        )
        return true
    }

    override suspend fun sendTyping(): Boolean = true
    override suspend fun toggleReaction(postNum: Long, emoji: String): Boolean = true
    override suspend fun requestPrivateChat(targetPublicId: String): Boolean = true
    override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean = true
    override suspend fun leaveRoom(roomId: String): Boolean = true
    override suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean = true
    override suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse> = Result.success(StickersAndGifsResponse("ok", emptyList(), emptyList()))
    override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<UploadResponse> = Result.failure(UnsupportedOperationException())
    override suspend fun submitFeedback(category: String, message: String, rating: Int): Result<FeedbackResponse> = Result.success(FeedbackResponse("ok", 1L, "Thank you"))
    override fun syncWidgets() {}
    override suspend fun leaveAllRooms(): Result<List<String>> = Result.success(emptyList())
    override suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> = Result.failure(UnsupportedOperationException())
    override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String = relativeOrAbsoluteUrl
    suspend fun emitIncomingEvent(event: IncomingEvent) {
        if (event is IncomingEvent.NewMessage) {
            _messages.update { existing ->
                val current = existing.toMutableList()
                val idx = current.indexOfFirst { it.postNum == event.message.postNum }
                if (idx >= 0) {
                    current[idx] = event.message
                } else {
                    current.add(event.message)
                }
                current
            }
        }
        _incomingEvents.emit(event)
    }
}

class PaginationAndIdempotencyAuditTest {

    private suspend fun awaitCondition(name: String, timeoutMs: Long = 3000, details: (() -> String)? = null, condition: () -> Boolean) {
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        while (!condition()) {
            if (start.elapsedNow().inWholeMilliseconds > timeoutMs) {
                val extra = details?.invoke() ?: ""
                throw AssertionError("Condition '$name' not met within $timeoutMs ms. $extra")
            }
            kotlinx.coroutines.delay(10)
        }
    }

    @Test
    fun testPaginationWithConcurrentLiveWebSocketArrival() = runBlocking {
        val repo = FakeAuditChatRepository()
        // Initial batch of 50 messages (#451..#500) to activate pagination (hasMoreOlderMessages = true)
        val initialHistory = (451L..500L).map { num ->
            MessageDto(postNum = num, content = "Msg $num", roomId = "main", anonName = "User $num")
        }
        repo._messages.value = initialHistory

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val viewModel = ChatViewModel("main", "Main Lobby", repo, scope = scope)

        // Wait for initial sync
        awaitCondition("initial 50 sync", details = { "size=${viewModel.uiState.value.messages.size}, isLoading=${viewModel.uiState.value.isLoading}, error=${viewModel.uiState.value.errorMessage}, repoSize=${repo.messages.value.size}" }) { viewModel.uiState.value.messages.size == 50 && !viewModel.uiState.value.isLoading }
        assertTrue(viewModel.uiState.value.hasMoreOlderMessages)
        assertEquals(451L, viewModel.uiState.value.messages.first().postNum)
        assertEquals(500L, viewModel.uiState.value.messages.last().postNum)

        // Prepare older messages #401..#450
        val olderHistory = (401L..450L).map { num ->
            MessageDto(postNum = num, content = "Older msg $num", roomId = "main", anonName = "User $num")
        }
        repo.olderMessagesToReturn = olderHistory

        // 1. Trigger pagination
        viewModel.loadOlderMessages()

        // 2. Exactly while pagination is in progress, emit a live WebSocket message #501
        val liveMsg501 = MessageDto(
            postNum = 501L,
            content = "Live live live 501!",
            roomId = "main",
            anonName = "Live User"
        )
        repo.emitIncomingEvent(IncomingEvent.NewMessage(liveMsg501))

        // Wait for pagination and websocket event to finish processing
        awaitCondition("pagination + live msg 101 items", details = { "isLoadingOlder=${viewModel.uiState.value.isLoadingOlder}, size=${viewModel.uiState.value.messages.size}, repoSize=${repo.messages.value.size}" }) { !viewModel.uiState.value.isLoadingOlder && viewModel.uiState.value.messages.size == 101 }

        val finalMessages = viewModel.uiState.value.messages

        // VERIFY:
        // Total messages must be 50 (older) + 50 (current) + 1 (live) = 101 messages
        assertEquals(101, finalMessages.size)

        // Verify strictly monotonic ordering: 401 to 501
        for (i in 0 until finalMessages.size) {
            assertEquals(401L + i, finalMessages[i].postNum)
        }

        // Verify older history -> current -> live
        assertEquals(401L, finalMessages.first().postNum)
        assertEquals(500L, finalMessages[99].postNum)
        assertEquals(501L, finalMessages.last().postNum)

        // Verify duplicate prevention
        val distinctPostNums = finalMessages.map { it.postNum }.toSet()
        assertEquals(101, distinctPostNums.size)

        viewModel.clear()
    }

    @Test
    fun testPinnedAnnouncementPaginationEdgeCase() = runBlocking {
        val repo = FakeAuditChatRepository()

        // Pinned announcement at postNum = 1, regular active messages at 500..549 (50 messages)
        val pinnedNotice = MessageDto(
            postNum = 1L,
            content = "Welcome to AfterClass Campus Hub!",
            roomId = "main",
            isPinned = 1,
            anonName = "Campus Admin"
        )
        val regularMessages = (500L..549L).map { num ->
            MessageDto(postNum = num, content = "Msg $num", roomId = "main", isPinned = 0)
        }
        repo._messages.value = listOf(pinnedNotice) + regularMessages

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val viewModel = ChatViewModel("main", "Main Lobby", repo, scope = scope)

        awaitCondition("pinned initial 51 sync") { viewModel.uiState.value.messages.size == 51 && !viewModel.uiState.value.isLoading }
        assertTrue(viewModel.uiState.value.hasMoreOlderMessages)

        val olderHistory = (450L..499L).map { num ->
            MessageDto(postNum = num, content = "Older msg $num", roomId = "main", isPinned = 0)
        }
        repo.olderMessagesToReturn = olderHistory

        // Load older messages
        viewModel.loadOlderMessages()
        awaitCondition("pinned older loaded 101 items") { !viewModel.uiState.value.isLoadingOlder && viewModel.uiState.value.messages.size == 101 }

        // Verify that pagination queried beforePostNum = 500 (NOT beforePostNum = 1)
        assertEquals(500L, repo.lastRequestedBeforePostNum)

        val finalMessages = viewModel.uiState.value.messages

        // Total: 1 (pinned) + 50 (older #450..#499) + 50 (regular #500..#549) = 101
        assertEquals(101, finalMessages.size)

        // Pinned message must remain at index 0
        assertEquals(1L, finalMessages.first().postNum)
        assertEquals(1, finalMessages.first().isPinned)

        // Next must be oldest unpinned message (#450)
        assertEquals(450L, finalMessages[1].postNum)
        assertEquals(0, finalMessages[1].isPinned)

        // Last must be newest message (#549)
        assertEquals(549L, finalMessages.last().postNum)

        viewModel.clear()
    }

    @Test
    fun testClientMsgIdIdempotencyAcrossOptimisticRetryAndPagination() = runBlocking {
        val repo = FakeAuditChatRepository()
        val initialHistory = (100L..149L).map { num ->
            MessageDto(postNum = num, content = "Msg $num", roomId = "main")
        }
        repo._messages.value = initialHistory

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val viewModel = ChatViewModel("main", "Main Lobby", repo, scope = scope)
        awaitCondition("idempotency initial 50", details = { "size=${viewModel.uiState.value.messages.size}, isLoading=${viewModel.uiState.value.isLoading}, error=${viewModel.uiState.value.errorMessage}, repoSize=${repo.messages.value.size}" }) { viewModel.uiState.value.messages.size == 50 && !viewModel.uiState.value.isLoading }

        // 1. User types and sends message
        viewModel.onInputTextChange("Hello idempotent world!")
        viewModel.sendMessage()

        val messagesAfterSend = viewModel.uiState.value.messages
        assertEquals(51, messagesAfterSend.size)
        val optimisticMsg = messagesAfterSend.last()
        assertTrue(optimisticMsg.postNum < 0, "Optimistic message must have negative postNum")
        val clientMsgId = optimisticMsg.clientMsgId

        // 2. While message is in-flight, trigger pagination of older messages
        repo.olderMessagesToReturn = (50L..99L).map { num ->
            MessageDto(postNum = num, content = "Older msg $num", roomId = "main")
        }
        viewModel.loadOlderMessages()
        awaitCondition("idempotency pagination 101") { !viewModel.uiState.value.isLoadingOlder && viewModel.uiState.value.messages.size == 101 }

        // Optimistic message must NOT be wiped out by pagination
        val messagesAfterPagination = viewModel.uiState.value.messages
        val optimisticStillPresent = messagesAfterPagination.any { it.clientMsgId == clientMsgId }
        assertTrue(optimisticStillPresent, "Optimistic message must survive pagination without being wiped out")

        // 3. Simulate server confirming via WebSocket with real postNum = 150 and matching clientMsgId
        val serverConfirmedMessage = MessageDto(
            postNum = 150L,
            clientMsgId = clientMsgId,
            content = "Hello idempotent world!",
            roomId = "main",
            anonName = "Snow Leopard #123"
        )
        repo.emitIncomingEvent(IncomingEvent.NewMessage(serverConfirmedMessage))
        awaitCondition("reconciled 150") { viewModel.uiState.value.messages.any { it.clientMsgId == clientMsgId && it.postNum == 150L } }

        val messagesAfterConfirm = viewModel.uiState.value.messages
        val matchingCountAfterConfirm = messagesAfterConfirm.count { it.clientMsgId == clientMsgId }
        assertEquals(1, matchingCountAfterConfirm, "Must have exactly 1 message matching clientMsgId")
        val confirmedInList = messagesAfterConfirm.first { it.clientMsgId == clientMsgId }
        assertEquals(150L, confirmedInList.postNum, "Optimistic message must be reconciled to server postNum 150")

        // 4. Simulate duplicate server response or network retry
        repo.emitIncomingEvent(IncomingEvent.NewMessage(serverConfirmedMessage))
        kotlinx.coroutines.delay(30)
        assertEquals(1, viewModel.uiState.value.messages.count { it.clientMsgId == clientMsgId }, "Duplicate WebSocket broadcast must be deduplicated")

        // 5. Simulate WebSocket reconnect and re-sync
        repo._connectionState.value = ConnectionState(ConnectionStatus.RECONNECTING)
        repo._connectionState.value = ConnectionState(ConnectionStatus.CONNECTED)
        repo.emitIncomingEvent(IncomingEvent.NewMessage(serverConfirmedMessage))
        kotlinx.coroutines.delay(30)

        // Confirm only one visible message exists
        assertEquals(1, viewModel.uiState.value.messages.count { it.clientMsgId == clientMsgId })

        viewModel.clear()
    }

    @Test
    fun testOptimisticMessageReconciliationWithoutServerClientMsgIdAndPlacement() = runBlocking {
        val repo = FakeAuditChatRepository()
        repo._myIdentity.value = UserIdentityDto("04cf4bea", "Giant Panda #04cf", avatarUrl = "")
        val sessionStorage = InMemorySessionStorage().apply {
            saveSessionId("test_session_user")
            saveIdentity("04cf4bea", "Giant Panda #04cf")
        }
        val viewModel = ChatViewModel(
            roomId = "main",
            roomName = "Public Chat",
            repository = repo,
            sessionStorage = sessionStorage
        )

        // Pre-populate history with existing messages
        val initialHistory = (100L..104L).map { num ->
            MessageDto(
                postNum = num,
                content = "Existing msg $num",
                roomId = "main",
                anonName = "Other User",
                publicId = "other_pid"
            )
        }
        repo._messages.value = initialHistory
        awaitCondition("initial history load") { viewModel.uiState.value.messages.size == 5 }

        // User sends first message "Hi"
        viewModel.onInputTextChange("Hi")
        viewModel.sendMessage()

        val messagesAfterHi = viewModel.uiState.value.messages
        assertEquals(6, messagesAfterHi.size)
        val optimisticHi = messagesAfterHi.last()
        assertTrue(optimisticHi.postNum < 0, "Optimistic postNum must be negative")
        assertEquals("Hi", optimisticHi.content)
        assertEquals("04cf4bea", optimisticHi.publicId)
        assertEquals("Giant Panda #04cf", optimisticHi.anonName)
        assertTrue(viewModel.isOwnClientMsgId(optimisticHi.clientMsgId), "clientMsgId must be marked as own")

        // User immediately sends second message "Hlo"
        viewModel.onInputTextChange("Hlo")
        viewModel.sendMessage()

        val messagesAfterHlo = viewModel.uiState.value.messages
        assertEquals(7, messagesAfterHlo.size)
        val optimisticHlo = messagesAfterHlo.last()
        assertTrue(optimisticHlo.postNum < 0)
        assertEquals("Hlo", optimisticHlo.content)

        // Simulate server (like production api.afterclass.bond) broadcasting "Hi" with client_msg_id = NULL
        val serverHi = MessageDto(
            postNum = 105L,
            clientMsgId = null, // Production server does not return client_msg_id
            publicId = "04cf4bea",
            anonName = "Giant Panda #04cf",
            content = "Hi",
            roomId = "main"
        )
        repo.emitIncomingEvent(IncomingEvent.NewMessage(serverHi))

        awaitCondition("reconciled Hi without client_msg_id") {
            val msgs = viewModel.uiState.value.messages
            // The optimistic "Hi" must be gone, server "Hi" present, optimistic "Hlo" still pending
            msgs.count { it.content == "Hi" } == 1 &&
            msgs.any { it.content == "Hi" && it.postNum == 105L } &&
            msgs.size == 7
        }

        // Simulate server broadcasting "Hlo" with client_msg_id = NULL
        val serverHlo = MessageDto(
            postNum = 106L,
            clientMsgId = null,
            publicId = "04cf4bea",
            anonName = "Giant Panda #04cf",
            content = "Hlo",
            roomId = "main"
        )
        repo.emitIncomingEvent(IncomingEvent.NewMessage(serverHlo))

        awaitCondition("reconciled Hlo without client_msg_id") {
            val msgs = viewModel.uiState.value.messages
            msgs.count { it.content == "Hlo" } == 1 &&
            msgs.any { it.content == "Hlo" && it.postNum == 106L } &&
            msgs.size == 7 &&
            msgs.none { it.postNum < 0 }
        }

        val finalMessages = viewModel.uiState.value.messages
        // Must contain exactly 7 messages: 100..104 from history + 105 ("Hi") + 106 ("Hlo")
        // Zero duplicates! Zero negative postNums remaining!
        assertEquals(7, finalMessages.size)
        assertEquals("Hi", finalMessages[finalMessages.size - 2].content)
        assertEquals(105L, finalMessages[finalMessages.size - 2].postNum)
        assertEquals("Hlo", finalMessages.last().content)
        assertEquals(106L, finalMessages.last().postNum)

        viewModel.clear()
    }
}
