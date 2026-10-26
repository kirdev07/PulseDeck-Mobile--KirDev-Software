package com.kirdev.pulsedeckmobile.media

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder
import com.kirdev.pulsedeckmobile.MainActivity
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.R
import com.kirdev.pulsedeckmobile.data.model.MediaSession as PcTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The Android media card (shade, lock screen) for what plays on the PC.
 *
 * Lives only while there is a track (playing or paused). When the PC reports nothing playing
 * or the link is lost, it removes the card, releases the MediaSession and stops itself. It never
 * asks the PC anything: state arrives on the WebSocket, the service only mirrors it.
 */
class MediaNotificationService : Service() {
    private val app get() = application as PulseDeskApp
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var session: MediaSession
    private lateinit var notifications: NotificationManager
    private var shownKey: Any? = null
    private var metadataKey: Any? = null
    private var stopJob: Job? = null
    private var remoteVolume = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL, "Музыка на ПК", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Что играет на компьютере и кнопки управления"
                setShowBadge(false)
            })
        session = MediaSession(this, "PulseDesk").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() = command("toggle")
                override fun onPause() = command("toggle")
                override fun onSkipToNext() = command("next")
                override fun onSkipToPrevious() = command("prev")
                override fun onSeekTo(pos: Long) = command("seek", pos / 1000.0)
            })
            setSessionActivity(openApp())
        }
        app.connection.hold(HOLDER)
        scope.launch {
            combine(app.connection.state, app.connection.media, app.connection.cover, app.connection.volume,
                app.settings.values) { state, track, cover, volume, prefs ->
                Snapshot(playerState(state, track), track, cover, volume?.level, prefs.remoteVolume, prefs.mediaNotification)
            }.collectLatest(::render)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must be in the foreground within seconds of startForegroundService, with a real card.
        startForeground(ID, build(app.connection.media.value, PlayerState.PAUSED), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        when (intent?.action) {
            ACTION_TOGGLE -> command("toggle")
            ACTION_NEXT -> command("next")
            ACTION_PREV -> command("prev")
        }
        return START_NOT_STICKY       // never brought back by the system to poll a PC nobody is looking at
    }

    private data class Snapshot(
        val state: PlayerState, val track: PcTrack?, val cover: Pair<String, android.graphics.Bitmap>?,
        val volume: Int?, val remoteVolumeAllowed: Boolean, val enabled: Boolean,
    )

    private suspend fun render(s: Snapshot) {
        if (!s.enabled || !s.state.hasTrack || s.track == null) {
            // A short grace avoids the card blinking between tracks (the PC sends null for a moment).
            if (stopJob == null) stopJob = scope.launch {
                delay(if (s.state == PlayerState.DISCONNECTED || !s.enabled) 0 else GRACE_MS)
                stopSelf()
            }
            return
        }
        stopJob?.cancel()
        stopJob = null
        val track = s.track
        val coverBitmap = s.cover?.takeIf { it.first == track.coverKey }?.second

        val metaKey = listOf(track.title, track.artist, track.album, track.duration, coverBitmap)
        if (metaKey != metadataKey) {             // metadata only when the track or cover changes
            metadataKey = metaKey
            session.setMetadata(MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist.ifBlank { track.source })
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, (track.duration * 1000).toLong())
                .apply { if (coverBitmap != null) putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, coverBitmap) }
                .build())
        }
        val playing = s.state == PlayerState.PLAYING
        var actions = PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
        if (track.canNext) actions = actions or PlaybackState.ACTION_SKIP_TO_NEXT
        if (track.canPrev) actions = actions or PlaybackState.ACTION_SKIP_TO_PREVIOUS
        if (track.duration > 0) actions = actions or PlaybackState.ACTION_SEEK_TO
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(actions)
            .setState(if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                (track.position * 1000).toLong(), if (playing) 1f else 0f, track.receivedAt)
            .build())
        session.isActive = true
        updateVolumeKeys(playing && s.remoteVolumeAllowed, s.volume)

        val noteKey = listOf(track.title, track.artist, track.source, playing, coverBitmap, track.canNext, track.canPrev)
        if (noteKey != shownKey) {                // re-post only when something visible changed
            shownKey = noteKey
            notifications.notify(ID, build(track, s.state, coverBitmap))
        }
    }

    /** Phone volume keys move the PC volume only while a track plays (not when paused or idle). */
    private fun updateVolumeKeys(remote: Boolean, level: Int?) {
        if (remote && level != null) {
            if (!remoteVolume) {
                remoteVolume = true
                session.setPlaybackToRemote(object : VolumeProvider(VOLUME_CONTROL_ABSOLUTE, 100, level) {
                    override fun onSetVolumeTo(volume: Int) {
                        currentVolume = volume.coerceIn(0, 100)
                        command("volume", currentVolume)
                    }
                    override fun onAdjustVolume(direction: Int) {
                        currentVolume = (currentVolume + direction * 5).coerceIn(0, 100)
                        command("volume", currentVolume)
                    }
                })
            }
        } else if (remoteVolume) {
            remoteVolume = false
            session.setPlaybackToLocal(android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA).build())
        }
    }

    private fun build(track: PcTrack?, state: PlayerState, cover: android.graphics.Bitmap? = null): Notification {
        val playing = state == PlayerState.PLAYING
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(track?.title ?: "PulseDesk")
            .setContentText(listOfNotNull(track?.artist?.ifBlank { null }, track?.source?.ifBlank { null }).joinToString(" · "))
            .setLargeIcon(cover)
            .setContentIntent(openApp())
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setOngoing(playing)
        builder.addAction(action(android.R.drawable.ic_media_previous, "Предыдущий", ACTION_PREV))
        builder.addAction(if (playing) action(android.R.drawable.ic_media_pause, "Пауза", ACTION_TOGGLE)
                          else action(android.R.drawable.ic_media_play, "Играть", ACTION_TOGGLE))
        builder.addAction(action(android.R.drawable.ic_media_next, "Следующий", ACTION_NEXT))
        return builder.setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .build()
    }

    private fun action(icon: Int, title: String, what: String) = Notification.Action.Builder(
        Icon.createWithResource(this, icon), title,
        PendingIntent.getService(this, what.hashCode(), Intent(this, MediaNotificationService::class.java).setAction(what),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)).build()

    private fun openApp() = PendingIntent.getActivity(this, 0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Commands go only over a live link; with none they are dropped, not queued for later. */
    private fun command(action: String, value: Number? = null) {
        app.connection.send(action, value)
    }

    override fun onDestroy() {
        scope.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        notifications.cancel(ID)
        session.isActive = false
        session.release()
        app.connection.release(HOLDER)
        running = false
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "media"
        private const val ID = 7
        private const val HOLDER = "media"
        private const val GRACE_MS = 4_000L
        private const val ACTION_TOGGLE = "toggle"
        private const val ACTION_NEXT = "next"
        private const val ACTION_PREV = "prev"
        @Volatile var running = false
            private set

        /** Called by the app while it is on screen and a track exists; a no-op if already shown. */
        fun start(context: Context) {
            if (running) return
            running = true
            context.startForegroundService(Intent(context, MediaNotificationService::class.java))
        }
    }
}
