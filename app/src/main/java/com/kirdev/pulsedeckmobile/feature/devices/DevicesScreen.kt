package com.kirdev.pulsedeckmobile.feature.devices

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PillButton
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.PairedPc
import com.kirdev.pulsedeckmobile.navigation.Routes

/** Screen 07: current PC, saved PCs, switch, forget, pair a new one. */
@Composable
fun DevicesScreen(app: PulseDeskApp, onBack: () -> Unit, go: (String) -> Unit) {
    val state by app.connection.state.collectAsStateWithLifecycle()
    var version by remember { mutableIntStateOf(0) }
    val pcs = remember(version) { app.pairing.all() }
    val active = remember(version) { app.pairing.load() }
    var forget by remember { mutableStateOf<PairedPc?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenHeader("Устройства", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            PillButton("Добавить компьютер", Modifier.fillMaxWidth(), primary = false) { go(Routes.PAIR) }
            SectionTitle("Сохранённые")
            PanelCard(Modifier.fillMaxWidth()) {
                if (pcs.isEmpty()) Text("Нет сохранённых ПК.", fontSize = 13.sp, color = Palette.Text3)
                for (pc in pcs) {
                    val current = pc.id == active?.id
                    val status = when {
                        !current -> "Нажмите, чтобы переключиться · ${pc.host}"
                        state is ConnectionState.Connected -> "Подключён" + if ((state as ConnectionState.Connected).usb) " · USB" else " · ${pc.host}"
                        else -> "Активный · ${pc.host}"
                    }
                    ListRow(AppIcons.Monitor, pc.name, status,
                        onClick = if (current) null else ({ app.pairing.activate(pc.id); app.connection.restart(); version++ }),
                        trailing = { TextButton(onClick = { forget = pc }) { Text("Забыть", color = Palette.Text2, fontSize = 12.sp) } })
                }
            }
            Text("«Забыть» удаляет ключ доступа с телефона. Чтобы отключить телефон и на ПК, отзовите его на экране «Телефон» в PulseDeck. " +
                "Там же можно дать телефону «только просмотр».",
                fontSize = 11.sp, color = Palette.Text3, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
    forget?.let { pc ->
        ConfirmDialog("Забыть ${pc.name}?", "Чтобы снова управлять этим ПК, понадобится новый код подключения.", "Забыть", {
            app.pairing.forget(pc.id)
            app.connection.restart()
            version++
        }) { forget = null }
    }
}
