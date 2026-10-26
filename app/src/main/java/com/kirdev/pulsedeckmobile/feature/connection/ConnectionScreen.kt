package com.kirdev.pulsedeckmobile.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ListRow
import com.kirdev.pulsedeckmobile.core.ui.PanelCard
import com.kirdev.pulsedeckmobile.core.ui.PanelTextField
import com.kirdev.pulsedeckmobile.core.ui.PillButton
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.SectionTitle
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.core.ui.VSpace
import com.kirdev.pulsedeckmobile.feature.connection.ConnectionViewModel.Step

/** Screen 02: address, search, found PCs; then the code with the certificate fingerprint. */
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel, onBack: (() -> Unit)?, onPaired: () -> Unit) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    LaunchedEffect(step) { if (step is Step.Paired) onPaired() }

    Column(Modifier.fillMaxSize().background(Palette.Black).safeDrawingPadding()) {
        if (onBack != null) ScreenHeader("Подключение к ПК", onBack) else TopTitle("Подключение к ПК")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            when (val current = step) {
                is Step.EnterCode -> EnterCode(current, viewModel::submit, viewModel::search)
                is Step.Probing -> Busy("Связываюсь с ${current.host}…")
                Step.Paired -> Busy("Подключено")
                else -> Choose(current, viewModel)
            }
        }
    }
}

@Composable
private fun Choose(step: Step, viewModel: ConnectionViewModel) {
    var address by remember { mutableStateOf("") }
    VSpace(8.dp)
    Text("Введите IP-адрес или выберите устройство", fontSize = 13.sp, color = Palette.Text2)
    VSpace(10.dp)
    PanelTextField(address, { address = it.take(40) }, placeholder = "192.168.1.5",
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go))
    VSpace(10.dp)
    PillButton("Подключиться", Modifier.fillMaxWidth(), enabled = address.isNotBlank()) { viewModel.connectManual(address) }
    VSpace(14.dp)
    Text("или", fontSize = 12.sp, color = Palette.Text3, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    VSpace(8.dp)
    PanelCard(Modifier.fillMaxWidth()) {
        ListRow(AppIcons.Network, "Автопоиск в сети", "Найти PulseDeck в этой сети Wi-Fi", chevron = true, onClick = viewModel::search)
    }

    SectionTitle("Найденные устройства")
    when (step) {
        Step.Searching -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            CircularProgressIndicator(color = Palette.Text2, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Text("   Поиск…", fontSize = 13.sp, color = Palette.Text2)
            TextButton(onClick = viewModel::cancel) { Text("Отменить", color = Palette.Text3) }
        }
        is Step.Found -> if (step.pcs.isEmpty()) {
            Text("Ничего не найдено. Проверьте, что PulseDeck запущен, или введите адрес.", fontSize = 13.sp, color = Palette.Text3)
        } else PanelCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (pc in step.pcs) {
                    ListRow(AppIcons.Monitor, pc.name,
                        if (pc.supported) pc.host else "${pc.host} · обновите PulseDeck до 1.0",
                        chevron = pc.supported, onClick = if (pc.supported) ({ viewModel.choose(pc) }) else null)
                }
            }
        }
        is Step.Failed -> Text(step.message, fontSize = 13.sp, color = Palette.Text)
        else -> Text("Нажмите «Автопоиск в сети».", fontSize = 13.sp, color = Palette.Text3)
    }
    VSpace(18.dp)
    Text(
        "Нет Wi-Fi? Подключите телефон кабелем: «Параметры разработчика» → «Отладка по USB», разрешите отладку " +
            "для этого ПК — PulseDeck сам направит связь через кабель.",
        fontSize = 11.sp, color = Palette.Text3, lineHeight = 15.sp,
    )
    VSpace(24.dp)
}

@Composable
private fun EnterCode(step: Step.EnterCode, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var code by remember(step.host) { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        VSpace(16.dp)
        Text(step.name, fontSize = 18.sp, color = Palette.Text)
        Text(step.host, fontSize = 12.sp, color = Palette.Text3)
        VSpace(14.dp)
        Text("На ПК откройте «Телефон» → «Показать код». Сверьте отпечаток и введите 6 цифр.",
            fontSize = 13.sp, color = Palette.Text2, textAlign = TextAlign.Center)
        VSpace(16.dp)
        PanelCard(Modifier.fillMaxWidth()) {
            Text("Отпечаток ПК", fontSize = 11.sp, color = Palette.Text3)
            Text(ConnectionViewModel.shortFingerprint(step.fingerprint), fontSize = 18.sp, color = Palette.Text,
                fontFamily = FontFamily.Monospace, letterSpacing = 1.sp)
            Text("Если он не совпадает с экраном ПК — не продолжайте.", fontSize = 11.sp, color = Palette.Text3)
        }
        VSpace(16.dp)
        OutlinedTextField(
            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) }, singleLine = true,
            placeholder = { Text("000000", color = Palette.Text3) }, enabled = !step.sending,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit(code) }),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp, letterSpacing = 6.sp,
                color = Palette.Text, textAlign = TextAlign.Center),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.Text2, unfocusedBorderColor = Palette.Border,
                cursorColor = Palette.Text, focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text),
            modifier = Modifier.fillMaxWidth(),
        )
        VSpace(12.dp)
        PillButton(if (step.sending) "Подключаю…" else "Подключить", Modifier.fillMaxWidth(),
            enabled = code.length == 6 && !step.sending) { onSubmit(code) }
        step.error?.let { VSpace(10.dp); Text(it, fontSize = 13.sp, color = Palette.Text, textAlign = TextAlign.Center) }
        TextButton(onClick = onBack) { Text("Выбрать другой ПК", color = Palette.Text3) }
    }
}

@Composable
private fun Busy(text: String) {
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Palette.Text2, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            VSpace(12.dp)
            Text(text, fontSize = 14.sp, color = Palette.Text2)
        }
    }
}
