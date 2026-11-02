package com.example.wordstore

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.wordstore.reminders.ReminderNotifier
import com.example.wordstore.ui.components.LocalPronunciationPlayer
import com.example.wordstore.ui.navigation.WordMintApp
import com.example.wordstore.ui.theme.WordMintTheme

class MainActivity : ComponentActivity() {

    private var openStudyRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as WordStoreApplication).container
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle()
            WordMintTheme(themeMode = settings.themeMode) {
                val colors = WordMintTheme.colors
                DisposableEffect(colors.isDark) {
                    val transparent = android.graphics.Color.TRANSPARENT
                    val style = if (colors.isDark) {
                        SystemBarStyle.dark(transparent)
                    } else {
                        SystemBarStyle.light(transparent, colors.ink.toArgb())
                    }
                    enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                    onDispose { }
                }
                CompositionLocalProvider(LocalPronunciationPlayer provides container.pronunciation) {
                    WordMintApp(
                        container = container,
                        onboardingDone = settings.onboardingDone,
                        openStudyRequest = openStudyRequest,
                        onStudyRequestHandled = { openStudyRequest = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ReminderNotifier.ACTION_OPEN_STUDY) openStudyRequest = true
    }
}
