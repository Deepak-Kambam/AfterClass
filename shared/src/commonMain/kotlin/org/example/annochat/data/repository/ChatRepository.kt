package org.example.annochat.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.annochat.data.model.WidgetDataSnapshot
import org.example.annochat.widget.syncPlatformWidgetData
import org.example.annochat.data.model.CreateRoomRequest
import org.example.annochat.data.model.FeedbackResponse
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.OutgoingEvent
import org.example.annochat.data.model.PredefinedChannels
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.RoomMemberDto
import org.example.annochat.data.model.StickersAndGifsResponse
import org.example.annochat.data.model.UploadResponse
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.session.InMemorySessionStorage
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.network.ApiClient
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.EndpointDiscoveryService
import org.example.annochat.network.NetworkConfig
import org.example.annochat.network.WebSocketClient

interface ChatRepository {
    val connectionState: StateFlow<ConnectionState>
    val myIdentity: StateFlow<UserIdentityDto?>
    val onlineCount: StateFlow<Int>
    val publicRooms: StateFlow<List<PublicRoomDto>>
    val discoverableGroups: StateFlow<List<PublicRoomDto>> get() = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    val userRooms: StateFlow<List<RoomDto>>
    val onlineUsers: StateFlow<List<OnlineUserDto>>
    val hasLoadedOnlineUsers: StateFlow<Boolean>
    val messages: StateFlow<List<MessageDto>>
    val incomingEvents: SharedFlow<IncomingEvent>
    val sessionStorage: SessionStorage get() = InMemorySessionStorage()
    fun getMyPublicId(): String? = null

    fun getSessionId(): String
    fun connect()
    fun disconnect()
    fun setCurrentRoomId(roomId: String)
    fun getCurrentRoomId(): String

    suspend fun loadPublicRooms(): Result<List<PublicRoomDto>>
    suspend fun loadDiscoverableGroups(): Result<List<PublicRoomDto>> = Result.success(emptyList())
    suspend fun loadUserRooms(): Result<List<RoomDto>>
    suspend fun createRoom(name: String, desc: String? = null, isPublic: Boolean = true): Result<RoomDto>
    suspend fun updateRoomName(roomId: String, newName: String): Result<Boolean> = Result.success(true)
    suspend fun deleteRoom(roomId: String): Result<Boolean> = Result.success(true)
    suspend fun getRoomMembers(roomId: String): Result<List<RoomMemberDto>> = Result.success(emptyList())
    suspend fun loadMessages(roomId: String = "main", limit: Int = 50, beforePostNum: Long? = null): Result<List<MessageDto>>
    suspend fun loadOnlineUsers(): Result<List<OnlineUserDto>>

    suspend fun sendMessage(
        content: String,
        roomId: String = "main",
        replyTo: Long? = null,
        imagePath: String? = null,
        imageThumb: String? = null,
        imageName: String? = null,
        imageSize: Long? = null,
        imageDims: String? = null,
        clientMsgId: String? = null
    ): Boolean

    suspend fun sendTyping(): Boolean
    suspend fun sendTyping(roomId: String?): Boolean = sendTyping()
    suspend fun toggleReaction(postNum: Long, emoji: String): Boolean
    suspend fun requestPrivateChat(targetPublicId: String): Boolean
    suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean
    suspend fun leaveRoom(roomId: String): Boolean
    suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean

    suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse>
    suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<UploadResponse>
    suspend fun submitFeedback(category: String, message: String, rating: Int = 5): Result<FeedbackResponse>
    fun syncWidgets()
    suspend fun leaveAllRooms(): Result<List<String>>
    suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray>
    fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String
}

