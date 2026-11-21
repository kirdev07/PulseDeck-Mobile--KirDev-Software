package com.kirdev.pulsedeckmobile.feature.remote

import androidx.compose.foundation.layout.Box
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.ui.pressScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.ui.rememberActions
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.remote.PcWindow
import com.kirdev.pulsedeckmobile.data.remote.PowerTimer
import com.kirdev.pulsedeckmobile.data.remote.Program
import com.kirdev.pulsedeckmobile.data.remote.Scenario
import com.kirdev.pulsedeckmobile.navigation.Routes

private enum class Power(val title: String, val verb: String) { SHUTDOWN("Выключение", "Выключить"), RESTART("Перезагрузка", "Перезагрузить") }

private enum class Sheet { WINDOWS, PROGRAMS }

/** «Управление A · Пульт» (approved mockup): hold-to-shut-down, timers, a pad of actions, volume. Only what the PC supports. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    var windows by remember { mutableStateOf<List<PcWindow>?>(null) }
    var programs by remember { mutableStateOf<List<Program>?>(null) }
    var scenarios by remember { mutableStateOf<List<Scenario>?>(null) }
    var screenshot by remember { mutableStateOf<ImageBitmap?>(null) }

    // A pending shutdown is shown with its countdown; checked every 5 s only on this screen, ticking in between.
    PollWhileVisible(5_000, connected) { if (connected) timer = api.powerTimer() }
    LaunchedEffect(timer) {
        val t = timer ?: return@LaunchedEffect
        delay(1000)
        if (t.seconds > 1) timer = t.copy(seconds = t.seconds - 1)
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        TopTitle("Управление")
        Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            timer?.let { t ->
                TimerCard(t, Modifier.enter(0)) { actions.run("Отменено") { api.cancelPower().also { timer = null } } }
                Spacer(Modifier.height(10.dp))
            }
            HoldPower(connected, Modifier.enter(1), onShortTap = { actions.say("Удерживайте кнопку 2 секунды") }) {
                actions.run("Выключение через 10 с. Можно отменить.") { api.shutdown(0); timer = api.powerTimer(); null }
            }
            Text("Держите кнопку 2 секунды — ПК выключится.\nТаймер: нажмите на время ниже.", fontSize = 13.sp, color = Palette.Text2,
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp).enter(2))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp).enter(3)) {
                listOf(5 to "5 мин", 15 to "15 мин", 30 to "30 мин", 60 to "1 ч").forEach { (minutes, label) ->
                    TimerChip(label, connected) {
                        confirm = "Выключить ПК через $label? Несохранённые документы могут потеряться. Отменить можно здесь же." to {
                            actions.run("Выключение через $label. Можно отменить.") { api.shutdown(minutes); timer = api.powerTimer(); null }
                        }
                    }
                }
            }
            val pads = listOf(
                Pad(AppIcons.Restart, "Перезагр.", strong = true) { power = Power.RESTART },
                Pad(AppIcons.Moon, "Сон") { confirm = "Перевести ПК в сон? Связь с телефоном прервётся." to { actions.run("ПК засыпает") { api.sleep() } } },
                Pad(AppIcons.Lock, "Замок") {
                    confirm = "Заблокировать ПК? Управление с телефона будет недоступно до разблокировки." to
                        { actions.run("ПК заблокирован") { api.lock() } }
                },
                Pad(AppIcons.Close, "Закрыть") {
                    confirm = "Закрыть активное окно на ПК? Несохранённые данные в нём могут пропасть." to
                        { actions.run("Окно закрыто") { api.closeActiveWindow() } }
                },
                Pad(AppIcons.Window, "Окна") { sheet = Sheet.WINDOWS; actions.run { windows = api.windows(); null } },
                Pad(AppIcons.Apps, "Программы") {
                    sheet = Sheet.PROGRAMS
                    actions.run { programs = api.programs(); scenarios = runCatching { api.scenarios() }.getOrDefault(emptyList()); null }
                },
                Pad(AppIcons.Image, "Скриншот") { actions.run { screenshot = takeScreenshot(api); null } },
                Pad(AppIcons.Pulse, "Процессы", needsPc = false) { go(Routes.PROCESSES) },
                Pad(AppIcons.Link, "Сайт") {
                    input = "Адрес сайта" to { url ->
                        val full = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
                        actions.run("Открыто на ПК") { api.openUrl(full) }
                    }
                },
                Pad(AppIcons.Search, "Поиск") { input = "Что найти" to { q -> actions.run("Открыто на ПК") { api.search(q) } } },
            )
            Column(Modifier.fillMaxWidth().padding(top = 22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pads.chunked(5).forEachIndexed { row, chunk ->
                    Row(Modifier.fillMaxWidth().enter(4 + row)) {
                        chunk.forEach { pad -> PadButton(pad, connected || !pad.needsPc, Modifier.weight(1f)) }
                    }
                }
            }
            volume?.let { v -> VolumeCard(v, Modifier.padding(top = 18.dp).enter(6), onMute = { actions.run { api.setMuted(!v.muted) } }) {
                app.connection.send("volume", (it * 100).toInt())
            } }
            Spacer(Modifier.height(24.dp))
        }
    }

    sheet?.let { which ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = Palette.Surface,
            dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Palette.Outline) }) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 28.dp).verticalScroll(rememberScrollState())) {
                when (which) {
                    Sheet.WINDOWS -> {
                        Text("Открытые окна", fontSize = 22.sp, color = Palette.Text, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        val list = windows
                        when {
                            list == null -> Text("Загрузка…", fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.padding(12.dp))
                            list.isEmpty() -> Text("Открытых окон нет.", fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.padding(12.dp))
                            else -> list.forEach { w -> WindowRow(w, onAction = { action ->
                                val run = { actions.run { api.window(w.hwnd, action); windows = stableOrder(windows, api.windows()); null } }
                                if (action == "close") confirm = "Закрыть окно «${w.title}»?" to run else run()
                            }) }
                        }
                    }
                    Sheet.PROGRAMS -> {
                        Text("Запуск на ПК", fontSize = 22.sp, color = Palette.Text, modifier = Modifier.padding(start = 12.dp, bottom = 8.dp))
                        val list = programs
                        when {
                            list == null -> Text("Загрузка…", fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.padding(12.dp))
                            list.isEmpty() -> Text("На ПК нет настроенных программ (PulseDeck → Программы).", fontSize = 13.sp,
                                color = Palette.Text2, modifier = Modifier.padding(12.dp))
                            else -> list.forEach { p -> ListRow(AppIcons.Apps, p.name, onClick = { actions.run("Запущено: ${p.name}") { api.runProgram(p.id) } }) }
                        }
                        scenarios?.takeIf { it.isNotEmpty() }?.let { list2 ->
                            SectionTitle("Сценарии", Modifier.padding(start = 8.dp))
                            list2.forEach { s -> ListRow(AppIcons.Pulse, s.name, "программ: ${s.count}",
                                onClick = { actions.run("Сценарий запущен") { api.runScenario(s.id) } }) }
                        }
                    }
                }
            }
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
    confirm?.let { (text, run) -> ConfirmDialog("Подтвердите", text, "Да", run, danger = false) { confirm = null } }
    input?.let { (title, run) -> InputDialog(title, "", "Открыть", run) { input = null } }
    screenshot?.let { ScreenshotViewer(it) { screenshot = null } }
}

private class Pad(val icon: ImageVector, val label: String, val strong: Boolean = false, val needsPc: Boolean = true, val onClick: () -> Unit)

@Composable
private fun PadButton(pad: Pad, enabled: Boolean, modifier: Modifier) {
    val source = remember { MutableInteractionSource() }
    val bg = when { !enabled -> Palette.Surface; pad.strong -> Palette.Container; else -> Palette.Surface2 }
    val fg = when { !enabled -> Palette.Disabled; pad.strong -> Palette.OnContainer; else -> Palette.Text }
    Column(modifier.clickable(source, indication = null, enabled = enabled, onClick = pad.onClick),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).pressScale(source).clip(CircleShape).background(bg)
            .indication(source, androidx.compose.material3.ripple()), contentAlignment = Alignment.Center) {
            Icon(pad.icon, null, tint = fg, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(pad.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) Palette.Text else Palette.Disabled,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The big red button: hold it 2 s and the ring fills — then the PC shuts down (in 10 s, cancellable).
 * Letting go earlier undoes the ring; a short tap only says to hold.
 */
