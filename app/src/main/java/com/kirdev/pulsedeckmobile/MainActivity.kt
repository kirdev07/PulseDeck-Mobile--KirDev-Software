package com.kirdev.pulsedeckmobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kirdev.pulsedeckmobile.core.theme.PulseDeskTheme
import com.kirdev.pulsedeckmobile.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private val app get() = application as PulseDeskApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        setContent { PulseDeskTheme { AppNavigation(app) } }
    }

    override fun onStart() {
        super.onStart()
        app.visible.value = true
        app.connection.setUiVisible(true)       // metrics only while the app is on screen
        app.connection.hold(UI)
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
