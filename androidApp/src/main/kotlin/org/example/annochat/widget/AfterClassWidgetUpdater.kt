package org.example.annochat.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.example.annochat.MainActivity
import org.example.annochat.R
import org.example.annochat.data.model.WIDGET_VIBE_QUOTES
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

object AfterClassWidgetUpdater {

    private const val PREFS_NAME = "widget_shared_prefs"
    private const val SESSION_PREFS_NAME = "annochat_session_prefs"

    val MOTIVATION_QUOTES = listOf(
        "“Strangers\ntoday,\nideas\ntomorrow.”",
        "“Quiet\nhours,\nreal\nthoughts.”",
        "“Find your\npeople in\nthe dark.”",
        "“Night owls\nbuild the\nfuture.”",
        "“Late hours,\nauthentic\nwords.”",
        "“Someone\nelse is\nstill awake.”",
        "“After hours,\nbelong\nsomewhere.”"
    )

    fun createDeepLinkPendingIntent(context: Context, actionUri: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(actionUri)
            putExtra("widget_action", actionUri)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun extractCleanPersona(rawName: String, rawTag: String): Pair<String, String> {
        val cleanName = rawName.substringBefore("#").trim().ifBlank { "Shoebill" }
        val tagCandidate = if (rawName.contains("#")) {
            rawName.substringAfter("#").trim()
        } else {
            rawTag
        }
        val cleanTag = tagCandidate.take(4).uppercase().ifBlank { "1C38" }
        return cleanName to cleanTag
    }

    /**
     * Records today's app visit to maintain real consecutive streak calculation.
     */
    fun recordDailyAppVisit(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val cal = Calendar.getInstance()
            val todayEpochDay = (cal.timeInMillis / (1000 * 60 * 60 * 24)).toInt()
            val lastVisitEpochDay = prefs.getInt("last_visit_epoch_day", 0)
            var currentStreak = prefs.getInt("user_streak_days", 1)

            if (lastVisitEpochDay == 0) {
                currentStreak = 1
            } else if (todayEpochDay == lastVisitEpochDay + 1) {
                currentStreak += 1
            } else if (todayEpochDay > lastVisitEpochDay + 1) {
                currentStreak = 1
            }

            prefs.edit()
                .putInt("last_visit_epoch_day", todayEpochDay)
                .putInt("user_streak_days", currentStreak)
                .apply()
        } catch (_: Throwable) {}
    }

    /**
     * Calculates the user's active daily streak.
     */
    fun getActiveStreak(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cal = Calendar.getInstance()
        val todayEpochDay = (cal.timeInMillis / (1000 * 60 * 60 * 24)).toInt()
        val lastVisitEpochDay = prefs.getInt("last_visit_epoch_day", 0)
        val storedStreak = prefs.getInt("user_streak_days", 1)

        return if (lastVisitEpochDay == 0) {
            1
        } else if (todayEpochDay > lastVisitEpochDay + 1) {
            1
        } else {
            storedStreak.coerceAtLeast(1)
        }
    }

