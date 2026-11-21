package com.kirdev.pulsedeckmobile.feature.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.ThinProgress
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.Metrics
import com.kirdev.pulsedeckmobile.monitoring.MetricsHistory
import com.kirdev.pulsedeckmobile.monitoring.Limit
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.enter
import androidx.compose.ui.graphics.drawscope.clipRect

/** Shortened hardware names: «Intel Core i5-13420H», «RTX 3050». */
object Names {
    fun cpu(name: String?): String = name.orEmpty()
        .replace(Regex("""\(R\)|\(TM\)|®|™"""), "")
        .replace(Regex("""\b\d+(st|nd|rd|th) Gen\b"""), "")
        .replace(Regex("""\b(CPU|Processor)\b"""), "")
        .replace(Regex("""@.*$"""), "")
        .replace(Regex("""\s+"""), " ").trim()

    fun gpu(name: String?): String = name.orEmpty()
        .replace(Regex("""\(R\)|\(TM\)"""), "")
        .replace(Regex("""\bNVIDIA GeForce\b|\bAMD Radeon\b|\bIntel\b|\bGPU\b|\bLaptop\b"""), "")
        .replace(Regex("""\b\d+\s?GB\b"""), "")
        .replace(Regex("""\s+"""), " ").trim()
}

/** One line of «Мониторинг B · Всё сразу»: a value, its sparkline (or bar), and what opens under it. */
private class Line(
    val key: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val name: String, val sub: String, val value: String,
    val series: List<Float> = emptyList(), val max: Float = 100f, val bar: Float? = null, val hot: Boolean = false,
)

/** «Мониторинг B» (approved mockup): everything at once; a tap opens the big chart of that line. */
@Composable
fun MonitoringScreen(app: PulseDeskApp) {
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val series by app.history.series.collectAsStateWithLifecycle()
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val seconds = (MetricsHistory.SIZE * prefs.intervalSeconds).toInt()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Мониторинг", fontSize = 28.sp, color = Palette.Text, modifier = Modifier.weight(1f))
            Text("каждые ${Format.seconds(prefs.intervalSeconds)}", fontSize = 12.sp, color = Palette.Text2)
        }
        val m = metrics
        if (m == null) {
            Placeholder("Нет данных: ПК не на связи.")
            return
        }
        val lines = lines(m, series, prefs)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEachIndexed { i, line ->
                Column(Modifier.enter(i + 1)) {
                    LineRow(line) { open = if (open == line.key) null else line.key }
                    androidx.compose.animation.AnimatedVisibility(open == line.key,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()) {
                        Column(Modifier.padding(top = 8.dp)) { Detail(line.key, m, series, seconds) }
                    }
                }
            }
            Text("Нажмите на строку — откроется большой график за $seconds с.", fontSize = 12.sp, color = Palette.Text2,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            Spacer(Modifier.height(20.dp))
        }
    }
}

private fun lines(m: Metrics, s: com.kirdev.pulsedeckmobile.monitoring.Series, prefs: com.kirdev.pulsedeckmobile.data.local.Prefs): List<Line> {
    fun over(key: Limit, v: Double?) = v != null && v >= prefs.limit(key)
    val out = mutableListOf<Line>()
    val cpuSub = listOfNotNull(Names.cpu(m.cpuName).ifBlank { null }, m.cpuCores?.let { "$it потоков" }).joinToString(" · ")
    out += Line("cpu", AppIcons.Cpu, "Процессор", cpuSub.ifBlank { Format.UNKNOWN }, Format.percent(m.cpu.load), s.cpu,
        hot = over(Limit.CPU, m.cpu.load))
    out += Line("ram", AppIcons.Ram, "Память",
        if (m.ram.total != null) "${Format.bytes(m.ram.used)} из ${Format.bytes(m.ram.total)}" else Format.UNKNOWN,
        Format.percent(m.ram.percent), s.ram, hot = over(Limit.RAM, m.ram.percent))
    out += Line("gpu", AppIcons.Gpu, "Видеокарта", Names.gpu(m.gpuName).ifBlank { Format.UNKNOWN }, Format.percent(m.gpu.load), s.gpu,
        hot = over(Limit.GPU, m.gpu.load))
    if (m.gpuMemoryTotal != null) out += Line("vram", AppIcons.Gpu, "Видеопамять",
        "${m.gpuMemoryUsed?.let { Format.bytes(it) } ?: Format.UNKNOWN} из ${Format.bytes(m.gpuMemoryTotal)}",
        Format.percent(m.vramPercent), bar = Format.fraction(m.vramPercent), hot = over(Limit.VRAM, m.vramPercent))
    val top = (s.down + s.up).maxOrNull()?.coerceAtLeast(1024f) ?: 1024f
    out += Line("net", AppIcons.Network, "Сеть", "↑ ${Format.speed(m.netUp)}", "↓ ${Format.speed(m.netDown)}", s.down, top)
    m.disks.forEach { d ->
        val freePct = if (d.total > 0) 100.0 * d.free / d.total else null
        out += Line("disk:${d.name}", AppIcons.Disk, "Диск ${d.name.trimEnd('\\')}",
            "свободно ${Format.bytes(d.free)} из ${Format.bytes(d.total)}", "${(d.usedFraction * 100).toInt()}%",
            bar = d.usedFraction, hot = freePct != null && freePct <= prefs.limit(Limit.DISK))
    }
    m.battery?.percent?.let { level ->
        out += Line("battery", AppIcons.Battery, "Батарея ПК", if (m.battery.plugged) "на зарядке" else "от батареи", "${level.toInt()}%",
            bar = (level / 100).toFloat(), hot = !m.battery.plugged && level <= prefs.limit(Limit.BATTERY))
    }
    return out
}

