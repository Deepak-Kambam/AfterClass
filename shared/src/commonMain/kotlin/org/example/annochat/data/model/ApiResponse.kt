package org.example.annochat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessagesResponse(
    @SerialName("status")
    val status: String = "",

    @SerialName("room_id")
    val roomId: String = "",

    @SerialName("messages")
    val messages: List<MessageDto> = emptyList(),

    @SerialName("is_host")
    val isHost: Boolean = false
)

@Serializable
data class UploadResponse(
    @SerialName("status")
    val status: String,

    @SerialName("image_path")
    val imagePath: String,

    @SerialName("image_thumb")
    val imageThumb: String,

    @SerialName("image_name")
    val imageName: String,

    @SerialName("image_size")
    val imageSize: Long,

    @SerialName("image_dims")
    val imageDims: String
)

@Serializable
data class FeedbackRequest(
    @SerialName("category")
    val category: String,

    @SerialName("message")
    val message: String,

    @SerialName("rating")
    val rating: Int = 5
)

@Serializable
data class FeedbackResponse(
    @SerialName("status")
    val status: String,

    @SerialName("feedback_id")
    val feedbackId: Long? = null,

    @SerialName("message")
    val message: String? = null
)

@Serializable
data class LeaveAllRoomsResponse(
    @SerialName("status")
    val status: String,

    @SerialName("deleted_rooms")
    val deletedRooms: List<String> = emptyList()
)

@Serializable
data class StickerItemDto(
    @SerialName("name")
    val name: String,

    @SerialName("path")
    val path: String,

    @SerialName("category")
    val category: String? = null
)

@Serializable
data class GifItemDto(
    @SerialName("name")
    val name: String,

    @SerialName("path")
    val path: String
)

@Serializable
data class StickersAndGifsResponse(
    @SerialName("status")
    val status: String,

    @SerialName("stickers")
    val stickers: List<StickerItemDto> = emptyList(),

    @SerialName("gifs")
    val gifs: List<GifItemDto> = emptyList()
)

@Serializable
data class GenericStatusResponse(
    @SerialName("status")
    val status: String,

    @SerialName("message")
    val message: String? = null
)
