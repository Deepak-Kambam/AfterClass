package org.example.annochat.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.example.annochat.data.model.AppNotification
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.NotificationType
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.generateSecureUuidString
import org.example.annochat.presentation.components.formatTimestamp

interface NotificationRepository {
    val notifications: StateFlow<List<AppNotification>>
    val unreadCount: StateFlow<Int>

    fun addNotification(notification: AppNotification)
    fun markAllAsRead()
    fun markAsRead(notificationId: String)
    fun removeNotification(notificationId: String)
    fun clearAll()
    suspend fun respondToInvite(inviteId: String, accept: Boolean): Boolean
}

class NotificationRepositoryImpl(
    private val chatRepository: ChatRepository,
    private val sessionStorage: SessionStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : NotificationRepository {

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    override val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    override val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private fun updateNotifications(newList: List<AppNotification>) {
        val bounded = if (newList.size > 50) newList.take(50) else newList
        _notifications.value = bounded
        _unreadCount.value = bounded.count { !it.isRead }
    }

    init {
        scope.launch {
            chatRepository.incomingEvents.collect { event ->
                handleIncomingEvent(event)
            }
        }
    }

    private fun handleIncomingEvent(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.PrivateChatInvite -> {
                val isGroupInvite = event.isGroup || !event.roomId.isNullOrBlank()
                val groupTitle = event.roomName?.ifBlank { null } ?: "Private Group"
                val notif = AppNotification(
                    id = event.inviteId.ifBlank { generateSecureUuidString() },
                    type = NotificationType.INVITE,
                    title = if (isGroupInvite) "Group Chat Invitation" else "Private Chat Request",
                    message = if (isGroupInvite) {
                        "${event.fromName} invited you to join \"$groupTitle\"."
                    } else {
                        "${event.fromName} wants to start an encrypted private chat with you."
                    },
                    timeLabel = "Just now",
                    inviteId = event.inviteId,
                    roomId = event.roomId,
                    roomName = event.roomName,
                    fromPublicId = event.fromId,
                    fromName = event.fromName,
                    fromAvatar = event.fromAvatar,
                    isGroup = isGroupInvite,
                    isRead = false,
                    isHandled = false
                )
                addNotification(notif)
            }
            is IncomingEvent.Announcement -> {
                val notif = AppNotification(
                    id = generateSecureUuidString(),
                    type = NotificationType.ANNOUNCEMENT,
                    title = "Campus Announcement",
                    message = event.text,
                    timeLabel = "Just now",
                    isRead = false
                )
                addNotification(notif)
            }
            is IncomingEvent.NewMessage -> {
                val isNotifEnabled = sessionStorage?.isMessageNotificationsEnabled() ?: true
                val myPid = chatRepository.myIdentity.value?.publicId
                val isOwn = event.message.publicId == myPid
                val currentRoom = chatRepository.getCurrentRoomId()
                val isCurrentRoom = event.message.roomId == currentRoom

                if (isNotifEnabled && !isOwn && !isCurrentRoom) {
                    val roomTitle = if (event.message.roomId == "main") "Public Stream" else "#${event.message.roomId}"
                    val preview = if (sessionStorage?.isNotificationPreviewEnabled() != false) {
                        "${event.message.anonName}: ${event.message.content?.take(80) ?: "Sent an attachment"}"
                    } else {
                        "New message in $roomTitle"
                    }
                    val notif = AppNotification(
                        id = "msg_${event.message.postNum}",
                        type = NotificationType.MESSAGE,
                        title = roomTitle,
                        message = preview,
                        timeLabel = event.message.createdAt?.let { formatTimestamp(it) } ?: "Just now",
                        roomId = event.message.roomId,
                        fromPublicId = event.message.publicId,
                        fromName = event.message.anonName,
                        fromAvatar = event.message.anonAvatar,
                        isRead = false
                    )
                    addNotification(notif)
                }
            }
            is IncomingEvent.PrivateChatDeclined -> {
                val notif = AppNotification(
                    id = generateSecureUuidString(),
                    type = NotificationType.SYSTEM,
                    title = "Chat Request Declined",
                    message = "${event.declinedBy} declined your chat request.",
                    timeLabel = "Just now",
                    isRead = false
                )
                addNotification(notif)
            }
            else -> {}
        }
    }

    override fun addNotification(notification: AppNotification) {
        val current = _notifications.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == notification.id }
        if (existingIndex >= 0) {
            current[existingIndex] = notification
        } else {
            current.add(0, notification)
        }
        updateNotifications(current)
    }

    override fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        updateNotifications(updated)
    }

    override fun markAsRead(notificationId: String) {
        val updated = _notifications.value.map {
            if (it.id == notificationId) it.copy(isRead = true) else it
        }
        updateNotifications(updated)
    }

    override fun removeNotification(notificationId: String) {
        updateNotifications(_notifications.value.filter { it.id != notificationId })
    }

    override fun clearAll() {
        updateNotifications(emptyList())
    }

    override suspend fun respondToInvite(inviteId: String, accept: Boolean): Boolean {
        val targetNotif = _notifications.value.firstOrNull { it.inviteId == inviteId }
        val result = chatRepository.respondPrivateChat(inviteId, accept)
        if (accept) {
            val rId = targetNotif?.roomId
            if (!rId.isNullOrBlank()) {
                sessionStorage?.addMyRoom(rId, isOwner = false, isPrivate = true)
            }
        }
        val updated = _notifications.value.map {
            if (it.inviteId == inviteId) it.copy(isHandled = true, isRead = true) else it
        }
        updateNotifications(updated)
        return result
    }
}
