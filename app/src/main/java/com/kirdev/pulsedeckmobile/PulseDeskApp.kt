package com.kirdev.pulsedeckmobile

import android.app.Application
import com.kirdev.pulsedeckmobile.core.theme.Accents
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.data.local.AppSettings
import com.kirdev.pulsedeckmobile.data.local.PairingStore
import com.kirdev.pulsedeckmobile.data.remote.PcApi
import com.kirdev.pulsedeckmobile.data.remote.PcConnection
import com.kirdev.pulsedeckmobile.data.update.Updater
import com.kirdev.pulsedeckmobile.feature.notifications.EventsFeed
import com.kirdev.pulsedeckmobile.media.MediaNotificationService
import com.kirdev.pulsedeckmobile.media.hasTrack
import com.kirdev.pulsedeckmobile.media.playerState
import com.kirdev.pulsedeckmobile.monitoring.MetricsHistory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Single instances shared by all screens (no DI framework: the graph is this small). */
class PulseDeskApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var pairing: PairingStore
    lateinit var settings: AppSettings
    lateinit var connection: PcConnection
    lateinit var api: PcApi
    lateinit var history: MetricsHistory
    lateinit var events: EventsFeed
    lateinit var updater: Updater

    /** The activity is started (on screen): set by MainActivity.onStart/onStop. */
    val visible = MutableStateFlow(false)

    override fun onCreate() {
        super.onCreate()
        pairing = PairingStore(this)
        settings = AppSettings(this)
        connection = PcConnection(pairing, scope)
        api = PcApi(connection.endpoint)
        history = MetricsHistory(connection.metrics, scope)
        events = EventsFeed(api)
        updater = Updater(this, scope)
        updater.checkAutomatically()

        scope.launch {
            settings.values.collect {
                Palette.theme = it.theme
                Palette.accent = Accents.getOrElse(it.accent) { Accents.first() }
                connection.setInterval(it.intervalSeconds)
            }
        }
        // Media card only for a real track, started while the app is on screen (Android forbids
        // starting it from the background). The service removes itself when the track is gone.
        scope.launch {
            combine(connection.state, connection.media, settings.values, visible) { state, media, prefs, shown ->
                shown && prefs.mediaNotification && playerState(state, media).hasTrack
            }.distinctUntilChanged().collect { wanted ->
                if (wanted) MediaNotificationService.start(this@PulseDeskApp)
            }
        }
    }
}
