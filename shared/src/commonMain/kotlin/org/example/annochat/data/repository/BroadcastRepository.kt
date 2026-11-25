package org.example.annochat.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.annochat.data.model.AdminBroadcastRequest
import org.example.annochat.data.model.BroadcastSeverity
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.SystemBroadcast
import org.example.annochat.data.session.SessionStorage
import org.example.annochat.network.ApiClient
import org.example.annochat.network.ConnectionStatus
import org.example.annochat.network.WebSocketClient

/**
 * Repository interface for managing Campus Broadcasts.
 */
interface BroadcastRepository {
    /**
     * Currently active, visible broadcast (or null if none active, expired, or dismissed).
     */
    val activeBroadcast: StateFlow<SystemBroadcast?>

    /**
     * SharedFlow emitting all incoming live broadcast events as they arrive over WebSocket.
     */
    val broadcastEvents: SharedFlow<SystemBroadcast>

    /**
     * Dismiss an active broadcast by its broadcastId so it is hidden and not shown again.
     */
    fun dismissBroadcast(broadcastId: String)

    /**
     * Notify repository of room navigation change to re-evaluate room-specific broadcast visibility.
     */
    fun onRoomChanged(roomId: String)

    /**
     * Trigger fallback sync with GET /api/broadcast/active.
     */
    suspend fun refreshActiveBroadcast()

    /**
     * Send a host/admin broadcast.
     */
    suspend fun sendAdminBroadcast(
        message: String,
        severity: BroadcastSeverity = BroadcastSeverity.INFO,
        roomId: String? = null,
        duration: Int = 0,
        targetAudience: String = "all_new"
    ): Result<SystemBroadcast>
}

class BroadcastRepositoryImpl(
    private val apiClient: ApiClient,
    private val webSocketClient: WebSocketClient,
    private val chatRepository: ChatRepository,
    private val sessionStorage: SessionStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : BroadcastRepository {

    private val _activeBroadcast = MutableStateFlow<SystemBroadcast?>(null)
    override val activeBroadcast: StateFlow<SystemBroadcast?> = _activeBroadcast.asStateFlow()

    private val _broadcastEvents = MutableSharedFlow<SystemBroadcast>(extraBufferCapacity = 32)
    override val broadcastEvents: SharedFlow<SystemBroadcast> = _broadcastEvents.asSharedFlow()

    private val dismissedIds = mutableSetOf<String>()
    private var dismissTimerJob: Job? = null
    private var latestCandidateBroadcast: SystemBroadcast? = null
    private var lastConnectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED

    init {
        // 1. Listen for incoming WebSocket events
        scope.launch {
            chatRepository.incomingEvents.collect { event ->
                if (event is IncomingEvent.SystemBroadcast) {
                    handleIncomingBroadcast(event.broadcast)
                }
            }
        }

        // 2. Listen for reconnection events to refresh active broadcast
        scope.launch {
            webSocketClient.connectionState.collect { connState ->
                val prev = lastConnectionStatus
                lastConnectionStatus = connState.status
                if (connState.status == ConnectionStatus.CONNECTED && prev != ConnectionStatus.CONNECTED) {
                    refreshActiveBroadcast()
                }
            }
        }

        // 3. Initial sync fallback on startup
        scope.launch {
            refreshActiveBroadcast()
        }
    }

    private fun isDismissed(broadcastId: String): Boolean {
        if (dismissedIds.contains(broadcastId)) return true
        return sessionStorage?.isBroadcastDismissed(broadcastId) == true
    }

    private fun isRoomMatching(msgRoom: String, targetRoom: String): Boolean {
        return org.example.annochat.data.model.isRoomMatching(msgRoom, targetRoom)
    }

    private fun isVisibleInCurrentRoom(broadcast: SystemBroadcast): Boolean {
        val bcRoom = broadcast.roomId?.trim()
        if (bcRoom.isNullOrBlank() || bcRoom == "global" || bcRoom == "all") {
            return true
        }
        val currentRoom = chatRepository.getCurrentRoomId()
        return isRoomMatching(bcRoom, currentRoom)
    }

    private fun handleIncomingBroadcast(broadcast: SystemBroadcast) {
        if (isDismissed(broadcast.broadcastId)) {
            return
        }

        // Emit to one-time event stream
        scope.launch {
            _broadcastEvents.emit(broadcast)
        }

        latestCandidateBroadcast = broadcast
        updateDisplayForBroadcast(broadcast)
    }

    private fun updateDisplayForBroadcast(broadcast: SystemBroadcast) {
        if (!isVisibleInCurrentRoom(broadcast)) {
            // Belongs to a different room; don't display in this room
            if (_activeBroadcast.value?.broadcastId == broadcast.broadcastId) {
                dismissTimerJob?.cancel()
                _activeBroadcast.value = null
            }
            return
        }

        dismissTimerJob?.cancel()
        _activeBroadcast.value = broadcast

        // Auto-dismiss countdown if duration > 0
        if (broadcast.duration > 0) {
            dismissTimerJob = scope.launch {
                delay(broadcast.duration * 1000L)
                if (_activeBroadcast.value?.broadcastId == broadcast.broadcastId) {
                    _activeBroadcast.value = null
                }
            }
        }
    }

    override fun dismissBroadcast(broadcastId: String) {
        dismissedIds.add(broadcastId)
        sessionStorage?.markBroadcastDismissed(broadcastId)

        if (_activeBroadcast.value?.broadcastId == broadcastId) {
            dismissTimerJob?.cancel()
            _activeBroadcast.value = null
        }
        if (latestCandidateBroadcast?.broadcastId == broadcastId) {
            latestCandidateBroadcast = null
        }
    }

    override fun onRoomChanged(roomId: String) {
        val candidate = latestCandidateBroadcast ?: return
        if (isDismissed(candidate.broadcastId)) return

        if (isVisibleInCurrentRoom(candidate)) {
            if (_activeBroadcast.value?.broadcastId != candidate.broadcastId) {
                updateDisplayForBroadcast(candidate)
            }
        } else {
            if (_activeBroadcast.value?.broadcastId == candidate.broadcastId) {
                dismissTimerJob?.cancel()
                _activeBroadcast.value = null
            }
        }
    }

    override suspend fun refreshActiveBroadcast() {
        try {
            val result = apiClient.getActiveBroadcast()
            result.onSuccess { response ->
                val broadcast = response.broadcast
                if (broadcast != null && !isDismissed(broadcast.broadcastId)) {
                    latestCandidateBroadcast = broadcast
                    updateDisplayForBroadcast(broadcast)
                }
            }
        } catch (_: Exception) {
            // Network fallback failure is handled non-intrusively
        }
    }

    override suspend fun sendAdminBroadcast(
        message: String,
        severity: BroadcastSeverity,
        roomId: String?,
        duration: Int,
        targetAudience: String
    ): Result<SystemBroadcast> {
        val req = AdminBroadcastRequest(
            message = message,
            severity = severity.name,
            roomId = roomId,
            duration = duration,
            targetAudience = targetAudience
        )
        val result = apiClient.sendBroadcast(req)
        return result.mapCatching { resp ->
            resp.broadcast ?: SystemBroadcast(
                broadcastId = "bc_${kotlin.random.Random.nextLong(100000, 999999)}",
                message = message,
                severity = severity,
                roomId = roomId,
                targetAudience = targetAudience,
                duration = duration
            )
        }
    }
}
