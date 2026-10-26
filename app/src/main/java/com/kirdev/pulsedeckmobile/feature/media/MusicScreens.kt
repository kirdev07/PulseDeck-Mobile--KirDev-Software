package com.kirdev.pulsedeckmobile.feature.media

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.kirdev.pulsedeckmobile.data.model.SystemVolume
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Numbers
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.ThinProgress
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.MediaSession
import com.kirdev.pulsedeckmobile.data.model.PlayStatus
import com.kirdev.pulsedeckmobile.feature.pulse.KiroXMotion
import kotlinx.coroutines.delay

/** Position that moves on the phone between PC updates; ticks only while playing. */
@Composable
private fun livePosition(session: MediaSession, tickMs: Long): Double {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(session.status, session.receivedAt) {
        now = SystemClock.elapsedRealtime()
        while (session.status == PlayStatus.PLAYING) {      // paused: no ticking at all
            delay(tickMs)
            now = SystemClock.elapsedRealtime()
        }
    }
    return session.positionAt(now)
}

/** Above the tabs, only while there is a track on the PC (playing or paused). */
@Composable
fun MiniPlayer(app: PulseDeskApp, onOpen: () -> Unit) {
    val session by app.connection.media.collectAsStateWithLifecycle()
    val cover by app.connection.cover.collectAsStateWithLifecycle()
    val track = session ?: return
    val position = livePosition(track, 1000)
    val playing = track.status == PlayStatus.PLAYING
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp).clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface).border(1.dp, Palette.Border, RoundedCornerShape(16.dp)).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(cover?.takeIf { it.first == track.coverKey }?.second, 42.dp, 10.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(listOf(track.artist, track.source).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Control(AppIcons.Prev, "Предыдущий", track.canPrev, 40.dp) { app.connection.send("prev") }
            Control(if (playing) AppIcons.Pause else AppIcons.Play, if (playing) "Пауза" else "Играть", true, 40.dp) {
                app.connection.send("toggle")
            }
            Control(AppIcons.Next, "Следующий", track.canNext, 40.dp) { app.connection.send("next") }
        }
        if (track.duration > 0) {
            ThinProgress((position / track.duration).toFloat(), Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp))
        }
    }
}

/** Screen 04: the full player. Only buttons the PC really supports (no shuffle, repeat, like). */
@Composable
fun MusicScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val session by app.connection.media.collectAsStateWithLifecycle()
    val cover by app.connection.cover.collectAsStateWithLifecycle()
    val volume by app.connection.volume.collectAsStateWithLifecycle()
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Музыка", onBack)
        val track = session
        if (track == null) {
            Placeholder("На ПК сейчас ничего не играет.")
            return
        }
        val position = livePosition(track, 500)
        val image = cover?.takeIf { it.first == track.coverKey }?.second
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                // Landscape: cover on the left, everything else on the right; no scrolling needed.
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(image, null, 22.dp, Modifier.fillMaxHeight().aspectRatio(1f))
                    Spacer(Modifier.width(28.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        PlayerControls(app, track, position, volume, prefs.animations)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Cover(image, null, 22.dp, Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(22.dp))
                    PlayerControls(app, track, position, volume, prefs.animations)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(app: PulseDeskApp, track: MediaSession, position: Double, volume: SystemVolume?, animations: Boolean) {
    val playing = track.status == PlayStatus.PLAYING
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth()) {
            Text("Сейчас играет на ПК" + if (track.source.isNotBlank()) " · ${track.source}" else "", fontSize = 12.sp, color = Palette.Text3)
            Text(track.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            if (track.artist.isNotBlank()) Text(track.artist, fontSize = 15.sp, color = Palette.Text2, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        if (track.duration > 0) {
            val shown = dragging?.let { it * track.duration } ?: position
            SeekBar((shown / track.duration).toFloat(), onDrag = { dragging = it }) {
                dragging = null
                app.connection.send("seek", (it * track.duration).coerceIn(0.0, track.duration))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Format.time(shown), style = Numbers.copy(color = Palette.Text3))
                Text("-" + Format.time(track.duration - shown), style = Numbers.copy(color = Palette.Text3))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp)) {
            Control(AppIcons.Prev, "Предыдущий трек", track.canPrev, 56.dp) { app.connection.send("prev") }
            Box(
                Modifier.size(72.dp).clip(CircleShape).border(1.5.dp, Palette.Text2, CircleShape)
                    .clickable { app.connection.send("toggle") }
                    .semantics { contentDescription = if (playing) "Пауза" else "Воспроизвести" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (playing) AppIcons.Pause else AppIcons.Play, null, tint = Palette.Text, modifier = Modifier.size(30.dp))
            }
            Control(AppIcons.Next, "Следующий трек", track.canNext, 56.dp) { app.connection.send("next") }
        }
        Spacer(Modifier.height(12.dp))
        // The wave moves only while playing and when animations are on.
        KiroXMotion(animate = playing && animations)
        Spacer(Modifier.height(12.dp))
        volume?.let { level ->
            var value by remember(level.level) { mutableFloatStateOf(level.level / 100f) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Volume, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f).semantics { contentDescription = "Громкость ПК" }) {
                    SeekBar(value, onDrag = { value = it }) {
                        value = it
                        app.connection.send("volume", (it * 100).toInt())
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("${(value * 100).toInt()}", style = Numbers.copy(color = Palette.Text3), modifier = Modifier.width(28.dp))
            }
            Text("Громкость ПК" + if (level.muted) " · звук выключен" else "", fontSize = 11.sp, color = Palette.Text3)
        }
    }
}

/** Thin bar with a 28 dp touch area: tap to jump, drag to scrub (sent when the finger lifts). */
@Composable
fun SeekBar(fraction: Float, onDrag: (Float) -> Unit, onSeek: (Float) -> Unit) {
    var current by remember { mutableFloatStateOf(0f) }
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { current = (it.x / size.width).coerceIn(0f, 1f); onDrag(current) },
                    onHorizontalDrag = { change, _ -> current = (change.position.x / size.width).coerceIn(0f, 1f); onDrag(current) },
                    onDragEnd = { onSeek(current) },
                    onDragCancel = { onSeek(current) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        ThinProgress(fraction, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Cover(image: Bitmap?, size: Dp?, radius: Dp, modifier: Modifier = Modifier) {
    Box(
        (if (size != null) modifier.size(size) else modifier).clip(RoundedCornerShape(radius)).background(Palette.Surface2),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(image.asImageBitmap(), contentDescription = "Обложка", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(AppIcons.Music, contentDescription = null, tint = Palette.Text3, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun Control(icon: ImageVector, label: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Palette.Text else Palette.Disabled,
            modifier = Modifier.size(if (size > 48.dp) 28.dp else 22.dp))
    }
}
