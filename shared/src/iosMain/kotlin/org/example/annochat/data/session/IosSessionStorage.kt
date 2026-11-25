package org.example.annochat.data.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.serialization.json.Json
import org.example.annochat.data.model.OnboardingState
import org.example.annochat.data.model.RoomDto
import platform.Foundation.NSUserDefaults

class IosUserDefaultsSessionStorage(
    private val userDefaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : SessionStorage {

    private var cachedSessionId: String? = null

    override fun getOrCreateSessionId(): String {
        cachedSessionId?.let { return it }
        val stored = userDefaults.stringForKey(KEY_SESSION_ID)
        if (!stored.isNullOrBlank()) {
            cachedSessionId = stored
            return stored
        }
        val newSessionId = InMemorySessionStorage.generateAnonymousSessionId()
        saveSessionId(newSessionId)
        return newSessionId
    }

    override fun getSessionId(): String? {
        return cachedSessionId ?: userDefaults.stringForKey(KEY_SESSION_ID).also { cachedSessionId = it }
    }

    override fun saveSessionId(sessionId: String) {
        cachedSessionId = sessionId
        userDefaults.setObject(sessionId, KEY_SESSION_ID)
        userDefaults.synchronize()
    }

    override fun clearSession() {
        cachedSessionId = null
        userDefaults.removeObjectForKey(KEY_SESSION_ID)
        userDefaults.synchronize()
    }

    override fun isOnboardingCompleted(): Boolean {
        return userDefaults.boolForKey(KEY_ONBOARDING_COMPLETED)
    }

    override fun setOnboardingCompleted(completed: Boolean) {
        userDefaults.setBool(completed, KEY_ONBOARDING_COMPLETED)
        userDefaults.synchronize()
    }

    override fun getOnboardingIntent(): String? {
        val stored = userDefaults.stringForKey(KEY_ONBOARDING_INTENT)
        return stored?.ifBlank { null }
    }

    override fun setOnboardingIntent(intent: String?) {
        val trimmed = intent?.trim()?.ifBlank { null }
        if (trimmed == null) {
            userDefaults.removeObjectForKey(KEY_ONBOARDING_INTENT)
        } else {
            userDefaults.setObject(trimmed, KEY_ONBOARDING_INTENT)
        }
        userDefaults.synchronize()
    }

    override fun getOnboardingVibe(): String? {
        val stored = userDefaults.stringForKey(KEY_ONBOARDING_VIBE)
        return stored?.ifBlank { null }
    }

    override fun setOnboardingVibe(vibe: String?) {
        val trimmed = vibe?.trim()?.ifBlank { null }
        if (trimmed == null) {
            userDefaults.removeObjectForKey(KEY_ONBOARDING_VIBE)
        } else {
            userDefaults.setObject(trimmed, KEY_ONBOARDING_VIBE)
        }
        userDefaults.synchronize()
    }

    override fun getOnboardingInterest(): String? {
        val stored = userDefaults.stringForKey(KEY_ONBOARDING_INTEREST)
        return stored?.ifBlank { null }
    }

    override fun setOnboardingInterest(interest: String?) {
        val trimmed = interest?.trim()?.ifBlank { null }
        if (trimmed == null) {
            userDefaults.removeObjectForKey(KEY_ONBOARDING_INTEREST)
        } else {
            userDefaults.setObject(trimmed, KEY_ONBOARDING_INTEREST)
        }
        userDefaults.synchronize()
    }

    override fun isAhaMomentCompleted(): Boolean {
        return userDefaults.boolForKey(KEY_AHA_MOMENT_COMPLETED)
    }

    override fun setAhaMomentCompleted(completed: Boolean) {
        userDefaults.setBool(completed, KEY_AHA_MOMENT_COMPLETED)
        userDefaults.synchronize()
    }

    override fun isPremiumScreenShown(): Boolean {
        return userDefaults.boolForKey(KEY_PREMIUM_SCREEN_SHOWN)
    }

    override fun setPremiumScreenShown(shown: Boolean) {
        userDefaults.setBool(shown, KEY_PREMIUM_SCREEN_SHOWN)
        userDefaults.synchronize()
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
        if (userDefaults.objectForKey(KEY_LIQUID_GLASS_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_LIQUID_GLASS_ENABLED)
    }

    override fun setLiquidGlassEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_LIQUID_GLASS_ENABLED)
        userDefaults.synchronize()
    }

    override fun isRotatingGlowEnabled(): Boolean {
        if (userDefaults.objectForKey(KEY_ROTATING_GLOW_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_ROTATING_GLOW_ENABLED)
    }

    override fun setRotatingGlowEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_ROTATING_GLOW_ENABLED)
        userDefaults.synchronize()
    }

    override fun isAnimationsEnabled(): Boolean {
        if (userDefaults.objectForKey(KEY_ANIMATIONS_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_ANIMATIONS_ENABLED)
    }

    override fun setAnimationsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_ANIMATIONS_ENABLED)
        userDefaults.synchronize()
    }

    override fun isMessageNotificationsEnabled(): Boolean {
        if (userDefaults.objectForKey(KEY_MESSAGE_NOTIFICATIONS_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_MESSAGE_NOTIFICATIONS_ENABLED)
    }

    override fun setMessageNotificationsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_MESSAGE_NOTIFICATIONS_ENABLED)
        userDefaults.synchronize()
    }

    override fun isPrivateChatRequestsEnabled(): Boolean {
        if (userDefaults.objectForKey(KEY_PRIVATE_CHAT_REQUESTS_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_PRIVATE_CHAT_REQUESTS_ENABLED)
    }

    override fun setPrivateChatRequestsEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_PRIVATE_CHAT_REQUESTS_ENABLED)
        userDefaults.synchronize()
    }

    override fun isNotificationPreviewEnabled(): Boolean {
        if (userDefaults.objectForKey(KEY_NOTIFICATION_PREVIEW_ENABLED) == null) {
            return true // Default enabled
        }
        return userDefaults.boolForKey(KEY_NOTIFICATION_PREVIEW_ENABLED)
    }

    override fun setNotificationPreviewEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_NOTIFICATION_PREVIEW_ENABLED)
        userDefaults.synchronize()
    }

    override fun getAppearanceSettings(): org.example.annochat.data.model.AppearanceSettings {
        val json = userDefaults.stringForKey(KEY_APPEARANCE_SETTINGS)
        return org.example.annochat.data.model.AppearanceSettings.fromJson(json)
    }

    override fun saveAppearanceSettings(settings: org.example.annochat.data.model.AppearanceSettings) {
        val json = org.example.annochat.data.model.AppearanceSettings.toJson(settings)
        userDefaults.setObject(json, KEY_APPEARANCE_SETTINGS)
        userDefaults.setBool(settings.afterglowEnabled, KEY_ROTATING_GLOW_ENABLED)
        userDefaults.setBool(settings.animationsEnabled, KEY_ANIMATIONS_ENABLED)
        userDefaults.synchronize()
    }

    override fun restoreDefaultAppearance() {
        userDefaults.removeObjectForKey(KEY_APPEARANCE_SETTINGS)
        userDefaults.setBool(true, KEY_ROTATING_GLOW_ENABLED)
        userDefaults.setBool(true, KEY_ANIMATIONS_ENABLED)
        userDefaults.synchronize()
    }

    override fun getCustomBackendUrl(): String? {
        val stored = userDefaults.stringForKey(KEY_CUSTOM_BACKEND_URL)
        return stored?.ifBlank { null }
    }


    override fun setCustomBackendUrl(url: String?) {
        val trimmed = url?.trim()?.ifBlank { null }
        if (trimmed == null) {
            userDefaults.removeObjectForKey(KEY_CUSTOM_BACKEND_URL)
        } else {
            userDefaults.setObject(trimmed, KEY_CUSTOM_BACKEND_URL)
        }
        userDefaults.synchronize()
    }

    override fun getSavedPublicId(): String? {
        val stored = userDefaults.stringForKey(KEY_SAVED_PUBLIC_ID)
        return stored?.ifBlank { null }
    }

    override fun getSavedAnonName(): String? {
        val stored = userDefaults.stringForKey(KEY_SAVED_ANON_NAME)
        return stored?.ifBlank { null }
    }

    override fun saveIdentity(publicId: String, anonName: String) {
        userDefaults.setObject(publicId, KEY_SAVED_PUBLIC_ID)
        userDefaults.setObject(anonName, KEY_SAVED_ANON_NAME)
        userDefaults.synchronize()
    }

    override fun getLastActiveRoomId(): String? {
        val stored = userDefaults.stringForKey(KEY_LAST_ACTIVE_ROOM_ID)
        return stored?.ifBlank { null }
    }

    override fun getLastActiveRoomName(): String? {
        val stored = userDefaults.stringForKey(KEY_LAST_ACTIVE_ROOM_NAME)
        return stored?.ifBlank { null }
    }

    override fun saveLastActiveRoom(roomId: String?, roomName: String?) {
        if (roomId.isNullOrBlank()) {
            userDefaults.removeObjectForKey(KEY_LAST_ACTIVE_ROOM_ID)
            userDefaults.removeObjectForKey(KEY_LAST_ACTIVE_ROOM_NAME)
        } else {
            userDefaults.setObject(roomId, KEY_LAST_ACTIVE_ROOM_ID)
            userDefaults.setObject(roomName ?: roomId, KEY_LAST_ACTIVE_ROOM_NAME)
        }
        userDefaults.synchronize()
    }

    override fun getLastActiveTab(): Int {
        return userDefaults.integerForKey(KEY_LAST_ACTIVE_TAB).toInt()
    }

    override fun saveLastActiveTab(tab: Int) {
        userDefaults.setInteger(tab.toLong(), KEY_LAST_ACTIVE_TAB)
        userDefaults.synchronize()
    }

    override fun getDraftMessage(roomId: String): String? {
        val stored = userDefaults.stringForKey(KEY_DRAFT_PREFIX + roomId)
        return stored?.ifBlank { null }
    }

    override fun saveDraftMessage(roomId: String, draft: String?) {
        val key = KEY_DRAFT_PREFIX + roomId
        val trimmed = draft?.trim()?.ifBlank { null }
        if (trimmed == null) {
            userDefaults.removeObjectForKey(key)
        } else {
            userDefaults.setObject(trimmed, key)
        }
        userDefaults.synchronize()
    }

    override fun isWidgetOnlineCountEnabled(): Boolean {
        return if (userDefaults.objectForKey(KEY_WIDGET_ONLINE_COUNT) != null) {
            userDefaults.boolForKey(KEY_WIDGET_ONLINE_COUNT)
        } else true
    }

    override fun setWidgetOnlineCountEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_WIDGET_ONLINE_COUNT)
        userDefaults.synchronize()
    }

    override fun isWidgetIdentityEnabled(): Boolean {
        return if (userDefaults.objectForKey(KEY_WIDGET_IDENTITY) != null) {
            userDefaults.boolForKey(KEY_WIDGET_IDENTITY)
        } else true
    }

    override fun setWidgetIdentityEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_WIDGET_IDENTITY)
        userDefaults.synchronize()
    }

    override fun isWidgetQuickMatchEnabled(): Boolean {
        return if (userDefaults.objectForKey(KEY_WIDGET_QUICK_MATCH) != null) {
            userDefaults.boolForKey(KEY_WIDGET_QUICK_MATCH)
        } else true
    }

    override fun setWidgetQuickMatchEnabled(enabled: Boolean) {
        userDefaults.setBool(enabled, KEY_WIDGET_QUICK_MATCH)
        userDefaults.synchronize()
    }

    override fun getWidgetUpdateIntervalMinutes(): Int {
        val stored = userDefaults.integerForKey(KEY_WIDGET_UPDATE_INTERVAL).toInt()
        return if (stored > 0) stored else 30
    }

    override fun setWidgetUpdateIntervalMinutes(minutes: Int) {
        userDefaults.setInteger(minutes.toLong(), KEY_WIDGET_UPDATE_INTERVAL)
        userDefaults.synchronize()
    }

    override fun isBroadcastDismissed(broadcastId: String): Boolean {
        val list = userDefaults.stringArrayForKey(KEY_DISMISSED_BROADCASTS)?.filterIsInstance<String>() ?: return false
        return list.contains(broadcastId)
    }

    override fun markBroadcastDismissed(broadcastId: String) {
        val current = (userDefaults.stringArrayForKey(KEY_DISMISSED_BROADCASTS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
        if (!current.contains(broadcastId)) {
            current.add(broadcastId)
            userDefaults.setObject(current, KEY_DISMISSED_BROADCASTS)
            userDefaults.synchronize()
        }
    }

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override fun addMyRoom(roomId: String, isOwner: Boolean, isPrivate: Boolean) {
        if (roomId.isBlank()) return
        val currentRooms = (userDefaults.stringArrayForKey(KEY_MY_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
        if (!currentRooms.contains(roomId)) {
            currentRooms.add(roomId)
            userDefaults.setObject(currentRooms, KEY_MY_ROOMS)
        }
        if (isOwner) {
            val currentOwned = (userDefaults.stringArrayForKey(KEY_MY_OWNED_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
            if (!currentOwned.contains(roomId)) {
                currentOwned.add(roomId)
                userDefaults.setObject(currentOwned, KEY_MY_OWNED_ROOMS)
            }
        }
        if (isPrivate) {
            userDefaults.setBool(true, KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId)
        }
        userDefaults.synchronize()
    }

    override fun removeMyRoom(roomId: String) {
        val currentRooms = (userDefaults.stringArrayForKey(KEY_MY_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
        currentRooms.remove(roomId)
        userDefaults.setObject(currentRooms, KEY_MY_ROOMS)

        val currentOwned = (userDefaults.stringArrayForKey(KEY_MY_OWNED_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
        currentOwned.remove(roomId)
        userDefaults.setObject(currentOwned, KEY_MY_OWNED_ROOMS)

        userDefaults.removeObjectForKey(KEY_SAVED_ROOM_PREFIX + roomId)
        userDefaults.removeObjectForKey(KEY_LOCALLY_PRIVATE_ROOM_PREFIX + roomId)
        userDefaults.synchronize()
    }

    override fun markRoomDeleted(roomId: String) {
        removeMyRoom(roomId)
        val currentDeleted = (userDefaults.stringArrayForKey(KEY_DELETED_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toMutableList()
        if (!currentDeleted.contains(roomId)) {
            currentDeleted.add(roomId)
            userDefaults.setObject(currentDeleted, KEY_DELETED_ROOMS)
            userDefaults.synchronize()
        }
    }

    override fun isRoomDeleted(roomId: String): Boolean {
        val list = userDefaults.stringArrayForKey(KEY_DELETED_ROOMS)?.filterIsInstance<String>() ?: return false
        return list.contains(roomId)
    }

    override fun isMyRoom(roomId: String): Boolean {
        val list = userDefaults.stringArrayForKey(KEY_MY_ROOMS)?.filterIsInstance<String>() ?: return false
        return list.contains(roomId)
    }

    override fun isMyOwnedRoom(roomId: String): Boolean {
        val list = userDefaults.stringArrayForKey(KEY_MY_OWNED_ROOMS)?.filterIsInstance<String>() ?: return false
        return list.contains(roomId)
    }

    override fun getMyRoomIds(): Set<String> {
        return (userDefaults.stringArrayForKey(KEY_MY_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toSet()
    }

    override fun getMyOwnedRoomIds(): Set<String> {
        return (userDefaults.stringArrayForKey(KEY_MY_OWNED_ROOMS)?.filterIsInstance<String>() ?: emptyList()).toSet()
    }

    override fun saveRoomSnapshot(room: RoomDto) {
        if (room.roomId.isBlank()) return
        addMyRoom(room.roomId, isOwner = room.isOwner, isPrivate = !room.isPublic || room.isPrivateGroup)
        try {
            val raw = jsonSerializer.encodeToString(RoomDto.serializer(), room)
            userDefaults.setObject(raw, KEY_SAVED_ROOM_PREFIX + room.roomId)
            userDefaults.synchronize()
        } catch (_: Exception) {}
    }

    override fun getSavedRoom(roomId: String): RoomDto? {
        val raw = userDefaults.stringForKey(KEY_SAVED_ROOM_PREFIX + roomId) ?: return null
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
            val toSave = messages.takeLast(100)
            val raw = jsonSerializer.encodeToString(toSave)
            userDefaults.setObject(raw, KEY_ROOM_MESSAGES_PREFIX + roomId)
            userDefaults.synchronize()
        } catch (_: Exception) {}
    }

    override fun getRoomMessages(roomId: String): List<org.example.annochat.data.model.MessageDto> {
        val raw = userDefaults.stringForKey(KEY_ROOM_MESSAGES_PREFIX + roomId) ?: return emptyList()
        return try {
            jsonSerializer.decodeFromString<List<org.example.annochat.data.model.MessageDto>>(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_ROOM_MESSAGES_PREFIX = "room_messages_snapshot_"
        private const val KEY_MY_ROOMS = "my_rooms_list"
        private const val KEY_MY_OWNED_ROOMS = "my_owned_rooms_list"
        private const val KEY_DELETED_ROOMS = "deleted_rooms_list"
        private const val KEY_SAVED_ROOM_PREFIX = "saved_room_snapshot_"
        private const val KEY_CUSTOM_ROOM_PREFIX = "custom_room_name_"
        private const val KEY_LOCALLY_PRIVATE_ROOM_PREFIX = "locally_private_room_"
        private const val KEY_DISMISSED_BROADCASTS = "dismissed_broadcasts_list"
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
    return remember { IosUserDefaultsSessionStorage() }
}
