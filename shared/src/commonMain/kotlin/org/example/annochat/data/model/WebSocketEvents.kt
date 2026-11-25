package org.example.annochat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Sealed hierarchy for all incoming events broadcasted by the FastAPI backend over WebSocket.
 */
sealed interface IncomingEvent {
    data class MyIdentity(val identity: UserIdentityDto) : IncomingEvent
    data class NewMessage(val message: MessageDto) : IncomingEvent
    data class OnlineCount(val count: Int, val users: List<OnlineUserDto>? = null) : IncomingEvent
    data class UserDisconnected(val publicId: String) : IncomingEvent
    data class Typing(
        val animalName: String,
        val publicId: String? = null,
        val sessionId: String? = null,
        val roomId: String? = null
    ) : IncomingEvent
    data class ReactionUpdate(val postNum: Long, val reactions: Map<String, Int>) : IncomingEvent
    data class PrivateChatInvite(
        val inviteId: String,
        val fromId: String,
        val fromName: String,
        val fromAvatar: String,
        val roomId: String? = null,
        val roomName: String? = null,
        val isGroup: Boolean = false,
        val isRandomMatch: Boolean = false
    ) : IncomingEvent
    data class PrivateChatDeclined(
        val declinedBy: String,
        val roomId: String? = null,
        val isGroup: Boolean = false
    ) : IncomingEvent
    data class PrivateChatSent(
        val targetName: String,
        val isGroup: Boolean = false,
        val roomName: String? = null
    ) : IncomingEvent
    data class RoomInvitationAccepted(
        val acceptedBy: String,
        val roomId: String? = null,
        val roomName: String? = null
    ) : IncomingEvent
    data class RoomJoined(val room: RoomDto, val systemMessage: MessageDto? = null) : IncomingEvent
    data class RoomUpdated(val room: RoomDto, val systemMessage: MessageDto? = null) : IncomingEvent
    data class RoomLeft(val roomId: String) : IncomingEvent
    data class RoomDeleted(val roomId: String, val roomName: String? = null, val reason: String? = null) : IncomingEvent
    data class DeleteMessage(val postNum: Long) : IncomingEvent
    data class PostsDeleted(val postNums: List<Long>) : IncomingEvent
    object ClearChat : IncomingEvent
    data class Announcement(val text: String) : IncomingEvent
    data class SystemBroadcast(val broadcast: org.example.annochat.data.model.SystemBroadcast) : IncomingEvent
    data class ServerError(val message: String) : IncomingEvent
    data class Pong(val time: Double? = null) : IncomingEvent
    data class Unknown(val rawType: String, val payloadJson: String) : IncomingEvent

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        /**
         * Safely parse incoming WebSocket JSON into a strongly typed IncomingEvent.
         * Unknown or malformed events will NOT crash the caller.
         */
        fun fromJson(rawJson: String): IncomingEvent {
            return try {
                val element = json.parseToJsonElement(rawJson).jsonObject
                val type = element["type"]?.jsonPrimitive?.content ?: return Unknown("none", rawJson)

                when (type) {
                    "my_identity" -> {
                        val identity = json.decodeFromJsonElement<UserIdentityDto>(element)
                        MyIdentity(identity)
                    }
                    "new_message" -> {
                        val msgElement = element["message"] ?: return Unknown(type, rawJson)
                        val message = json.decodeFromJsonElement<MessageDto>(msgElement)
                        NewMessage(message)
                    }
                    "online_count" -> {
                        val count = element["count"]?.jsonPrimitive?.intOrNull ?: 0
                        val users = element["users"]?.let {
                            try { json.decodeFromJsonElement<List<OnlineUserDto>>(it) } catch (_: Exception) { null }
                        }
                        OnlineCount(count, users)
                    }
                    "user_disconnected" -> {
                        val publicId = element["public_id"]?.jsonPrimitive?.content ?: ""
                        UserDisconnected(publicId)
                    }
                    "typing" -> {
                        val animalName = element["animal_name"]?.jsonPrimitive?.content ?: "Anonymous"
                        val publicId = element["public_id"]?.jsonPrimitive?.content
                        val sessionId = element["session_id"]?.jsonPrimitive?.content
                        val roomId = element["room_id"]?.jsonPrimitive?.content
                        Typing(animalName, publicId, sessionId, roomId)
                    }
                    "reaction_update" -> {
                        val postNum = element["post_num"]?.jsonPrimitive?.longOrNull ?: 0L
                        val reactionsElement = element["reactions"]?.jsonObject
                        val reactions = reactionsElement?.mapValues { it.value.jsonPrimitive.intOrNull ?: 0 } ?: emptyMap()
                        ReactionUpdate(postNum, reactions)
                    }
                    "private_chat_invite" -> {
                        val inviteId = element["invite_id"]?.jsonPrimitive?.content ?: ""
                        val fromId = element["from_id"]?.jsonPrimitive?.content ?: ""
                        val fromName = element["from_name"]?.jsonPrimitive?.content ?: "Anonymous"
                        val fromAvatar = element["from_avatar"]?.jsonPrimitive?.content ?: ""
                        val roomId = element["room_id"]?.jsonPrimitive?.content
                        val roomName = element["room_name"]?.jsonPrimitive?.content
                        val isGroup = element["is_group"]?.jsonPrimitive?.booleanOrNull ?: false
                        val isRandomMatch = element["is_random_match"]?.jsonPrimitive?.booleanOrNull ?: false
                        PrivateChatInvite(inviteId, fromId, fromName, fromAvatar, roomId, roomName, isGroup, isRandomMatch)
                    }
                    "private_chat_declined" -> {
                        val declinedBy = element["declined_by"]?.jsonPrimitive?.content ?: "Peer"
                        val roomId = element["room_id"]?.jsonPrimitive?.content
                        val isGroup = element["is_group"]?.jsonPrimitive?.booleanOrNull ?: false
                        PrivateChatDeclined(declinedBy, roomId, isGroup)
                    }
                    "private_chat_sent" -> {
                        val targetName = element["target_name"]?.jsonPrimitive?.content ?: "Peer"
                        val isGroup = element["is_group"]?.jsonPrimitive?.booleanOrNull ?: false
                        val roomName = element["room_name"]?.jsonPrimitive?.content
                        PrivateChatSent(targetName, isGroup, roomName)
                    }
                    "room_invitation_accepted", "private_chat_accepted" -> {
                        val acceptedBy = element["accepted_by"]?.jsonPrimitive?.content ?: element["from_name"]?.jsonPrimitive?.content ?: "Peer"
                        val roomId = element["room_id"]?.jsonPrimitive?.content
                        val roomName = element["room_name"]?.jsonPrimitive?.content
                        RoomInvitationAccepted(acceptedBy, roomId, roomName)
                    }
                    "room_joined" -> {
                        val roomElement = element["room"] ?: return Unknown(type, rawJson)
                        val room = json.decodeFromJsonElement<RoomDto>(roomElement)
                        val sysMsg = element["system_message"]?.let {
                            try { json.decodeFromJsonElement<MessageDto>(it) } catch (_: Exception) { null }
                        }
                        RoomJoined(room, sysMsg)
                    }
                    "room_updated" -> {
                        val roomElement = element["room"] ?: return Unknown(type, rawJson)
                        val room = json.decodeFromJsonElement<RoomDto>(roomElement)
                        val sysMsg = element["system_message"]?.let {
                            try { json.decodeFromJsonElement<MessageDto>(it) } catch (_: Exception) { null }
                        }
                        RoomUpdated(room, sysMsg)
                    }
                    "room_left" -> {
                        val roomId = element["room_id"]?.jsonPrimitive?.content ?: ""
                        RoomLeft(roomId)
                    }
                    "room_deleted" -> {
                        val roomId = element["room_id"]?.jsonPrimitive?.content ?: ""
                        val roomName = element["room_name"]?.jsonPrimitive?.content
                        val reason = element["reason"]?.jsonPrimitive?.content
                        RoomDeleted(roomId, roomName, reason)
                    }
                    "delete_message", "hide_message" -> {
                        val postNum = element["post_num"]?.jsonPrimitive?.longOrNull ?: 0L
                        DeleteMessage(postNum)
                    }
                    "posts_deleted" -> {
                        val nums = element["post_nums"]?.jsonArray?.mapNotNull { it.jsonPrimitive.longOrNull } ?: emptyList()
                        PostsDeleted(nums)
                    }
                    "clear_chat" -> ClearChat
                    "announcement" -> {
                        val text = element["text"]?.jsonPrimitive?.content ?: ""
                        Announcement(text)
                    }
                    "system_broadcast" -> {
                        val bcId = element["broadcast_id"]?.jsonPrimitive?.content
                            ?: element["id"]?.jsonPrimitive?.content
                            ?: "bc_${kotlin.random.Random.nextLong(100000, 999999)}"
                        val msg = element["message"]?.jsonPrimitive?.content ?: ""
                        val sevStr = element["severity"]?.jsonPrimitive?.content
                        val severity = org.example.annochat.data.model.BroadcastSeverity.fromString(sevStr)
                        val roomId = element["room_id"]?.jsonPrimitive?.content
                        val audience = element["target_audience"]?.jsonPrimitive?.content ?: "all_new"
                        val duration = element["duration"]?.jsonPrimitive?.intOrNull ?: 0
                        val createdAt = element["created_at"]?.jsonPrimitive?.content
                        val broadcast = org.example.annochat.data.model.SystemBroadcast(
                            broadcastId = bcId,
                            message = msg,
                            severity = severity,
                            roomId = roomId,
                            targetAudience = audience,
                            duration = duration,
                            createdAt = createdAt
                        )
                        SystemBroadcast(broadcast)
                    }
                    "error" -> {
                        val message = element["message"]?.jsonPrimitive?.content ?: "Unknown server error"
                        ServerError(message)
                    }
                    "pong" -> {
                        val time = element["time"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        Pong(time)
                    }
                    else -> Unknown(type, rawJson)
                }
            } catch (e: Exception) {
                Unknown("parse_error: ${e.message}", rawJson)
            }
        }
    }
}

