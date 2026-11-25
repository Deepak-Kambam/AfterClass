package org.example.annochat.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class LiveActivityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            AfterClassWidgetUpdater.updateLiveActivityWidget(context, appWidgetManager, appWidgetId)
        }
        AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AfterClassWidgetUpdater.schedulePeriodicSync(context)
        AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
    }
}
