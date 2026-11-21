package com.kirdev.pulsedeckmobile.feature.applications

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.theme.Tone
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ChoiceChip
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.appear
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.ui.rememberActions
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.remote.ApiError
import com.kirdev.pulsedeckmobile.data.remote.PcProcess

private enum class Sort(val title: String) { CPU("Процессор"), MEMORY("Память"), NAME("Имя") }

private val TONES = listOf(Tone.Violet, Tone.Blue, Tone.Teal, Tone.Pink, Tone.Amber)

/**
 * «Процессы» (approved mockup): search, sort, a load bar on every row; protected ones carry a lock.
 * The PC has no list of installed programs, so there is no such tab. Refreshed every 5 s only while open.
 */
@Composable
fun ProcessesScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val actions = rememberActions()
    var list by remember { mutableStateOf<List<PcProcess>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(Sort.CPU) }
    var kill by remember { mutableStateOf<PcProcess?>(null) }

    PollWhileVisible(5_000) {
        try {
            list = app.api.processes()
            error = null
        } catch (e: ApiError) {
            error = if (e.status == 404) "PulseDeck на ПК не умеет показывать процессы. Обновите PulseDeck." else e.message
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Процессы", onBack) {
            list?.let { Text("${it.size}", fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.padding(end = 12.dp)) }
        }
        // Material 3 search bar: a pill on the high surface.
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp))
            .background(Palette.Surface2).padding(start = 18.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Search, null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                query, { query = it }, singleLine = true, modifier = Modifier.weight(1f),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Palette.Text),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Palette.accent),
                decorationBox = { field ->
                    if (query.isEmpty()) Text("Поиск процесса", fontSize = 16.sp, color = Palette.Text2)
                    field()
                },
            )
            if (query.isNotEmpty()) IconButtonSmall(AppIcons.Close, "Очистить", size = 44.dp, onClick = { query = "" })
        }
        Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sort.entries.forEach { s -> ChoiceChip(s.title, s == sort) { sort = s } }
        }
        val shown = list?.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }?.let { l ->
            when (sort) {
                Sort.CPU -> l.sortedByDescending { it.cpu }
                Sort.MEMORY -> l.sortedByDescending { it.memory }
                Sort.NAME -> l.sortedBy { it.name.lowercase() }
            }
        }
        when {
            error != null && list == null -> Placeholder(error!!)
            shown == null -> Placeholder("Загрузка…")
            shown.isEmpty() -> Placeholder("Ничего не найдено.")
            else -> {
                val top = (shown.maxOfOrNull { it.memory } ?: 1L).coerceAtLeast(1L)
                LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(top = 6.dp, bottom = 110.dp)) {
                    itemsIndexed(shown, key = { _, it -> it.pid }) { i, p ->
                        val bar = if (sort == Sort.MEMORY) p.memory.toFloat() / top else (p.cpu / 100.0 * 4).toFloat()
                        ProcessRow(p, TONES[(p.name.firstOrNull()?.code ?: 0) % TONES.size], bar, Modifier.enter(i.coerceAtMost(8))) { kill = p }
                    }
                    item {
                        Text("CPU — в % от всего процессора. Процессы с замком PulseDeck не завершает.", fontSize = 11.sp, color = Palette.Text3,
                            modifier = Modifier.padding(4.dp))
                    }
                }
            }
        }
    }
    kill?.let { p ->
        ConfirmDialog("Завершить «${p.name}»?", "Несохранённые данные этой программы пропадут. Системные процессы PulseDeck не трогает.",
            "Завершить", {
                actions.run { app.api.killProcess(p.pid).also { list = list?.filterNot { it.pid == p.pid } } }
            }) { kill = null }
    }
}

@Composable
private fun ProcessRow(p: PcProcess, tone: Tone, bar: Float, modifier: Modifier, onKill: () -> Unit) {
    val hot = p.cpu > 5.0
    val ink = if (hot) Tone.Pink.fg else Palette.accent
    val grow = appear(250, 700)
    val shown by animateFloatAsState(bar.coerceIn(0.03f, 1f), tween(500), label = "load")
    val track = Palette.Track
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tone.bg), contentAlignment = Alignment.Center) {
            Text(p.name.firstOrNull()?.uppercase() ?: "?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = tone.fg)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.name.ifBlank { "PID ${p.pid}" }, fontSize = 15.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f))
                Text("${"%.1f".format(p.cpu)}%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink)
            }
            Canvas(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(4.dp)) {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(track, cornerRadius = r)
                drawRoundRect(ink, size = Size(size.width * shown * grow, size.height), cornerRadius = r)
            }
            Text("${Format.bytes(p.memory)} · PID ${p.pid}", fontSize = 12.sp, color = Palette.Text2)
        }
        if (p.protected) Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Lock, "Системный — не завершается", tint = Palette.Text3, modifier = Modifier.size(18.dp))
        } else IconButtonSmall(AppIcons.Close, "Завершить ${p.name}", size = 44.dp, onClick = onKill)
    }
}
