package org.example.annochat.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class IdentityWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            AfterClassWidgetUpdater.updateIdentityWidget(context, appWidgetManager, appWidgetId)
        }
        AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AfterClassWidgetUpdater.schedulePeriodicSync(context)
        AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(context)
    }
}
