package org.example.annochat.data.model

import kotlinx.serialization.Serializable

enum class NotificationType {
    INVITE,
    ANNOUNCEMENT,
    MESSAGE,
    SYSTEM
}

@Serializable
data class AppNotification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timeLabel: String = "Just now",
    val roomId: String? = null,
    val roomName: String? = null,
    val inviteId: String? = null,
    val fromPublicId: String? = null,
    val fromName: String? = null,
    val fromAvatar: String? = null,
    val isGroup: Boolean = false,
    val isRead: Boolean = false,
    val isHandled: Boolean = false
)
