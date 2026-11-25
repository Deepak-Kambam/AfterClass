package org.example.annochat.widget

import org.example.annochat.data.model.WidgetDataSnapshot
import platform.Foundation.NSUserDefaults

actual fun syncPlatformWidgetData(snapshot: WidgetDataSnapshot) {
    try {
        val userDefaults = NSUserDefaults(suiteName = "group.org.example.annochat.widget")
        userDefaults.setInteger(snapshot.onlineCount.toLong(), forKey = "online_count")
        userDefaults.setObject(snapshot.mostActiveRoom, forKey = "most_active_room")
        userDefaults.setObject(snapshot.userAnonName ?: "Anonymous", forKey = "user_anon_name")
        userDefaults.setObject(snapshot.userPublicId ?: "", forKey = "user_public_id")
        userDefaults.setObject(snapshot.activityBars.joinToString(","), forKey = "activity_bars_csv")
        userDefaults.setInteger(snapshot.lastUpdatedEpochMs, forKey = "last_updated_epoch_ms")
        userDefaults.setBool(snapshot.showOnlineCount, forKey = "show_online_count")
        userDefaults.setBool(snapshot.showIdentity, forKey = "show_identity")
        userDefaults.setBool(snapshot.showQuickMatch, forKey = "show_quick_match")
        userDefaults.setInteger(snapshot.updateFrequencyMinutes.toLong(), forKey = "update_frequency_minutes")
        userDefaults.synchronize()
    } catch (_: Throwable) {
        // Silently ignore if widget sync fails on iOS
    }
}
