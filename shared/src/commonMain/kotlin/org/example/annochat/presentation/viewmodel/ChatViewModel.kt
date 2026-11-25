package org.example.annochat.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.MessageDto
import org.example.annochat.data.model.OnlineUserDto
import org.example.annochat.data.model.StickersAndGifsResponse
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.model.generateClientMsgId
import org.example.annochat.data.repository.ChatRepository
import androidx.compose.runtime.Immutable
import org.example.annochat.presentation.components.resolveAnimalSlug
import org.example.annochat.network.ConnectionState
import org.example.annochat.network.ConnectionStatus

@Immutable
data class ChatUiState(
    val roomId: String,
    val roomName: String,
    val isLoading: Boolean = false,
    val isLoadingOlder: Boolean = false,
    val hasMoreOlderMessages: Boolean = false,
    val messages: List<MessageDto> = emptyList(),
    val myIdentity: UserIdentityDto? = null,
    val onlineCount: Int = 0,
    val connectionState: ConnectionState? = null,
    val typingUsers: List<String> = emptyList(),
    val inputText: String = "",
    val replyingTo: MessageDto? = null,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val selectedMessageIds: Set<Long> = emptySet(),
    val newlyArrivedPostNums: Set<Long> = emptySet(),
    val activeFloatingReactions: List<FloatingReaction> = emptyList(),
    val mediaUploadProgress: Float? = null,
    val mediaUploadStatus: String? = null
)

@Immutable
data class FloatingReaction(
    val id: String,
    val postNum: Long,
    val emoji: String,
    val timestampMs: Long = 0L
)

private data class PendingOptimistic(
    val message: MessageDto,
    val minServerPostNum: Long,
    val queuedTimeMs: Long
)

