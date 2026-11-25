package org.example.annochat.widget

import android.content.Context
import android.content.Intent
import org.example.annochat.data.model.WidgetDataSnapshot

object AppContextHolder {
    @Volatile
    var context: Context? = null
        get() {
            if (field != null) return field
            return try {
                val activityThreadClass = Class.forName("android.app.ActivityThread")
                val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
                val app = currentApplicationMethod.invoke(null) as? android.app.Application
                field = app?.applicationContext
                field
            } catch (_: Throwable) {
                null
            }
        }
}

actual fun syncPlatformWidgetData(snapshot: WidgetDataSnapshot) {
    val ctx = AppContextHolder.context ?: return
    try {
        val prefs = ctx.getSharedPreferences("widget_shared_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("online_count", snapshot.onlineCount)
            .putString("most_active_room", snapshot.mostActiveRoom)
            .putString("user_anon_name", snapshot.userAnonName ?: "Anonymous")
            .putString("user_public_id", snapshot.userPublicId ?: "")
            .putString("activity_bars_csv", snapshot.activityBars.joinToString(","))
            .putLong("last_updated_epoch_ms", System.currentTimeMillis())
            .putBoolean("show_online_count", snapshot.showOnlineCount)
            .putBoolean("show_identity", snapshot.showIdentity)
            .putBoolean("show_quick_match", snapshot.showQuickMatch)
            .putInt("update_frequency_minutes", snapshot.updateFrequencyMinutes)
            .apply()

        val intent = Intent("org.example.annochat.widget.ACTION_WIDGET_UPDATE").apply {
            setPackage(ctx.packageName)
        }
        ctx.sendBroadcast(intent)
    } catch (_: Throwable) {
        // Silently ignore if widget sync fails
    }
}
