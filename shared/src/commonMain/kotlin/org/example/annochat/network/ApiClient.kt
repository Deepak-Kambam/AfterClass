package org.example.annochat.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.example.annochat.data.model.ActiveBroadcastResponse
import org.example.annochat.data.model.AdminBroadcastRequest
import org.example.annochat.data.model.AdminBroadcastResponse
import org.example.annochat.data.model.AppVersionResponse
import org.example.annochat.data.model.CreateRoomRequest
import org.example.annochat.data.model.CreateRoomResponse
import org.example.annochat.data.model.FeedbackRequest
import org.example.annochat.data.model.FeedbackResponse
import org.example.annochat.data.model.LeaveAllRoomsResponse
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.MessagesResponse
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.OnlineUsersResponse
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.PublicRoomsResponse
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.RoomMemberDto
import org.example.annochat.data.model.RoomsResponse
import org.example.annochat.data.model.StickersAndGifsResponse
import org.example.annochat.data.model.UploadResponse

/**
 * REST API client communicating with the existing FastAPI AnonChat backend.
 */
open class ApiClient(
    var config: NetworkConfig,
    private val client: HttpClient = createDefaultHttpClient()
) {
    fun updateConfig(newConfig: NetworkConfig) {
        this.config = newConfig
    }
    /**
     * GET /api/public_rooms
     */
    suspend fun getPublicRooms(): Result<List<PublicRoomDto>> = safeApiCall {
        val response: PublicRoomsResponse = client.get("${config.httpBaseUrl}/api/public_rooms").body()
        response.rooms
    }

    /**
     * GET /api/public_groups
     */
    suspend fun getDiscoverablePublicGroups(): Result<List<PublicRoomDto>> = safeApiCall {
        val response: PublicRoomsResponse = client.get("${config.httpBaseUrl}/api/public_groups").body()
        response.rooms
    }

    /**
     * GET /api/rooms?session_id=<sessionId>
     */
    suspend fun getUserRooms(sessionId: String): Result<List<RoomDto>> = safeApiCall {
        val response: RoomsResponse = client.get("${config.httpBaseUrl}/api/rooms") {
            parameter("session_id", sessionId)
        }.body()
        response.rooms
    }

    /**
     * POST /api/rooms/create
     */
    suspend fun createRoom(request: CreateRoomRequest): Result<RoomDto> = safeApiCall {
        val httpResponse: HttpResponse = client.post("${config.httpBaseUrl}/api/rooms/create") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = try { httpResponse.bodyAsText() } catch (_: Exception) { "" }
            val detailMsg = try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<JsonObject>(errorBody)["detail"]?.jsonPrimitive?.content
            } catch (_: Exception) { null }
            val errorMsg = detailMsg ?: "Failed to create group (HTTP ${httpResponse.status.value})"
            throw Exception(errorMsg)
        }
        val responseBodyText = httpResponse.bodyAsText()
        val lenientJson = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
        val response: CreateRoomResponse = try {
            lenientJson.decodeFromString<CreateRoomResponse>(responseBodyText)
        } catch (_: Exception) {
            try {
                val directRoom = lenientJson.decodeFromString<RoomDto>(responseBodyText)
                CreateRoomResponse(status = "ok", room = directRoom)
            } catch (inner: Exception) {
                throw inner
            }
        }
        response.room ?: throw Exception(response.detail ?: "Room creation returned empty room")
    }

    /**
     * POST /api/rooms/update_name
     */
    suspend fun updateRoomName(roomId: String, newName: String, sessionId: String): Result<Boolean> = safeApiCall {
        val httpResponse: HttpResponse = client.post("${config.httpBaseUrl}/api/rooms/update_name") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("session_id", sessionId)
                put("room_id", roomId)
                put("name", newName)
            })
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = try { httpResponse.bodyAsText() } catch (_: Exception) { "" }
            val detailMsg = try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<JsonObject>(errorBody)["detail"]?.jsonPrimitive?.content
            } catch (_: Exception) { null }
            throw Exception(detailMsg ?: "Failed to rename group (HTTP ${httpResponse.status.value})")
        }
        true
    }

    /**
     * POST /api/rooms/delete
     */
    suspend fun deleteRoom(roomId: String, sessionId: String): Result<Boolean> = safeApiCall {
        val httpResponse: HttpResponse = client.post("${config.httpBaseUrl}/api/rooms/delete") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("session_id", sessionId)
                put("room_id", roomId)
            })
        }
        if (!httpResponse.status.isSuccess()) {
            val errorBody = try { httpResponse.bodyAsText() } catch (_: Exception) { "" }
            val detailMsg = try {
                val json = Json { ignoreUnknownKeys = true }
                json.decodeFromString<JsonObject>(errorBody)["detail"]?.jsonPrimitive?.content
            } catch (_: Exception) { null }
            throw Exception(detailMsg ?: "Failed to delete group (HTTP ${httpResponse.status.value})")
        }
        true
    }

    /**
     * GET /api/room_members?room_id=<roomId>
     */
    suspend fun getRoomMembers(roomId: String): Result<List<RoomMemberDto>> = safeApiCall {
        val httpResponse: HttpResponse = client.get("${config.httpBaseUrl}/api/room_members") {
            parameter("room_id", roomId)
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Failed to fetch room members (HTTP ${httpResponse.status.value})")
        }
        val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
        val responseText = httpResponse.bodyAsText()
        val obj = lenientJson.decodeFromString<JsonObject>(responseText)
        val membersElement = obj["members"] ?: return@safeApiCall emptyList()
        lenientJson.decodeFromJsonElement<List<RoomMemberDto>>(membersElement)
    }

    /**
     * GET /api/messages?room_id=<roomId>&limit=<limit>&session_id=<sessionId>
     */
    suspend fun getMessages(
        roomId: String = "main",
        limit: Int = 50,
        sessionId: String,
        beforePostNum: Long? = null
    ): Result<List<MessageDto>> = safeApiCall {
        val httpResponse = client.get("${config.httpBaseUrl}/api/messages") {
            parameter("room_id", roomId)
            parameter("limit", limit)
            parameter("session_id", sessionId)
            if (beforePostNum != null) {
                parameter("before_post_num", beforePostNum)
            }
        }
        if (!httpResponse.status.isSuccess()) {
            throw Exception("Failed to fetch messages (HTTP ${httpResponse.status.value})")
        } else {
            val response: MessagesResponse = httpResponse.body()
            response.messages
        }
    }

    /**
     * GET /api/online_users
     */
    suspend fun getOnlineUsers(): Result<List<OnlineUserDto>> = safeApiCall {
        val response: OnlineUsersResponse = client.get("${config.httpBaseUrl}/api/online_users").body()
        response.users
    }

    /**
     * POST /api/upload (Multipart image upload)
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        filename: String = "image.jpg",
        mimeType: String = "image/jpeg"
    ): Result<UploadResponse> = safeApiCall {
        val response: HttpResponse = client.submitFormWithBinaryData(
            url = "${config.httpBaseUrl}/api/upload",
            formData = formData {
                append("file", imageBytes, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        )
        if (!response.status.isSuccess()) {
            throw Exception("Upload failed with HTTP ${response.status.value}")
        }
        response.body()
    }

    /**
     * POST /api/feedback
     */
    suspend fun submitFeedback(
        category: String,
        message: String,
        rating: Int = 5
    ): Result<FeedbackResponse> = safeApiCall {
        val response: FeedbackResponse = client.post("${config.httpBaseUrl}/api/feedback") {
            contentType(ContentType.Application.Json)
            setBody(FeedbackRequest(category = category, message = message, rating = rating))
        }.body()
        response
    }

    /**
     * POST /api/leave_all_rooms?session_id=<sessionId>
     */
    suspend fun leaveAllRooms(sessionId: String): Result<List<String>> = safeApiCall {
        val response: LeaveAllRoomsResponse = client.post("${config.httpBaseUrl}/api/leave_all_rooms") {
            parameter("session_id", sessionId)
        }.body()
        response.deletedRooms
    }

    /**
     * GET /api/stickers_and_gifs
     */
    suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse> = safeApiCall {
        client.get("${config.httpBaseUrl}/api/stickers_and_gifs").body()
    }

    /**
     * GET /api/broadcast/active
     */
    open suspend fun getActiveBroadcast(): Result<ActiveBroadcastResponse> = safeApiCall {
        val httpResponse = client.get("${config.httpBaseUrl}/api/broadcast/active")
        if (!httpResponse.status.isSuccess()) {
            ActiveBroadcastResponse(status = "none", broadcast = null)
        } else {
            httpResponse.body()
        }
    }

    /**
     * POST /api/host/broadcast
     */
    open suspend fun sendBroadcast(
        request: AdminBroadcastRequest,
        token: String? = null
    ): Result<AdminBroadcastResponse> = safeApiCall {
        val response: AdminBroadcastResponse = client.post("${config.httpBaseUrl}/api/host/broadcast") {
            contentType(ContentType.Application.Json)
            if (!token.isNullOrBlank()) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(request)
        }.body()
        response
    }

    /**
     * GET /api/app/version
     * Fetches the latest published Android APK release metadata from the server.
     */
    open suspend fun checkAppVersion(): Result<AppVersionResponse> = safeApiCall {
        val endpoints = listOf(
            "${config.httpBaseUrl}/api/app/version",
            "${config.httpBaseUrl}/api/version",
            "${config.httpBaseUrl}/version.json",
            "${config.httpBaseUrl}/updates/version.json"
        )
        var lastException: Exception? = null
        for (url in endpoints) {
            try {
                val httpResponse = client.get(url)
                if (httpResponse.status.isSuccess()) {
                    return@safeApiCall httpResponse.body<AppVersionResponse>()
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: Exception("Failed to check app version from server")
    }

    /**
     * Helper to construct absolute media URL for uploaded images or animal avatars.
     */
    fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String {
        if (relativeOrAbsoluteUrl.startsWith("http://") || relativeOrAbsoluteUrl.startsWith("https://")) {
            return relativeOrAbsoluteUrl
        }
        val cleanPath = "/" + relativeOrAbsoluteUrl.trimStart('/')
        return "${config.httpBaseUrl}$cleanPath"
    }

    /**
     * Downloads raw bytes of a media asset (avatar image, uploaded photo, etc.).
     */
    suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> = safeApiCall {
        val fullUrl = resolveMediaUrl(urlOrPath)
        val response = client.get(fullUrl)
        if (!response.status.isSuccess()) {
            throw Exception("Failed to load media: HTTP ${response.status.value}")
        }
        response.body<ByteArray>()
    }

    private inline fun <T> safeApiCall(block: () -> T): Result<T> {
        return try {
            if (config.environment == NetworkEnvironment.REMOTE && !config.isHostConfigured) {
                return Result.failure(
                    IllegalStateException("Backend endpoint is not resolved yet (host='${config.host}')")
                )
            }
            Result.success(block())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        coerceInputValues = true
                    })
                }
            }
        }
    }
}
