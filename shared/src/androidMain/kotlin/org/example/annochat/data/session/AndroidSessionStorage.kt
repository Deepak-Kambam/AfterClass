package org.example.annochat.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.json.Json
import org.example.annochat.data.model.OnboardingState
import org.example.annochat.data.model.RoomDto

class AndroidPreferencesSessionStorage(
    private val preferences: SharedPreferences
) : SessionStorage {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private var cachedSessionId: String? = null

    override fun getOrCreateSessionId(): String {
        cachedSessionId?.let { return it }
        val stored = preferences.getString(KEY_SESSION_ID, null)
        if (!stored.isNullOrBlank()) {
            cachedSessionId = stored
            return stored
        }
        val newSessionId = InMemorySessionStorage.generateAnonymousSessionId()
        saveSessionId(newSessionId)
        return newSessionId
    }

    override fun getSessionId(): String? {
        return cachedSessionId ?: preferences.getString(KEY_SESSION_ID, null).also { cachedSessionId = it }
    }

    override fun saveSessionId(sessionId: String) {
        cachedSessionId = sessionId
        preferences.edit().putString(KEY_SESSION_ID, sessionId).apply()
    }

    override fun clearSession() {
        cachedSessionId = null
        preferences.edit().remove(KEY_SESSION_ID).apply()
    }

    override fun isOnboardingCompleted(): Boolean {
        return preferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override fun getOnboardingIntent(): String? {
        return preferences.getString(KEY_ONBOARDING_INTENT, null)?.ifBlank { null }
    }

    override fun setOnboardingIntent(intent: String?) {
        val trimmed = intent?.trim()?.ifBlank { null }
        if (trimmed == null) {
            preferences.edit().remove(KEY_ONBOARDING_INTENT).apply()
        } else {
            preferences.edit().putString(KEY_ONBOARDING_INTENT, trimmed).apply()
        }
    }

    override fun getOnboardingVibe(): String? {
        return preferences.getString(KEY_ONBOARDING_VIBE, null)?.ifBlank { null }
    }

    override fun setOnboardingVibe(vibe: String?) {
        val trimmed = vibe?.trim()?.ifBlank { null }
        if (trimmed == null) {
            preferences.edit().remove(KEY_ONBOARDING_VIBE).apply()
        } else {
            preferences.edit().putString(KEY_ONBOARDING_VIBE, trimmed).apply()
        }
    }

    override fun getOnboardingInterest(): String? {
        return preferences.getString(KEY_ONBOARDING_INTEREST, null)?.ifBlank { null }
    }

    override fun setOnboardingInterest(interest: String?) {
        val trimmed = interest?.trim()?.ifBlank { null }
        if (trimmed == null) {
            preferences.edit().remove(KEY_ONBOARDING_INTEREST).apply()
        } else {
            preferences.edit().putString(KEY_ONBOARDING_INTEREST, trimmed).apply()
        }
    }

    override fun isAhaMomentCompleted(): Boolean {
        return preferences.getBoolean(KEY_AHA_MOMENT_COMPLETED, false)
    }

    override fun setAhaMomentCompleted(completed: Boolean) {
        preferences.edit().putBoolean(KEY_AHA_MOMENT_COMPLETED, completed).apply()
    }

    override fun isPremiumScreenShown(): Boolean {
        return preferences.getBoolean(KEY_PREMIUM_SCREEN_SHOWN, false)
    }

    override fun setPremiumScreenShown(shown: Boolean) {
        preferences.edit().putBoolean(KEY_PREMIUM_SCREEN_SHOWN, shown).apply()
    }

    override fun getOnboardingState(): OnboardingState {
        val intent = getOnboardingIntent()
        val vibe = getOnboardingVibe()
        val interest = getOnboardingInterest()
        return OnboardingState(
            intent = intent,
            vibe = vibe,
            conversationInterest = interest,
            completedPhase1 = !intent.isNullOrBlank() && !vibe.isNullOrBlank() && !interest.isNullOrBlank(),
            isAhaMomentCompleted = isAhaMomentCompleted(),
            isPremiumScreenShown = isPremiumScreenShown()
        )
    }

    override fun saveOnboardingState(state: OnboardingState) {
        setOnboardingIntent(state.intent)
        setOnboardingVibe(state.vibe)
        setOnboardingInterest(state.conversationInterest)
        setAhaMomentCompleted(state.isAhaMomentCompleted)
        setPremiumScreenShown(state.isPremiumScreenShown)
    }

    override fun isLiquidGlassEnabled(): Boolean {
        return preferences.getBoolean(KEY_LIQUID_GLASS_ENABLED, true)
    }

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LIQUID_GLASS_ENABLED, enabled).apply()
    }

    override fun isRotatingGlowEnabled(): Boolean {
        return preferences.getBoolean(KEY_ROTATING_GLOW_ENABLED, true)
    }

    override fun setRotatingGlowEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ROTATING_GLOW_ENABLED, enabled).apply()
    }

    override fun isAnimationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_ANIMATIONS_ENABLED, true)
    }

    override fun setAnimationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ANIMATIONS_ENABLED, enabled).apply()
    }

    override fun isMessageNotificationsEnabled(): Boolean {
        return preferences.getBoolean(KEY_MESSAGE_NOTIFICATIONS_ENABLED, true)
    }

    override fun setMessageNotificationsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_MESSAGE_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    override fun isPrivateChatRequestsEnabled(): Boolean {
        return preferences.getBoolean(KEY_PRIVATE_CHAT_REQUESTS_ENABLED, true)
    }

    override fun setPrivateChatRequestsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_PRIVATE_CHAT_REQUESTS_ENABLED, enabled).apply()
    }

    override fun isNotificationPreviewEnabled(): Boolean {
        return preferences.getBoolean(KEY_NOTIFICATION_PREVIEW_ENABLED, true)
    }

    override fun setNotificationPreviewEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_NOTIFICATION_PREVIEW_ENABLED, enabled).apply()
    }

    override fun getAppearanceSettings(): org.example.annochat.data.model.AppearanceSettings {
        val json = preferences.getString(KEY_APPEARANCE_SETTINGS, null)
        return org.example.annochat.data.model.AppearanceSettings.fromJson(json)
    }

    override fun saveAppearanceSettings(settings: org.example.annochat.data.model.AppearanceSettings) {
        val json = org.example.annochat.data.model.AppearanceSettings.toJson(settings)
        preferences.edit()
            .putString(KEY_APPEARANCE_SETTINGS, json)
            .putBoolean(KEY_ROTATING_GLOW_ENABLED, settings.afterglowEnabled)
            .putBoolean(KEY_ANIMATIONS_ENABLED, settings.animationsEnabled)
            .apply()
    }

    override fun restoreDefaultAppearance() {
        preferences.edit()
            .remove(KEY_APPEARANCE_SETTINGS)
            .putBoolean(KEY_ROTATING_GLOW_ENABLED, true)
            .putBoolean(KEY_ANIMATIONS_ENABLED, true)
            .apply()
    }

    override fun getCustomBackendUrl(): String? {
        return preferences.getString(KEY_CUSTOM_BACKEND_URL, null)?.ifBlank { null }
    }

    override fun setCustomBackendUrl(url: String?) {
        val trimmed = url?.trim()?.ifBlank { null }
        if (trimmed == null) {
            preferences.edit().remove(KEY_CUSTOM_BACKEND_URL).apply()
        } else {
            preferences.edit().putString(KEY_CUSTOM_BACKEND_URL, trimmed).apply()
        }
    }

    override fun getSavedPublicId(): String? {
        return preferences.getString(KEY_SAVED_PUBLIC_ID, null)?.ifBlank { null }
    }

    override fun getSavedAnonName(): String? {
        return preferences.getString(KEY_SAVED_ANON_NAME, null)?.ifBlank { null }
    }

    override fun saveIdentity(publicId: String, anonName: String) {
        preferences.edit()
            .putString(KEY_SAVED_PUBLIC_ID, publicId)
            .putString(KEY_SAVED_ANON_NAME, anonName)
            .apply()
    }

    override fun getLastActiveRoomId(): String? {
        return preferences.getString(KEY_LAST_ACTIVE_ROOM_ID, null)?.ifBlank { null }
    }

    override fun getLastActiveRoomName(): String? {
        return preferences.getString(KEY_LAST_ACTIVE_ROOM_NAME, null)?.ifBlank { null }
    }

    override fun saveLastActiveRoom(roomId: String?, roomName: String?) {
        val editor = preferences.edit()
        if (roomId.isNullOrBlank()) {
            editor.remove(KEY_LAST_ACTIVE_ROOM_ID)
            editor.remove(KEY_LAST_ACTIVE_ROOM_NAME)
        } else {
            editor.putString(KEY_LAST_ACTIVE_ROOM_ID, roomId)
            editor.putString(KEY_LAST_ACTIVE_ROOM_NAME, roomName ?: roomId)
        }
        editor.apply()
    }

    override fun getLastActiveTab(): Int {
        return preferences.getInt(KEY_LAST_ACTIVE_TAB, 0)
    }

    override fun saveLastActiveTab(tab: Int) {
        preferences.edit().putInt(KEY_LAST_ACTIVE_TAB, tab).apply()
    }

    override fun getDraftMessage(roomId: String): String? {
        return preferences.getString(KEY_DRAFT_PREFIX + roomId, null)?.ifBlank { null }
    }

    override fun saveDraftMessage(roomId: String, draft: String?) {
        val key = KEY_DRAFT_PREFIX + roomId
        val trimmed = draft?.trim()?.ifBlank { null }
        if (trimmed == null) {
            preferences.edit().remove(key).apply()
        } else {
            preferences.edit().putString(key, trimmed).apply()
        }
    }

    override fun isWidgetOnlineCountEnabled(): Boolean {
        return preferences.getBoolean(KEY_WIDGET_ONLINE_COUNT, true)
    }

    override fun setWidgetOnlineCountEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WIDGET_ONLINE_COUNT, enabled).apply()
    }

    override fun isWidgetIdentityEnabled(): Boolean {
        return preferences.getBoolean(KEY_WIDGET_IDENTITY, true)
    }

    override fun setWidgetIdentityEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WIDGET_IDENTITY, enabled).apply()
    }

    override fun isWidgetQuickMatchEnabled(): Boolean {
        return preferences.getBoolean(KEY_WIDGET_QUICK_MATCH, true)
    }

    override fun setWidgetQuickMatchEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_WIDGET_QUICK_MATCH, enabled).apply()
    }

    override fun getWidgetUpdateIntervalMinutes(): Int {
        return preferences.getInt(KEY_WIDGET_UPDATE_INTERVAL, 30)
    }

    override fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_WIDGET_UPDATE_INTERVAL, minutes).apply()
    }

    override fun isBroadcastDismissed(broadcastId: String): Boolean {
        val set = preferences.getStringSet(KEY_DISMISSED_BROADCASTS, null) ?: return false
        return set.contains(broadcastId)
    }

    override fun markBroadcastDismissed(broadcastId: String) {
        val current = (preferences.getStringSet(KEY_DISMISSED_BROADCASTS, null) ?: emptySet()).toMutableSet()
        current.add(broadcastId)
        preferences.edit().putStringSet(KEY_DISMISSED_BROADCASTS, current).apply()
    }

    override fun getCustomRoomName(roomId: String): String? {
        val name = preferences.getString(KEY_CUSTOM_ROOM_PREFIX + roomId, null)
        return name?.ifBlank { null }
    }

    override fun saveCustomRoomName(roomId: String, name: String?) {
        val editor = preferences.edit()
        if (name.isNullOrBlank()) {
            editor.remove(KEY_CUSTOM_ROOM_PREFIX + roomId)
        } else {
            editor.putString(KEY_CUSTOM_ROOM_PREFIX + roomId, name.trim())
        }
        editor.apply()
    }

    override fun isRoomLocallyMarkedPrivate(roomId: String): Boolean {
        return preferences.getBoolean(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId, false)
    }

    override fun markRoomLocallyPrivate(roomId: String, isPrivate: Boolean) {
        val editor = preferences.edit()
        if (isPrivate) {
            editor.putBoolean(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId, true)
        } else {
            editor.remove(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId)
        }
        editor.apply()
    }

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun addMyRoom(roomId: String, isOwner: Boolean, isPrivate: Boolean) {
        if (roomId.isBlank()) return
        val currentRooms = (preferences.getStringSet(KEY_MY_ROOMS, null) ?: emptySet()).toMutableSet()
        currentRooms.add(roomId)
        val editor = preferences.edit().putStringSet(KEY_MY_ROOMS, currentRooms)
        if (isOwner) {
            val currentOwned = (preferences.getStringSet(KEY_MY_OWNED_ROOMS, null) ?: emptySet()).toMutableSet()
            currentOwned.add(roomId)
            editor.putStringSet(KEY_MY_OWNED_ROOMS, currentOwned)
        }
        if (isPrivate) {
            editor.putBoolean(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId, true)
        }
        editor.apply()
    }

    override fun removeMyRoom(roomId: String) {
        val currentRooms = (preferences.getStringSet(KEY_MY_ROOMS, null) ?: emptySet()).toMutableSet()
        currentRooms.remove(roomId)
        val currentOwned = (preferences.getStringSet(KEY_MY_OWNED_ROOMS, null) ?: emptySet()).toMutableSet()
        currentOwned.remove(roomId)
        preferences.edit()
            .putStringSet(KEY_MY_ROOMS, currentRooms)
            .putStringSet(KEY_MY_OWNED_ROOMS, currentOwned)
            .remove(KEY_SAVED_ROOM_PREFIX + roomId)
            .remove(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId)
            .apply()
    }

    override fun markRoomDeleted(roomId: String) {
        removeMyRoom(roomId)
        val currentDeleted = preferences.getStringSet(KEY_DELETED_ROOMS, null)?.toMutableSet() ?: mutableSetOf()
        currentDeleted.add(roomId)
        preferences.edit().putStringSet(KEY_DELETED_ROOMS, currentDeleted).apply()
    }

    override fun isRoomDeleted(roomId: String): Boolean {
        val set = preferences.getStringSet(KEY_DELETED_ROOMS, null) ?: return false
        return set.contains(roomId)
    }

    override fun isMyRoom(roomId: String): Boolean {
        val set = preferences.getStringSet(KEY_MY_ROOMS, null) ?: return false
        return set.contains(roomId)
    }

    override fun isMyOwnedRoom(roomId: String): Boolean {
        val set = preferences.getStringSet(KEY_MY_OWNED_ROOMS, null) ?: return false
        return set.contains(roomId)
    }

    override fun getMyRoomIds(): Set<String> {
        return preferences.getStringSet(KEY_MY_ROOMS, null)?.toSet() ?: emptySet()
    }

    override fun getMyOwnedRoomIds(): Set<String> {
        return preferences.getStringSet(KEY_MY_OWNED_ROOMS, null)?.toSet() ?: emptySet()
    }

    override fun saveRoomSnapshot(room: RoomDto) {
        if (room.roomId.isBlank()) return
        addMyRoom(room.roomId, isOwner = room.isOwner, isPrivate = !room.isPublic || room.isPrivateGroup)
        try {
            val raw = jsonSerializer.encodeToString(RoomDto.serializer(), room)
            preferences.edit().putString(KEY_SAVED_ROOM_PREFIX + room.roomId, raw).apply()
        } catch (_: Exception) {}
    }

    override fun getSavedRoom(roomId: String): RoomDto? {
        val raw = preferences.getString(KEY_SAVED_ROOM_PREFIX + roomId, null) ?: return null
        return try {
            jsonSerializer.decodeFromString(RoomDto.serializer(), raw)
        } catch (_: Exception) {
            null
        }
    }

    override fun getAllSavedRooms(): List<RoomDto> {
        val ids = getMyRoomIds()
        if (ids.isEmpty()) return emptyList()
        val list = mutableListOf<RoomDto>()
        for (id in ids) {
            getSavedRoom(id)?.let { list.add(it) }
        }
        return list
    }

    override fun saveRoomMessages(roomId: String, messages: List<org.example.annochat.data.model.MessageDto>) {
        try {
            val json = Json { ignoreUnknownKeys = true }
            val toSave = messages.takeLast(100)
            val text = json.encodeToString(toSave)
            preferences.edit().putString(KEY_ROOM_MESSAGES_PREFIX + roomId, text).apply()
        } catch (_: Exception) {}
    }

    override fun getRoomMessages(roomId: String): List<org.example.annochat.data.model.MessageDto> {
        val text = preferences.getString(KEY_ROOM_MESSAGES_PREFIX + roomId, null) ?: return emptyList()
        return try {
            val json = Json { ignoreUnknownKeys = true }
            json.decodeFromString<List<org.example.annochat.data.model.MessageDto>>(text)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val PREFS_NAME = "annochat_session_prefs"
        private const val KEY_ROOM_MESSAGES_PREFIX = "room_messages_snapshot_"
        private const val KEY_MY_ROOMS = "my_rooms_set"
        private const val KEY_MY_OWNED_ROOMS = "my_owned_rooms_set"
        private const val KEY_DELETED_ROOMS = "deleted_rooms_set"
        private const val KEY_SAVED_ROOM_PREFIX = "saved_room_snapshot_"
        private const val KEY_CUSTOM_ROOM_PREFIX = "custom_room_name_"
        private const val KEY_LOCALLY_PRIVATE_ROOM_PREFIX = "locally_private_room_"
        private const val KEY_DISMISSED_BROADCASTS = "dismissed_broadcasts_set"
        private const val KEY_SESSION_ID = "anonymous_session_id"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_ONBOARDING_INTENT = "onboarding_intent"
        private const val KEY_ONBOARDING_VIBE = "onboarding_vibe"
        private const val KEY_ONBOARDING_INTEREST = "onboarding_interest"
        private const val KEY_AHA_MOMENT_COMPLETED = "aha_moment_completed"
        private const val KEY_PREMIUM_SCREEN_SHOWN = "premium_screen_shown"
        private const val KEY_LIQUID_GLASS_ENABLED = "liquid_glass_enabled"
        private const val KEY_ROTATING_GLOW_ENABLED = "rotating_glow_enabled"
        private const val KEY_ANIMATIONS_ENABLED = "animations_enabled"
        private const val KEY_MESSAGE_NOTIFICATIONS_ENABLED = "message_notifications_enabled"
        private const val KEY_PRIVATE_CHAT_REQUESTS_ENABLED = "private_chat_requests_enabled"
        private const val KEY_NOTIFICATION_PREVIEW_ENABLED = "notification_preview_enabled"
        private const val KEY_APPEARANCE_SETTINGS = "appearance_settings_json"
        private const val KEY_CUSTOM_BACKEND_URL = "custom_backend_url"
        private const val KEY_SAVED_PUBLIC_ID = "saved_public_id"
        private const val KEY_SAVED_ANON_NAME = "saved_anon_name"
        private const val KEY_LAST_ACTIVE_ROOM_ID = "last_active_room_id"
        private const val KEY_LAST_ACTIVE_ROOM_NAME = "last_active_room_name"
        private const val KEY_LAST_ACTIVE_TAB = "last_active_tab"
        private const val KEY_DRAFT_PREFIX = "draft_room_"
        private const val KEY_WIDGET_ONLINE_COUNT = "widget_online_count_enabled"
        private const val KEY_WIDGET_IDENTITY = "widget_identity_enabled"
        private const val KEY_WIDGET_QUICK_MATCH = "widget_quick_match_enabled"
        private const val KEY_WIDGET_UPDATE_INTERVAL = "widget_update_interval_minutes"
    }
}

@Composable
actual fun rememberPlatformSessionStorage(): SessionStorage {
    val context = LocalContext.current.applicationContext
    return remember { AndroidPreferencesSessionStorage(context) }
}
