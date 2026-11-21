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
private fun Control(icon: ImageVector, label: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).clickable(enabled = enabled, onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) Palette.Text else Palette.Disabled,
            modifier = Modifier.size(if (size > 48.dp) 28.dp else 22.dp))
    }
}
