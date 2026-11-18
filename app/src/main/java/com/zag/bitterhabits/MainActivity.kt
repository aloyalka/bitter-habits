package com.zag.bitterhabits

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zag.bitterhabits.data.Store
import com.zag.bitterhabits.reminders.Reminders
import com.zag.bitterhabits.ui.BitterTheme
import com.zag.bitterhabits.ui.C
import com.zag.bitterhabits.ui.EditScreen
import com.zag.bitterhabits.ui.HomeScreen
import com.zag.bitterhabits.ui.SettingsScreen
import com.zag.bitterhabits.ui.SetupScreen

class MainActivity : ComponentActivity() {

    private val pendingUntick = mutableStateOf<Pair<String, Long>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        Store.init(this)
        Reminders.ensureChannel(this)
        readIntent(intent)

        setContent {
            BitterTheme {
                val data by Store.data.collectAsStateWithLifecycle()
                // "home", "settings", "new", or a habit id being edited
                var screen by rememberSaveable { mutableStateOf("home") }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(C.bg)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
                    when {
                        !data.setupDone -> SetupScreen(onWriteOwn = { screen = "new" })
                        screen == "settings" -> SettingsScreen(data, onBack = { screen = "home" }, onEdit = { screen = it })
                        screen == "new" -> EditScreen(data, null, onDone = { screen = "home" })
                        screen != "home" && data.habit(screen) != null -> EditScreen(data, screen, onDone = { screen = "home" })
                        else -> HomeScreen(
                            data,
                            onAdd = { screen = "new" },
                            onSettings = { screen = "settings" },
                            onEdit = { screen = it },
                            pendingUntick = pendingUntick.value,
                            onPendingHandled = { pendingUntick.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    /** The widget opens the app with these extras when unticking would break a perfect week. */
    private fun readIntent(i: Intent?) {
        val hid = i?.getStringExtra(EXTRA_HABIT) ?: return
        val day = i.getLongExtra(EXTRA_DAY, Long.MIN_VALUE)
        if (day != Long.MIN_VALUE) pendingUntick.value = hid to day
        i.removeExtra(EXTRA_HABIT)
    }

    companion object {
        const val EXTRA_HABIT = "untick_hid"
        const val EXTRA_DAY = "untick_day"
    }
}