class ChatRepositoryImpl(
    val apiClient: ApiClient,
    val webSocketClient: WebSocketClient,
    override val sessionStorage: SessionStorage = InMemorySessionStorage(),
    val discoveryService: EndpointDiscoveryService? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : ChatRepository {

    override fun getMyPublicId(): String? = _myIdentity.value?.publicId ?: sessionStorage.getSavedPublicId()

    override val connectionState: StateFlow<ConnectionState> = webSocketClient.connectionState

    private val _myIdentity = MutableStateFlow<UserIdentityDto?>(
        run {
            val pid = sessionStorage.getSavedPublicId()
            val name = sessionStorage.getSavedAnonName()
            if (!pid.isNullOrBlank() && !name.isNullOrBlank()) {
                UserIdentityDto(publicId = pid, anonName = name, avatarUrl = "")
            } else null
        }
    )
    override val myIdentity: StateFlow<UserIdentityDto?> = _myIdentity.asStateFlow()

    private val _onlineCount = MutableStateFlow(0)
    override val onlineCount: StateFlow<Int> = _onlineCount.asStateFlow()

    private val _publicRooms = MutableStateFlow<List<PublicRoomDto>>(PredefinedChannels.DEFAULT_CHANNELS)
    override val publicRooms: StateFlow<List<PublicRoomDto>> = _publicRooms.asStateFlow()

    private val _discoverableGroups = MutableStateFlow<List<PublicRoomDto>>(emptyList())
    override val discoverableGroups: StateFlow<List<PublicRoomDto>> = _discoverableGroups.asStateFlow()

    private val _userRooms = MutableStateFlow<List<RoomDto>>(
        sessionStorage.getAllSavedRooms()
            .filter { !PredefinedChannels.isPredefined(it.roomId) && !sessionStorage.isRoomDeleted(it.roomId) }
    )
    override val userRooms: StateFlow<List<RoomDto>> = _userRooms.asStateFlow()

    private val _onlineUsers = MutableStateFlow<List<OnlineUserDto>>(emptyList())
    override val onlineUsers: StateFlow<List<OnlineUserDto>> = _onlineUsers.asStateFlow()

    private val _hasLoadedOnlineUsers = MutableStateFlow(false)
    override val hasLoadedOnlineUsers: StateFlow<Boolean> = _hasLoadedOnlineUsers.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageDto>>(emptyList())
    override val messages: StateFlow<List<MessageDto>> = _messages.asStateFlow()

    private val _incomingEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    override val incomingEvents: SharedFlow<IncomingEvent> = _incomingEvents.asSharedFlow()

    @kotlin.concurrent.Volatile
    private var currentRoomId: String = "main"
    private val roomMessagesCache = mutableMapOf<String, List<MessageDto>>()
    private val onlinePeersMap = mutableMapOf<String, OnlineUserDto>()
    private val peerIdentityCache = mutableMapOf<String, OnlineUserDto>()

    override fun setCurrentRoomId(roomId: String) {
        val cleanRoom = roomId.trim()
        val isDifferent = !isRoomMatching(currentRoomId, cleanRoom)
        currentRoomId = cleanRoom
        if (isDifferent) {
            // Strictly reset or restore messages scoped ONLY to this room
            val cached = (roomMessagesCache[cleanRoom] ?: sessionStorage.getRoomMessages(cleanRoom))
                .filter { isRoomMatching(it.roomId, cleanRoom) }
            _messages.value = cached
        }
        scope.launch {
            try {
                webSocketClient.send(OutgoingEvent.JoinRoom(roomId = cleanRoom))
            } catch (_: Exception) {}
        }
    }

    override fun getCurrentRoomId(): String = currentRoomId

    init {
        scope.launch {
            webSocketClient.incomingEvents.collect { event ->
                handleIncomingEvent(event)
                _incomingEvents.emit(event)
            }
        }
        scope.launch {
            combine(_onlineCount, _myIdentity, _userRooms, _publicRooms) { _, _, _, _ -> }
                .collect {
                    syncWidgets()
                }
        }
    }

    override fun getSessionId(): String {
        return sessionStorage.getOrCreateSessionId()
    }

    private suspend fun ensureEndpointDiscovered(forceRefresh: Boolean = false): Result<NetworkConfig> {
        val customUrl = sessionStorage.getCustomBackendUrl()
        if (!customUrl.isNullOrBlank()) {
            val customConfig = NetworkConfig.remote(customUrl)
            apiClient.updateConfig(customConfig)
            webSocketClient.updateConfig(customConfig)
            println("[ChatRepository] Using custom backend URL override -> ${customConfig.httpBaseUrl}")
            return Result.success(customConfig)
        }
        val discovery = discoveryService ?: return Result.success(apiClient.config)
        val result = discovery.resolveConfig(forceRefresh = forceRefresh)
        result.onSuccess { resolvedConfig ->
            apiClient.updateConfig(resolvedConfig)
            webSocketClient.updateConfig(resolvedConfig)
            println("[ChatRepository] Active backend origin configured -> ${resolvedConfig.httpBaseUrl}")
        }
        return result
    }

    private suspend fun <T> executeApiCall(call: suspend () -> Result<T>): Result<T> {
        if (discoveryService != null && discoveryService.getCachedConfig() == null) {
            val discoveryResult = ensureEndpointDiscovered(forceRefresh = false)
            if (discoveryResult.isFailure) {
                val error = discoveryResult.exceptionOrNull() ?: IllegalStateException("Unable to discover backend endpoint")
                println("[ChatRepository] Initial discovery failed: ${error.message}")
                return Result.failure(error)
            }
        }
        var result = call()
        if (result.isFailure && discoveryService != null) {
            println("[ChatRepository] API call failed: ${result.exceptionOrNull()?.message}. Retrying with fresh discovery...")
            val retryDiscovery = ensureEndpointDiscovered(forceRefresh = true)
            if (retryDiscovery.isSuccess) {
                result = call()
            }
        }
        return result
    }

    override fun connect() {
        val sessionId = getSessionId()
        if (discoveryService != null && discoveryService.getCachedConfig() == null) {
            scope.launch {
                ensureEndpointDiscovered(forceRefresh = false)
                webSocketClient.connect(sessionId)
                loadOnlineUsers()
            }
        } else {
            webSocketClient.connect(sessionId)
            scope.launch {
                loadOnlineUsers()
            }
        }
    }

    override fun disconnect() {
        syncWidgets()
        webSocketClient.disconnect()
    }

    override suspend fun loadPublicRooms(): Result<List<PublicRoomDto>> {
        val result = executeApiCall { apiClient.getPublicRooms() }
        result.onSuccess { rooms ->
            // Chats -> Public Channels displays ONLY predefined public chat channels.
            // Never merge user rooms or groups created in Online here.
            val predefined = rooms.filter { PredefinedChannels.isPredefined(it.id) }
            _publicRooms.value = if (predefined.isNotEmpty()) predefined else PredefinedChannels.DEFAULT_CHANNELS
        }.onFailure {
            if (_publicRooms.value.isEmpty()) {
                _publicRooms.value = PredefinedChannels.DEFAULT_CHANNELS
            }
        }
        return result
    }

    override suspend fun loadDiscoverableGroups(): Result<List<PublicRoomDto>> {
        val result = executeApiCall { apiClient.getDiscoverablePublicGroups() }
        result.onSuccess { groups ->
            val nonPredefined = groups.filter { !PredefinedChannels.isPredefined(it.id) && !sessionStorage.isRoomDeleted(it.id) }
            _discoverableGroups.value = nonPredefined
        }
        return result
    }

    private fun sanitizeOwnedRoom(room: RoomDto, myPid: String?, myIdentity: org.example.annochat.data.model.UserIdentityDto?): RoomDto {
        val isLocallyOwned = sessionStorage.isMyOwnedRoom(room.roomId)
        if (!isLocallyOwned && !room.isOwner) return room
        if (myPid.isNullOrBlank()) return room

        val currentName = myIdentity?.anonName ?: sessionStorage.getSavedAnonName() ?: "You"
        val currentAvatar = myIdentity?.avatarUrl

        // Filter out any stale/old identity that was previously marked as owner/admin or matching previous owner ID
        val filtered = room.members.filter { m ->
            m.publicId.equals(myPid, ignoreCase = true) ||
            (m.isOwner != 1 &&
             !m.role.equals("ADMIN", ignoreCase = true) &&
             !m.role.equals("OWNER", ignoreCase = true) &&
             !m.publicId.equals(room.effectiveOwnerPublicId, ignoreCase = true) &&
             !m.publicId.equals(room.ownerPublicId, ignoreCase = true) &&
             !m.publicId.equals(room.createdBy, ignoreCase = true))
        }.toMutableList()

        val selfIndex = filtered.indexOfFirst { it.publicId.equals(myPid, ignoreCase = true) }
        if (selfIndex >= 0) {
            filtered[selfIndex] = filtered[selfIndex].copy(
                anonName = currentName,
                anonAvatar = currentAvatar ?: filtered[selfIndex].anonAvatar,
                isOwner = 1,
                role = "ADMIN"
            )
        } else {
            filtered.add(
                0,
                RoomMemberDto(
                    roomId = room.roomId,
                    publicId = myPid,
                    anonName = currentName,
                    anonAvatar = currentAvatar,
                    isOwner = 1,
                    role = "ADMIN"
                )
            )
        }

        val updated = room.copy(
            ownerPublicId = myPid,
            createdBy = myPid,
            isOwner = true,
            isMember = true,
            members = filtered,
            rawIsAdmin = true,
            rawMemberCount = maxOf(1, filtered.size)
        )
        sessionStorage.saveRoomSnapshot(updated)
        return updated
    }

    override suspend fun loadUserRooms(): Result<List<RoomDto>> {
        val result = executeApiCall { apiClient.getUserRooms(getSessionId()) }
        val savedRooms: List<RoomDto> = sessionStorage.getAllSavedRooms().filter { !PredefinedChannels.isPredefined(it.roomId) && !sessionStorage.isRoomDeleted(it.roomId) }
        val myPid = _myIdentity.value?.publicId ?: sessionStorage.getSavedPublicId()
        val currentIdentity = _myIdentity.value
        result.onSuccess { serverRooms ->
            // Filter serverRooms: only include user-created rooms (never predefined chat channels).
            // Exclude any rooms that the user explicitly deleted.
            val filteredServerRooms = serverRooms.filter { r ->
                !PredefinedChannels.isPredefined(r.roomId) &&
                !sessionStorage.isRoomDeleted(r.roomId) && (
                    if (!myPid.isNullOrBlank()) {
                        r.createdBy == myPid ||
                        r.members.any { m -> m.publicId == myPid } ||
                        sessionStorage.isMyRoom(r.roomId)
                    } else {
                        sessionStorage.isMyRoom(r.roomId)
                    }
                )
            }

            val mappedServerRooms = filteredServerRooms.map { r ->
                val isLocallyOwned = sessionStorage.isMyOwnedRoom(r.roomId)
                val isOwner = r.isOwner || (myPid != null && r.createdBy == myPid) || isLocallyOwned
                val isMember = r.isMember || isOwner || (myPid != null && r.members.any { m -> m.publicId == myPid }) || sessionStorage.isMyRoom(r.roomId)
                val customName = sessionStorage.getCustomRoomName(r.roomId)
                val base = r.copy(
                    name = if (!customName.isNullOrBlank()) customName else r.name,
                    isOwner = isOwner,
                    isMember = isMember
                )
                if (isLocallyOwned || isOwner) {
                    sanitizeOwnedRoom(base, myPid, currentIdentity)
                } else {
                    base
                }
            }
            val combined = mappedServerRooms.toMutableList()
            for (saved in savedRooms) {
                if (combined.none { it.roomId == saved.roomId }) {
                    val isLocallyOwned = sessionStorage.isMyOwnedRoom(saved.roomId)
                    val isOwner = saved.isOwner || isLocallyOwned
                    val isMember = saved.isMember || isOwner || sessionStorage.isMyRoom(saved.roomId)
                    val customName = sessionStorage.getCustomRoomName(saved.roomId)
                    val base = saved.copy(
                        name = if (!customName.isNullOrBlank()) customName else saved.name,
                        isOwner = isOwner,
                        isMember = isMember
                    )
                    val finalRoom = if (isLocallyOwned || isOwner) {
                        sanitizeOwnedRoom(base, myPid, currentIdentity)
                    } else {
                        base
                    }
                    combined.add(finalRoom)
                }
            }
            _userRooms.value = combined
            synchronizedPeersFromRooms(combined)
        }.onFailure {
            if (savedRooms.isNotEmpty()) {
                val mappedSavedRooms = savedRooms.map { r ->
                    val isLocallyOwned = sessionStorage.isMyOwnedRoom(r.roomId)
                    val isOwner = r.isOwner || isLocallyOwned
                    val isMember = r.isMember || isOwner || sessionStorage.isMyRoom(r.roomId)
                    val customName = sessionStorage.getCustomRoomName(r.roomId)
                    val base = r.copy(
                        name = if (!customName.isNullOrBlank()) customName else r.name,
                        isOwner = isOwner,
                        isMember = isMember
                    )
                    if (isLocallyOwned || isOwner) {
                        sanitizeOwnedRoom(base, myPid, currentIdentity)
                    } else {
                        base
                    }
                }
                _userRooms.value = mappedSavedRooms
            }
        }
        return result
    }

    override suspend fun createRoom(name: String, desc: String?, isPublic: Boolean): Result<RoomDto> {
        val sessionId = getSessionId()
        val request = CreateRoomRequest(
            sessionId = sessionId,
            name = name.trim(),
            desc = desc?.trim()?.ifBlank { null },
            isPublic = isPublic
        )
        val result = executeApiCall { apiClient.createRoom(request) }
        var finalRoomToReturn: RoomDto? = null
        result.onSuccess { createdRoom ->
            val cleanName = if (name.isNotBlank()) name.trim() else createdRoom.name
            val myPid = getMyPublicId()
            val myIdentity = _myIdentity.value
            val currentAdminMember = if (!myPid.isNullOrBlank()) {
                RoomMemberDto(
                    roomId = createdRoom.roomId,
                    publicId = myPid,
                    anonName = myIdentity?.anonName ?: sessionStorage.getSavedAnonName() ?: "You",
                    anonAvatar = myIdentity?.avatarUrl,
                    isOwner = 1,
                    role = "ADMIN"
                )
            } else null
            val initialMembers = if (currentAdminMember != null) listOf(currentAdminMember) else emptyList()
            val finalRoom = createdRoom.copy(
                name = cleanName,
                desc = desc?.trim()?.ifBlank { null } ?: createdRoom.desc,
                ownerPublicId = myPid ?: createdRoom.ownerPublicId,
                createdBy = myPid ?: createdRoom.createdBy,
                isOwner = true,
                isMember = true,
                isPublic = isPublic,
                members = initialMembers,
                rawMemberCount = maxOf(1, initialMembers.size)
            )
            finalRoomToReturn = finalRoom
            sessionStorage.addMyRoom(finalRoom.roomId, isOwner = true, isPrivate = !isPublic)
            sessionStorage.saveRoomSnapshot(finalRoom)
            if (name.isNotBlank()) {
                sessionStorage.saveCustomRoomName(finalRoom.roomId, name.trim())
            }
            sessionStorage.markRoomLocallyPrivate(finalRoom.roomId, isPrivate = !isPublic)
            _userRooms.update { current ->
                val filtered = current.filter { it.roomId != finalRoom.roomId }
                listOf(finalRoom) + filtered
            }
            if (isPublic) {
                val pubDto = PublicRoomDto(
                    id = finalRoom.roomId,
                    name = finalRoom.name,
                    desc = finalRoom.desc,
                    memberCount = 1
                )
                _discoverableGroups.update { current ->
                    val filtered = current.filter { it.id != finalRoom.roomId }
                    listOf(pubDto) + filtered
                }
            }
            syncWidgets()
        }
        val roomToReturn = finalRoomToReturn
        return if (result.isSuccess && roomToReturn != null) {
            Result.success(roomToReturn)
        } else {
            result
        }
    }

    override suspend fun updateRoomName(roomId: String, newName: String): Result<Boolean> {
        val sessionId = getSessionId()
        val clean = newName.trim()
        val myPid = getMyPublicId()
        val room = _userRooms.value.firstOrNull { it.roomId == roomId }
        val isLocallyOwner = sessionStorage.isMyOwnedRoom(roomId)

        // Permission check: strictly require Admin authority
        if (room != null && !room.isUserAdmin(myPid, isLocallyOwner)) {
            return Result.failure(IllegalStateException("Only the group admin can modify group settings."))
        }

        // 1. Attempt server HTTP endpoint update
        val apiResult = executeApiCall { apiClient.updateRoomName(roomId, clean, sessionId) }
        if (apiResult.isFailure) {
            val err = apiResult.exceptionOrNull()
            val msg = err?.message ?: ""
            if (msg.contains("403") || msg.contains("not authorized", ignoreCase = true) || msg.contains("Forbidden", ignoreCase = true)) {
                return Result.failure(err ?: IllegalStateException("Only the group admin can modify group settings."))
            }
        }

        // 2. Persist custom room name locally & in room snapshot
        sessionStorage.saveCustomRoomName(roomId, clean)
        val saved = sessionStorage.getSavedRoom(roomId)
        if (saved != null) {
            sessionStorage.saveRoomSnapshot(saved.copy(name = clean))
        }

        // 3. Update memory state for immediate local UI reflection
        _userRooms.update { current ->
            current.map { r ->
                if (r.roomId == roomId) r.copy(name = clean) else r
            }
        }

        // 4. Post system notice into target room history for members in the room
        webSocketClient.send(
            OutgoingEvent.PostMessage(
                content = "🏷️ Changed group name to \"$clean\"",
                roomId = roomId
            )
        )

        // 5. Broadcast global synchronization signal to 'main' stream (delivered to 100% of online devices)
        webSocketClient.send(
            OutgoingEvent.PostMessage(
                content = "__RENAME__:$roomId:$clean",
                roomId = "main"
            )
        )

        syncWidgets()
        return Result.success(true)
    }

    override suspend fun deleteRoom(roomId: String): Result<Boolean> {
        val sessionId = getSessionId()
        val myPid = getMyPublicId()
        val room = _userRooms.value.firstOrNull { it.roomId == roomId }
        val isLocallyOwner = sessionStorage.isMyOwnedRoom(roomId)
        val isAdmin = (room != null && room.isUserAdmin(myPid, isLocallyOwner)) || isLocallyOwner

        if (!isAdmin) {
            // Non-admin user: cannot disband group on server.
            // If they are leaving, clear from their local list only:
            sessionStorage.removeMyRoom(roomId)
            _userRooms.update { current ->
                current.filterNot { it.roomId == roomId }
            }
            syncWidgets()
            return Result.failure(IllegalStateException("Only the group admin can delete or disband this group."))
        }

        val result = executeApiCall { apiClient.deleteRoom(roomId, sessionId) }
        if (result.isSuccess) {
            sessionStorage.markRoomDeleted(roomId)
            _userRooms.update { current ->
                current.filterNot { it.roomId == roomId }
            }
            _discoverableGroups.update { current ->
                current.filterNot { it.id == roomId }
            }
            syncWidgets()
        }
        return result
    }

    override suspend fun getRoomMembers(roomId: String): Result<List<RoomMemberDto>> {
        val cleanRoom = roomId.trim()
        val result = executeApiCall { apiClient.getRoomMembers(cleanRoom) }
        return result.map { members ->
            val scopedMembers = members.filter { org.example.annochat.data.model.isRoomMatching(it.roomId, cleanRoom) || it.roomId.isNullOrBlank() }
                .map { if (it.roomId.isNullOrBlank()) it.copy(roomId = cleanRoom) else it }
            val myPid = getMyPublicId()
            val isLocallyOwner = sessionStorage.isMyOwnedRoom(cleanRoom)
            val currentIdentity = _myIdentity.value

            val sanitizedScopedMembers = if (isLocallyOwner && !myPid.isNullOrBlank()) {
                val cleaned = scopedMembers.filter { m ->
                    m.publicId.equals(myPid, ignoreCase = true) ||
                    (m.isOwner != 1 &&
                     !m.role.equals("ADMIN", ignoreCase = true) &&
                     !m.role.equals("OWNER", ignoreCase = true))
                }.toMutableList()
                val selfIndex = cleaned.indexOfFirst { it.publicId.equals(myPid, ignoreCase = true) }
                if (selfIndex >= 0) {
                    cleaned[selfIndex] = cleaned[selfIndex].copy(
                        anonName = currentIdentity?.anonName ?: sessionStorage.getSavedAnonName() ?: cleaned[selfIndex].anonName,
                        anonAvatar = currentIdentity?.avatarUrl ?: cleaned[selfIndex].anonAvatar,
                        isOwner = 1,
                        role = "ADMIN"
                    )
                } else {
                    cleaned.add(
                        0,
                        RoomMemberDto(
                            roomId = cleanRoom,
                            publicId = myPid,
                            anonName = currentIdentity?.anonName ?: sessionStorage.getSavedAnonName() ?: "You",
                            anonAvatar = currentIdentity?.avatarUrl,
                            isOwner = 1,
                            role = "ADMIN"
                        )
                    )
                }
                cleaned
            } else {
                scopedMembers
            }

            _userRooms.update { current ->
                current.map { r ->
                    if (isRoomMatching(r.roomId, cleanRoom)) {
                        val isAmAdmin = r.isUserAdmin(myPid, isLocallyOwner)
                        val updated = r.copy(
                            ownerPublicId = if (isLocallyOwner && !myPid.isNullOrBlank()) myPid else r.ownerPublicId,
                            createdBy = if (isLocallyOwner && !myPid.isNullOrBlank()) myPid else r.createdBy,
                            members = sanitizedScopedMembers,
                            rawIsAdmin = isAmAdmin,
                            rawMemberCount = maxOf(sanitizedScopedMembers.size, 1)
                        )
                        if (isLocallyOwner) {
                            sessionStorage.saveRoomSnapshot(updated)
                        }
                        updated
                    } else r
                }
            }
            sanitizedScopedMembers
        }
    }

    override suspend fun loadMessages(roomId: String, limit: Int, beforePostNum: Long?): Result<List<MessageDto>> {
        val cleanRoom = roomId.trim()
        val result = executeApiCall { apiClient.getMessages(cleanRoom, limit, getSessionId(), beforePostNum) }
        result.onSuccess { rawList ->
            // Strictly scope every message to cleanRoom
            val list = rawList.filter { isRoomMatching(it.roomId, cleanRoom) }
            // Extract latest global group rename announcement if present in history
            val renameRegex = Regex("""(?:🏷️\s*(?:Changed group name to|Group name changed to)\s*\"([^\"]+)\"|__RENAME__:([^\n]+))""")
            val latestRename = list.lastOrNull { msg ->
                val content = msg.content
                content != null && renameRegex.containsMatchIn(content)
            }
            if (latestRename != null) {
                val content = latestRename.content
                val match = if (content != null) renameRegex.find(content) else null
                val extractedName = match?.let { (it.groupValues[1].ifBlank { null } ?: it.groupValues[2]).trim() }
                if (!extractedName.isNullOrBlank()) {
                    sessionStorage.saveCustomRoomName(cleanRoom, extractedName)
                    val saved = sessionStorage.getSavedRoom(cleanRoom)
                    if (saved != null) {
                        sessionStorage.saveRoomSnapshot(saved.copy(name = extractedName))
                    }
                    _userRooms.update { current ->
                        current.map { r ->
                            if (r.roomId == cleanRoom) r.copy(name = extractedName) else r
                        }
                    }
                }
            }

            if (beforePostNum != null) {
                val existing = (roomMessagesCache[cleanRoom] ?: sessionStorage.getRoomMessages(cleanRoom))
                    .filter { isRoomMatching(it.roomId, cleanRoom) }
                val merged = (list + existing)
                    .distinctBy { it.clientMsgId?.takeIf { id -> id.isNotBlank() } ?: it.postNum.toString() }
                    .sortedWith(compareBy({ it.isPinned == 0 }, { it.postNum }))
                roomMessagesCache[cleanRoom] = merged
                if (isRoomMatching(currentRoomId, cleanRoom)) {
                    _messages.value = merged
                }
            } else {
                roomMessagesCache[cleanRoom] = list
                if (isRoomMatching(currentRoomId, cleanRoom)) {
                    _messages.value = list
                }
                // Persist the latest message snapshot so we can show cached messages on re-entry
                sessionStorage.saveRoomMessages(cleanRoom, list)
            }
            synchronizedPeersFromMessages(list)
        }
        return result
    }

    override suspend fun loadOnlineUsers(): Result<List<OnlineUserDto>> {
        val result = executeApiCall { apiClient.getOnlineUsers() }
        result.onSuccess { users ->
            val myPid = _myIdentity.value?.publicId
            val actualOnline = users.filter { it.publicId.isNotBlank() && it.publicId != myPid }
            onlinePeersMap.clear()
            actualOnline.forEach { u ->
                onlinePeersMap[u.publicId] = u
                peerIdentityCache[u.publicId] = u
            }
            _onlineUsers.value = actualOnline
        }
        _hasLoadedOnlineUsers.value = true
        return Result.success(_onlineUsers.value)
    }

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
        currentRoomId = roomId
        return webSocketClient.send(
            OutgoingEvent.PostMessage(
                content = content,
                roomId = roomId,
                replyTo = replyTo,
                imagePath = imagePath,
                imageThumb = imageThumb,
                imageName = imageName,
                imageSize = imageSize,
                imageDims = imageDims,
                clientMsgId = clientMsgId
            )
        )
    }

    override suspend fun sendTyping(): Boolean {
        return sendTyping(null)
    }

    override suspend fun sendTyping(roomId: String?): Boolean {
        val pubId = _myIdentity.value?.publicId ?: sessionStorage.getSavedPublicId()
        return webSocketClient.send(OutgoingEvent.Typing(roomId = roomId, publicId = pubId))
    }

    override suspend fun toggleReaction(postNum: Long, emoji: String): Boolean {
        return webSocketClient.send(OutgoingEvent.React(postNum = postNum, emoji = emoji))
    }

    override suspend fun requestPrivateChat(targetPublicId: String): Boolean {
        return webSocketClient.send(OutgoingEvent.RequestPrivateChat(targetPublicId = targetPublicId))
    }

    override suspend fun respondPrivateChat(inviteId: String, accepted: Boolean): Boolean {
        return webSocketClient.send(OutgoingEvent.RespondPrivateChat(inviteId = inviteId, accepted = accepted))
    }

    override suspend fun leaveRoom(roomId: String): Boolean {
        val sent = webSocketClient.send(OutgoingEvent.LeaveRoom(roomId = roomId))
        _userRooms.update { current ->
            current.map { r ->
                if (r.roomId == roomId) {
                    r.copy(isMember = false, isOwner = false)
                } else r
            }
        }
        _discoverableGroups.update { current ->
            current.filterNot { it.id == roomId }
        }
        return sent
    }

    override suspend fun addUserToRoom(roomId: String, targetPublicId: String): Boolean {
        val myName = _myIdentity.value?.anonName
        val myAvatar = _myIdentity.value?.avatarUrl
        val customName = sessionStorage.getCustomRoomName(roomId)
        val currentRoomName = _userRooms.value.find { it.roomId == roomId }?.name ?: customName ?: "Private Group"

        // 1. Send primary AddUserToRoom event
        webSocketClient.send(OutgoingEvent.AddUserToRoom(roomId = roomId, targetPublicId = targetPublicId))

        // 2. Also send RequestPrivateChat with room details as guaranteed cross-compatible fallback
        webSocketClient.send(
            OutgoingEvent.RequestPrivateChat(
                targetPublicId = targetPublicId,
                roomId = roomId,
                roomName = currentRoomName,
                fromName = myName,
                fromAvatar = myAvatar
            )
        )
        return true
    }

    override suspend fun getStickersAndGifs(): Result<StickersAndGifsResponse> {
        return executeApiCall { apiClient.getStickersAndGifs() }
    }

    override suspend fun uploadImage(bytes: ByteArray, filename: String, mimeType: String): Result<UploadResponse> {
        return executeApiCall { apiClient.uploadImage(bytes, filename, mimeType) }
    }

    override suspend fun submitFeedback(category: String, message: String, rating: Int): Result<FeedbackResponse> {
        return executeApiCall { apiClient.submitFeedback(category, message, rating) }
    }

    override suspend fun leaveAllRooms(): Result<List<String>> {
        val result = executeApiCall { apiClient.leaveAllRooms(getSessionId()) }
        result.onSuccess {
            _userRooms.value = emptyList()
        }
        return result
    }

    override suspend fun getMediaBytes(urlOrPath: String): Result<ByteArray> {
        return apiClient.getMediaBytes(urlOrPath)
    }

    override fun resolveMediaUrl(relativeOrAbsoluteUrl: String): String {
        return apiClient.resolveMediaUrl(relativeOrAbsoluteUrl)
    }

    private fun synchronizedPeersFromRooms(rooms: List<RoomDto>) {
        val myPid = _myIdentity.value?.publicId
        rooms.forEach { room ->
            room.members.forEach { m ->
                if (m.publicId.isNotBlank() && m.publicId != myPid) {
                    val existing = peerIdentityCache[m.publicId]
                    if (existing == null || existing.anonName.isNullOrBlank()) {
                        peerIdentityCache[m.publicId] = OnlineUserDto(
                            publicId = m.publicId,
                            anonName = m.anonName,
                            anonAvatar = m.anonAvatar
                        )
                    }
                }
            }
        }
    }

    private fun synchronizedPeersFromMessages(msgs: List<MessageDto>) {
        val myPid = _myIdentity.value?.publicId
        msgs.forEach { msg ->
            val pid = msg.publicId
            if (!pid.isNullOrBlank() && pid != myPid) {
                val existing = peerIdentityCache[pid]
                if (existing == null || existing.anonName.isNullOrBlank()) {
                    peerIdentityCache[pid] = OnlineUserDto(
                        publicId = pid,
                        anonName = msg.anonName,
                        anonAvatar = msg.anonAvatar
                    )
                }
            }
        }
    }

    private fun refreshResolvedOnlineUsers() {
        _onlineUsers.value = resolveCurrentOnlineUsers()
    }

    private fun resolveCurrentOnlineUsers(): List<OnlineUserDto> {
        val myPid = _myIdentity.value?.publicId
        val currentCount = _onlineCount.value
        if (currentCount <= 1) {
            return emptyList()
        }
        return onlinePeersMap.values.filter { it.publicId.isNotBlank() && it.publicId != myPid }
    }

    private fun handleIncomingEvent(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.MyIdentity -> {
                _myIdentity.value = event.identity
                sessionStorage.saveIdentity(event.identity.publicId, event.identity.anonName)
                val myPid = event.identity.publicId
                onlinePeersMap.remove(myPid)
                peerIdentityCache.remove(myPid)
                refreshResolvedOnlineUsers()
                scope.launch {
                    loadUserRooms()
                }
            }
            is IncomingEvent.UserDisconnected -> {
                onlinePeersMap.remove(event.publicId)
                _onlineUsers.value = _onlineUsers.value.filter { it.publicId != event.publicId }
                if (_onlineCount.value > 1) {
                    _onlineCount.value = maxOf(1, _onlineCount.value - 1)
                }
                if (_onlineCount.value <= 1) {
                    onlinePeersMap.clear()
                    _onlineUsers.value = emptyList()
                }
            }
            is IncomingEvent.OnlineCount -> {
                val oldCount = _onlineCount.value
                _onlineCount.value = event.count
                if (event.count <= 1) {
                    onlinePeersMap.clear()
                    _onlineUsers.value = emptyList()
                } else if (event.users != null) {
                    val myPid = _myIdentity.value?.publicId
                    val actualOnline = event.users.filter { it.publicId.isNotBlank() && it.publicId != myPid }
                    onlinePeersMap.clear()
                    actualOnline.forEach { u ->
                        onlinePeersMap[u.publicId] = u
                        peerIdentityCache[u.publicId] = u
                    }
                    _onlineUsers.value = actualOnline
                } else if (oldCount != event.count) {
                    scope.launch {
                        loadOnlineUsers()
                    }
                }
            }
            is IncomingEvent.NewMessage -> {
                val msg = event.message
                if (!msg.publicId.isNullOrBlank() && msg.publicId != _myIdentity.value?.publicId) {
                    val existing = peerIdentityCache[msg.publicId]
                    if (existing == null || existing.anonName.isNullOrBlank()) {
                        peerIdentityCache[msg.publicId] = OnlineUserDto(
                            publicId = msg.publicId,
                            anonName = msg.anonName,
                            anonAvatar = msg.anonAvatar
                        )
                    }
                }

                // Global group rename synchronization via real-time WebSocket
                val msgContent = msg.content
                if (msgContent != null && msgContent.startsWith("__RENAME__:")) {
                    val rawPayload = msgContent.removePrefix("__RENAME__:").trim()
                    val parts = rawPayload.split(":", limit = 2)
                    if (parts.size == 2) {
                        val targetRoomId = parts[0].trim()
                        val newName = parts[1].trim()
                        if (targetRoomId.isNotBlank() && newName.isNotBlank()) {
                            sessionStorage.saveCustomRoomName(targetRoomId, newName)
                            val saved = sessionStorage.getSavedRoom(targetRoomId)
                            if (saved != null) {
                                sessionStorage.saveRoomSnapshot(saved.copy(name = newName))
                            }
                            _userRooms.update { current ->
                                current.map { r ->
                                    if (r.roomId == targetRoomId) r.copy(name = newName) else r
                                }
                            }
                            syncWidgets()
                        }
                    }
                    // Crucial: __RENAME__ is a background sync payload, DO NOT append to chat messages UI!
                    return
                }

                val renameRegex = Regex("""(?:🏷️\s*(?:Changed group name to|Group name changed to)\s*\"([^\"]+)\"|(?:^|\s)changed the group name to ['"]([^'"]+)['"])""")
                val renameMatch = if (msgContent != null) renameRegex.find(msgContent) else null
                if (renameMatch != null) {
                    val extractedName = (renameMatch.groupValues[1].ifBlank { null } ?: renameMatch.groupValues[2]).trim()
                    if (extractedName.isNotBlank()) {
                        sessionStorage.saveCustomRoomName(msg.roomId, extractedName)
                        val saved = sessionStorage.getSavedRoom(msg.roomId)
                        if (saved != null) {
                            sessionStorage.saveRoomSnapshot(saved.copy(name = extractedName))
                        }
                        _userRooms.update { current ->
                            current.map { r ->
                                if (r.roomId == msg.roomId) r.copy(name = extractedName) else r
                            }
                        }
                        syncWidgets()
                    }
                }

                val msgRoom = event.message.roomId.trim()
                // Update persistent and in-memory cache for this specific room
                val existingCache = (roomMessagesCache[msgRoom] ?: sessionStorage.getRoomMessages(msgRoom)).toMutableList()
                val cacheIndex = existingCache.indexOfFirst {
                    (!it.clientMsgId.isNullOrBlank() && it.clientMsgId == event.message.clientMsgId) || it.postNum == event.message.postNum
                }
                if (cacheIndex >= 0) {
                    existingCache[cacheIndex] = event.message
                } else {
                    existingCache.add(event.message)
                }
                roomMessagesCache[msgRoom] = existingCache
                sessionStorage.saveRoomMessages(msgRoom, existingCache)

                // ONLY update active _messages if msgRoom matches currentRoomId!
                val curRoom = currentRoomId.trim()
                val isMatch = isRoomMatching(msgRoom, curRoom)
                if (isMatch) {
                    _messages.update { existing ->
                        val currentList = existing.filter { isRoomMatching(it.roomId, curRoom) }.toMutableList()
                        val existingIndex = currentList.indexOfFirst {
                            (!it.clientMsgId.isNullOrBlank() && it.clientMsgId == event.message.clientMsgId) || it.postNum == event.message.postNum
                        }
                        if (existingIndex >= 0) {
                            currentList[existingIndex] = event.message
                        } else {
                            currentList.add(event.message)
                        }
                        currentList
                    }
                }
            }
            is IncomingEvent.ReactionUpdate -> {
                val currentList = _messages.value.map { msg ->
                    if (msg.postNum == event.postNum) {
                        msg.copy(reactions = event.reactions)
                    } else {
                        msg
                    }
                }
                _messages.value = currentList
            }
            is IncomingEvent.DeleteMessage -> {
                val currentList = _messages.value.filter { it.postNum != event.postNum }
                _messages.value = currentList
            }
            is IncomingEvent.PostsDeleted -> {
                val postNumSet = event.postNums.toSet()
                val currentList = _messages.value.filter { it.postNum !in postNumSet }
                _messages.value = currentList
            }
            is IncomingEvent.ClearChat -> {
                _messages.value = emptyList()
            }
            is IncomingEvent.RoomJoined -> {
                if (!PredefinedChannels.isPredefined(event.room.roomId)) {
                    val currentRooms = _userRooms.value.toMutableList()
                    val idx = currentRooms.indexOfFirst { it.roomId == event.room.roomId }
                    if (idx >= 0) {
                        currentRooms[idx] = event.room
                    } else {
                        currentRooms.add(event.room)
                    }
                    _userRooms.value = currentRooms
                }
                synchronizedPeersFromRooms(listOf(event.room))
                refreshResolvedOnlineUsers()
            }
            is IncomingEvent.RoomUpdated -> {
                sessionStorage.saveCustomRoomName(event.room.roomId, event.room.name)
                val saved = sessionStorage.getSavedRoom(event.room.roomId)
                if (saved != null) {
                    sessionStorage.saveRoomSnapshot(saved.copy(name = event.room.name))
                }
                if (!PredefinedChannels.isPredefined(event.room.roomId)) {
                    val currentRooms = _userRooms.value.map {
                        if (it.roomId == event.room.roomId) event.room else it
                    }
                    _userRooms.value = currentRooms
                }
                synchronizedPeersFromRooms(listOf(event.room))
                refreshResolvedOnlineUsers()
            }
            is IncomingEvent.RoomLeft, is IncomingEvent.RoomDeleted -> {
                val targetRoomId = if (event is IncomingEvent.RoomLeft) event.roomId else (event as IncomingEvent.RoomDeleted).roomId
                _userRooms.value = _userRooms.value.filter { it.roomId != targetRoomId }
                _discoverableGroups.value = _discoverableGroups.value.filter { it.id != targetRoomId }
            }
            is IncomingEvent.PrivateChatInvite -> {
                if (event.fromId.isNotBlank() && event.fromId != _myIdentity.value?.publicId) {
                    peerIdentityCache[event.fromId] = OnlineUserDto(
                        publicId = event.fromId,
                        anonName = event.fromName,
                        anonAvatar = event.fromAvatar
                    )
                }
            }
            else -> {}
        }
    }

    private fun isRoomMatching(msgRoom: String?, targetRoom: String?): Boolean {
        return org.example.annochat.data.model.isRoomMatching(msgRoom, targetRoom)
    }

    override fun syncWidgets() {
        try {
            val count = _onlineCount.value
            val mostActive = _userRooms.value.maxByOrNull { it.messageCount }?.name
                ?: _publicRooms.value.firstOrNull()?.name
                ?: "Campus Lobby"
            val identity = _myIdentity.value
            val anonName = identity?.anonName ?: sessionStorage.getSavedAnonName() ?: "Anonymous"
            val publicId = identity?.publicId ?: sessionStorage.getSavedPublicId() ?: ""

            val snapshot = WidgetDataSnapshot(
                onlineCount = count,
                mostActiveRoom = mostActive,
                userAnonName = anonName,
                userPublicId = publicId,
                lastUpdatedEpochMs = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds,
                showOnlineCount = sessionStorage.isWidgetOnlineCountEnabled(),
                showIdentity = sessionStorage.isWidgetIdentityEnabled(),
                showQuickMatch = sessionStorage.isWidgetQuickMatchEnabled(),
                updateFrequencyMinutes = sessionStorage.getWidgetUpdateIntervalMinutes()
            )
            syncPlatformWidgetData(snapshot)
        } catch (_: Throwable) {}
    }
}

