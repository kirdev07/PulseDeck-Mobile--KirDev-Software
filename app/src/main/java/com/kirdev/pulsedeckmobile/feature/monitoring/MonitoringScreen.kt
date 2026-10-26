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

private enum class Section(val title: String) { CPU("CPU"), GPU("GPU"), RAM("RAM"), DISKS("Диски"), NET("Сеть") }

/** Screen 06. Charts come from the same live stream as the home screen (no extra requests). */
@Composable
fun MonitoringScreen(app: PulseDeskApp) {
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    val series by app.history.series.collectAsStateWithLifecycle()
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.CPU) }
    val seconds = (MetricsHistory.SIZE * prefs.intervalSeconds).toInt()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopTitle("Мониторинг")
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in Section.entries) Chip(s.title, s == section) { section = s }
        }
        val m = metrics
        if (m == null) {
            Placeholder("Нет данных: ПК не на связи.")
            return
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            when (section) {
                Section.CPU -> {
                    Headline("Процессор", Names.cpu(m.cpuName).ifBlank { Format.UNKNOWN }, Format.percent(m.cpu.load))
                    ChartCard(series.cpu, 100f, "$seconds с назад", "Сейчас")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Fact("Потоки", m.cpuCores?.toString() ?: Format.UNKNOWN, Modifier.weight(1f))
                        Fact("Нагрузка", Format.percent(m.cpu.load), Modifier.weight(1f))
                        Fact("Работает", Format.uptime(m.uptime), Modifier.weight(1f))
                    }
                    if (m.perCore.isNotEmpty()) {
                        SectionTitle("Загрузка по ядрам")
                        CoreBars(m.perCore)
                    }
                }
                Section.GPU -> {
                    Headline("Видеокарта", Names.gpu(m.gpuName).ifBlank { Format.UNKNOWN }, Format.percent(m.gpu.load))
                    ChartCard(series.gpu, 100f, "$seconds с назад", "Сейчас")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Fact("Видеопамять", m.gpuMemoryTotal?.let { Format.bytes(it) } ?: Format.UNKNOWN, Modifier.weight(1f))
                        Fact("Занято", m.gpuMemoryUsed?.let { Format.bytes(it) } ?: Format.UNKNOWN, Modifier.weight(1f))
                    }
                }
                Section.RAM -> {
                    Headline("Оперативная память", "${Format.bytes(m.ram.used)} из ${Format.bytes(m.ram.total)}", Format.percent(m.ram.percent))
                    ChartCard(series.ram, 100f, "$seconds с назад", "Сейчас")
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Fact("Свободно", if (m.ram.total != null && m.ram.used != null) Format.bytes(m.ram.total - m.ram.used) else Format.UNKNOWN,
                            Modifier.weight(1f))
                        Fact("Всего", Format.bytes(m.ram.total), Modifier.weight(1f))
                    }
                }
                Section.DISKS -> Disks(m)
                Section.NET -> {
                    Headline("Сеть", "Скорость приёма и отдачи", "↓ ${Format.speed(m.netDown)}")
                    val top = (series.down + series.up).maxOrNull()?.coerceAtLeast(1024f) ?: 1024f
                    ChartCard(series.down, top, "$seconds с назад", "Приём", second = series.up)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        Fact("Приём", Format.speed(m.netDown), Modifier.weight(1f))
                        Fact("Отдача", Format.speed(m.netUp), Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Обновление каждые ${Format.seconds(prefs.intervalSeconds)} · меняется в настройках", fontSize = 11.sp, color = Palette.Text3)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(text, fontSize = 13.sp, color = if (selected) Palette.Black else Palette.Text2,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) Palette.accent else Palette.Surface)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun Headline(title: String, subtitle: String, value: String) {
    PanelCard(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, color = Palette.Text)
                Text(subtitle, fontSize = 12.sp, color = Palette.Text3)
            }
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
        }
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

@Composable
private fun Disks(m: Metrics) {
    if (m.disks.isEmpty()) {
        Placeholder("ПК не прислал список дисков.")
        return
    }
    m.disks.forEach { disk ->
        PanelCard(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(disk.name.trimEnd('\\') + if (disk.label.isNotBlank()) "  ${disk.label}" else "", fontSize = 15.sp,
                    color = Palette.Text, modifier = Modifier.weight(1f))
                Text("${(disk.usedFraction * 100).toInt()}%", fontSize = 15.sp, color = Palette.Text)
            }
            Spacer(Modifier.size(8.dp))
            ThinProgress(disk.usedFraction, Modifier.fillMaxWidth())
            Spacer(Modifier.size(6.dp))
            Text("Свободно ${Format.bytes(disk.free)} из ${Format.bytes(disk.total)}", fontSize = 12.sp, color = Palette.Text3)
        }
    }
    Spacer(Modifier.width(1.dp))
}
