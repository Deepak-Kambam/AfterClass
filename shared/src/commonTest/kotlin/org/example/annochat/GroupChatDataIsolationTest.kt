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
import kotlin.test.assertTrue

class IsolatedTestChatRepository(
    override val sessionStorage: InMemorySessionStorage
) : ChatRepository {
    private val _connectionState = MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _myIdentity = MutableStateFlow<UserIdentityDto?>(
        UserIdentityDto(publicId = "usr_tester_123", anonName = "Tester User", avatarUrl = "")
    )
    override val myIdentity: StateFlow<UserIdentityDto?> = _myIdentity.asStateFlow()

    private val _onlineCount = MutableStateFlow(3)
    override val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    private val _publicRooms = MutableStateFlow<List<PublicRoomDto>>(emptyList())
    override val publicRooms: StateFlow<List<PublicRoomDto>> = _publicRooms.asStateFlow()

    private val _userRooms = MutableStateFlow<List<RoomDto>>(emptyList())
    override val userRooms: StateFlow<List<RoomDto>> = _userRooms.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<OnlineUserDto>>(emptyList())
    override val onlineUsers: StateFlow<List<OnlineUserDto>> = _onlineUsers.asStateFlow()

    private val _hasLoadedOnlineUsers = MutableStateFlow(true)
    override val hasLoadedOnlineUsers: StateFlow<Boolean> = _hasLoadedOnlineUsers.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    override val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    override val incomingEvents: SharedFlow<IncomingEvent> = _incomingEvents.asSharedFlow()

    var activeRoomId: String = "main"
    val backendStore = mutableMapOf<String, MutableList<MessageDto>>()

    override fun getSessionId(): String = "test_session_id"
    override fun connect() {}
    override fun disconnect() {}

    override fun setCurrentRoomId(roomId: String) {
        val cleanRoom = roomId.trim()
        val isDifferent = !isRoomMatching(activeRoomId, cleanRoom)
        activeRoomId = cleanRoom
        if (isDifferent) {
            val cached = (backendStore[cleanRoom] ?: sessionStorage.getRoomMessages(cleanRoom))
                .filter { isRoomMatching(it.roomId, cleanRoom) }
            _messages.value = cached
        }
    }

    override fun getCurrentRoomId(): String = activeRoomId

    override suspend fun loadPublicRooms(): Result<List<PublicRoomDto>> = Result.success(emptyList())
    override suspend fun loadUserRooms(): Result<List<RoomDto>> = Result.success(emptyList())
    override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean): Result<RoomDto> =
        Result.success(RoomDto(roomId = "grp_${name.lowercase()}", name = name, isPublic = isPublic))

    override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?): Result<List<MessageDto>> {
        val cleanRoom = roomId.trim()
        val rawList = backendStore[cleanRoom] ?: mutableListOf()
        val scopedList = rawList.filter { isRoomMatching(it.roomId, cleanRoom) }
        sessionStorage.saveRoomMessages(cleanRoom, scopedList)
        if (isRoomMatching(activeRoomId, cleanRoom)) {
            _messages.value = scopedList
        }
        return Result.success(scopedList)
    }

    override suspend fun loadOnlineUsers(): Result<List<OnlineUserDto>> = Result.success(emptyList())

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
        val cleanRoom = roomId.trim()
        val list = backendStore.getOrPut(cleanRoom) { mutableListOf() }
        val newMsg = MessageDto(
            postNum = (list.maxOfOrNull { it.postNum } ?: 1000L) + 1L,
            clientMsgId = clientMsgId,
            publicId = _myIdentity.value?.publicId,
            anonName = _myIdentity.value?.anonName ?: "Tester",
            content = content,
            roomId = cleanRoom,
            createdAt = "2026-09-27T12:00:00"
        )
        list.add(newMsg)
        sessionStorage.saveRoomMessages(cleanRoom, list)
        if (isRoomMatching(activeRoomId, cleanRoom)) {
            _messages.value = list.filter { isRoomMatching(it.roomId, activeRoomId) }
        }
        return true
    }

    suspend fun simulateIncomingRealtimeMessage(msg: MessageDto) {
        val msgRoom = msg.roomId.trim()
        val list = backendStore.getOrPut(msgRoom) { mutableListOf() }
        list.add(msg)
        sessionStorage.saveRoomMessages(msgRoom, list)
        if (isRoomMatching(msgRoom, activeRoomId)) {
            _messages.value = list.filter { isRoomMatching(it.roomId, activeRoomId) }
        }
        _incomingEvents.emit(IncomingEvent.NewMessage(msg))
    }

    override suspend fun sendTyping(): Boolean = true
    override suspend fun toggleReaction(postNum: Long, emoji: String): Boolean = true
    override suspend fun requestPrivateChat(targetPublicId: String): Boolean = true
    override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean = true
    override suspend fun leaveRoom(roomId: String): Boolean = true
    override suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean = true
    override suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse> = Result.failure(UnsupportedOperationException())
    override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<UploadResponse> = Result.failure(UnsupportedOperationException())
    override suspend fun submitFeedback(category: String, message: String, rating: Int): Result<FeedbackResponse> = Result.failure(UnsupportedOperationException())
    override suspend fun leaveAllRooms(): Result<List<String>> = Result.success(emptyList())
    override suspend fun updateRoomName(roomId: String, newName: String): Result<Boolean> = Result.success(true)
    override suspend fun deleteRoom(roomId: String): Result<Boolean> = Result.success(true)

    val roomMembersStore = mutableMapOf<String, MutableList<RoomMemberDto>>()
    fun setTestOnlineUsers(users: List<OnlineUserDto>) { _onlineUsers.value = users }

    override suspend fun getRoomMembers(roomId: String): Result<List<RoomMemberDto>> {
        val cleanRoom = roomId.trim()
        val members = roomMembersStore[cleanRoom] ?: emptyList()
        val scoped = members.filter { isRoomMatching(it.roomId, cleanRoom) || it.roomId.isNullOrBlank() }
        return Result.success(scoped)
    }

    override suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> = Result.failure(UnsupportedOperationException())
    override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String = relativeOrAbsoluteUrl
    override fun syncWidgets() {}
    override val discoverableGroups: StateFlow<List<PublicRoomDto>> = MutableStateFlow(emptyList())
}

