package com.kirdev.pulsedeckmobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.kirdev.pulsedeckmobile.core.theme.PulseDeskTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.core.ui.AppOverlays
import com.kirdev.pulsedeckmobile.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private val app get() = application as PulseDeskApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // No notification question at start: the music card is a media notification (shown without
        // the permission); warnings ask for it from their switch in Settings.
        applyOrientation(app.settings.values.value.rotate)        // before the first frame: no turn on start
        lifecycleScope.launch {
            app.settings.values.collect {
                com.kirdev.pulsedeckmobile.core.ui.Frames.apply(window, display, it.frameRate)
                applyOrientation(it.rotate)
            }
        }
        debugOpen(intent)
        setContent {
            val prefs by app.settings.values.collectAsStateWithLifecycle()
            PulseDeskTheme(prefs) {
                Box(Modifier.fillMaxSize()) {
                    AppNavigation(app)
                    AppOverlays(app)          // local network permission (Android 17), update prompt
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        debugOpen(intent)
    }

    private fun debugOpen(intent: android.content.Intent?) {
        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        intent?.getStringExtra("open")?.let { app.debugOpen.value = it }
        // Debug builds only, for screenshots of the landscape layout: --es orientation landscape|portrait|auto
        intent?.getStringExtra("orientation")?.let {
            debugOrientation = when (it) {
                "landscape" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                "portrait" -> android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> null
            }
            applyOrientation(app.settings.values.value.rotate)
        }
    }

    private var debugOrientation: Int? = null

    /** «Поворот экрана» off: always portrait; on: with the phone, if Android's auto-rotate is on. */
    private fun applyOrientation(rotate: Boolean) {
        val wanted = debugOrientation ?: if (rotate) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_USER
            else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (requestedOrientation != wanted) requestedOrientation = wanted
    }

    override fun onStart() {
        super.onStart()
        app.visible.value = true
        app.connection.setUiVisible(true)       // metrics only while the app is on screen
        app.connection.hold(UI)
        app.updater.checkAutomatically()       // a new version downloads at once and is offered
    }

    override fun onStop() {
        app.visible.value = false
        app.connection.setUiVisible(false)
        app.connection.release(UI)              // closes the link unless a track keeps the media card
        super.onStop()
    }

    private companion object {
        const val UI = "ui"
    }
}