@Composable
private fun LineRow(line: Line, onClick: () -> Unit) {
    val bg = if (line.hot) Palette.Container else Palette.Surface
    val fg = if (line.hot) Palette.OnContainer else Palette.Text
    val ink = if (line.hot) Palette.OnContainer else Palette.accent
    val pulse = com.kirdev.pulsedeckmobile.core.ui.breathe(line.hot)
    val drawn = com.kirdev.pulsedeckmobile.core.ui.appear(300, 1100)
    val long = line.value.length > 5
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(bg).clickable(onClick = onClick)
        .padding(start = 16.dp, end = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (line.hot) Palette.OnContainer.copy(alpha = 0.18f * pulse + 0.06f) else Palette.Tonal),
            contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(line.icon, null, tint = if (line.hot) fg else Palette.OnTonal, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(line.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
            Text(line.sub, fontSize = 12.sp, color = fg.copy(alpha = 0.75f), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        val track = if (line.hot) Palette.OnContainer.copy(alpha = 0.25f) else Palette.Track
        Canvas(Modifier.width(if (long) 56.dp else 72.dp).height(30.dp)) {
            val bar = line.bar
            if (bar != null) {
                val h = 6.dp.toPx()
                val y = (size.height - h) / 2
                val r = androidx.compose.ui.geometry.CornerRadius(h / 2)
                drawRoundRect(track, Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, h), r)
                val w = size.width * bar.coerceIn(0f, 1f) * drawn
                if (w > 0f) drawRoundRect(ink, Offset(0f, y), androidx.compose.ui.geometry.Size(w.coerceAtLeast(h), h), r)
                return@Canvas
            }
            val pts = line.series.takeLast(30)
            if (pts.size < 2) return@Canvas
            val step = size.width / 29f
            val start = size.width - step * (pts.size - 1)
            val path = Path()
            pts.forEachIndexed { i, v ->
                val x = start + step * i
                val y = 2f + (size.height - 4f) * (1f - (v / line.max).coerceIn(0f, 1f))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            clipRect(right = size.width * drawn) {
                drawPath(path, ink, style = Stroke(2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
        }
        Text(line.value, fontSize = if (long) 14.sp else 20.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(if (long) 92.dp else 58.dp))
    }
}

/** What opens under a line: the big chart for the last minutes and the facts behind the number. */
@Composable
private fun Detail(key: String, m: Metrics, series: com.kirdev.pulsedeckmobile.monitoring.Series, seconds: Int) {
    when {
        key == "cpu" -> {
            ChartCard(series.cpu, 100f, "$seconds с назад", "Сейчас")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Fact("Минимум", series.cpu.minOrNull()?.let { "${it.toInt()}%" } ?: Format.UNKNOWN, Modifier.weight(1f))
                Fact("Среднее", if (series.cpu.isEmpty()) Format.UNKNOWN else "${series.cpu.average().toInt()}%", Modifier.weight(1f))
                Fact("Пик", series.cpu.maxOrNull()?.let { "${it.toInt()}%" } ?: Format.UNKNOWN, Modifier.weight(1f))
            }
            if (m.perCore.isNotEmpty()) {
                SectionTitle("Загрузка по ядрам")
                CoreBars(m.perCore)
            }
        }
        key == "ram" -> {
            ChartCard(series.ram, 100f, "$seconds с назад", "Сейчас")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Fact("Свободно", if (m.ram.total != null && m.ram.used != null) Format.bytes(m.ram.total - m.ram.used) else Format.UNKNOWN,
                    Modifier.weight(1f))
                Fact("Всего", Format.bytes(m.ram.total), Modifier.weight(1f))
            }
        }
        key == "gpu" -> {
            ChartCard(series.gpu, 100f, "$seconds с назад", "Сейчас")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Fact("Видеопамять", m.gpuMemoryTotal?.let { Format.bytes(it) } ?: Format.UNKNOWN, Modifier.weight(1f))
                Fact("Занято", m.gpuMemoryUsed?.let { Format.bytes(it) } ?: Format.UNKNOWN, Modifier.weight(1f))
            }
        }
        key == "net" -> {
            val top = (series.down + series.up).maxOrNull()?.coerceAtLeast(1024f) ?: 1024f
            ChartCard(series.down, top, "$seconds с назад", "Приём · отдача тонкой линией", second = series.up)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                Fact("Приём", Format.speed(m.netDown), Modifier.weight(1f))
                Fact("Отдача", Format.speed(m.netUp), Modifier.weight(1f))
            }
        }
        key.startsWith("disk:") -> m.disks.firstOrNull { "disk:${it.name}" == key }?.let { d ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Fact("Свободно", Format.bytes(d.free), Modifier.weight(1f))
                Fact("Занято", Format.bytes(d.total - d.free), Modifier.weight(1f))
                Fact("Всего", Format.bytes(d.total), Modifier.weight(1f))
            }
            if (d.label.isNotBlank()) Text("Метка: ${d.label}", fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.padding(4.dp))
        }
        else -> Text("Для этой строки графика нет: ПК присылает только текущее значение.", fontSize = 12.sp, color = Palette.Text2,
            modifier = Modifier.padding(4.dp))
    }
}

