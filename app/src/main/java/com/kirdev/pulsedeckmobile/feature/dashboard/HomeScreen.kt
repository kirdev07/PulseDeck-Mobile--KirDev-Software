package com.kirdev.pulsedeckmobile.feature.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.LiveDot
import com.kirdev.pulsedeckmobile.core.ui.appear
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.ui.pressScale
import com.kirdev.pulsedeckmobile.core.ui.rememberActions
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.Metrics
import com.kirdev.pulsedeckmobile.data.model.PcActivity
import com.kirdev.pulsedeckmobile.feature.monitoring.Names
import com.kirdev.pulsedeckmobile.feature.remote.ScreenshotViewer
import com.kirdev.pulsedeckmobile.feature.remote.takeScreenshot
import com.kirdev.pulsedeckmobile.monitoring.MetricsHistory
import com.kirdev.pulsedeckmobile.navigation.Routes
import java.util.Calendar

/** «Главная A · Плитки» (approved mockup): the PC, its key numbers as tiles, four quick actions. Real data only. */
@Composable
fun HomeScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val state by app.connection.state.collectAsStateWithLifecycle()
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val activity by app.connection.activity.collectAsStateWithLifecycle()
    val series by app.history.series.collectAsStateWithLifecycle()
    val volume by app.connection.volume.collectAsStateWithLifecycle()
    val actions = rememberActions()
    val connected = state is ConnectionState.Connected
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var screenshot by remember { mutableStateOf<ImageBitmap?>(null) }
    val m = metrics

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        Header(state, Modifier.enter(0)) { go(Routes.EVENTS) }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatusCard(state, activity, m, Modifier.enter(1)) { go(Routes.DEVICES) }
            CpuCard(m, series.cpu, Modifier.enter(2)) { go(Routes.MONITORING) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.enter(3)) {
                BarTile("Память", Format.percent(m?.ram?.percent), Format.fraction(m?.ram?.percent),
                    if (m?.ram?.total != null) "${Format.bytes(m.ram.used)} из ${Format.bytes(m.ram.total)}" else Format.UNKNOWN,
                    Modifier.weight(1f)) { go(Routes.MONITORING) }
                BarTile("Видеокарта", Format.percent(m?.gpu?.load), Format.fraction(m?.gpu?.load),
                    gpuCaption(m), Modifier.weight(1f)) { go(Routes.MONITORING) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.enter(4)) {
                TextTile("Сеть", "↓ ${Format.speed(m?.netDown)}", "↑ ${Format.speed(m?.netUp)}", Modifier.weight(1f)) { go(Routes.MONITORING) }
                val disk = m?.disks?.firstOrNull()
                TextTile(disk?.let { "Диск ${it.name.trimEnd('\\')}" } ?: "Диск",
                    disk?.let { Format.bytes(it.free) } ?: Format.UNKNOWN,
                    disk?.let { "свободно из ${Format.bytes(it.total)}" } ?: "нет данных", Modifier.weight(1f)) { go(Routes.MONITORING) }
            }
            m?.battery?.percent?.let { level ->          // laptops only
                val plugged = m.battery.plugged
                TextTile("Батарея ПК", "${level.toInt()}%",
                    if (plugged) "на зарядке" else if (level <= 20) "мало заряда — подключите зарядку" else "от батареи",
                    Modifier.fillMaxWidth().enter(5)) { go(Routes.MONITORING) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp).enter(6)) {
                Quick(AppIcons.Lock, "Блокировка", connected, Modifier.weight(1f)) {
                    confirm = "Заблокировать ПК? Управление с телефона будет недоступно до разблокировки." to
                        { actions.run("ПК заблокирован") { app.api.lock() } }
                }
                val muted = volume?.muted == true
                Quick(AppIcons.Volume, if (muted) "Без звука" else "Звук", connected && volume != null, Modifier.weight(1f), dim = muted) {
                    actions.run(if (muted) "Звук включён" else "Звук выключен") { app.api.setMuted(!muted) }
                }
                Quick(AppIcons.Image, "Скриншот", connected, Modifier.weight(1f)) {
                    actions.run { screenshot = takeScreenshot(app.api); null }
                }
                Quick(AppIcons.Moon, "Сон", connected, Modifier.weight(1f)) {
                    confirm = "Перевести ПК в сон? Связь с телефоном прервётся." to { actions.run("ПК засыпает") { app.api.sleep() } }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    confirm?.let { (text, run) -> ConfirmDialog("Подтвердите", text, "Да", run, danger = false) { confirm = null } }
    screenshot?.let { ScreenshotViewer(it) { screenshot = null } }
}

private fun gpuCaption(m: Metrics?): String {
    val name = Names.gpu(m?.gpuName)
    val memory = m?.gpuMemoryTotal?.let { Format.bytes(it) }
    return listOfNotNull(name.ifBlank { null }, memory).joinToString(" · ").ifBlank { Format.UNKNOWN }
}

@Composable
private fun Header(state: ConnectionState, modifier: Modifier, onEvents: () -> Unit) {
    val name = when (state) {
        is ConnectionState.Connected -> state.pcName
        is ConnectionState.Connecting -> state.pc.name
        is ConnectionState.Reconnecting -> state.pc.name
        is ConnectionState.CertificateChanged -> state.pc.name
        else -> "PulseDesk"
    }
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting(), fontSize = 13.sp, color = Palette.Text2)
            Text(name, fontSize = 26.sp, fontWeight = FontWeight.Medium, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Surface).clickable(onClick = onEvents),
            contentAlignment = Alignment.Center) {
            Icon(AppIcons.Bell, "События", tint = Palette.Text2, modifier = Modifier.size(22.dp))
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Доброе утро"
    in 12..17 -> "Добрый день"
    in 18..22 -> "Добрый вечер"
    else -> "Доброй ночи"
}

@Composable
private fun StatusCard(state: ConnectionState, activity: PcActivity?, m: Metrics?, modifier: Modifier, onClick: () -> Unit) {
    val (title, live) = when (state) {
        is ConnectionState.Connected -> (if (state.usb) "Подключён по USB" else "Подключён · ${state.pc.host}") to true
        is ConnectionState.Connecting -> "Подключение…" to false
        is ConnectionState.Reconnecting -> "${state.reason}. Повтор через ${state.inSeconds} с" to false
        is ConnectionState.CertificateChanged -> "Сертификат ПК изменился — подключите заново" to false
        else -> "Не подключён" to false
    }
    val sub = buildList {
        if (live && activity != null) add((if (activity.fullscreen) "На весь экран: " else "Сейчас: ") + activity.app)
        if (live && m?.uptime != null) add("работает ${Format.uptime(m.uptime)}")
        if (!live) add("Нажмите, чтобы выбрать ПК")
    }.joinToString(" · ")
    val bg = if (live) Palette.Container else Palette.Surface
    val fg = if (live) Palette.OnContainer else Palette.Text
    val source = remember { MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().pressScale(source).clip(RoundedCornerShape(28.dp)).background(bg)
            .clickable(source, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LiveDot(if (live) Palette.Ok else Palette.Disabled, live, size = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub.isNotBlank()) Text(sub, fontSize = 13.sp, color = fg.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(AppIcons.Chevron, null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

/** Processor: the number and its last minutes as a filled line, drawn from left to right on open. */
@Composable
private fun CpuCard(m: Metrics?, values: List<Float>, modifier: Modifier, onClick: () -> Unit) {
    val drawn = appear(250, 1200)
    val line = Palette.accent
    val name = Names.cpu(m?.cpuName)
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Palette.Surface).clickable(onClick = onClick)
        .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (name.isBlank()) "Процессор" else "Процессор · $name", fontSize = 13.sp, color = Palette.Text2,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Format.percent(m?.cpu?.load), fontSize = 34.sp, color = line)
        }
        Canvas(Modifier.fillMaxWidth().height(70.dp).padding(top = 4.dp)) {
            if (values.size < 2) {
                drawLine(line.copy(alpha = 0.3f), Offset(0f, size.height - 2f), Offset(size.width, size.height - 2f), 2f)
                return@Canvas
            }
            val step = size.width / (MetricsHistory.SIZE - 1)
            val start = size.width - step * (values.size - 1)
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = start + step * i
                val y = size.height * (1f - (v / 100f).coerceIn(0f, 1f)) * 0.92f + size.height * 0.04f
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val area = Path().apply {
                addPath(path)
                lineTo(size.width, size.height); lineTo(start, size.height); close()
            }
            clipRect(right = size.width * drawn) {
                drawPath(area, line.copy(alpha = 0.18f))
                drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun BarTile(title: String, value: String, fraction: Float, caption: String, modifier: Modifier, onClick: () -> Unit) {
    val grow = appear(350)
    val shown by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600), label = "bar")
    val ink = Palette.accent
    val track = Palette.Track
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Palette.Surface).clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, fontSize = 13.sp, color = Palette.Text2)
        Text(value, fontSize = 28.sp, color = Palette.Text, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            val w = (size.width * shown * grow).coerceAtLeast(if (shown > 0f) size.height else 0f)
            if (w > 0f) drawRoundRect(ink, size = Size(w, size.height), cornerRadius = r)
        }
        Text(caption, fontSize = 11.sp, color = Palette.Text2, modifier = Modifier.padding(top = 6.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TextTile(title: String, big: String, small: String, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Palette.Surface).clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, fontSize = 13.sp, color = Palette.Text2)
        Text(big, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp))
        Text(small, fontSize = 13.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Quick(icon: ImageVector, label: String, enabled: Boolean, modifier: Modifier, dim: Boolean = false, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val fg = if (enabled) Palette.OnTonal else Palette.Disabled
    Column(
        modifier.height(72.dp).pressScale(source).clip(RoundedCornerShape(20.dp))
            .background(if (enabled) Palette.Tonal else Palette.Surface)
            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = if (dim) fg.copy(alpha = 0.5f) else fg, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
    }
}

