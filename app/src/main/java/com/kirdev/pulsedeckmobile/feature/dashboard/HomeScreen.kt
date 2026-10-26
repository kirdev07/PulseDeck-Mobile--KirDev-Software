package com.kirdev.pulsedeckmobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.ThinProgress
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.feature.monitoring.Names
import com.kirdev.pulsedeckmobile.feature.notifications.EventRow
import com.kirdev.pulsedeckmobile.feature.notifications.EventsFeed
import com.kirdev.pulsedeckmobile.navigation.Routes

/** Screen 03: the PC, its key numbers, recent events, shortcuts. Real data only, «—» when unknown. */
@Composable
fun HomeScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val state by app.connection.state.collectAsStateWithLifecycle()
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val events by app.events.events.collectAsStateWithLifecycle()
    val connected = state is ConnectionState.Connected
    PollWhileVisible(EventsFeed.POLL_MS, connected) { if (connected) app.events.refresh() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        TopTitle("PulseDesk") {
            IconButtonSmall(AppIcons.Bell, "События", onClick = { go(Routes.EVENTS) })
            IconButtonSmall(AppIcons.Settings, "Настройки", onClick = { go(Routes.SETTINGS) })
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            PcCard(state) { go(Routes.DEVICES) }
            Spacer(Modifier.size(12.dp))
            val m = metrics
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("CPU", Format.percent(m?.cpu?.load), Names.cpu(m?.cpuName), Format.fraction(m?.cpu?.load), Modifier.weight(1f)) {
                    go(Routes.MONITORING)
                }
                Tile("RAM", Format.percent(m?.ram?.percent),
                    if (m?.ram?.total != null) "${Format.bytes(m.ram.used)} / ${Format.bytes(m.ram.total)}" else null,
                    Format.fraction(m?.ram?.percent), Modifier.weight(1f)) { go(Routes.MONITORING) }
                Tile("GPU", Format.percent(m?.gpu?.load), Names.gpu(m?.gpuName), Format.fraction(m?.gpu?.load), Modifier.weight(1f)) {
                    go(Routes.MONITORING)
                }
            }
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallStat(AppIcons.Network, "↓ ${Format.speed(m?.netDown)}", "↑ ${Format.speed(m?.netUp)}", Modifier.weight(1f))
                SmallStat(AppIcons.Pulse, "Работает", Format.uptime(m?.uptime), Modifier.weight(1f))
            }
            if (m != null && m.disks.isNotEmpty()) {
                SectionTitle("Диски")
                PanelCard(Modifier.fillMaxWidth()) {
                    m.disks.forEachIndexed { i, disk ->
                        if (i > 0) Spacer(Modifier.size(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(disk.name.trimEnd('\\'), fontSize = 13.sp, color = Palette.Text, modifier = Modifier.width(36.dp))
                            Column(Modifier.weight(1f)) {
                                ThinProgress(disk.usedFraction, Modifier.fillMaxWidth())
                                Text("свободно ${Format.bytes(disk.free)} из ${Format.bytes(disk.total)}" +
                                    if (disk.label.isNotBlank()) " · ${disk.label}" else "", fontSize = 11.sp, color = Palette.Text3)
                            }
                        }
                    }
                }
            }
            SectionTitle("Последние события", action = "Все") { go(Routes.EVENTS) }
            if (events.isEmpty()) {
                Text(if (connected) "Событий пока нет." else "Нет связи с ПК.", fontSize = 13.sp, color = Palette.Text3)
            } else PanelCard(Modifier.fillMaxWidth()) {
                events.take(3).forEach { EventRow(it) }
            }
            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PcCard(state: ConnectionState, onClick: () -> Unit) {
    val (name, status, live) = when (state) {
        is ConnectionState.Connected -> Triple(state.pcName, if (state.usb) "Подключён · USB" else "Подключён · ${state.pc.host}", true)
        is ConnectionState.Connecting -> Triple(state.pc.name, "Подключение…", false)
        is ConnectionState.Reconnecting -> Triple(state.pc.name, "${state.reason}. Повтор через ${state.inSeconds} с", false)
        is ConnectionState.CertificateChanged -> Triple(state.pc.name, "Сертификат ПК изменился — подключите заново", false)
        else -> Triple("ПК", "Не подключён", false)
    }
    PanelCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Monitor, null, tint = Palette.Text, modifier = Modifier.size(40.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (live) Palette.Text else Palette.Disabled))
                    Spacer(Modifier.width(6.dp))
                    Text(status, fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Icon(AppIcons.Chevron, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun Tile(title: String, value: String, caption: String?, fraction: Float, modifier: Modifier, onClick: () -> Unit) {
    PanelCard(modifier, onClick = onClick) {
        Text(title, fontSize = 11.sp, color = Palette.Text3)
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
        Text(caption ?: " ", fontSize = 10.sp, color = Palette.Text3, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.size(6.dp))
        ThinProgress(fraction, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SmallStat(icon: ImageVector, top: String, bottom: String, modifier: Modifier) {
    PanelCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Palette.Text2, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(top, fontSize = 13.sp, color = Palette.Text, maxLines = 1)
                Text(bottom, fontSize = 12.sp, color = Palette.Text3, maxLines = 1)
            }
        }
    }
}
