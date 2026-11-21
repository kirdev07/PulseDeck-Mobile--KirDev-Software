package com.kirdev.pulsedeckmobile.feature.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ConfirmDialog
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.LiveDot
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.appear
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.utils.Format
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.PairedPc
import com.kirdev.pulsedeckmobile.navigation.Routes

/** «Мои ПК» (approved mockup): the current PC large, the saved ones with «Войти» and «Забыть», «Добавить ПК». */
@Composable
fun DevicesScreen(app: PulseDeskApp, onBack: () -> Unit, go: (String) -> Unit) {
    val state by app.connection.state.collectAsStateWithLifecycle()
    val metrics by app.connection.metrics.collectAsStateWithLifecycle()
    var version by remember { mutableIntStateOf(0) }
    val pcs = remember(version) { app.pairing.all() }
    val active = remember(version) { app.pairing.load() }
    var forget by remember { mutableStateOf<PairedPc?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            ScreenHeader("Мои ПК", onBack)
            Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                active?.let { pc -> CurrentCard(pc, state, metrics?.uptime, Modifier.enter(0)) { forget = pc } }
                val others = pcs.filter { it.id != active?.id }
                if (others.isNotEmpty()) SectionTitle("Сохранённые", Modifier.enter(1))
                others.forEachIndexed { i, pc ->
                    Row(Modifier.fillMaxWidth().enter(i + 2).clip(RoundedCornerShape(24.dp)).background(Palette.Surface)
                        .padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Surface2), contentAlignment = Alignment.Center) {
                            Icon(AppIcons.Monitor, null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pc.name, fontSize = 15.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pc.host, fontSize = 12.sp, color = Palette.Text2, maxLines = 1)
                        }
                        Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Tonal)
                            .clickable { app.pairing.activate(pc.id); app.connection.restart(); version++ }.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center) {
                            Text("Войти", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.OnTonal)
                        }
                        IconButtonSmall(AppIcons.Trash, "Забыть ${pc.name}", size = 44.dp, onClick = { forget = pc })
                    }
                }
                if (pcs.isEmpty()) Text("Нет сохранённых ПК. Нажмите «Добавить ПК».", fontSize = 13.sp, color = Palette.Text2,
                    modifier = Modifier.padding(4.dp))
                Row(Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 96.dp), verticalAlignment = Alignment.Top) {
                    Icon(AppIcons.Info, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("«Забыть» удаляет ключ доступа с телефона. Чтобы отключить телефон и на ПК, отзовите его в PulseDeck → «Телефон». " +
                        "Там же можно дать телефону «только просмотр».", fontSize = 12.sp, color = Palette.Text2)
                }
            }
        }
        val pop = appear(450, 450)
        Row(Modifier.align(Alignment.BottomEnd).padding(16.dp).height(56.dp)
            .graphicsLayer { scaleX = 0.6f + 0.4f * pop; scaleY = 0.6f + 0.4f * pop; alpha = pop }.clip(RoundedCornerShape(16.dp)).background(Palette.Container).clickable { go(Routes.PAIR) }
            .padding(start = 16.dp, end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.Plus, null, tint = Palette.OnContainer, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Добавить ПК", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.OnContainer)
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

@Composable
private fun CurrentCard(pc: PairedPc, state: ConnectionState, uptime: Long?, modifier: Modifier, onForget: () -> Unit) {
    val connected = state is ConnectionState.Connected
    val status = when (state) {
        is ConnectionState.Connected -> "подключён сейчас · " + if (state.usb) "USB" else pc.host
        is ConnectionState.Connecting -> "подключение…"
        is ConnectionState.Reconnecting -> "нет связи · повтор через ${state.inSeconds} с"
        else -> "активный · ${pc.host}"
    }
    val bg = if (connected) Palette.Container else Palette.Surface
    val fg = if (connected) Palette.OnContainer else Palette.Text
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(bg).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (connected) Palette.accent.copy(alpha = 0.25f) else Palette.Surface2),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.Monitor, null, tint = if (connected) Palette.accent else Palette.Text2, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(if (state is ConnectionState.Connected) state.pcName else pc.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LiveDot(if (connected) Palette.Ok else Palette.Disabled, connected)
                    Spacer(Modifier.width(6.dp))
                    Text(status, fontSize = 13.sp, color = fg.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButtonSmall(AppIcons.Trash, "Забыть ${pc.name}", size = 44.dp, onClick = onForget)
        }
        Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Fact("Адрес", pc.host, connected, Modifier.weight(1f))
            Fact("ПК работает", if (connected) Format.uptime(uptime) else Format.UNKNOWN, connected, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Fact(title: String, value: String, strong: Boolean, modifier: Modifier) {
    val fg = if (strong) Palette.OnContainer else Palette.Text
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(if (strong) Palette.onAccent.copy(alpha = 0.35f) else Palette.Surface2)
        .padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(title, fontSize = 12.sp, color = fg.copy(alpha = 0.75f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
