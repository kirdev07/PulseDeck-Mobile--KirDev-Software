package com.kirdev.pulsedeckmobile.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.BuildConfigInfo
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.theme.Tone
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.LiveDot
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.ui.pressScale
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.navigation.Routes

/** «Меню» (approved mockup): the PC on top, big tiles for every section that works, the version below. */
@Composable
fun MenuScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val media by app.connection.media.collectAsStateWithLifecycle()
    val state by app.connection.state.collectAsStateWithLifecycle()
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PcCard(state, metrics?.uptime, Modifier.enter(0)) { go(Routes.DEVICES) }
        val tiles = listOf(
            Tile(AppIcons.Music, "Музыка", media?.let { "${it.source} · " + if (it.status.name == "PLAYING") "играет" else "пауза" } ?: "ничего не играет",
                Tone.Pink, Routes.MUSIC),
            Tile(AppIcons.Apps, "Процессы", "что запущено на ПК", Tone.Blue, Routes.PROCESSES),
            Tile(AppIcons.Devices, "Устройства", "ПК, переключение", Tone.Teal, Routes.DEVICES),
            Tile(AppIcons.Bell, "События", "блокировка, нагрузка", Tone.Amber, Routes.EVENTS),
            Tile(AppIcons.Brush, "Оформление", (if (prefs.style == "mono") "чёрно-белый" else "цвет") + ", " +
                mapOf("dark" to "тёмная", "light" to "светлая", "system" to "как в системе")[prefs.mode].orEmpty(), Tone.Violet, Routes.APPEARANCE),
            Tile(AppIcons.Settings, "Настройки", "уведомления, пороги", Tone.Violet, Routes.SETTINGS),
        )
        tiles.chunked(2).forEachIndexed { row, pair ->
            Row(Modifier.enter(row + 1), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { TileCard(it, Modifier.weight(1f)) { go(it.route) } }
            }
        }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(horizontal = 16.dp, vertical = 14.dp)
            .enter(4), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Info, null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text("О приложении", fontSize = 15.sp, color = Palette.Text, modifier = Modifier.weight(1f))
            Text("PulseDesk ${BuildConfigInfo.version(app)}", fontSize = 13.sp, color = Palette.Text2)
        }
    }
}

private class Tile(val icon: ImageVector, val title: String, val sub: String, val tone: Tone, val route: String)

@Composable
private fun TileCard(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Column(modifier.heightIn(min = 104.dp).pressScale(source).clip(RoundedCornerShape(24.dp)).background(Palette.Surface)
        .clickable(source, indication = androidx.compose.material3.ripple(), onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tile.tone.bg), contentAlignment = Alignment.Center) {
            Icon(tile.icon, null, tint = tile.tone.fg, modifier = Modifier.size(22.dp))
        }
        Column {
            Text(tile.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
            Text(tile.sub, fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PcCard(state: ConnectionState, uptime: Long?, modifier: Modifier, onSwitch: () -> Unit) {
    val (name, status, live) = when (state) {
        is ConnectionState.Connected -> Triple(state.pcName,
            (if (state.usb) "USB" else state.pc.host) + (uptime?.let { " · ${Format.uptime(it)}" } ?: ""), true)
        is ConnectionState.Connecting -> Triple(state.pc.name, "Подключение…", false)
        is ConnectionState.Reconnecting -> Triple(state.pc.name, "Нет связи · повтор через ${state.inSeconds} с", false)
        is ConnectionState.CertificateChanged -> Triple(state.pc.name, "Сертификат изменился", false)
        else -> Triple("ПК не выбран", "Нажмите «Выбрать»", false)
    }
    val bg = if (live) Palette.Container else Palette.Surface
    val fg = if (live) Palette.OnContainer else Palette.Text
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg).clickable(onClick = onSwitch).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(if (live) Palette.accent.copy(alpha = 0.25f) else Palette.Track),
            contentAlignment = Alignment.Center) {
            Icon(AppIcons.Monitor, null, tint = if (live) Palette.accent else Palette.Text2, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                LiveDot(if (live) Palette.Ok else Palette.Disabled, live)
                Spacer(Modifier.width(6.dp))
                Text(status, fontSize = 13.sp, color = fg.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .clickable(onClick = onSwitch).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(if (live) "Сменить" else "Выбрать", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}
