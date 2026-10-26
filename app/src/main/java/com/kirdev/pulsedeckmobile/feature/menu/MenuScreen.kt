package com.kirdev.pulsedeckmobile.feature.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.navigation.Routes

/** Everything that is not a tab. Only sections that work; no «coming soon» entries. */
@Composable
fun MenuScreen(app: PulseDeskApp, go: (String) -> Unit) {
    val media by app.connection.media.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        TopTitle("Меню")
        PanelCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            ListRow(AppIcons.Music, "Музыка", media?.let { "${it.title} · ${it.source}" } ?: "Сейчас ничего не играет",
                chevron = true, onClick = { go(Routes.MUSIC) })
            ListRow(AppIcons.Apps, "Процессы", "Что запущено на ПК, завершение", chevron = true, onClick = { go(Routes.PROCESSES) })
            ListRow(AppIcons.Devices, "Устройства", "Компьютеры, переключение, новый ПК", chevron = true, onClick = { go(Routes.DEVICES) })
            ListRow(AppIcons.Bell, "События ПК", "Блокировка, предупреждения о нагрузке", chevron = true, onClick = { go(Routes.EVENTS) })
        }
        PanelCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            ListRow(AppIcons.Settings, "Настройки", "Подключение, мониторинг, музыка, безопасность", chevron = true,
                onClick = { go(Routes.SETTINGS) })
            ListRow(AppIcons.Brush, "Оформление", "Тема, акцент, анимации", chevron = true, onClick = { go(Routes.APPEARANCE) })
        }
    }
}