class GroupChatDataIsolationTest {

    private suspend fun awaitCondition(
        name: String,
        timeoutMs: Long = 2000,
        details: (() -> String)? = null,
        condition: () -> Boolean
    ) {
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
    fun testIsRoomMatchingLogic() {
        // Exact matches
        assertTrue(isRoomMatching("main", "main"))
        assertTrue(isRoomMatching("#PRIVATE", "private"))
        assertTrue(isRoomMatching("cse_stream", "cse-stream"))
        assertTrue(isRoomMatching("grp_public_123", "grp_public_123"))

        // Default public stream alias
        assertTrue(isRoomMatching("", "main"))
        assertTrue(isRoomMatching("main", ""))

        // Strict isolation between distinct groups
        assertFalse(isRoomMatching("grp_public_123", "grp_private_456"))
        assertFalse(isRoomMatching("main", "grp_private_456"))
        assertFalse(isRoomMatching("grp_public_123", "main"))
        assertFalse(isRoomMatching("cse", "ece"))
        assertFalse(isRoomMatching("grp_alpha", "grp_alphabet"))
        assertFalse(isRoomMatching("", "grp_private_456"))
    }

    @Test
    fun testExactUserScenarioPublicAndPrivateSwitching() = runBlocking {
        val sessionStorage = InMemorySessionStorage()
        val repo = IsolatedTestChatRepository(sessionStorage)

        // 1. Open Public
        val publicVm = ChatViewModel(
            roomId = "main",
            roomName = "Public",
            repository = repo,
            sessionStorage = sessionStorage
        )

        awaitCondition("public initial loaded") { !publicVm.uiState.value.isLoading }

        // 2. Send "PUBLIC_TEST_123" in Public
        publicVm.onInputTextChange("PUBLIC_TEST_123")
        publicVm.sendMessage()

        awaitCondition("public msg sent", details = { "msgs=${publicVm.uiState.value.messages}, isSending=${publicVm.uiState.value.isSending}" }) {
            publicVm.uiState.value.messages.any { it.content == "PUBLIC_TEST_123" && it.postNum > 0 }
        }
        val publicMsgs = publicVm.uiState.value.messages
        assertTrue(publicMsgs.any { it.content == "PUBLIC_TEST_123" && it.roomId == "main" })

        // 4. Leave Public (dispose VM)
        publicVm.clear()

        // 5. Open Private
        val privateVm = ChatViewModel(
            roomId = "grp_private_456",
            roomName = "Private",
            repository = repo,
            sessionStorage = sessionStorage
        )

        awaitCondition("private initial loaded") { !privateVm.uiState.value.isLoading }

        // 6. Verify "PUBLIC_TEST_123" is NOT visible in Private!
        val privateInitialMsgs = privateVm.uiState.value.messages
        assertFalse(
            privateInitialMsgs.any { it.content == "PUBLIC_TEST_123" },
            "PUBLIC_TEST_123 must NOT appear in Private group!"
        )
        assertEquals(0, privateInitialMsgs.size)

        // 7. Send "PRIVATE_TEST_456" in Private
        privateVm.onInputTextChange("PRIVATE_TEST_456")
        privateVm.sendMessage()

        awaitCondition("private msg sent", details = { "msgs=${privateVm.uiState.value.messages}, isSending=${privateVm.uiState.value.isSending}" }) {
            privateVm.uiState.value.messages.any { it.content == "PRIVATE_TEST_456" && it.postNum > 0 }
        }
        val privateAfterSend = privateVm.uiState.value.messages
        assertTrue(privateAfterSend.any { it.content == "PRIVATE_TEST_456" && it.roomId == "grp_private_456" })
        assertFalse(privateAfterSend.any { it.content == "PUBLIC_TEST_123" })

        // 8. Leave Private, Switch back to Public
        privateVm.clear()
        val publicVm2 = ChatViewModel(
            roomId = "main",
            roomName = "Public",
            repository = repo,
            sessionStorage = sessionStorage
        )

        // 9. Verify only Public messages are shown, including "PUBLIC_TEST_123"
        awaitCondition("publicVm2 loaded") {
            publicVm2.uiState.value.messages.any { it.content == "PUBLIC_TEST_123" }
        }
        val publicMsgs2 = publicVm2.uiState.value.messages
        assertTrue(publicMsgs2.any { it.content == "PUBLIC_TEST_123" })
        assertFalse(publicMsgs2.any { it.content == "PRIVATE_TEST_456" })

        // 10. Switch back to Private
        publicVm2.clear()
        val privateVm2 = ChatViewModel(
            roomId = "grp_private_456",
            roomName = "Private",
            repository = repo,
            sessionStorage = sessionStorage
        )

        // 11. Verify "PRIVATE_TEST_456" is shown and Public messages are absent
        awaitCondition("privateVm2 loaded") {
            privateVm2.uiState.value.messages.any { it.content == "PRIVATE_TEST_456" }
        }
        val privateMsgs2 = privateVm2.uiState.value.messages
        assertTrue(privateMsgs2.any { it.content == "PRIVATE_TEST_456" })
        assertFalse(privateMsgs2.any { it.content == "PUBLIC_TEST_123" })

        // 12. Test third group: CSE Group
        privateVm2.clear()
        val cseVm = ChatViewModel(
            roomId = "cse",
            roomName = "CSE Group",
            repository = repo,
            sessionStorage = sessionStorage
        )
        awaitCondition("cseVm initial loaded") { !cseVm.uiState.value.isLoading }
        assertEquals(0, cseVm.uiState.value.messages.size)
        cseVm.onInputTextChange("CSE_TEST_789")
        cseVm.sendMessage()

        awaitCondition("cse msg sent") {
            cseVm.uiState.value.messages.any { it.content == "CSE_TEST_789" && it.postNum > 0 }
        }
        val cseMsgs = cseVm.uiState.value.messages
        assertTrue(cseMsgs.any { it.content == "CSE_TEST_789" && it.roomId == "cse" })
        assertFalse(cseMsgs.any { it.content == "PUBLIC_TEST_123" })
        assertFalse(cseMsgs.any { it.content == "PRIVATE_TEST_456" })
        cseVm.clear()
    }

    @Test
    fun testRealtimeMessagesWhileAnotherGroupIsOpen() = runBlocking {
        val sessionStorage = InMemorySessionStorage()
        val repo = IsolatedTestChatRepository(sessionStorage)

        // Currently in "grp_private_1"
        val privateVm = ChatViewModel(
            roomId = "grp_private_1",
            roomName = "Private 1",
            repository = repo,
            sessionStorage = sessionStorage
        )

        // Simulate incoming WebSocket message for "main"
        val publicIncomingMsg = MessageDto(
            postNum = 2001L,
            publicId = "usr_other",
            anonName = "Chatter",
            content = "Hello Public from afar",
            roomId = "main",
            createdAt = "2026-09-27T12:05:00"
        )
        repo.simulateIncomingRealtimeMessage(publicIncomingMsg)

        // Give a short delay to verify message does NOT leak into private
        kotlinx.coroutines.delay(50)

        // Active Private chat must NOT receive or display the message sent to Public
        val currentPrivateMsgs = privateVm.uiState.value.messages
        assertFalse(
            currentPrivateMsgs.any { it.content == "Hello Public from afar" },
            "Realtime message for Public room must not be injected into Private chat!"
        )

        // Switch to Public room
        privateVm.clear()
        val publicVm = ChatViewModel(
            roomId = "main",
            roomName = "Public",
            repository = repo,
            sessionStorage = sessionStorage
        )

        // Public room must have the message
        awaitCondition("public loaded realtime message") {
            publicVm.uiState.value.messages.any { it.content == "Hello Public from afar" }
        }
        val currentPublicMsgs = publicVm.uiState.value.messages
        assertTrue(
            currentPublicMsgs.any { it.content == "Hello Public from afar" },
            "Public room must display its own realtime message when opened!"
        )
        publicVm.clear()
    }

    @Test
    fun testOpeningEmptyGroupDoesNotReusePreviousGroupMessages() = runBlocking {
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val sessionStorage = InMemorySessionStorage()
        val repo = IsolatedTestChatRepository(sessionStorage)

        // 1. Send messages in Group A
        repo.setCurrentRoomId("grp_alpha")
        repo.sendMessage(content = "Alpha message", roomId = "grp_alpha")

        // 2. Open empty Group B
        val betaVm = ChatViewModel(
            roomId = "grp_beta",
            roomName = "Beta Group",
            repository = repo,
            sessionStorage = sessionStorage,
            scope = testScope
        )

        // 3. Messages in Beta group must be empty, not Alpha
        assertEquals(0, betaVm.uiState.value.messages.size)
        assertFalse(betaVm.uiState.value.messages.any { it.content == "Alpha message" })
        betaVm.clear()
    }

    @Test
    fun testGroupMemberIsolationBetweenGroupAAndGroupB() = runBlocking {
        val sessionStorage = InMemorySessionStorage()
        val repo = IsolatedTestChatRepository(sessionStorage)

        // Populate Group A members (Alice, Bob [offline])
        val groupAMembers = mutableListOf(
            RoomMemberDto(roomId = "grp_A", publicId = "usr_alice", anonName = "Alice", role = "ADMIN"),
            RoomMemberDto(roomId = "grp_A", publicId = "usr_bob", anonName = "Bob", role = "MEMBER")
        )
        // Populate Group B members (Charlie, Dave)
        val groupBMembers = mutableListOf(
            RoomMemberDto(roomId = "grp_B", publicId = "usr_charlie", anonName = "Charlie", role = "ADMIN"),
            RoomMemberDto(roomId = "grp_B", publicId = "usr_dave", anonName = "Dave", role = "MEMBER")
        )
        repo.roomMembersStore["grp_A"] = groupAMembers
        repo.roomMembersStore["grp_B"] = groupBMembers

        // Global online users (Eve is online globally on campus, but NOT in Group A or Group B)
        repo.setTestOnlineUsers(listOf(
            OnlineUserDto(publicId = "usr_alice", anonName = "Alice"),
            OnlineUserDto(publicId = "usr_charlie", anonName = "Charlie"),
            OnlineUserDto(publicId = "usr_eve", anonName = "Eve") // global campus online user
        ))

        // Query Group A
        val resA = repo.getRoomMembers("grp_A").getOrThrow()
        assertEquals(2, resA.size)
        assertTrue(resA.any { it.publicId == "usr_alice" })
        assertTrue(resA.any { it.publicId == "usr_bob" }, "Offline member Bob must still appear in Group A")
        assertFalse(resA.any { it.publicId == "usr_charlie" }, "Group B members must not appear in Group A")
        assertFalse(resA.any { it.publicId == "usr_dave" }, "Group B members must not appear in Group A")
        assertFalse(resA.any { it.publicId == "usr_eve" }, "Global online user must NOT appear in Group A members list")

        // Query Group B
        val resB = repo.getRoomMembers("grp_B").getOrThrow()
        assertEquals(2, resB.size)
        assertTrue(resB.any { it.publicId == "usr_charlie" })
        assertTrue(resB.any { it.publicId == "usr_dave" })
        assertFalse(resB.any { it.publicId == "usr_alice" }, "Group A members must not appear in Group B")
        assertFalse(resB.any { it.publicId == "usr_bob" }, "Group A members must not appear in Group B")
        assertFalse(resB.any { it.publicId == "usr_eve" }, "Global online user must NOT appear in Group B members list")
    }
}
