package org.example.annochat

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.viewmodel.ChatViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypingIndicatorLogicTest {

    private class FakeTypingChatRepository(
        val myPid: String = "pid_self_123",
        val myName: String = "Snow Leopard",
        val mySess: String = "sess_self_123"
    ) : ChatRepository {
        val inEvents = MutableSharedFlow<IncomingEvent>(replay = 16, extraBufferCapacity = 64)
        override val incomingEvents: SharedFlow<IncomingEvent> = inEvents.asSharedFlow()

        val connState = MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
        override val connectionState: StateFlow<ConnectionState> = connState.asStateFlow()

        val myIdent = MutableStateFlow<UserIdentityDto?>(
            UserIdentityDto(
                publicId = myPid,
                anonName = myName,
                animalName = myName,
                avatarUrl = "/api/animal/snow_leopard",
                isHost = false
            )
        )
        override val myIdentity: StateFlow<UserIdentityDto?> = myIdent.asStateFlow()

        val msgState = MutableStateFlow<List<MessageDto>>(emptyList())
        override val messages: StateFlow<List<MessageDto>> = msgState.asStateFlow()

        var lastTypingSentRoomId: String? = null
        var typingSentCount = 0

        override fun getSessionId(): String = mySess
        override fun connect() {}
        override fun disconnect() {}
        override fun setCurrentRoomId(roomId: String) {}
        override fun getCurrentRoomId(): String = "main"

        override suspend fun loadPublicRooms(): Result<List<PublicRoomDto>> = Result.success(emptyList())
        override suspend fun loadUserRooms(): Result<List<RoomDto>> = Result.success(emptyList())
        override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean): Result<RoomDto> =
            Result.success(RoomDto(roomId = "grp_test", name = name, isPublic = isPublic))

        override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?): Result<List<MessageDto>> =
            Result.success(emptyList())

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
        ): Boolean = true

        override suspend fun sendTyping(): Boolean {
            typingSentCount++
            return true
        }

        override suspend fun sendTyping(roomId: String?): Boolean {
            lastTypingSentRoomId = roomId
            typingSentCount++
            return true
        }

        override val onlineCount: StateFlow<Int> = MutableStateFlow(1)
        override val publicRooms: StateFlow<List<PublicRoomDto>> = MutableStateFlow(emptyList())
        override val userRooms: StateFlow<List<RoomDto>> = MutableStateFlow(emptyList())
        override val onlineUsers: StateFlow<List<OnlineUserDto>> = MutableStateFlow(emptyList())
        override val hasLoadedOnlineUsers: StateFlow<Boolean> = MutableStateFlow(true)

        override suspend fun toggleReaction(postNum: Long, emoji: String): Boolean = true
        override suspend fun requestPrivateChat(targetPublicId: String): Boolean = true
        override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean = true
        override suspend fun leaveRoom(roomId: String): Boolean = true
        override suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean = true
        override suspend fun getStickersAndGifs(): Result<org.example.annochat.data.model.StickersAndGifsResponse> =
            Result.success(org.example.annochat.data.model.StickersAndGifsResponse("ok", emptyList(), emptyList()))
        override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<org.example.annochat.data.model.UploadResponse> =
            Result.failure(Exception("Not used"))
        override suspend fun submitFeedback(category: String, message: String, rating: Int): Result<org.example.annochat.data.model.FeedbackResponse> =
            Result.success(org.example.annochat.data.model.FeedbackResponse("ok", 1))
        override suspend fun leaveAllRooms(): Result<List<String>> = Result.success(emptyList())
        override suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> = Result.success(ByteArray(0))
        override fun syncWidgets() {}
        override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String = relativeOrAbsoluteUrl
    }

    private suspend fun awaitCondition(
        name: String,
        timeoutMs: Long = 2000,
        condition: () -> Boolean
    ) {
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        while (!condition()) {
            if (start.elapsedNow().inWholeMilliseconds > timeoutMs) {
                throw AssertionError("Condition '$name' not met within $timeoutMs ms.")
            }
            delay(10)
        }
    }

    @Test
    fun testSelfTypingExcludedByPublicId() = runBlocking {
        val repo = FakeTypingChatRepository(myPid = "pid_me_999", myName = "Arctic Fox")
        val storage = InMemorySessionStorage()
        storage.saveIdentity("pid_me_999", "Arctic Fox")
        storage.saveSessionId("sess_me_999")

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        // Incoming typing event containing my own publicId
        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Different Name",
                publicId = "pid_me_999",
                roomId = "main"
            )
        )

        delay(100)
        // Must NOT appear in typing users
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testSelfTypingExcludedBySessionId() = runBlocking {
        val repo = FakeTypingChatRepository(myPid = "pid_me_111", mySess = "sess_me_111")
        val storage = InMemorySessionStorage()
        storage.saveSessionId("sess_me_111")

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Anonymous",
                sessionId = "sess_me_111",
                roomId = "main"
            )
        )

        delay(100)
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testSelfTypingExcludedByNameAndYou() = runBlocking {
        val repo = FakeTypingChatRepository(myName = "Giant Panda")
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        // Typing event with "You"
        repo.inEvents.emit(IncomingEvent.Typing(animalName = "You", roomId = "main"))
        delay(100)
        assertEquals(emptyList(), vm.uiState.value.typingUsers)

        // Typing event with my anonName
        repo.inEvents.emit(IncomingEvent.Typing(animalName = "Giant Panda", roomId = "main"))
        delay(100)
        assertEquals(emptyList(), vm.uiState.value.typingUsers)

        vm.clear()
    }

    @Test
    fun testOtherUsersTypingShowsNormally() = runBlocking {
        val repo = FakeTypingChatRepository()
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Red Panda",
                publicId = "pid_other_1",
                roomId = "main"
            )
        )

        awaitCondition("Red Panda typing visible") {
            vm.uiState.value.typingUsers.contains("Red Panda")
        }
        assertEquals(listOf("Red Panda"), vm.uiState.value.typingUsers)

        // Second distinct user typing
        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Capybara",
                publicId = "pid_other_2",
                roomId = "main"
            )
        )

        awaitCondition("Both typers visible") {
            vm.uiState.value.typingUsers.size == 2
        }
        assertEquals(listOf("Red Panda", "Capybara"), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testRoomScopingPreventsLeakage() = runBlocking {
        val repo = FakeTypingChatRepository()
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        // Typing event for private group "grp_private_1"
        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Koala",
                publicId = "pid_koala",
                roomId = "grp_private_1"
            )
        )

        delay(100)
        // Must NOT appear in main room
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testTypingUserRemovedWhenMessageSent() = runBlocking {
        val repo = FakeTypingChatRepository()
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Cheetah",
                publicId = "pid_cheetah",
                roomId = "main"
            )
        )
        awaitCondition("Cheetah typing visible") {
            vm.uiState.value.typingUsers.contains("Cheetah")
        }

        // Cheetah posts a message
        repo.inEvents.emit(
            IncomingEvent.NewMessage(
                MessageDto(
                    postNum = 101L,
                    publicId = "pid_cheetah",
                    anonName = "Cheetah",
                    content = "Hey everyone!",
                    roomId = "main"
                )
            )
        )

        awaitCondition("Cheetah typing cleared after message") {
            vm.uiState.value.typingUsers.isEmpty()
        }
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testTypingUserRemovedWhenDisconnected() = runBlocking {
        val repo = FakeTypingChatRepository()
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Otter",
                publicId = "pid_otter",
                roomId = "main"
            )
        )
        awaitCondition("Otter typing visible") {
            vm.uiState.value.typingUsers.contains("Otter")
        }

        // Otter disconnects
        repo.inEvents.emit(IncomingEvent.UserDisconnected(publicId = "pid_otter"))

        awaitCondition("Otter typing cleared after disconnect") {
            vm.uiState.value.typingUsers.isEmpty()
        }
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }

    @Test
    fun testDisconnectClearsTypingUsers() = runBlocking {
        val repo = FakeTypingChatRepository()
        val storage = InMemorySessionStorage()

        val vm = ChatViewModel(roomId = "main", roomName = "Public", repository = repo, sessionStorage = storage)

        repo.inEvents.emit(
            IncomingEvent.Typing(
                animalName = "Otter",
                publicId = "pid_otter",
                roomId = "main"
            )
        )
        awaitCondition("Otter typing visible") {
            vm.uiState.value.typingUsers.contains("Otter")
        }

        // Connection drops
        repo.connState.value = ConnectionState(ConnectionStatus.DISCONNECTED)

        awaitCondition("Typing cleared on disconnect") {
            vm.uiState.value.typingUsers.isEmpty()
        }
        assertEquals(emptyList(), vm.uiState.value.typingUsers)
        vm.clear()
    }
}
