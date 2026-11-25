package org.example.annochat.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.annochat.data.model.IncomingEvent
import org.example.annochat.data.model.OutgoingEvent
import kotlin.math.min
import kotlin.math.pow

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class ConnectionState(
    val status: ConnectionStatus,
    val errorMessage: String? = null
)

/**
 * Robust WebSocket client with auto-reconnect, exponential backoff, and typed event dispatching.
 */
open class WebSocketClient(
    var config: NetworkConfig,
    private val httpClient: HttpClient = createDefaultWebSocketHttpClient(),
    private val onReconnectAttempt: (suspend () -> Unit)? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    fun updateConfig(newConfig: NetworkConfig) {
        this.config = newConfig
    }

    protected val _connectionState = MutableStateFlow(ConnectionState(ConnectionStatus.DISCONNECTED))
    open val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    protected val _incomingEvents = MutableSharedFlow<IncomingEvent>(extraBufferCapacity = 64)
    open val incomingEvents: SharedFlow<IncomingEvent> = _incomingEvents.asSharedFlow()

    private var connectionJob: Job? = null
    private var activeSession: DefaultClientWebSocketSession? = null
    private var currentSessionId: String? = null
    private var manualDisconnect = false

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Connect to the FastAPI WebSocket endpoint (/ws?session_id=<sessionId>).
     */
    fun connect(sessionId: String) {
        if (_connectionState.value.status == ConnectionStatus.CONNECTED && currentSessionId == sessionId) {
            return
        }

        currentSessionId = sessionId
        manualDisconnect = false

        connectionJob?.cancel()
        connectionJob = coroutineScope.launch {
            var attempt = 0
            while (isActive && !manualDisconnect) {
                try {
                    if (config.environment == NetworkEnvironment.REMOTE && !config.isHostConfigured) {
                        println("[WebSocketClient] Host not configured yet, invoking discovery...")
                        onReconnectAttempt?.invoke()
                        delay(1000)
                        continue
                    }

                    val status = if (attempt == 0) ConnectionStatus.CONNECTING else ConnectionStatus.RECONNECTING
                    _connectionState.value = ConnectionState(status)

                    val wsUrl = config.websocketUrl(sessionId)
                    println("[WebSocketClient] Connecting to $wsUrl")

                    httpClient.webSocket(urlString = wsUrl) {
                        activeSession = this
                        attempt = 0
                        _connectionState.value = ConnectionState(ConnectionStatus.CONNECTED)
                        println("[WebSocketClient] Connected successfully!")

                        val pingJob = launch {
                            while (isActive) {
                                delay(20_000)
                                if (isActive) {
                                    send(OutgoingEvent.Ping())
                                }
                            }
                        }

                        try {
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val event = IncomingEvent.fromJson(text)
                                    _incomingEvents.emit(event)
                                }
                            }
                        } finally {
                            pingJob.cancel()
                        }
                    }
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState(
                        status = ConnectionStatus.ERROR,
                        errorMessage = e.message ?: "WebSocket connection failed"
                    )
                } finally {
                    activeSession = null
                }

                if (manualDisconnect) break

                // Exponential backoff reconnect with randomized jitter: 1s, 2s, 4s, up to max 15s + jitter
                attempt++
                val baseDelay = min(15000L, (1000L * 2.0.pow((attempt - 1).coerceAtMost(5).toDouble())).toLong())
                val jitter = kotlin.random.Random.nextLong(0, min(1500L, (baseDelay * 0.25).toLong() + 1))
                val delayMs = baseDelay + jitter
                _connectionState.value = ConnectionState(ConnectionStatus.RECONNECTING)
                delay(delayMs)
                try {
                    onReconnectAttempt?.invoke()
                } catch (_: Exception) {}
            }

            if (manualDisconnect) {
                _connectionState.value = ConnectionState(ConnectionStatus.DISCONNECTED)
            }
        }
    }

    /**
     * Send a typed outgoing action to the backend.
     */
    suspend fun send(event: OutgoingEvent): Boolean {
        val session = activeSession ?: return false
        return try {
            val payload = when (event) {
                is OutgoingEvent.PostMessage -> json.encodeToString(event)
                is OutgoingEvent.Typing -> json.encodeToString(event)
                is OutgoingEvent.React -> json.encodeToString(event)
                is OutgoingEvent.RequestPrivateChat -> json.encodeToString(event)
                is OutgoingEvent.RespondPrivateChat -> json.encodeToString(event)
                is OutgoingEvent.AddUserToRoom -> json.encodeToString(event)
                is OutgoingEvent.LeaveRoom -> json.encodeToString(event)
                is OutgoingEvent.JoinRoom -> json.encodeToString(event)
                is OutgoingEvent.Ping -> json.encodeToString(event)
            }
            session.send(Frame.Text(payload))
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Manually close the WebSocket connection.
     */
    fun disconnect() {
        manualDisconnect = true
        coroutineScope.launch {
            try {
                activeSession?.close()
            } catch (_: Exception) {}
            activeSession = null
        }
        connectionJob?.cancel()
        _connectionState.value = ConnectionState(ConnectionStatus.DISCONNECTED)
    }

    companion object {
        fun createDefaultWebSocketHttpClient(): HttpClient {
            return HttpClient {
                install(WebSockets) {
                    pingIntervalMillis = 20_000
                }
            }
        }
    }
}
