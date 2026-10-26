package com.kirdev.pulsedeckmobile.feature.applications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelTextField
import com.kirdev.pulsedeckmobile.core.ui.Placeholder
import com.kirdev.pulsedeckmobile.core.ui.PollWhileVisible
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.rememberActions
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.remote.ApiError
import com.kirdev.pulsedeckmobile.data.remote.PcProcess

/**
 * Screen 10 («Приложения» → running processes). The PC has no list of installed programs,
 * so there is no such tab. Refreshed every 5 s only while the screen is open.
 */
@Composable
fun ProcessesScreen(app: PulseDeskApp, onBack: () -> Unit) {
    val actions = rememberActions()
    var list by remember { mutableStateOf<List<PcProcess>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
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
        ScreenHeader("Процессы", onBack)
        PanelTextField(query, { query = it }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), placeholder = "Поиск процесса")
        val shown = list?.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        when {
            error != null && list == null -> Placeholder(error!!)
            shown == null -> Placeholder("Загрузка…")
            shown.isEmpty() -> Placeholder("Ничего не найдено.")
            else -> {
                Text("Сначала самые нагруженные · CPU в % от всего процессора", fontSize = 11.sp, color = Palette.Text3,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
                LazyColumn(Modifier.padding(horizontal = 8.dp)) {
                    items(shown, key = { it.pid }) { p ->
                        ListRow(AppIcons.Apps, p.name.ifBlank { "PID ${p.pid}" },
                            "CPU ${p.cpu}% · ${Format.bytes(p.memory)} · PID ${p.pid}",
                            trailing = if (p.protected) null else ({ TextButton(onClick = { kill = p }) { Text("Завершить", color = Palette.Text2, fontSize = 12.sp) } }))
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