@Composable
private fun Fact(title: String, value: String, modifier: Modifier) {
    PanelCard(modifier) {
        Text(title, fontSize = 11.sp, color = Palette.Text3)
        Text(value, fontSize = 15.sp, color = Palette.Text)
    }
}

/** Line chart of the last values; [second] draws a fainter second line (upload). */
@Composable
private fun ChartCard(values: List<Float>, max: Float, left: String, right: String, second: List<Float> = emptyList()) {
    PanelCard(Modifier.fillMaxWidth()) {
        val line = Palette.accent
        val faint = Palette.Text3
        val grid = Palette.Border
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            for (i in 0..3) {
                val y = size.height * i / 3
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            fun plot(points: List<Float>, color: androidx.compose.ui.graphics.Color, width: Float) {
                if (points.size < 2) return
                val step = size.width / (MetricsHistory.SIZE - 1)
                val start = size.width - step * (points.size - 1)
                val path = Path()
                points.forEachIndexed { i, v ->
                    val x = start + step * i
                    val y = size.height * (1f - (v / max).coerceIn(0f, 1f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = width))
            }
            plot(second, faint, 1.5.dp.toPx())
            plot(values, line, 2.dp.toPx())
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(left, fontSize = 10.sp, color = Palette.Text3, modifier = Modifier.weight(1f))
            Text(right, fontSize = 10.sp, color = Palette.Text3)
        }
    }
}

@Composable
private fun CoreBars(cores: List<Double>) {
    PanelCard(Modifier.fillMaxWidth()) {
        // Up to 8 columns per row so 16- and 24-thread CPUs still fit a phone screen.
        cores.chunked(8).forEachIndexed { row, chunk ->
            if (row > 0) Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                chunk.forEach { load ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(6.dp)).background(Palette.Surface2),
                            contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((load / 100).toFloat().coerceIn(0.03f, 1f)).background(Palette.accent))
                        }
                        Text("${load.toInt()}%", fontSize = 10.sp, color = Palette.Text3)
                    }
                }
                repeat(8 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

