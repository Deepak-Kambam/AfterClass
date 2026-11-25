package org.example.annochat.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.annochat.data.model.PublicRoomDto
import org.example.annochat.data.model.RoomDto
import org.example.annochat.data.model.UserIdentityDto
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.network.ConnectionState
import androidx.compose.runtime.Immutable
import org.example.annochat.presentation.components.NavTab

import org.example.annochat.data.model.RoomActivityLevel
import org.example.annochat.data.model.IncomingEvent
import kotlin.time.TimeSource
import kotlin.time.TimeMark

@Immutable
data class RoomsUiState(
    val isLoading: Boolean = false,
    val isRetrying: Boolean = false,
    val publicRooms: List<PublicRoomDto> = emptyList(),
    val userRooms: List<RoomDto> = emptyList(),
    val myIdentity: UserIdentityDto? = null,
    val onlineCount: Int = 0,
    val connectionState: ConnectionState? = null,
    val errorMessage: String? = null,
    val selectedTab: NavTab = NavTab.CHATS,
    val onlinePrimarySegment: Int = 0,
    val onlineGroupsSegment: Int = 0,
    val roomActivityLevels: Map<String, RoomActivityLevel> = emptyMap(),
    val activityTickerText: String? = null
)

class RoomsViewModel(
    private val repository: ChatRepository,
    private val sessionStorage: org.example.annochat.data.session.SessionStorage? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val initialTab = sessionStorage?.let {
        val tabIdx = it.getLastActiveTab()
        NavTab.entries.getOrElse(tabIdx) { NavTab.CHATS }
    } ?: NavTab.CHATS

    private val roomRecentMarks = mutableMapOf<String, TimeMark>()
    private var tickerClearJob: kotlinx.coroutines.Job? = null

    private fun postActivityTicker(text: String) {
        tickerClearJob?.cancel()
        _uiState.value = _uiState.value.copy(activityTickerText = text)
        tickerClearJob = scope.launch {
            kotlinx.coroutines.delay(6000)
            _uiState.value = _uiState.value.copy(activityTickerText = null)
        }
    }

    private val _uiState = MutableStateFlow(RoomsUiState(isLoading = true, selectedTab = initialTab))
    val uiState: StateFlow<RoomsUiState> = _uiState.asStateFlow()

    init {
        // Observe repository flows
        scope.launch {
            repository.publicRooms.collect { rooms ->
                _uiState.value = _uiState.value.copy(
                    publicRooms = rooms,
                    roomActivityLevels = computeActivityLevels(rooms)
                )
            }
        }
        scope.launch {
            repository.userRooms.collect { rooms ->
                _uiState.value = _uiState.value.copy(userRooms = rooms)
            }
        }
        scope.launch {
            repository.myIdentity.collect { identity ->
                _uiState.value = _uiState.value.copy(myIdentity = identity)
            }
        }
        scope.launch {
            repository.onlineCount.collect { count ->
                _uiState.value = _uiState.value.copy(onlineCount = count)
            }
        }
        scope.launch {
            repository.connectionState.collect { conn ->
                _uiState.value = _uiState.value.copy(connectionState = conn)
            }
        }

        // Real-time event listener for live room heat and campus activity ticker
        scope.launch {
            repository.incomingEvents.collect { event ->
                when (event) {
                    is IncomingEvent.NewMessage -> {
                        val rid = event.message.roomId
                        if (!rid.isNullOrBlank()) {
                            recordRoomActivity(rid)
                            val roomName = _uiState.value.publicRooms.find { it.id == rid }?.name
                            val target = if (!roomName.isNullOrBlank()) "#$roomName" else "a room"
                            postActivityTicker("New message in $target")
                        }
                    }
                    is IncomingEvent.RoomJoined -> {
                        recordRoomActivity(event.room.roomId)
                        val roomName = event.room.name.ifBlank { "a room" }
                        postActivityTicker("Someone joined #$roomName")
                    }
                    is IncomingEvent.RoomUpdated -> {
                        recordRoomActivity(event.room.roomId)
                        val roomName = event.room.name.ifBlank { "a room" }
                        postActivityTicker("#$roomName was updated")
                    }
                    is IncomingEvent.OnlineCount -> {
                        if (event.count > 1) {
                            postActivityTicker("${event.count} students active right now")
                        }
                    }
                    is IncomingEvent.Typing -> {
                        postActivityTicker("Someone is typing…")
                    }
                    else -> {}
                }
            }
        }

        loadRooms()
    }

    private fun recordRoomActivity(roomId: String) {
        roomRecentMarks[roomId] = TimeSource.Monotonic.markNow()
        _uiState.value = _uiState.value.copy(
            roomActivityLevels = computeActivityLevels(_uiState.value.publicRooms)
        )
    }

    private fun computeActivityLevels(rooms: List<PublicRoomDto>): Map<String, RoomActivityLevel> {
        return rooms.associate { room ->
            val mark = roomRecentMarks[room.id]
            val elapsedSec = mark?.elapsedNow()?.inWholeSeconds ?: Long.MAX_VALUE
            val level = when {
                elapsedSec < 90 -> RoomActivityLevel.VERY_ACTIVE
                elapsedSec < 600 || room.memberCount >= 8 -> RoomActivityLevel.ACTIVE
                elapsedSec < 1800 || room.memberCount >= 2 -> RoomActivityLevel.LOW_ACTIVITY
                else -> RoomActivityLevel.IDLE
            }
            room.id to level
        }
    }

    fun loadRooms() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.connect()
            val pubResult = repository.loadPublicRooms()
            val userResult = repository.loadUserRooms()
            repository.loadOnlineUsers()

            if (pubResult.isFailure && userResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRetrying = false,
                    errorMessage = pubResult.exceptionOrNull()?.message ?: "Failed to load rooms"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRetrying = false,
                    errorMessage = null
                )
            }
        }
    }

    fun retryRooms() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isRetrying = true)
            repository.connect()
            val pubResult = repository.loadPublicRooms()
            val userResult = repository.loadUserRooms()
            repository.loadOnlineUsers()

            if (pubResult.isFailure && userResult.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRetrying = false,
                    errorMessage = pubResult.exceptionOrNull()?.message ?: "Failed to load rooms"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRetrying = false,
                    errorMessage = null
                )
            }
        }
    }

    fun setTab(tab: NavTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        sessionStorage?.saveLastActiveTab(tab.ordinal)
    }

    fun setOnlinePrimarySegment(segment: Int) {
        _uiState.value = _uiState.value.copy(onlinePrimarySegment = segment)
    }

    fun setOnlineGroupsSegment(segment: Int) {
        _uiState.value = _uiState.value.copy(onlineGroupsSegment = segment)
    }
}
