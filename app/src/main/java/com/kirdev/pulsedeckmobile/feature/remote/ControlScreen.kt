package com.kirdev.pulsedeckmobile.feature.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Numbers
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.InputDialog
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.ui.rememberActions
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.remote.PcWindow
import com.kirdev.pulsedeckmobile.data.remote.PowerTimer
import com.kirdev.pulsedeckmobile.data.remote.Program
import com.kirdev.pulsedeckmobile.data.remote.Scenario
import com.kirdev.pulsedeckmobile.feature.media.SeekBar
import com.kirdev.pulsedeckmobile.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Power(val title: String, val verb: String) { SHUTDOWN("Выключение", "Выключить"), RESTART("Перезагрузка", "Перезагрузить") }

/** Screen 08: power, volume, windows, programs, scenarios, web, screenshot. Only what the PC supports. */
@Composable
fun ControlScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val api = app.api
    val actions = rememberActions()
    val state by app.connection.state.collectAsStateWithLifecycle()
    val volume by app.connection.volume.collectAsStateWithLifecycle()
    val connected = state is ConnectionState.Connected
    var timer by remember { mutableStateOf<PowerTimer?>(null) }
    var power by remember { mutableStateOf<Power?>(null) }
    var confirm by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var input by remember { mutableStateOf<Pair<String, (String) -> Unit>?>(null) }
    var windows by remember { mutableStateOf<List<PcWindow>?>(null) }
    var programs by remember { mutableStateOf<List<Program>?>(null) }
    var scenarios by remember { mutableStateOf<List<Scenario>?>(null) }
    var screenshot by remember { mutableStateOf<ImageBitmap?>(null) }

    // A pending shutdown is shown with its countdown; checked every 5 s only on this screen.
    PollWhileVisible(5_000, connected) { if (connected) timer = api.powerTimer() }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        TopTitle("Управление")
        Column(Modifier.padding(horizontal = 16.dp)) {
            timer?.let { t ->
                PanelCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    val what = if (t.action == "restart") "Перезагрузка" else "Выключение"
                    Text("$what через ${t.seconds / 60} мин ${t.seconds % 60} с", fontSize = 15.sp, color = Palette.Text)
                    TextButton(onClick = { actions.run("Отменено") { api.cancelPower().also { timer = null } } }) {
                        Text("Отменить", color = Palette.Text)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(AppIcons.Power, "Выключить", Modifier.weight(1f)) { power = Power.SHUTDOWN }
                Tile(AppIcons.Restart, "Перезагрузить", Modifier.weight(1f)) { power = Power.RESTART }
                Tile(AppIcons.Moon, "Сон", Modifier.weight(1f)) {
                    confirm = "Перевести ПК в сон? Связь с телефоном прервётся." to { actions.run("ПК засыпает") { api.sleep() } }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(AppIcons.Lock, "Блокировка", Modifier.weight(1f)) {
                    confirm = "Заблокировать ПК? Управление с телефона будет недоступно до разблокировки." to
                        { actions.run("ПК заблокирован") { api.lock() } }
                }
                Tile(AppIcons.Close, "Закрыть окно", Modifier.weight(1f)) {
                    confirm = "Закрыть активное окно на ПК? Несохранённые данные в нём могут пропасть." to
                        { actions.run("Окно закрыто") { api.closeActiveWindow() } }
                }
                Tile(AppIcons.Apps, "Процессы", Modifier.weight(1f)) { go(Routes.PROCESSES) }
            }

            volume?.let { v ->
                SectionTitle("Громкость ПК")
                PanelCard(Modifier.fillMaxWidth()) {
                    var value by remember(v.level) { mutableFloatStateOf(v.level / 100f) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Volume, null, tint = if (v.muted) Palette.Disabled else Palette.Text2,
                            modifier = Modifier.size(22.dp).clip(RoundedCornerShape(6.dp))
                                .clickable { actions.run { api.setMuted(!v.muted) } })
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            SeekBar(value, onDrag = { value = it }) { value = it; app.connection.send("volume", (it * 100).toInt()) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(if (v.muted) "без звука" else "${(value * 100).toInt()}", style = Numbers.copy(color = Palette.Text3))
                    }
                }
            }

            SectionTitle("Дополнительно")
            PanelCard(Modifier.fillMaxWidth()) {
                ListRow(AppIcons.Window, "Окна", "Свернуть, развернуть, закрыть", chevron = windows == null, onClick = {
                    actions.run { windows = api.windows(); null }
                })
                windows?.let { list ->
                    if (list.isEmpty()) Text("Открытых окон нет.", fontSize = 12.sp, color = Palette.Text3, modifier = Modifier.padding(12.dp))
                    list.forEach { w -> WindowRow(w, onAction = { action ->
                        val run = { actions.run { api.window(w.hwnd, action); windows = stableOrder(windows, api.windows()); null } }
                        if (action == "close") confirm = "Закрыть окно «${w.title}»?" to run else run()
                    }) }
                }
                ListRow(AppIcons.Apps, "Запустить программу", "Программы, настроенные в PulseDeck на ПК", chevron = programs == null,
                    onClick = { actions.run { programs = api.programs(); null } })
                programs?.let { list ->
                    if (list.isEmpty()) Text("На ПК нет настроенных программ (PulseDeck → Программы).", fontSize = 12.sp,
                        color = Palette.Text3, modifier = Modifier.padding(12.dp))
                    list.forEach { p -> ListRow(null, "   ${p.name}", onClick = { actions.run("Запущено: ${p.name}") { api.runProgram(p.id) } }) }
                }
                ListRow(AppIcons.Pulse, "Сценарии", "Несколько программ одной кнопкой", chevron = scenarios == null,
                    onClick = { actions.run { scenarios = api.scenarios(); null } })
                scenarios?.let { list ->
                    if (list.isEmpty()) Text("Сценариев нет.", fontSize = 12.sp, color = Palette.Text3, modifier = Modifier.padding(12.dp))
                    list.forEach { s -> ListRow(null, "   ${s.name}", "   программ: ${s.count}",
                        onClick = { actions.run("Сценарий запущен") { api.runScenario(s.id) } }) }
                }
                ListRow(AppIcons.Link, "Открыть сайт", "В браузере на ПК", chevron = true, onClick = {
                    input = "Адрес сайта" to { url ->
                        val full = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                        actions.run("Открыто на ПК") { api.openUrl(full) }
                    }
                })
                ListRow(AppIcons.Search, "Поиск в интернете", "Google на ПК", chevron = true, onClick = {
                    input = "Что найти" to { q -> actions.run("Открыто на ПК") { api.search(q) } }
                })
                ListRow(AppIcons.Image, "Скриншот экрана", "Снимок главного монитора", chevron = true, onClick = {
                    actions.run {
                        val bytes = api.screenshot(0, 1600)
                        screenshot = withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
                        null
                    }
                })
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    power?.let { kind ->
        PowerDialog(kind, onDismiss = { power = null }) { minutes ->
            val text = if (minutes == 0) "${kind.title} через 10 с. Можно отменить." else "${kind.title} через $minutes мин. Можно отменить."
            actions.run(text) {
                if (kind == Power.SHUTDOWN) api.shutdown(minutes) else api.restart(minutes)
                timer = api.powerTimer()
                null
            }
        }
    }
    confirm?.let { (text, run) -> ConfirmDialog("Подтвердите", text, "Да", run) { confirm = null } }
    input?.let { (title, run) -> InputDialog(title, "", "Открыть", run) { input = null } }
    screenshot?.let { image ->
        // Full screen on black; a tap anywhere closes it.
        Dialog(onDismissRequest = { screenshot = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Image(image, "Скриншот ПК", contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().background(Palette.Black).clickable { screenshot = null })
        }
    }
}

/** Shutdown and restart: when, with an explicit confirmation. The PC allows cancelling. */
@Composable
private fun PowerDialog(kind: Power, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val options = listOf(0 to "Через 10 секунд", 5 to "Через 5 минут", 15 to "Через 15 минут", 30 to "Через 30 минут", 60 to "Через час")
    var minutes by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.Surface2,
        title = { Text("${kind.verb} ПК?", color = Palette.Text) },
        text = {
            Column {
                Text("Несохранённые данные на ПК могут пропасть.", fontSize = 13.sp, color = Palette.Text2)
                Spacer(Modifier.height(8.dp))
                options.forEach { (value, label) ->
                    Text((if (value == minutes) "●  " else "○  ") + label, fontSize = 14.sp,
                        color = if (value == minutes) Palette.Text else Palette.Text2,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { minutes = value }
                            .padding(vertical = 8.dp, horizontal = 4.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { onDismiss(); onConfirm(minutes) }) { Text(kind.verb, color = Palette.Text) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.Text2) } },
    )
}

@Composable
private fun Tile(icon: ImageVector, title: String, modifier: Modifier, onClick: () -> Unit) {
    PanelCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Palette.Text, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 12.sp, color = Palette.Text2, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

/** The PC lists windows in z-order, which changes after every action; keep rows where they were. */
internal fun stableOrder(old: List<PcWindow>?, fresh: List<PcWindow>): List<PcWindow> {
    if (old == null) return fresh
    val byHwnd = fresh.associateBy { it.hwnd }
    val kept = old.mapNotNull { byHwnd[it.hwnd] }
    return kept + fresh.filter { w -> kept.none { it.hwnd == w.hwnd } }
}

@Composable
private fun WindowRow(window: PcWindow, onAction: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, bottom = 6.dp)) {
        Text(window.title, fontSize = 13.sp, color = Palette.Text, maxLines = 1)
        Row {
            for ((action, label) in listOf("focus" to "Показать", "min" to "Свернуть", "max" to "Развернуть", "close" to "Закрыть")) {
                Text(label, fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Palette.Surface2).clickable { onAction(action) }.padding(horizontal = 10.dp, vertical = 5.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}