/**
 * Outgoing action models sent from the mobile client to the FastAPI backend over WebSocket.
 */
@Serializable
sealed class OutgoingEvent {
    abstract val action: String

    @Serializable
    data class PostMessage(
        @SerialName("action") override val action: String = "post_message",
        @SerialName("content") val content: String,
        @SerialName("room_id") val roomId: String = "main",
        @SerialName("reply_to") val replyTo: Long? = null,
        @SerialName("image_path") val imagePath: String? = null,
        @SerialName("image_thumb") val imageThumb: String? = null,
        @SerialName("image_name") val imageName: String? = null,
        @SerialName("image_size") val imageSize: Long? = null,
        @SerialName("image_dims") val imageDims: String? = null,
        @SerialName("client_msg_id") val clientMsgId: String? = null
    ) : OutgoingEvent()

    @Serializable
    data class Typing(
        @SerialName("action") override val action: String = "typing",
        @SerialName("room_id") val roomId: String? = null,
        @SerialName("public_id") val publicId: String? = null
    ) : OutgoingEvent()

    @Serializable
    data class React(
        @SerialName("action") override val action: String = "react",
        @SerialName("post_num") val postNum: Long,
        @SerialName("emoji") val emoji: String
    ) : OutgoingEvent()

    @Serializable
    data class RequestPrivateChat(
        @SerialName("action") override val action: String = "request_private_chat",
        @SerialName("target_public_id") val targetPublicId: String,
        @SerialName("room_id") val roomId: String? = null,
        @SerialName("room_name") val roomName: String? = null,
        @SerialName("from_name") val fromName: String? = null,
        @SerialName("from_avatar") val fromAvatar: String? = null
    ) : OutgoingEvent()

    @Serializable
    data class RespondPrivateChat(
        @SerialName("action") override val action: String = "respond_private_chat",
        @SerialName("invite_id") val inviteId: String,
        @SerialName("accepted") val accepted: Boolean
    ) : OutgoingEvent()

    @Serializable
    data class AddUserToRoom(
        @SerialName("action") override val action: String = "add_user_to_room",
        @SerialName("room_id") val roomId: String,
        @SerialName("target_public_id") val targetPublicId: String
    ) : OutgoingEvent()

    @Serializable
    data class LeaveRoom(
        @SerialName("action") override val action: String = "leave_room",
        @SerialName("room_id") val roomId: String
    ) : OutgoingEvent()

    @Serializable
    data class JoinRoom(
        @SerialName("action") override val action: String = "join_room",
        @SerialName("room_id") val roomId: String
    ) : OutgoingEvent()

    @Serializable
    data class Ping(
        @SerialName("action") override val action: String = "ping",
        @SerialName("time") val time: Double = 0.0
    ) : OutgoingEvent()
}
