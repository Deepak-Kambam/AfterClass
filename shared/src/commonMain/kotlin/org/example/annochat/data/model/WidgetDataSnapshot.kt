package org.example.annochat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Lightweight snapshot of application data used to hydrate Home Screen Widgets
 * across Android (AppWidget) and iOS (WidgetKit).
 */
@Serializable
data class WidgetDataSnapshot(
    @SerialName("online_count")
    val onlineCount: Int = 1,

    @SerialName("most_active_room")
    val mostActiveRoom: String = "Campus Lobby",

    @SerialName("user_anon_name")
    val userAnonName: String? = null,

    @SerialName("user_public_id")
    val userPublicId: String? = null,

    @SerialName("user_avatar_url")
    val userAvatarUrl: String? = null,

    @SerialName("activity_bars")
    val activityBars: List<Float> = listOf(
        0.35f, 0.45f, 0.60f, 0.85f, 0.95f, 0.70f, 0.55f,
        0.40f, 0.50f, 0.75f, 0.90f, 1.00f, 0.80f, 0.65f
    ),

    @SerialName("last_updated_epoch_ms")
    val lastUpdatedEpochMs: Long = 0L,

    @SerialName("show_online_count")
    val showOnlineCount: Boolean = true,

    @SerialName("show_identity")
    val showIdentity: Boolean = true,

    @SerialName("show_quick_match")
    val showQuickMatch: Boolean = true,

    @SerialName("update_frequency_minutes")
    val updateFrequencyMinutes: Int = 30
)

/**
 * Curated list of rotating nocturnal quotes for the Time & Vibe widget.
 */
val WIDGET_VIBE_QUOTES = listOf(
    "Someone on campus is awake.",
    "After hours, belong somewhere.",
    "Quiet hours. Real thoughts.",
    "The campus never truly sleeps.",
    "Late night study breaks & whispers.",
    "Someone out there relates.",
    "Find your people in the quiet hours.",
    "Anonymous thoughts under the stars."
)