class ChatViewModel(
    val roomId: String,
    val roomName: String,
    val repository: ChatRepository,
    val sessionStorage: org.example.annochat.data.session.SessionStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val initialDraft = sessionStorage?.getDraftMessage(roomId) ?: ""

    private val _uiState = MutableStateFlow(
        ChatUiState(
            roomId = roomId,
            roomName = sessionStorage?.getCustomRoomName(roomId)?.ifBlank { null } ?: roomName,
            inputText = initialDraft,
            isLoading = true
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var typingClearJob: Job? = null
    private var typingDebounceJob: Job? = null
    private var lastTypingSentEpochMs = 0L
    private val monotonicTime = TimeSource.Monotonic

    private data class TypingUserRecord(
        val name: String,
        val publicId: String?,
        val lastSeenEpochMs: Long
    )
    private val activeTypers = MutableStateFlow<Map<String, TypingUserRecord>>(emptyMap())

    private val pendingOptimistic = MutableStateFlow<List<PendingOptimistic>>(emptyList())
    private val sentClientMsgIds = MutableStateFlow<Set<String>>(emptySet())

    fun isOwnClientMsgId(clientMsgId: String?): Boolean {
        if (clientMsgId.isNullOrBlank()) return false
        return sentClientMsgIds.value.contains(clientMsgId)
    }

    fun getMyPublicId(): String? {
        return _uiState.value.myIdentity?.publicId
            ?: repository.myIdentity.value?.publicId
            ?: sessionStorage?.getSavedPublicId()
    }

    fun getMyAnonName(): String? {
        return _uiState.value.myIdentity?.anonName
            ?: _uiState.value.myIdentity?.animalName
            ?: repository.myIdentity.value?.anonName
            ?: repository.myIdentity.value?.animalName
            ?: sessionStorage?.getSavedAnonName()
    }

    fun getMyAvatarUrl(): String? {
        return _uiState.value.myIdentity?.avatarUrl
            ?: repository.myIdentity.value?.avatarUrl
            ?: run {
                val name = getMyAnonName()
                val pid = getMyPublicId()
                if (!name.isNullOrBlank() || !pid.isNullOrBlank()) {
                    val slug = resolveAnimalSlug(null, name, pid)
                    "/api/animal/$slug"
                } else null
            }
    }

    fun updateRoomName(newName: String, onComplete: ((Result<Boolean>) -> Unit)? = null) {
        val clean = newName.trim()
        if (clean.length < 2 || clean.length > 32) {
            onComplete?.invoke(Result.failure(IllegalArgumentException("Name must be between 2 and 32 characters")))
            return
        }
        scope.launch {
            val result = repository.updateRoomName(roomId, clean)
            if (result.isSuccess) {
                sessionStorage?.saveCustomRoomName(roomId, clean)
                sessionStorage?.saveLastActiveRoom(roomId, clean)
                _uiState.update { it.copy(roomName = clean) }
            }
            onComplete?.invoke(result)
        }
    }

    fun deleteGroup(onComplete: ((Result<Boolean>) -> Unit)? = null) {
        scope.launch {
            val result = repository.deleteRoom(roomId)
            onComplete?.invoke(result)
        }
    }

    private fun reconcilePendingOptimistic(serverMessages: List<MessageDto>): List<MessageDto> {
        val myPid = getMyPublicId()
        val myName = getMyAnonName()
        val nowMs = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds

        pendingOptimistic.update { currentList ->
            currentList.filterNot { pending ->
                val opt = pending.message

                // 1. Exact match by clientMsgId
                val exactMatch = if (!opt.clientMsgId.isNullOrBlank()) {
                    serverMessages.any { it.clientMsgId == opt.clientMsgId }
                } else false

                // 2. Backward-compatible fallback match for production servers without client_msg_id support
                val fallbackMatch = if (!exactMatch) {
                    serverMessages.any { serverMsg ->
                        serverMsg.postNum > pending.minServerPostNum &&
                        serverMsg.content == opt.content &&
                        isServerMsgFromMe(serverMsg, myPid, myName)
                    }
                } else false

                // 3. Safety timeout: drop message if pending > 45s without confirmation
                val isTimedOut = (nowMs - pending.queuedTimeMs) > 45_000L

                exactMatch || fallbackMatch || isTimedOut
            }
        }
        return pendingOptimistic.value.map { it.message }
    }

    private fun isServerMsgFromMe(serverMsg: MessageDto, myPid: String?, myName: String?): Boolean {
        if (!serverMsg.clientMsgId.isNullOrBlank() && sentClientMsgIds.value.contains(serverMsg.clientMsgId)) return true
        if (!myPid.isNullOrBlank() && serverMsg.publicId == myPid) return true
        if (!myName.isNullOrBlank() && serverMsg.anonName == myName) return true
        if (serverMsg.anonName == "You") return true
        return false
    }

    init {
        repository.setCurrentRoomId(roomId)

        // Observe repository state strictly scoped to this room
        scope.launch {
            repository.messages.collect { list ->
                val roomScoped = list.filter { isRoomMatching(it.roomId, roomId) }
                val pendingMessages = reconcilePendingOptimistic(roomScoped)
                _uiState.update { current ->
                    val oldPostNums = current.messages.map { it.postNum }.toSet()
                    val newlyArrived = if (current.messages.isNotEmpty()) {
                        (roomScoped + pendingMessages).map { it.postNum }.filter { it !in oldPostNums }.toSet()
                    } else {
                        emptySet()
                    }
                    current.copy(
                        messages = roomScoped + pendingMessages,
                        isLoading = false,
                        hasMoreOlderMessages = current.hasMoreOlderMessages || roomScoped.size >= 50,
                        newlyArrivedPostNums = current.newlyArrivedPostNums + newlyArrived
                    )
                }
            }
        }
        scope.launch {
            repository.myIdentity.collect { identity ->
                _uiState.update { it.copy(myIdentity = identity) }
            }
        }
        scope.launch {
            repository.onlineCount.collect { count ->
                _uiState.update { it.copy(onlineCount = count) }
            }
        }
        scope.launch {
            repository.connectionState.collect { conn ->
                if (conn.status != ConnectionStatus.CONNECTED) {
                    clearTypingUsers()
                }
                _uiState.update { it.copy(connectionState = conn) }
            }
        }
        scope.launch {
            repository.incomingEvents.collect { event ->
                handleIncomingEvent(event)
            }
        }
        scope.launch {
            repository.userRooms.collect { rooms ->
                rooms.find { isRoomMatching(it.roomId, roomId) }?.let { r ->
                    if (r.name.isNotBlank() && r.name != _uiState.value.roomName) {
                        _uiState.update { it.copy(roomName = r.name) }
                    }
                }
            }
        }

        loadHistory()
    }

    fun loadHistory() {
        scope.launch {
            try {
                // Immediately show ONLY cached messages for this room so the user doesn't see a blank screen on re-entry,
                // and never show messages from another room while loading.
                val cached = (sessionStorage?.getRoomMessages(roomId) ?: emptyList())
                    .filter { isRoomMatching(it.roomId, roomId) }
                if (cached.isNotEmpty()) {
                    val pending = pendingOptimistic.value.map { it.message }
                    _uiState.update {
                        it.copy(
                            messages = (cached + pending).distinctBy { m -> m.clientMsgId?.takeIf { id -> id.isNotBlank() } ?: m.postNum.toString() },
                            isLoading = true,
                            errorMessage = null
                        )
                    }
                }
                var result = repository.loadMessages(roomId, limit = 50)

                // Silently retry once on 403: happens when opening/creating a new group
                // where membership hasn't been registered yet. Never show 403 to the user.
                if (result.isFailure && result.exceptionOrNull()?.message?.contains("403") == true) {
                    delay(1500)
                    result = repository.loadMessages(roomId, limit = 50)
                }

                if (result.isFailure) {
                    // Suppress error entirely if we have any messages to show (cached or live)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Never show 403 errors — they're an internal auth timing issue
                            errorMessage = if (it.messages.isNotEmpty() ||
                                (result.exceptionOrNull()?.message?.contains("403") == true)) null
                            else (result.exceptionOrNull()?.message ?: "Failed to load messages")
                        )
                    }
                } else {
                    val rawList = result.getOrThrow()
                    val list = rawList.filter { isRoomMatching(it.roomId, roomId) }
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
                            sessionStorage?.saveCustomRoomName(roomId, extractedName)
                            sessionStorage?.saveLastActiveRoom(roomId, extractedName)
                            _uiState.update { it.copy(roomName = extractedName) }
                        }
                    }
                    _uiState.update { current ->
                        val combinedServer = (list + current.messages.filter { it.postNum > 0 && isRoomMatching(it.roomId, roomId) })
                            .distinctBy { it.clientMsgId?.takeIf { id -> id.isNotBlank() } ?: it.postNum.toString() }
                            .sortedWith(compareBy({ it.isPinned == 0 }, { it.postNum }))
                        val pending = reconcilePendingOptimistic(combinedServer)
                        current.copy(
                            isLoading = false,
                            errorMessage = null,
                            hasMoreOlderMessages = list.size >= 50,
                            messages = combinedServer + pending
                        )
                    }
                }
            } catch (e: Throwable) {
                // Suppress 403 and any error if we already have messages loaded
                val msg = e.message ?: ""
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = if (it.messages.isNotEmpty() || msg.contains("403")) null
                        else msg.ifBlank { null }
                    )
                }
            }
        }
    }

    fun loadOlderMessages() {
        if (_uiState.value.isLoadingOlder || !_uiState.value.hasMoreOlderMessages) return
        val unpinned = _uiState.value.messages.filter { it.postNum > 0 && it.isPinned == 0 }
        val oldestPostNum = (if (unpinned.isNotEmpty()) unpinned.minOfOrNull { it.postNum } else _uiState.value.messages.filter { it.postNum > 0 }.minOfOrNull { it.postNum }) ?: return

        // Set loading synchronously to guard against concurrent rapid scroll events
        _uiState.update { it.copy(isLoadingOlder = true) }

        scope.launch {
            try {
                var result = repository.loadMessages(roomId = roomId, limit = 50, beforePostNum = oldestPostNum)
                // Silently retry once on 403 — membership may not be registered yet for new groups
                if (result.isFailure && result.exceptionOrNull()?.message?.contains("403") == true) {
                    delay(1500)
                    result = repository.loadMessages(roomId = roomId, limit = 50, beforePostNum = oldestPostNum)
                }
                if (result.isSuccess) {
                    val olderList = result.getOrThrow().filter { isRoomMatching(it.roomId, roomId) }
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = false,
                            hasMoreOlderMessages = olderList.size >= 50
                        )
                    }
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: ""
                    _uiState.update {
                        it.copy(
                            isLoadingOlder = false,
                            // Never surface 403 to the user — it's an internal membership timing issue
                            errorMessage = if (errMsg.contains("403")) null
                            else errMsg.ifBlank { "Failed to load older messages" }
                        )
                    }
                }
            } catch (e: Throwable) {
                val msg = e.message ?: ""
                _uiState.update {
                    it.copy(
                        isLoadingOlder = false,
                        errorMessage = if (msg.contains("403")) null else msg.ifBlank { null }
                    )
                }
            }
        }
    }

    fun onInputTextChange(newText: String) {
        _uiState.value = _uiState.value.copy(inputText = newText)
        sessionStorage?.saveDraftMessage(roomId, newText)
        if (newText.isNotBlank()) {
            val now = monotonicTime.markNow().elapsedNow().inWholeMilliseconds
            if (now - lastTypingSentEpochMs > 2500L) {
                lastTypingSentEpochMs = now
                typingDebounceJob?.cancel()
                scope.launch {
                    repository.sendTyping(roomId = roomId)
                }
            } else {
                typingDebounceJob?.cancel()
                typingDebounceJob = scope.launch {
                    delay(400)
                    lastTypingSentEpochMs = monotonicTime.markNow().elapsedNow().inWholeMilliseconds
                    repository.sendTyping(roomId = roomId)
                }
            }
        } else {
            typingDebounceJob?.cancel()
        }
    }

    fun setReplyTo(message: MessageDto?) {
        _uiState.value = _uiState.value.copy(replyingTo = message)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun toggleMessageSelection(postNum: Long) {
        val current = _uiState.value.selectedMessageIds.toMutableSet()
        if (current.contains(postNum)) current.remove(postNum) else current.add(postNum)
        _uiState.value = _uiState.value.copy(selectedMessageIds = current)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedMessageIds = emptySet())
    }

    fun sendMessage() {
        typingDebounceJob?.cancel()
        val text = _uiState.value.inputText.trim()
        if (text.isBlank()) return
        sessionStorage?.saveDraftMessage(roomId, null)

        val replyMsg = _uiState.value.replyingTo
        val myPid = getMyPublicId()
        val myName = getMyAnonName()
        val myAvatar = getMyAvatarUrl()

        // Generate a unique client message ID for idempotency and optimistic reconciliation
        val clientMsgId = generateClientMsgId()
        sentClientMsgIds.update { it + clientMsgId }

        val optimisticPostNum = -kotlin.random.Random.nextLong(100000, 999999)
        val optimisticMessage = MessageDto(
            postNum = optimisticPostNum,
            clientMsgId = clientMsgId,
            publicId = myPid,
            anonName = myName ?: "You",
            anonAvatar = myAvatar,
            content = text,
            roomId = roomId,
            replyTo = replyMsg?.postNum,
            replyName = replyMsg?.anonName,
            replySnippet = replyMsg?.content?.take(60),
            createdAt = "Just now"
        )

        val minServerPostNum = _uiState.value.messages.filter { it.postNum > 0 }.maxOfOrNull { it.postNum } ?: 0L
        val pending = PendingOptimistic(
            message = optimisticMessage,
            minServerPostNum = minServerPostNum,
            queuedTimeMs = TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
        )
        pendingOptimistic.update { it + pending }

        _uiState.update { current ->
            val serverOnly = current.messages.filter { it.postNum > 0 }
            val pendingMessages = pendingOptimistic.value.map { it.message }
            current.copy(
                messages = serverOnly + pendingMessages,
                inputText = "",
                replyingTo = null,
                isSending = true,
                newlyArrivedPostNums = current.newlyArrivedPostNums + optimisticMessage.postNum
            )
        }

        scope.launch {
            repository.setCurrentRoomId(roomId)
            val success = repository.sendMessage(
                content = text,
                roomId = roomId,
                replyTo = replyMsg?.postNum,
                clientMsgId = clientMsgId
            )
            _uiState.update { it.copy(isSending = false) }
            if (!success) {
                pendingOptimistic.update { list -> list.filterNot { it.message.clientMsgId == clientMsgId } }
                _uiState.update { current ->
                    val serverOnly = current.messages.filter { it.postNum > 0 }
                    val pendingMessages = pendingOptimistic.value.map { it.message }
                    current.copy(
                        messages = serverOnly + pendingMessages,
                        errorMessage = "Failed to send message. Please check connection."
                    )
                }
            }
        }
    }

    fun triggerFloatingReaction(postNum: Long, emoji: String) {
        val newReaction = FloatingReaction(
            id = "${postNum}_${emoji}_${monotonicTime.markNow().elapsedNow().inWholeMilliseconds}",
            postNum = postNum,
            emoji = emoji
        )
        _uiState.update { current ->
            current.copy(activeFloatingReactions = (current.activeFloatingReactions + newReaction).takeLast(3))
        }
        scope.launch {
            delay(1400)
            _uiState.update { current ->
                current.copy(activeFloatingReactions = current.activeFloatingReactions.filter { it.id != newReaction.id })
            }
        }
    }

    fun toggleReaction(postNum: Long, emoji: String) {
        triggerFloatingReaction(postNum, emoji)
        scope.launch {
            repository.toggleReaction(postNum, emoji)
        }
    }

    fun sendMediaMessage(imagePath: String, imageName: String, content: String? = null) {
        val clientMsgId = generateClientMsgId()
        sentClientMsgIds.update { it + clientMsgId }

        val myPid = getMyPublicId()
        val myName = getMyAnonName()
        val myAvatar = getMyAvatarUrl()

        val nowMs = monotonicTime.markNow().elapsedNow().inWholeMilliseconds
        val finalContent = content ?: (if (imagePath.startsWith("sticker_")) "[Sticker] $imageName" else "[GIF] $imageName")

        val optimisticPostNum = -kotlin.random.Random.nextLong(100000, 999999)
        val optimisticMessage = MessageDto(
            postNum = optimisticPostNum,
            clientMsgId = clientMsgId,
            publicId = myPid,
            anonName = myName ?: "You",
            anonAvatar = myAvatar,
            content = finalContent,
            roomId = roomId,
            imagePath = imagePath,
            imageName = imageName,
            createdAt = "Just now"
        )

        val minServerPostNum = _uiState.value.messages.filter { it.postNum > 0 }.maxOfOrNull { it.postNum } ?: 0L
        val pending = PendingOptimistic(
            message = optimisticMessage,
            minServerPostNum = minServerPostNum,
            queuedTimeMs = nowMs
        )
        pendingOptimistic.update { it + pending }

        _uiState.update { current ->
            val serverOnly = current.messages.filter { it.postNum > 0 }
            val pendingMessages = pendingOptimistic.value.map { it.message }
            current.copy(
                messages = serverOnly + pendingMessages,
                isSending = true,
                newlyArrivedPostNums = current.newlyArrivedPostNums + optimisticMessage.postNum
            )
        }

        scope.launch {
            repository.setCurrentRoomId(roomId)
            val success = repository.sendMessage(
                content = finalContent,
                roomId = roomId,
                imagePath = imagePath,
                imageName = imageName,
                clientMsgId = clientMsgId
            )
            _uiState.update { it.copy(isSending = false) }
            if (!success) {
                pendingOptimistic.update { list -> list.filterNot { it.message.clientMsgId == clientMsgId } }
                _uiState.update { current ->
                    val serverOnly = current.messages.filter { it.postNum > 0 }
                    val pendingMessages = pendingOptimistic.value.map { it.message }
                    current.copy(
                        messages = serverOnly + pendingMessages,
                        errorMessage = "Failed to send sticker/media."
                    )
                }
            }
        }
    }

    private var mediaUploadJob: Job? = null

    fun cancelMediaUpload() {
        mediaUploadJob?.cancel()
        mediaUploadJob = null
        _uiState.value = _uiState.value.copy(
            isSending = false,
            mediaUploadProgress = null,
            mediaUploadStatus = null
        )
    }

    fun uploadAndSendImage(bytes: ByteArray, fileName: String, mimeType: String, caption: String = "") {
        uploadAndSendMultipleImages(listOf(org.example.annochat.presentation.components.BakedMediaItem(bytes, fileName, mimeType, caption)))
    }

    fun uploadAndSendMultipleImages(images: List<org.example.annochat.presentation.components.BakedMediaItem>) {
        if (images.isEmpty()) return
        mediaUploadJob?.cancel()
        mediaUploadJob = scope.launch {
            val totalCount = images.size
            _uiState.value = _uiState.value.copy(
                isSending = true,
                mediaUploadProgress = 0.05f,
                mediaUploadStatus = if (totalCount > 1) "Sending 1/$totalCount (5%)" else "Sending 5%"
            )
            var hasFailure = false
            for ((index, item) in images.withIndex()) {
                if (!isActive) break
                val baseProgress = index.toFloat() / totalCount.toFloat()
                val slice = 1f / totalCount.toFloat()

                // Smooth simulated progress tick during HTTP image upload
                val tickerJob = launch {
                    var currentTick = 5f
                    while (isActive && currentTick < 80f) {
                        delay(70)
                        currentTick += (80f - currentTick) * 0.12f
                        val currentFraction = baseProgress + (currentTick / 100f) * slice
                        val percent = (currentFraction * 100).toInt().coerceIn(1, 98)
                        _uiState.value = _uiState.value.copy(
                            mediaUploadProgress = currentFraction.coerceIn(0.01f, 0.98f),
                            mediaUploadStatus = if (totalCount > 1) {
                                "Sending ${index + 1}/$totalCount ($percent%)"
                            } else {
                                "Sending $percent%"
                            }
                        )
                    }
                }

                val uploadResult = repository.uploadImage(item.bytes, item.fileName, item.mimeType)
                tickerJob.cancel()

                if (uploadResult.isSuccess) {
                    val uploadResp = uploadResult.getOrThrow()
                    repository.setCurrentRoomId(roomId)

                    // Sending message packet over WebSocket (92% of this slice)
                    val wsFraction = baseProgress + 0.92f * slice
                    val wsPercent = (wsFraction * 100).toInt().coerceIn(1, 99)
                    _uiState.value = _uiState.value.copy(
                        mediaUploadProgress = wsFraction,
                        mediaUploadStatus = if (totalCount > 1) {
                            "Sending ${index + 1}/$totalCount ($wsPercent%)"
                        } else {
                            "Sending $wsPercent%"
                        }
                    )

                    val clientMsgId = generateClientMsgId()
                    sentClientMsgIds.update { it + clientMsgId }
                    val sent = repository.sendMessage(
                        content = item.caption.trim(),
                        roomId = roomId,
                        imagePath = uploadResp.imagePath,
                        imageThumb = uploadResp.imageThumb,
                        imageName = uploadResp.imageName,
                        imageSize = uploadResp.imageSize,
                        imageDims = uploadResp.imageDims,
                        clientMsgId = clientMsgId
                    )
                    if (!sent) hasFailure = true

                    val doneFraction = (index + 1).toFloat() / totalCount.toFloat()
                    val donePercent = (doneFraction * 100).toInt().coerceIn(1, 100)
                    _uiState.value = _uiState.value.copy(
                        mediaUploadProgress = doneFraction,
                        mediaUploadStatus = if (totalCount > 1) {
                            "Sent ${index + 1}/$totalCount ($donePercent%)"
                        } else {
                            "Sent 100%"
                        }
                    )
                    delay(180)
                } else {
                    hasFailure = true
                }
            }

            // Small linger at 100% so user perceives completion
            delay(200)
            _uiState.value = _uiState.value.copy(
                isSending = false,
                mediaUploadProgress = null,
                mediaUploadStatus = null,
                errorMessage = if (hasFailure) "One or more images could not be sent." else null
            )
            mediaUploadJob = null
        }
    }

    fun addUserToRoom(targetPublicId: String, onResult: (Boolean) -> Unit) {
        scope.launch {
            val success = repository.addUserToRoom(roomId, targetPublicId)
            onResult(success)
        }
    }

    fun leaveRoom(onComplete: () -> Unit) {
        scope.launch {
            repository.leaveRoom(roomId)
            onComplete()
        }
    }

    fun loadOnlineUsers(onLoaded: (List<OnlineUserDto>) -> Unit) {
        scope.launch {
            val result = repository.loadOnlineUsers()
            val list = result.getOrNull() ?: emptyList()
            onLoaded(list)
        }
    }

    fun loadStickersAndGifs(onLoaded: (StickersAndGifsResponse?) -> Unit) {
        scope.launch {
            val result = repository.getStickersAndGifs()
            onLoaded(result.getOrNull())
        }
    }

    private fun handleIncomingEvent(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.NewMessage -> {
                val content = event.message.content
                if (content != null && content.startsWith("__RENAME__:")) {
                    val rawPayload = content.removePrefix("__RENAME__:").trim()
                    val parts = rawPayload.split(":", limit = 2)
                    if (parts.size == 2) {
                        val targetRoomId = parts[0].trim()
                        val newName = parts[1].trim()
                        if (isRoomMatching(targetRoomId, roomId) && newName.isNotBlank()) {
                            sessionStorage?.saveCustomRoomName(roomId, newName)
                            sessionStorage?.saveLastActiveRoom(roomId, newName)
                            _uiState.update { it.copy(roomName = newName) }
                        }
                    }
                } else if (isRoomMatching(event.message.roomId, roomId)) {
                    val renameRegex = Regex("""(?:🏷️\s*(?:Changed group name to|Group name changed to)\s*\"([^\"]+)\"|(?:^|\s)changed the group name to ['"]([^'"]+)['"])""")
                    val match = if (content != null) renameRegex.find(content) else null
                    if (match != null) {
                        val extractedName = (match.groupValues[1].ifBlank { null } ?: match.groupValues[2]).trim()
                        if (extractedName.isNotBlank()) {
                            sessionStorage?.saveCustomRoomName(roomId, extractedName)
                            sessionStorage?.saveLastActiveRoom(roomId, extractedName)
                            _uiState.update { it.copy(roomName = extractedName) }
                        }
                    }
                }
                // If this sender was typing, remove them immediately
                val senderPid = event.message.publicId
                val senderName = event.message.anonName
                removeTypingUser(senderPid, senderName)
            }
            is IncomingEvent.UserDisconnected -> {
                removeTypingUser(event.publicId, null)
            }
            is IncomingEvent.RoomUpdated -> {
                if (isRoomMatching(event.room.roomId, roomId)) {
                    val clean = event.room.name
                    sessionStorage?.saveCustomRoomName(roomId, clean)
                    sessionStorage?.saveLastActiveRoom(roomId, clean)
                    _uiState.update { it.copy(roomName = clean) }
                }
            }
            is IncomingEvent.Typing -> {
                // Room scoping: Room A typing events never appear in Room B
                if (event.roomId != null && !isRoomMatching(event.roomId, roomId)) {
                    return
                }

                // Current user exclusion: Never display own typing status
                if (isSelfTypingEvent(event)) {
                    return
                }

                val now = monotonicTime.markNow().elapsedNow().inWholeMilliseconds
                val key = event.publicId?.ifBlank { null } ?: event.animalName
                activeTypers.update { current ->
                    current + (key to TypingUserRecord(
                        name = event.animalName,
                        publicId = event.publicId,
                        lastSeenEpochMs = now
                    ))
                }
                refreshTypingUsersState()
                startTypingExpirationLoop()
            }
            is IncomingEvent.ReactionUpdate -> {
                val existingMsg = _uiState.value.messages.find { it.postNum == event.postNum }
                val oldReactions = existingMsg?.reactions ?: emptyMap()
                val increasedEmoji = event.reactions.entries.firstOrNull { (k, v) -> v > (oldReactions[k] ?: 0) }?.key
                if (increasedEmoji != null) {
                    triggerFloatingReaction(event.postNum, increasedEmoji)
                }
            }
            is IncomingEvent.ServerError -> {
                // Suppress 403 server errors — internal auth timing, not user-facing
                if (!event.message.contains("403") && !event.message.contains("Not a participant")) {
                    _uiState.value = _uiState.value.copy(errorMessage = event.message)
                }
            }
            else -> {}
        }
    }

    fun isSelfTypingEvent(event: IncomingEvent.Typing): Boolean {
        val myPid = _uiState.value.myIdentity?.publicId
            ?: sessionStorage?.getSavedPublicId()
            ?: getMyPublicId()
        if (!event.publicId.isNullOrBlank() && !myPid.isNullOrBlank()) {
            if (event.publicId.equals(myPid, ignoreCase = true)) return true
        }

        val mySess = sessionStorage?.getSessionId()
            ?: repository.getSessionId()
        if (!event.sessionId.isNullOrBlank() && !mySess.isNullOrBlank()) {
            if (event.sessionId.equals(mySess, ignoreCase = true)) return true
        }

        val myName = _uiState.value.myIdentity?.anonName
            ?: sessionStorage?.getSavedAnonName()
            ?: getMyAnonName()
        val myAnimal = _uiState.value.myIdentity?.animalName

        if (event.animalName.equals("You", ignoreCase = true)) return true
        if (!myName.isNullOrBlank() && event.animalName.equals(myName, ignoreCase = true)) return true
        if (!myAnimal.isNullOrBlank() && event.animalName.equals(myAnimal, ignoreCase = true)) return true

        return false
    }

    private fun removeTypingUser(publicId: String?, name: String?) {
        var changed = false
        activeTypers.update { current ->
            val filtered = current.filterNot { (k, v) ->
                val matchPid = !publicId.isNullOrBlank() && (k.equals(publicId, ignoreCase = true) || v.publicId.equals(publicId, ignoreCase = true))
                val matchName = !name.isNullOrBlank() && (k.equals(name, ignoreCase = true) || v.name.equals(name, ignoreCase = true))
                matchPid || matchName
            }
            if (filtered.size != current.size) changed = true
            filtered
        }
        if (changed) {
            refreshTypingUsersState()
        }
    }

    private fun refreshTypingUsersState() {
        val names = activeTypers.value.values.map { it.name }.distinct()
        _uiState.update { it.copy(typingUsers = names) }
    }

    private fun startTypingExpirationLoop() {
        if (typingClearJob?.isActive == true) return
        typingClearJob = scope.launch {
            while (activeTypers.value.isNotEmpty()) {
                delay(400)
                val currentNow = monotonicTime.markNow().elapsedNow().inWholeMilliseconds
                var changed = false
                activeTypers.update { current ->
                    val remaining = current.filterValues { currentNow - it.lastSeenEpochMs <= 3500L }
                    if (remaining.size != current.size) changed = true
                    remaining
                }
                if (changed) {
                    refreshTypingUsersState()
                }
            }
        }
    }

    private fun clearTypingUsers() {
        activeTypers.value = emptyMap()
        typingClearJob?.cancel()
        _uiState.update { it.copy(typingUsers = emptyList()) }
    }

    private fun isRoomMatching(msgRoom: String, targetRoom: String): Boolean {
        return org.example.annochat.data.model.isRoomMatching(msgRoom, targetRoom)
    }

    fun clear() {
        clearTypingUsers()
        typingDebounceJob?.cancel()
        mediaUploadJob?.cancel()
        pendingOptimistic.value = emptyList()
        scope.cancel()
    }
}