@Composable
private fun HoldPower(enabled: Boolean, modifier: Modifier, onShortTap: () -> Unit, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val doneNow by rememberUpdatedState(onDone)
    val tapNow by rememberUpdatedState(onShortTap)
    val ring = Palette.roles.oec
    val track = Palette.Surface2
    val sink = 1f - 0.06f * progress.value
    Box(modifier.size(190.dp).semantics { contentDescription = "Выключить ПК: удерживайте 2 секунды" }
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(onPress = {
                val job = scope.launch {
                    progress.animateTo(1f, tween(HOLD_MS, easing = LinearEasing))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    doneNow()
                    progress.animateTo(0f, tween(400))
                }
                val started = System.currentTimeMillis()
                tryAwaitRelease()
                if (progress.value < 1f) {
                    job.cancel()
                    scope.launch { progress.animateTo(0f, tween(250)) }
                    if (System.currentTimeMillis() - started < 350) tapNow()
                }
            })
        }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
            if (progress.value > 0f) drawArc(ring, -90f, 360f * progress.value, false, Offset(inset, inset), arc,
                style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(Modifier.size(154.dp).graphicsLayer { scaleX = sink; scaleY = sink }.clip(CircleShape)
            .background(if (enabled) Palette.ErrorContainer else Palette.Surface),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            val fg = if (enabled) Palette.OnErrorContainer else Palette.Disabled
            Icon(AppIcons.Power, null, tint = fg, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(6.dp))
            Text(if (enabled) "Удерживайте" else "Нет связи", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

private const val HOLD_MS = 2000

@Composable
private fun TimerChip(label: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(Modifier.height(36.dp).clip(shape).border(1.dp, if (enabled) Palette.Outline else Palette.Disabled, shape)
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) Palette.Text else Palette.Disabled)
    }
}

@Composable
private fun TimerCard(t: PowerTimer, modifier: Modifier, onCancel: () -> Unit) {
    val what = if (t.action == "restart") "Перезагрузка" else "Выключение"
    val pulse = com.kirdev.pulsedeckmobile.core.ui.breathe(true, 1600)
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.ErrorContainer)
        .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(AppIcons.Power, null, tint = Palette.OnErrorContainer.copy(alpha = 0.4f + 0.6f * pulse), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$what через ${t.seconds / 60}:${(t.seconds % 60).toString().padStart(2, '0')}", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = Palette.OnErrorContainer)
            Text("Несохранённые документы могут потеряться", fontSize = 12.sp, color = Palette.OnErrorContainer.copy(alpha = 0.8f))
        }
        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Palette.OnErrorContainer).clickable(onClick = onCancel)
            .padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text("Отменить", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.ErrorContainer)
        }
    }
}

