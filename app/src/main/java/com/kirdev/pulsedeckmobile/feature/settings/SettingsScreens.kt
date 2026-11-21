package com.kirdev.pulsedeckmobile.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.BuildConfigInfo
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.currentDisplay
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PanelSwitch
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.local.Prefs
import com.kirdev.pulsedeckmobile.data.update.UpdateState
import com.kirdev.pulsedeckmobile.feature.pulse.KiroXMotion
import com.kirdev.pulsedeckmobile.navigation.Routes
import com.kirdev.pulsedeckmobile.monitoring.AlertCenter
import com.kirdev.pulsedeckmobile.monitoring.Limit
import androidx.compose.runtime.setValue

/** Screen 12. Every switch here changes real behaviour; nothing decorative. */
@Composable
fun SettingsScreen(app: PulseDeskApp, onBack: () -> Unit, go: (String) -> Unit) {
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    val update: ((Prefs) -> Prefs) -> Unit = app.settings::update
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Настройки", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Подключение")
            PanelCard(Modifier.fillMaxWidth()) {
                ListRow(AppIcons.Devices, "Компьютеры", "Сохранённые ПК, переключение, новый ПК", chevron = true, onClick = { go(Routes.DEVICES) })
                ListRow(AppIcons.Usb, "USB-кабель", "Включите «Отладку по USB» — PulseDeck сам переключит связь на кабель")
            }

            SectionTitle("Мониторинг")
            PanelCard(Modifier.fillMaxWidth()) {
                Text("Частота обновления", fontSize = 14.sp, color = Palette.Text)
                Text("Реже — меньше нагрузка на ПК и батарею. В фоне показатели не запрашиваются.", fontSize = 12.sp, color = Palette.Text3)
                Spacer(Modifier.height(8.dp))
                Chips(listOf(1.0, 1.5, 3.0, 5.0, 10.0), prefs.intervalSeconds, Format::seconds) { v -> update { it.copy(intervalSeconds = v) } }
            }

            SectionTitle("Музыка")
            PanelCard(Modifier.fillMaxWidth()) {
                Toggle("Карточка в шторке", "Обложка и кнопки на экране блокировки, пока на ПК играет трек. Без трека карточки нет.",
                    prefs.mediaNotification) { on -> update { it.copy(mediaNotification = on) } }
                Toggle("Громкость кнопками телефона", "Кнопки громкости меняют звук ПК, пока трек играет (не на паузе).",
                    prefs.remoteVolume) { on -> update { it.copy(remoteVolume = on) } }
                Toggle("Ползунок волной", "Полоса времени трека волной, пока он играет, — как в плеере Android и One UI.",
                    prefs.waveSeek) { on -> update { it.copy(waveSeek = on) } }
            }

            SectionTitle("Предупреждения")
            Alerts(prefs, update)

            SectionTitle("Интерфейс")
            PanelCard(Modifier.fillMaxWidth()) {
                ListRow(AppIcons.Palette, "Оформление", (if (prefs.style == "mono") "Чёрно-белый" else "Цветной") + ", " +
                    mapOf("dark" to "тёмная", "light" to "светлая", "system" to "как в системе")[prefs.mode].orEmpty(), chevron = true,
                    onClick = { go(Routes.APPEARANCE) })
            }

            SectionTitle("Безопасность")
            PanelCard(Modifier.fillMaxWidth()) {
                Text("Связь только по TLS с проверкой сертификата ПК. Ключ доступа хранится зашифрованным (Android Keystore). " +
                    "Выключение, перезагрузка, сон, завершение процессов и удаление файлов всегда спрашивают подтверждение. " +
                    "Права телефона («полный доступ» или «только просмотр») задаются на ПК: PulseDeck → «Телефон».",
                    fontSize = 12.sp, color = Palette.Text2, lineHeight = 17.sp)
            }

            SectionTitle("О приложении")
            PanelCard(Modifier.fillMaxWidth()) {
                Text("PulseDesk ${BuildConfigInfo.version(app)}", fontSize = 14.sp, color = Palette.Text)
                Text("Клиент PulseDeck для Windows · KiroX Systems", fontSize = 12.sp, color = Palette.Text3)
            }

            SectionTitle("Обновления")
            Updates(app)
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Screen 13: style, theme, colour, AMOLED, animations — with a live preview (Material 3 mockups). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    val set: ((Prefs) -> Prefs) -> Unit = app.settings::update
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Оформление", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Live preview: the card, a bar and the buttons in the chosen colours.
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Palette.Surface).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Container), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(AppIcons.Monitor, null, tint = Palette.OnContainer, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DESKTOP-PC", fontSize = 15.sp, color = Palette.Text)
                    com.kirdev.pulsedeckmobile.core.ui.ThinProgress(0.62f, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.height(22.dp).size(54.dp, 22.dp).clip(RoundedCornerShape(6.dp)).background(Palette.Tonal))
                        Box(Modifier.size(40.dp, 22.dp).clip(RoundedCornerShape(11.dp)).background(Palette.accent))
                    }
                }
            }

            SectionTitle("Стиль")
            com.kirdev.pulsedeckmobile.core.ui.Segmented(listOf("color" to "Цветной", "mono" to "Чёрно-белый"), prefs.style) { v ->
                set { it.copy(style = v) }
            }
            SectionTitle("Тема")
            com.kirdev.pulsedeckmobile.core.ui.Segmented(listOf("dark" to "Тёмная", "light" to "Светлая", "system" to "Как в системе"),
                prefs.mode) { v -> set { it.copy(mode = v) } }

            if (prefs.style != "mono") {
                SectionTitle("Цвет")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (android.os.Build.VERSION.SDK_INT >= 31) {
                        // Material You: four quarters hint at «from the wallpaper».
                        val on = prefs.accentKey == "wallpaper"
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                            .border(if (on) 3.dp else 0.dp, if (on) Palette.Text else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { set { it.copy(accentKey = "wallpaper") } }) {
                            Column(Modifier.fillMaxSize()) {
                                Row(Modifier.weight(1f)) {
                                    Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFC2A5F9)))
                                    Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF5B8C4)))
                                }
                                Row(Modifier.weight(1f)) {
                                    Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF9CD3C7)))
                                    Box(Modifier.weight(1f).fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF2CB86)))
                                }
                            }
                        }
                    }
                    for ((key, _, color) in com.kirdev.pulsedeckmobile.core.theme.AccentChoices) {
                        val on = prefs.accentKey == key
                        Box(Modifier.size(48.dp).clip(CircleShape)
                            .border(3.dp, if (on) Palette.Text else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                            .padding(5.dp).clip(CircleShape).background(color)
                            .clickable { set { it.copy(accentKey = key) } })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(when (prefs.accentKey) {
                    "wallpaper" -> "Цвета ваших обоев (Material You)."
                    else -> com.kirdev.pulsedeckmobile.core.theme.AccentChoices.firstOrNull { it.first == prefs.accentKey }?.second.orEmpty()
                }, fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.padding(start = 4.dp))
            }

            SectionTitle("Экран")
            PanelCard(Modifier.fillMaxWidth(), color = Palette.roles.s1) {
                Toggle("Чёрный фон AMOLED", if (Palette.roles.dark) "Чистый чёрный: экономит заряд на OLED-экранах."
                    else "Работает в тёмной теме.", prefs.amoled) { on ->
                    set { it.copy(amoled = on) }
                }
                Toggle("Анимации", "Волна, переходы, полоски «играет». Если в Android анимации выключены, они выключены и здесь.",
                    prefs.animations) { on -> set { it.copy(animations = on) } }
                Toggle("Ползунок волной", "Полоса времени трека волной, пока он играет.", prefs.waveSeek) { on -> set { it.copy(waveSeek = on) } }
                Toggle("Поворот экрана", "Альбомная ориентация, когда телефон повёрнут (если в Android включён автоповорот). " +
                    "Выключено — всегда книжная.", prefs.rotate) { on -> set { it.copy(rotate = on) } }
            }
            SectionTitle("Частота кадров")
            val activity = androidx.activity.compose.LocalActivity.current
            val top = com.kirdev.pulsedeckmobile.core.ui.Frames.maxRate(activity?.let { it.currentDisplay() })
            val names = mapOf("auto" to "Как в системе", "eco" to "Экономно · 60", "max" to "Максимум · $top Гц")
            Chips(listOf("auto", "eco") + (if (top > 60) listOf("max") else emptyList()), prefs.frameRate,
                { names[it].orEmpty() }) { v -> set { it.copy(frameRate = v) } }
            Spacer(Modifier.height(6.dp))
            Text("Экономно — экран переключается на 60 Гц и меньше тратит батарею. Максимум — самая плавная прокрутка и анимации. " +
                "Некоторые оболочки (например, MIUI) держат свою частоту из настроек телефона.",
                fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.padding(horizontal = 4.dp))
            SectionTitle("Облегчённый режим")
            Chips(listOf("auto", "on", "off"), prefs.lite, { mapOf("auto" to "Авто", "on" to "Включён", "off" to "Выключен")[it].orEmpty() }) { v ->
                set { it.copy(lite = v) }
            }
            Spacer(Modifier.height(6.dp))
            Text("Для слабых телефонов: анимации 30 кадров, без переходов между экранами и полосок «играет». Этот телефон: " +
                com.kirdev.pulsedeckmobile.core.ui.Device.summary() + ".",
                fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** «Предупреждения»: limits, the «no connection» notice and system notifications. */
@Composable
private fun Alerts(prefs: Prefs, update: ((Prefs) -> Prefs) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var allowed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(AlertCenter.allowed(context)) }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted
        val activity = context as? android.app.Activity
        if (!granted && activity != null && !activity.shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
            openNotificationSettings(context)
        }
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {      // back from the system settings
        allowed = AlertCenter.allowed(context)
        onPauseOrDispose { }
    }
    PanelCard(Modifier.fillMaxWidth()) {
        Toggle("Сообщать о нагрузке", "Если значение держится за порогом 10 секунд. Диск и батарея ПК — когда осталось меньше порога. " +
            "Показатели приходят, пока приложение открыто.", prefs.alerts) { on -> update { it.copy(alerts = on) } }
        if (prefs.alerts) {
            for (limit in Limit.entries) LimitRow(limit, prefs.limit(limit)) { v -> update { it.copy(limits = it.limits + (limit to v)) } }
        }
        Toggle("Сообщать, если пропала связь", "Через 30 секунд без связи с ПК — и в фоне, пока в шторке карточка трека.",
            prefs.alertOffline) { on -> update { it.copy(alertOffline = on) } }
        Toggle("Системные уведомления",
            if (allowed) "Предупреждения приходят и в шторку телефона." else "Android не разрешает уведомления PulseDesk. Нажмите, чтобы разрешить.",
            prefs.notify && allowed) { on ->
            if (on && !allowed) {
                // Android's own question first; once it no longer shows (denied twice), the app's settings.
                if (android.os.Build.VERSION.SDK_INT >= 33) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(context)
            }
            update { it.copy(notify = on) }
        }
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun LimitRow(limit: Limit, value: Int, onChange: (Int) -> Unit) {
    var current by androidx.compose.runtime.remember(value) { androidx.compose.runtime.mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(limit.title, fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.weight(1f))
            Text("${if (limit.over) "≥" else "≤"} ${current.toInt()}%", fontSize = 13.sp, color = Palette.Text)
        }
        androidx.compose.material3.Slider(
            value = current, onValueChange = { current = it }, onValueChangeFinished = { onChange(current.toInt()) },
            valueRange = limit.range.first.toFloat()..limit.range.last.toFloat(),
            colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = Palette.accent, activeTrackColor = Palette.accent,
                inactiveTrackColor = Palette.Track),
        )
    }
}

/** GitHub Releases: check, show what is new, download; Android's installer does the rest. */
@Composable
private fun Updates(app: PulseDeskApp) {
    val state by app.updater.state.collectAsStateWithLifecycle()
    val current = app.updater.current
    val check = "Проверить" to { app.updater.check() }
    val (text, action) = when (val s = state) {
        UpdateState.Idle -> "Установлена версия $current." to check
        UpdateState.Checking -> "Проверяю обновления…" to null
        is UpdateState.UpToDate -> "Установлена последняя версия ${s.version}." to check
        is UpdateState.Available -> "Доступна версия ${s.release.version} (у вас $current)." to
            ("Скачать и установить" to { app.updater.download(s.release) })
        is UpdateState.Downloading -> "Скачиваю ${s.release.version}: ${(s.fraction * 100).toInt()}%" to null
        is UpdateState.Ready -> ("Версия ${s.release.version} скачана. Подтвердите установку в окне Android. " +
            "Если окна нет — разрешите PulseDesk устанавливать приложения и нажмите «Установить».") to
            ("Установить" to { app.updater.install(s.file) })
        is UpdateState.Failed -> s.message to check
    }
    val notes = (state as? UpdateState.Available)?.release?.notes.orEmpty().replace("**", "").replace("`", "").trim()
    PanelCard(Modifier.fillMaxWidth()) {
        Text(text, fontSize = 14.sp, color = Palette.Text)
        if (notes.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(notes.take(600), fontSize = 12.sp, color = Palette.Text2, lineHeight = 17.sp)
        }
        if (action != null) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = action.second) { Text(action.first, color = Palette.accent, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold) }
            }
        }
    }
}

@Composable
private fun Toggle(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 14.sp, color = Palette.Text)
            Text(text, fontSize = 12.sp, color = Palette.Text3)
        }
        PanelSwitch(checked, onChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> Chips(options: List<T>, selected: T, label: (T) -> String, onClick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            val on = option == selected
            com.kirdev.pulsedeckmobile.core.ui.ChoiceChip(label(option), on) { onClick(option) }
        }
    }
}
