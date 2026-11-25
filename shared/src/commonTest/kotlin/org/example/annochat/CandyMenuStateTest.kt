package org.example.annochat

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.example.annochat.data.model.AppNotification
import org.example.annochat.data.model.FeedbackResponse
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.NotificationType
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.StickersAndGifsResponse
import org.example.annochat.data.model.UploadResponse
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.data.repository.NotificationRepositoryImpl
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.presentation.components.formatNotificationBadge
import org.example.annochat.presentation.components.sanitizeRoomId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeTestChatRepository : ChatRepository {
    override val connectionState: StateFlow<ConnectionState> =
        MutableStateFlow(ConnectionState(ConnectionStatus.CONNECTED))
    override val myIdentity: StateFlow<UserIdentityDto?> =
        MutableStateFlow(UserIdentityDto(publicId = "test_pid", anonName = "Snow Leopard", avatarUrl = ""))
    override val onlineCount: StateFlow<Int> = MutableStateFlow(5)
    override val publicRooms: StateFlow<List<PublicRoomDto>> = MutableStateFlow(emptyList())
    override val userRooms: StateFlow<List<RoomDto>> = MutableStateFlow(emptyList())
    override val onlineUsers: StateFlow<List<OnlineUserDto>> = MutableStateFlow(emptyList())
    override val hasLoadedOnlineUsers: StateFlow<Boolean> = MutableStateFlow(true)
    override val messages: StateFlow<List<MessageDto>> = MutableStateFlow(emptyList())

    val testEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    override val incomingEvents: SharedFlow<IncomingEvent> = testEvents.asSharedFlow()

    var respondedInviteId: String? = null
    var respondedAccepted: Boolean? = null

    override fun getSessionId(): String = "sess_test"
    override fun connect() {}
    override fun disconnect() {}
    override fun setCurrentRoomId(roomId: String) {}
    override fun getCurrentRoomId(): String = "main"

    override suspend fun loadPublicRooms(): Result<List<PublicRoomDto>> = Result.success(emptyList())
    override suspend fun loadUserRooms(): Result<List<RoomDto>> = Result.success(emptyList())
    override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean): Result<RoomDto> = Result.success(RoomDto(roomId = "test_room", name = name))
    override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?): Result<List<MessageDto>> = Result.success(emptyList())
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

    override suspend fun sendTyping(): Boolean = true
    override suspend fun toggleReaction(postNum: Long, emoji: String): Boolean = true
    override suspend fun requestPrivateChat(targetPublicId: String): Boolean = true
    override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean {
        respondedInviteId = inviteId
        respondedAccepted = accepted
        return true
    }
    override suspend fun leaveRoom(roomId: String): Boolean = true
    override suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean = true
    override suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse> = Result.success(StickersAndGifsResponse("ok", emptyList(), emptyList()))
    override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<UploadResponse> = Result.failure(Exception("Not used"))
    override suspend fun submitFeedback(category: String, message: String, rating: Int): Result<FeedbackResponse> = Result.success(FeedbackResponse("ok", 1))
    override suspend fun leaveAllRooms(): Result<List<String>> = Result.success(emptyList())
    override suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> = Result.success(ByteArray(0))
    override fun syncWidgets() {}
    override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String = relativeOrAbsoluteUrl
}

class CandyMenuStateTest {

    @Test
    fun testNotificationBadgeFormatting() {
        // 0 unread: no badge
        assertNull(formatNotificationBadge(0))
        assertNull(formatNotificationBadge(-1))
        assertNull(formatNotificationBadge(-100))

        // 1-9: exact digit
        assertEquals("1", formatNotificationBadge(1))
        assertEquals("5", formatNotificationBadge(5))
        assertEquals("9", formatNotificationBadge(9))

        // 10-99: exact 2 digits
        assertEquals("10", formatNotificationBadge(10))
        assertEquals("42", formatNotificationBadge(42))
        assertEquals("99", formatNotificationBadge(99))

        // 100+: "99+"
        assertEquals("99+", formatNotificationBadge(100))
        assertEquals("99+", formatNotificationBadge(101))
        assertEquals("99+", formatNotificationBadge(9999))
    }

    @Test
    fun testSanitizeRoomId() {
        assertEquals("latenightstudy", sanitizeRoomId("Late Night Study"))
        assertEquals("cse_stream", sanitizeRoomId("CSE_Stream"))
        assertEquals("campus-gaming", sanitizeRoomId("Campus-Gaming!"))
        assertEquals("room123", sanitizeRoomId("  #room123  "))
        assertTrue(sanitizeRoomId("   ").isNotBlank())
    }

    @Test
    fun testNotificationRepositoryFlows() = runBlocking {
        val fakeRepo = FakeTestChatRepository()
        val notifRepo = NotificationRepositoryImpl(fakeRepo)

        // Ensure collector has attached before emitting events
        var waitCount = 0
        while (fakeRepo.testEvents.subscriptionCount.value == 0 && waitCount < 100) {
            kotlinx.coroutines.delay(10)
            waitCount++
        }

        assertEquals(0, notifRepo.unreadCount.first())
        assertTrue(notifRepo.notifications.first().isEmpty())

        // 1. Receive private chat invite
        fakeRepo.testEvents.emit(
            IncomingEvent.PrivateChatInvite(
                inviteId = "inv_123",
                fromId = "pid_sender",
                fromName = "Arctic Fox",
                fromAvatar = "/api/animal/arctic_fox",
                isRandomMatch = false
            )
        )
        kotlinx.coroutines.delay(250)

        val notifsAfterInvite = notifRepo.notifications.first()
        assertEquals(1, notifsAfterInvite.size)
        assertEquals("inv_123", notifsAfterInvite[0].id)
        assertEquals(NotificationType.INVITE, notifsAfterInvite[0].type)
        assertEquals("Arctic Fox", notifsAfterInvite[0].fromName)
        assertFalse(notifsAfterInvite[0].isRead)
        assertEquals(1, notifRepo.unreadCount.first())

        // 2. Mark all as read
        notifRepo.markAllAsRead()
        assertEquals(0, notifRepo.unreadCount.first())
        assertTrue(notifRepo.notifications.first()[0].isRead)

        // 3. Receive campus announcement
        fakeRepo.testEvents.emit(
            IncomingEvent.Announcement(text = "Hostel Wi-Fi maintenance tonight at 2 AM.")
        )
        kotlinx.coroutines.delay(250)
        val notifsAfterAnnouncement = notifRepo.notifications.first()
        assertEquals(2, notifsAfterAnnouncement.size)
        assertEquals(1, notifRepo.unreadCount.first())
        assertEquals(NotificationType.ANNOUNCEMENT, notifsAfterAnnouncement[0].type)

        // 4. Respond to invite
        val responded = notifRepo.respondToInvite("inv_123", accept = true)
        assertTrue(responded)
        assertEquals("inv_123", fakeRepo.respondedInviteId)
        assertEquals(true, fakeRepo.respondedAccepted)
        assertTrue(notifRepo.notifications.first().first { it.inviteId == "inv_123" }.isHandled)

        // 5. Remove single notification
        notifRepo.removeNotification("inv_123")
        assertEquals(1, notifRepo.notifications.first().size)

        // 6. Clear all
        notifRepo.clearAll()
        assertTrue(notifRepo.notifications.first().isEmpty())
        assertEquals(0, notifRepo.unreadCount.first())
    }
}