    /**
     * Schedules a repeating 15-minute background sync alarm to keep widgets updated when app is killed.
     */
    fun schedulePeriodicSync(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
                action = "org.example.annochat.widget.ACTION_PERIODIC_SYNC"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val interval = 15 * 60 * 1000L
            val triggerAt = System.currentTimeMillis() + interval
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                triggerAt,
                interval,
                pendingIntent
            )
        } catch (_: Throwable) {}
    }

    /**
     * Queries the live backend in the background to fetch real-time online user count
     * even when the main app process is completely killed from multitasking.
     */
    fun fetchLiveWidgetStatusInBackground(context: Context) {
        Thread {
            try {
                val sessionPrefs = context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE)
                val customUrl = sessionPrefs.getString("custom_backend_url", null)
                val baseUrl = customUrl?.ifBlank { null } ?: "https://api.afterclass.bond"
                val cleanBaseUrl = baseUrl.trimEnd('/')

                val url = URL("$cleanBaseUrl/api/online_users")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("Accept", "application/json")
                }

                if (connection.responseCode == 200) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()

                    val json = JSONObject(body)
                    val usersArray = json.optJSONArray("users")
                    val freshOnlineCount = usersArray?.length() ?: 0

                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val currentStored = prefs.getInt("online_count", -1)

                    prefs.edit()
                        .putInt("online_count", freshOnlineCount)
                        .putLong("last_updated_epoch_ms", System.currentTimeMillis())
                        .apply()

                    if (freshOnlineCount != currentStored) {
                        val appWidgetManager = AppWidgetManager.getInstance(context)

                        // Update Time & Vibe widgets
                        val timeVibeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TimeVibeWidgetProvider::class.java))
                        for (id in timeVibeIds) {
                            val views = RemoteViews(context.packageName, R.layout.widget_time_vibe)
                            views.setTextViewText(R.id.tv_widget_pill_count, "$freshOnlineCount")
                            appWidgetManager.partiallyUpdateAppWidget(id, views)
                        }
                    }
                }
            } catch (_: Throwable) {
                // Ignore network errors gracefully when offline
            }
        }.start()
    }

    fun updateTimeVibeWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val views = RemoteViews(context.packageName, R.layout.widget_time_vibe)

        val pendingIntent = createDeepLinkPendingIntent(context, "afterclass://widget?action=open", 1001)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Select rotating nocturnal quote based on current hour/minute
        val quoteIndex = ((System.currentTimeMillis() / (1000 * 60 * 15)) % WIDGET_VIBE_QUOTES.size).toInt()
        val quote = WIDGET_VIBE_QUOTES[quoteIndex]
        views.setTextViewText(R.id.tv_widget_quote, "“$quote”")

        // Real live online count - never fall back to static 312
        val onlineCount = prefs.getInt("online_count", 0)
        views.setTextViewText(R.id.tv_widget_pill_count, "$onlineCount")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateLiveActivityWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val views = RemoteViews(context.packageName, R.layout.widget_live_activity)

        // Deep links:
        val profilePendingIntent = createDeepLinkPendingIntent(context, "afterclass://profile", 1002)
        views.setOnClickPendingIntent(R.id.card_widget_streak, profilePendingIntent)
        views.setOnClickPendingIntent(R.id.card_widget_streak_sub, profilePendingIntent)

        val roomsPendingIntent = createDeepLinkPendingIntent(context, "afterclass://rooms", 1003)
        views.setOnClickPendingIntent(R.id.card_widget_motivation, roomsPendingIntent)

        val defaultPendingIntent = createDeepLinkPendingIntent(context, "afterclass://widget?action=open", 1004)
        views.setOnClickPendingIntent(R.id.widget_root, defaultPendingIntent)

        // Dynamic Streak Count
        val streakCount = getActiveStreak(context)
        views.setTextViewText(R.id.tv_widget_streak_count, streakCount.toString())

        // Dynamic 7-day consistency dots based on current day of week
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Convert to Monday = 0, Tuesday = 1, ..., Sunday = 6
        val todayIndex = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2

        val dotIds = intArrayOf(
            R.id.dot_day_1, R.id.dot_day_2, R.id.dot_day_3,
            R.id.dot_day_4, R.id.dot_day_5, R.id.dot_day_6, R.id.dot_day_7
        )

        for (i in dotIds.indices) {
            val isActive = i <= todayIndex && (todayIndex - i) < streakCount
            val res = if (isActive) R.drawable.bg_widget_dot_active else R.drawable.bg_widget_dot_inactive
            views.setImageViewResource(dotIds[i], res)
        }

        // Daily rotating motivation quote: A new quote every day
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val quote = MOTIVATION_QUOTES[dayOfYear % MOTIVATION_QUOTES.size]
        views.setTextViewText(R.id.tv_widget_motivation_quote, quote)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateIdentityWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessionPrefs = context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        val views = RemoteViews(context.packageName, R.layout.widget_identity)

        val pendingIntent = createDeepLinkPendingIntent(context, "afterclass://widget?action=profile", 1005)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        val rawName = prefs.getString("user_anon_name", null)
            ?: sessionPrefs.getString("saved_anon_name", "Shoebill")
            ?: "Shoebill"
        val rawPublicId = prefs.getString("user_public_id", null)
            ?: sessionPrefs.getString("saved_public_id", "1C38")
            ?: "1C38"
        val (cleanName, cleanTag) = extractCleanPersona(rawName, rawPublicId)

        views.setTextViewText(R.id.tv_widget_identity_name, cleanName)
        views.setTextViewText(R.id.tv_widget_identity_tag, "#$cleanTag")

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val timeVibeIds = appWidgetManager.getAppWidgetIds(ComponentName(context, TimeVibeWidgetProvider::class.java))
        for (id in timeVibeIds) {
            updateTimeVibeWidget(context, appWidgetManager, id)
        }

        val liveActivityIds = appWidgetManager.getAppWidgetIds(ComponentName(context, LiveActivityWidgetProvider::class.java))
        for (id in liveActivityIds) {
            updateLiveActivityWidget(context, appWidgetManager, id)
        }

        val identityIds = appWidgetManager.getAppWidgetIds(ComponentName(context, IdentityWidgetProvider::class.java))
        for (id in identityIds) {
            updateIdentityWidget(context, appWidgetManager, id)
        }
    }
}

class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                AfterClassWidgetUpdater.schedulePeriodicSync(context)
                AfterClassWidgetUpdater.updateAllWidgets(context)
                AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
            }
            "org.example.annochat.widget.ACTION_PERIODIC_SYNC" -> {
                AfterClassWidgetUpdater.updateAllWidgets(context)
                AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
            }
            else -> {
                AfterClassWidgetUpdater.updateAllWidgets(context)
                AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
            }
        }
    }
}