@Composable
private fun VolumeCard(v: com.kirdev.pulsedeckmobile.data.model.SystemVolume, modifier: Modifier, onMute: () -> Unit, onSet: (Float) -> Unit) {
    var value by remember(v.level) { mutableFloatStateOf(v.level / 100f) }
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onMute).semantics { contentDescription = if (v.muted) "Включить звук" else "Выключить звук" },
            contentAlignment = Alignment.Center) {
            Icon(AppIcons.Volume, null, tint = if (v.muted) Palette.Disabled else Palette.Text2, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            com.kirdev.pulsedeckmobile.core.ui.M3Slider(value, onMove = { value = it }, label = "Громкость ПК", onDone = { value = it; onSet(it) })
        }
        Spacer(Modifier.width(10.dp))
        Text(if (v.muted) "без звука" else "${(value * 100).toInt()}", style = Numbers.copy(color = Palette.Text2))
    }
}

/** Shutdown and restart: when, with an explicit confirmation. The PC allows cancelling. */
@Composable
private fun PowerDialog(kind: Power, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val options = listOf(0 to "Сейчас", 5 to "5 мин", 15 to "15 мин", 30 to "30 мин", 60 to "1 ч")
    var minutes by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Palette.Surface2, shape = RoundedCornerShape(28.dp),
        icon = { Icon(if (kind == Power.SHUTDOWN) AppIcons.Power else AppIcons.Restart, null, tint = Palette.Error) },
        title = { Text("${kind.verb} ПК?", color = Palette.Text, fontSize = 24.sp) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (minutes == 0) "Через 10 секунд. Несохранённые документы могут потеряться. Отменить можно здесь же."
                    else "Через $minutes мин. Несохранённые документы могут потеряться. Отменить можно здесь же до срока.",
                    fontSize = 14.sp, color = Palette.Text2, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { (value, label) ->
                        com.kirdev.pulsedeckmobile.core.ui.ChoiceChip(label, value == minutes) { minutes = value }
                    }
                }
            }
        },
        confirmButton = {
            Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Error)
                .clickable { onDismiss(); onConfirm(minutes) }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                Text(kind.verb, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Palette.roles.onErr)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена", color = Palette.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) } },
    )
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
                Text(label, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = Palette.OnTonal,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Palette.Tonal).clickable { onAction(action) }
                        .padding(horizontal = 10.dp, vertical = 6.dp))
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}
