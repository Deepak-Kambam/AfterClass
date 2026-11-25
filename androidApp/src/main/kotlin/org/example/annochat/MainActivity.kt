package org.example.annochat

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.annochat.data.repository.ChatRepository
import org.example.annochat.widget.AppContextHolder

class MainActivity : ComponentActivity() {
    private var repositoryRef: ChatRepository? = null
    private var deepLinkUri by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        // Initialize platform environment configuration (fails closed to false/release if omitted)
        PlatformConfig.isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        AppContextHolder.context = applicationContext

        deepLinkUri = intent?.dataString ?: intent?.getStringExtra("widget_action")

        // Track daily visit for streak and initialize background widget periodic sync
        org.example.annochat.widget.AfterClassWidgetUpdater.recordDailyAppVisit(applicationContext)
        org.example.annochat.widget.AfterClassWidgetUpdater.schedulePeriodicSync(applicationContext)
        org.example.annochat.widget.AfterClassWidgetUpdater.updateAllWidgets(applicationContext)
        org.example.annochat.widget.AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(applicationContext)

        setContent {
            App(
                deepLinkUri = deepLinkUri,
                onRepositoryCreated = { repositoryRef = it }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkUri = intent.dataString ?: intent.getStringExtra("widget_action")
    }

    override fun onDestroy() {
        super.onDestroy()
        repositoryRef?.disconnect()
        repositoryRef = null
        org.example.annochat.widget.AfterClassWidgetUpdater.updateAllWidgets(applicationContext)
        org.example.annochat.widget.AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(applicationContext)
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN && isFinishing) {
            repositoryRef?.disconnect()
            org.example.annochat.widget.AfterClassWidgetUpdater.updateAllWidgets(applicationContext)
            org.example.annochat.widget.AfterClassWidgetUpdater.fetchLiveWidgetStatusInBackground(applicationContext)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}