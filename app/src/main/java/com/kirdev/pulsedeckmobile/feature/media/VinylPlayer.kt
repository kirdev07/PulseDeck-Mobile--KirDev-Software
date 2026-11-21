package com.kirdev.pulsedeckmobile.feature.media

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Numbers
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.Device
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.rememberFrameTime
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.MediaSession
import com.kirdev.pulsedeckmobile.data.model.PlayStatus
import com.kirdev.pulsedeckmobile.data.model.Player
import com.kirdev.pulsedeckmobile.data.model.SystemVolume

/**
 * Screen 04: the player as a record (approved mockup «Плеер C»). The cover sits in the middle of a
 * vinyl disc that turns while the track plays; the tonearm lies on the record while playing and
 * lifts off on pause. Source of the music is the chip at the top; PC volume in a card below.
 */
@Composable
fun MusicScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val session by app.connection.media.collectAsStateWithLifecycle()
    val cover by app.connection.cover.collectAsStateWithLifecycle()
    val volume by app.connection.volume.collectAsStateWithLifecycle()
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    val players by app.connection.players.collectAsStateWithLifecycle()
    val source by app.connection.source.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonSmall(AppIcons.Back, "Назад", onClick = onBack)
            Spacer(Modifier.weight(1f))
            session?.let { SourceChip(it.source, players, source) { id -> app.connection.chooseSource(id) } }
        }
        val track = session
        if (track == null) {
            Placeholder("На ПК сейчас ничего не играет.")
            return
        }
        val image = cover?.takeIf { it.first == track.coverKey }?.second
        val playing = track.status == PlayStatus.PLAYING
        val motion = prefs.animations && !Device.lite
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth > maxHeight) {
                // Landscape: the record on the left, everything else on the right.
                Row(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Vinyl(image, playing, motion, Modifier.fillMaxHeight().aspectRatio(1f))
                    Spacer(Modifier.width(28.dp))
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                        TrackAndControls(app, track, volume, prefs.waveSeek, prefs.animations)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(12.dp))
                    Vinyl(image, playing, motion, Modifier.widthIn(max = 320.dp).fillMaxWidth().aspectRatio(1f))
                    Spacer(Modifier.height(28.dp))
                    TrackAndControls(app, track, volume, prefs.waveSeek, prefs.animations)
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TrackAndControls(app: PulseDeskApp, track: MediaSession, volume: SystemVolume?, wave: Boolean, animations: Boolean) {
    val playing = track.status == PlayStatus.PLAYING
    var dragging by remember { mutableStateOf<Float?>(null) }
    val position = livePositionOf(track)
    Text(track.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Palette.Text, maxLines = 2,
        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    if (track.artist.isNotBlank()) Text(track.artist, fontSize = 16.sp, color = Palette.Text2, maxLines = 1,
        overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(18.dp))
    if (track.duration > 0) {
        val shown = dragging?.let { it * track.duration } ?: position
        SeekBar((shown / track.duration).toFloat(), onDrag = { dragging = it }, wave = wave, playing = playing,
            motion = animations && !Device.lite) {
            dragging = null
            app.connection.send("seek", (it * track.duration).coerceIn(0.0, track.duration))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(Format.time(shown), style = Numbers.copy(color = Palette.Text2, fontSize = 12.sp))
            Text(Format.time(track.duration), style = Numbers.copy(color = Palette.Text2, fontSize = 12.sp))
        }
    }
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        PlainControl(AppIcons.Prev, "Предыдущий трек", track.canPrev) { app.connection.send("prev") }
        Box(
            Modifier.size(84.dp).clip(CircleShape).background(Palette.accent).clickable { app.connection.send("toggle") }
                .semantics { contentDescription = if (playing) "Пауза" else "Воспроизвести" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) AppIcons.Pause else AppIcons.Play, null, tint = Palette.onAccent, modifier = Modifier.size(36.dp))
        }
        PlainControl(AppIcons.Next, "Следующий трек", track.canNext) { app.connection.send("next") }
    }
    volume?.let { level ->
        Spacer(Modifier.height(24.dp))
        var value by remember(level.level) { mutableFloatStateOf(level.level / 100f) }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Volume, null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Громкость ПК · ${(value * 100).toInt()}" + if (level.muted) " · звук выключен" else "",
                    fontSize = 12.sp, color = Palette.Text2)
                SeekBar(value, onDrag = { value = it }) { value = it; app.connection.send("volume", (it * 100).toInt()) }
            }
        }
    }
}

