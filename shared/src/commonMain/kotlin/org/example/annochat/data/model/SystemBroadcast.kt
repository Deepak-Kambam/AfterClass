package org.example.annochat.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Severity levels for campus-wide and room-specific broadcasts.
 */
@Serializable
enum class BroadcastSeverity {
    @SerialName("INFO")
    INFO,

    @SerialName("WARNING")
    WARNING,

    @SerialName("EMERGENCY")
    EMERGENCY;

    companion object {
        fun fromString(value: String?): BroadcastSeverity {
            return when (value?.trim()?.uppercase()) {
                "EMERGENCY" -> EMERGENCY
                "WARNING" -> WARNING
                else -> INFO
            }
        }
    }
}

/**
 * Strongly typed model for AfterClass Campus System Broadcasts.
 */
@Immutable
@Serializable
data class SystemBroadcast(
    @SerialName("broadcast_id")
    val broadcastId: String,

    @SerialName("message")
    val message: String,

    @SerialName("severity")
    val severity: BroadcastSeverity = BroadcastSeverity.INFO,

    @SerialName("room_id")
    val roomId: String? = null,

    @SerialName("target_audience")
    val targetAudience: String = "all_new",

    @SerialName("duration")
    val duration: Int = 0,

    @SerialName("created_at")
    val createdAt: String? = null
) {
    val isGlobal: Boolean
        get() = roomId.isNullOrBlank() || roomId == "global" || roomId == "all"
}

/**
 * Fallback REST response for active broadcast synchronization:
 * GET /api/broadcast/active
 */
@Serializable
data class ActiveBroadcastResponse(
    @SerialName("status")
    val status: String = "ok",

    @SerialName("broadcast")
    val broadcast: SystemBroadcast? = null
)

/**
 * Request payload sent by authorized hosts/admins to trigger a broadcast:
 * POST /api/host/broadcast or POST /api/admin/broadcast
 */
@Serializable
data class AdminBroadcastRequest(
    @SerialName("message")
    val message: String,

    @SerialName("severity")
    val severity: String = "INFO",

    @SerialName("room_id")
    val roomId: String? = null,

    @SerialName("target_audience")
    val targetAudience: String = "all_new",

    @SerialName("duration")
    val duration: Int = 0
)

/**
 * Response payload returned after triggering an admin broadcast.
 */
@Serializable
data class AdminBroadcastResponse(
    @SerialName("status")
    val status: String,

    @SerialName("broadcast")
    val broadcast: SystemBroadcast? = null,

    @SerialName("message")
    val message: String? = null
)
