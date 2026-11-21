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
import com.kirdev.pulsedeckmobile.data.model.Player
import androidx.compose.animation.core.animateFloat
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
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(20.dp))
            .background(Palette.Surface2).clickable(onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Cover(cover?.takeIf { it.first == track.coverKey }?.second, 44.dp, 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Text(listOf(track.artist, track.source).filter { it.isNotBlank() }.joinToString(" · "),
                    fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Control(AppIcons.Prev, "Предыдущий", track.canPrev, 40.dp) { app.connection.send("prev") }
            Box(Modifier.padding(horizontal = 4.dp).size(44.dp).clip(RoundedCornerShape(14.dp)).background(Palette.accent)
                .clickable { app.connection.send("toggle") }.semantics { contentDescription = if (playing) "Пауза" else "Играть" },
                contentAlignment = Alignment.Center) {
                Icon(if (playing) AppIcons.Pause else AppIcons.Play, null, tint = Palette.onAccent, modifier = Modifier.size(20.dp))
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
                        PlayerControls(app, track, position, volume, prefs.animations, prefs.waveSeek)
                    }
                }
            } else {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Cover(image, null, 32.dp, Modifier.widthIn(max = 340.dp).fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(22.dp))
                    PlayerControls(app, track, position, volume, prefs.animations, prefs.waveSeek)
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PlayerControls(
    app: PulseDeskApp, track: MediaSession, position: Double, volume: SystemVolume?, animations: Boolean, wave: Boolean,
) {
    val playing = track.status == PlayStatus.PLAYING
    var dragging by remember { mutableStateOf<Float?>(null) }
    val players by app.connection.players.collectAsStateWithLifecycle()
    val source by app.connection.source.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playing && animations && !com.kirdev.pulsedeckmobile.core.ui.Device.lite) {
                    NowPlayingBars(Modifier.size(14.dp, 12.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text("Сейчас играет на ПК" + if (track.source.isNotBlank()) " · ${track.source}" else "", fontSize = 12.sp, color = Palette.Text3)
            }
            Text(track.title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            if (track.artist.isNotBlank()) Text(track.artist, fontSize = 15.sp, color = Palette.Text2, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(14.dp))
        if (track.duration > 0) {
            val shown = dragging?.let { it * track.duration } ?: position
            SeekBar((shown / track.duration).toFloat(), onDrag = { dragging = it },
                wave = wave, playing = playing, motion = animations) {
                dragging = null
                app.connection.send("seek", (it * track.duration).coerceIn(0.0, track.duration))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(Format.time(shown), style = Numbers.copy(color = Palette.Text3))
                Text("-" + Format.time(track.duration - shown), style = Numbers.copy(color = Palette.Text3))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TonalControl(AppIcons.Prev, "Предыдущий трек", track.canPrev) { app.connection.send("prev") }
            Box(
                Modifier.size(112.dp, 80.dp).clip(RoundedCornerShape(28.dp)).background(Palette.accent)
                    .clickable { app.connection.send("toggle") }
                    .semantics { contentDescription = if (playing) "Пауза" else "Воспроизвести" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (playing) AppIcons.Pause else AppIcons.Play, null, tint = Palette.onAccent, modifier = Modifier.size(34.dp))
            }
            TonalControl(AppIcons.Next, "Следующий трек", track.canNext) { app.connection.send("next") }
        }
        Spacer(Modifier.height(12.dp))
        // The wave moves only while playing and when animations are on (not twice with the wave seek bar).
        if (!wave) {
            KiroXMotion(animate = playing && animations)
            Spacer(Modifier.height(12.dp))
        }
        volume?.let { level ->
            var value by remember(level.level) { mutableFloatStateOf(level.level / 100f) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(AppIcons.Volume, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    com.kirdev.pulsedeckmobile.core.ui.M3Slider(value, onMove = { value = it }, label = "Громкость ПК", onDone = {
                        value = it
                        app.connection.send("volume", (it * 100).toInt())
                    })
                }
                Spacer(Modifier.width(10.dp))
                Text("${(value * 100).toInt()}", style = Numbers.copy(color = Palette.Text3), modifier = Modifier.width(28.dp))
            }
            Text("Громкость ПК" + if (level.muted) " · звук выключен" else "", fontSize = 11.sp, color = Palette.Text3)
        }
        if (players.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth()) {
                Text("Источник музыки", fontSize = 12.sp, color = Palette.Text3)
                Spacer(Modifier.height(6.dp))
                SourceChips(players, source) { app.connection.chooseSource(it) }
            }
        }
    }
}

/** Whose track to show and control: «Автоматически» (the PC decides) or one app on the PC. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SourceChips(players: List<Player>, selected: String, onPick: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((id, label) in listOf("" to "Автоматически") + players.map { it.id to it.source }) {
            com.kirdev.pulsedeckmobile.core.ui.ChoiceChip(label, id == selected) { onPick(id) }
        }
    }
}

/** Four bars dancing next to «Сейчас играет» while a track plays. */
@Composable
private fun NowPlayingBars(modifier: Modifier) {
    val accent = Palette.accent
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    val heights = listOf(1000 to 0, 800 to 300, 1150 to 600, 900 to 150).mapIndexed { i, (d, offset) ->
        transition.animateFloat(0.25f, 1f, androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(d / 2), androidx.compose.animation.core.RepeatMode.Reverse,
            androidx.compose.animation.core.StartOffset(offset)), label = "eq$i")
    }
    androidx.compose.foundation.Canvas(modifier) {
        val gap = 2.dp.toPx()
        val w = (size.width - gap * 3) / 4
        heights.forEachIndexed { i, h ->
            val bh = size.height * h.value
            drawRoundRect(accent, androidx.compose.ui.geometry.Offset(i * (w + gap), size.height - bh),
                androidx.compose.ui.geometry.Size(w, bh), androidx.compose.ui.geometry.CornerRadius(w / 2))
        }
    }
}

/** Thin bar with a 28 dp touch area: tap to jump, drag to scrub (sent when the finger lifts). */
@Composable
fun SeekBar(
    fraction: Float, onDrag: (Float) -> Unit, wave: Boolean = false, playing: Boolean = false, motion: Boolean = true,
    onSeek: (Float) -> Unit,
) {
    var current by remember { mutableFloatStateOf(0f) }
    // The newest callbacks: the track (and its length) may change while this bar stays on screen.
    val seekNow by androidx.compose.runtime.rememberUpdatedState(onSeek)
    val dragNow by androidx.compose.runtime.rememberUpdatedState(onDrag)
    Box(
        Modifier.fillMaxWidth().height(28.dp)
            .pointerInput(Unit) { detectTapGestures { seekNow((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { current = (it.x / size.width).coerceIn(0f, 1f); dragNow(current) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        current = (change.position.x / size.width).coerceIn(0f, 1f); dragNow(current)
                    },
                    onDragEnd = { seekNow(current) },
                    onDragCancel = { seekNow(current) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        if (wave) SquigglyProgress(fraction, playing, motion, Modifier.fillMaxWidth().height(28.dp))
        else ThinProgress(fraction, Modifier.fillMaxWidth())
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
private fun TonalControl(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(64.dp).clip(CircleShape).background(if (enabled) Palette.Tonal else Palette.Track)
            .clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Palette.OnTonal else Palette.Disabled, modifier = Modifier.size(26.dp))
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
