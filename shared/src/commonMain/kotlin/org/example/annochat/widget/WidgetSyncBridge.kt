package org.example.annochat.widget

import org.example.annochat.data.model.WidgetDataSnapshot

/**
 * Platform bridge to sync widget snapshot data to native platform storage:
 * - Android: SharedPreferences ('widget_shared_prefs') & AppWidget update broadcasts
 * - iOS: NSUserDefaults (App Group 'group.org.example.annochat.widget')
 */
expect fun syncPlatformWidgetData(snapshot: WidgetDataSnapshot)
