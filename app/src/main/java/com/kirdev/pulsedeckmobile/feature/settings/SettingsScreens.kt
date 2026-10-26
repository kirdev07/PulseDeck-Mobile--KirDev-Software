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
import com.kirdev.pulsedeckmobile.core.theme.Accents
import com.kirdev.pulsedeckmobile.core.theme.AppTheme
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PanelSwitch
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.local.Prefs
import com.kirdev.pulsedeckmobile.feature.pulse.KiroXMotion
import com.kirdev.pulsedeckmobile.navigation.Routes

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
            }

            SectionTitle("Интерфейс")
            PanelCard(Modifier.fillMaxWidth()) {
                ListRow(AppIcons.Brush, "Оформление", "${prefs.theme.title}, анимации", chevron = true, onClick = { go(Routes.APPEARANCE) })
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
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Screen 13: theme, accent, animations, with a live preview. */
@Composable
fun AppearanceScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val prefs by app.settings.values.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Оформление", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            SectionTitle("Тема")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (theme in AppTheme.entries) {
                    val selected = theme == prefs.theme
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(theme.surface)
                            .border(if (selected) 2.dp else 1.dp, if (selected) Palette.accent else theme.border, RoundedCornerShape(16.dp))
                            .clickable { app.settings.update { it.copy(theme = theme) } }.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(10.dp)).background(theme.black),
                            contentAlignment = Alignment.Center) { KiroXMotion(animate = false) }
                        Spacer(Modifier.height(8.dp))
                        Text(theme.title, fontSize = 13.sp, color = Palette.Text)
                    }
                }
            }
            SectionTitle("Цвет акцента")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Accents.forEachIndexed { index, color ->
                    Box(Modifier.size(40.dp).clip(CircleShape).background(color)
                        .border(if (index == prefs.accent) 3.dp else 0.dp, Palette.Text2, CircleShape)
                        .clickable { app.settings.update { it.copy(accent = index) } })
                }
            }
            SectionTitle("Анимации")
            PanelCard(Modifier.fillMaxWidth()) {
                Toggle("Анимации", "Волна и плавные переходы. Если в Android анимации выключены, они выключены и здесь.",
                    prefs.animations) { on -> app.settings.update { it.copy(animations = on) } }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { KiroXMotion(animate = prefs.animations) }
            }
            Spacer(Modifier.height(24.dp))
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
            Text(label(option), fontSize = 13.sp, color = if (on) Palette.Black else Palette.Text2,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Palette.accent else Palette.Surface2)
                    .clickable { onClick(option) }.padding(horizontal = 14.dp, vertical = 7.dp))
        }
    }
}