/** Position that moves on the phone between PC updates (twice a second while playing). */
@Composable
private fun livePositionOf(track: MediaSession): Double {
    var now by remember { mutableStateOf(android.os.SystemClock.elapsedRealtime()) }
    androidx.compose.runtime.LaunchedEffect(track.status, track.receivedAt) {
        now = android.os.SystemClock.elapsedRealtime()
        while (track.status == PlayStatus.PLAYING) {
            kotlinx.coroutines.delay(500)
            now = android.os.SystemClock.elapsedRealtime()
        }
    }
    return track.positionAt(now)
}

/**
 * The record: grooves, the cover as its round label, the spindle hole, the tonearm. Turns at a
 * calm 45°/s while playing (at most «Частота кадров», still without animations or in light mode).
 */
@Composable
private fun Vinyl(cover: Bitmap?, playing: Boolean, motion: Boolean, modifier: Modifier) {
    val time = rememberFrameTime(playing && motion)
    var angle by remember { mutableFloatStateOf(0f) }
    var lastT by remember { mutableFloatStateOf(-1f) }
    // Accumulate the turn so a pause stops the record where it is (no jump back).
    val t = time.value
    if (playing && motion) {
        if (lastT >= 0f && t >= lastT) angle = (angle + (t - lastT) / 1000f * 45f) % 360f
        lastT = t
    } else lastT = -1f
    val arm by animateFloatAsState(if (playing) 1f else 0f, tween(600), label = "tonearm")
    val groove = lerp(Palette.Black, Palette.Text, 0.07f)
    val disc = lerp(Palette.Black, Palette.Text, 0.04f)
    val armColor = Palette.Text2
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxSize().padding(6.dp).shadow(18.dp, CircleShape).clip(CircleShape).background(disc))
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val r = size.minDimension / 2
            // grooves: thin rings from the label to the edge
            var ring = r * 0.36f
            while (ring < r * 0.97f) {
                drawCircle(groove, ring, style = Stroke(1f))
                ring += r * 0.035f
            }
            // a soft sheen on the grooves that turns with the record (kept inside the disc)
            rotate(angle) {
                drawArc(Color.White.copy(alpha = 0.05f), -60f, 40f, false, Offset(r * 0.33f, r * 0.33f),
                    androidx.compose.ui.geometry.Size(r * 1.34f, r * 1.34f), style = Stroke(r * 0.55f))
            }
        }
        // the label: the cover, round, turning with the record
        Box(Modifier.fillMaxSize(0.56f).graphicsLayer { rotationZ = angle }.clip(CircleShape).background(Palette.Container),
            contentAlignment = Alignment.Center) {
            if (cover != null) Image(cover.asImageBitmap(), "Обложка", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(AppIcons.Music, null, tint = Palette.OnContainer, modifier = Modifier.size(40.dp))
        }
        Box(Modifier.size(14.dp).clip(CircleShape).background(Palette.Black))
        // tonearm: from the corner; on the record while playing, lifted aside on pause
        Canvas(Modifier.fillMaxSize()) {
            val pivot = Offset(size.width * 0.96f, size.height * 0.04f)
            // on the grooves while playing; swung out past the edge on pause
            rotate(-30f * (1f - arm), pivot) {
                val tip = Offset(size.width * 0.84f, size.height * 0.56f)
                drawLine(armColor, pivot, tip, 5.dp.toPx(), StrokeCap.Round)
                drawCircle(armColor, 9.dp.toPx(), pivot)
                drawCircle(armColor, 6.dp.toPx(), tip)
            }
        }
    }
}

/** Which app on the PC: a chip at the top with a green dot; a tap lists the players. */
@Composable
private fun SourceChip(current: String, players: List<Player>, selected: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Tonal)
                .clickable(enabled = players.isNotEmpty()) { open = true }.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Palette.Ok))
            Spacer(Modifier.width(8.dp))
            Text((current.ifBlank { "Музыка" }) + " · на ПК", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                color = Palette.OnTonal, maxLines = 1)
            if (players.isNotEmpty()) Icon(AppIcons.Chevron, null, tint = Palette.OnTonal,
                modifier = Modifier.padding(start = 2.dp).size(16.dp).graphicsLayer { rotationZ = 90f })
        }
        DropdownMenu(open, onDismissRequest = { open = false }, containerColor = Palette.Surface2) {
            for (player in listOf(Player("", "Автоматически", "", "")) + players) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(player.source, color = Palette.Text, fontSize = 15.sp)
                            if (player.title.isNotBlank()) Text(player.title, color = Palette.Text2, fontSize = 12.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    },
                    leadingIcon = {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            if (player.id == selected) Icon(AppIcons.Check, null, tint = Palette.accent, modifier = Modifier.size(20.dp))
                        }
                    },
                    onClick = { open = false; onPick(player.id) },
                )
            }
        }
    }
}

@Composable
private fun PlainControl(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(56.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = if (enabled) Palette.Text else Palette.Disabled, modifier = Modifier.size(30.dp))
    }
}

