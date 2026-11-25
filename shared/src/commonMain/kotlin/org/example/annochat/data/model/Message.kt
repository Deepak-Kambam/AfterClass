package org.example.annochat.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message data transfer model matching the SQLite schema and REST/WS message payloads.
 */
@Immutable
@Serializable
data class MessageDto(
    @SerialName("id")
    val id: Long? = null,

    @SerialName("post_num")
    val postNum: Long,

    @SerialName("client_msg_id")
    val clientMsgId: String? = null,

    @SerialName("public_id")
    val publicId: String? = null,

    @SerialName("anon_name")
    val anonName: String? = null,

    @SerialName("anon_avatar")
    val anonAvatar: String? = null,

    @SerialName("content")
    val content: String? = null,

    @SerialName("image_path")
    val imagePath: String? = null,

    @SerialName("image_thumb")
    val imageThumb: String? = null,

    @SerialName("image_name")
    val imageName: String? = null,

    @SerialName("image_size")
    val imageSize: Long? = null,

    @SerialName("image_dims")
    val imageDims: String? = null,

    @SerialName("reply_to")
    val replyTo: Long? = null,

    @SerialName("reply_name")
    val replyName: String? = null,

    @SerialName("reply_snippet")
    val replySnippet: String? = null,

    @SerialName("room_id")
    val roomId: String = "main",

    @SerialName("reactions")
    val reactions: Map<String, Int> = emptyMap(),

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("is_deleted")
    val isDeleted: Int = 0,

    @SerialName("is_pinned")
    val isPinned: Int = 0
)

/**
 * Generates a unique collision-resistant client message ID for idempotency & optimistic reconciliation.
 */
fun generateClientMsgId(): String {
    val chars = "0123456789abcdef"
    val part1 = (1..12).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
    val part2 = (1..12).map { chars[kotlin.random.Random.nextInt(chars.length)] }.joinToString("")
    return "cmsg_${part1}_$part2"
}
