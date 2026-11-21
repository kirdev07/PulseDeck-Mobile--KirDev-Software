package com.kirdev.pulsedeckmobile.feature.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kirdev.pulsedeckmobile.core.ui.IconButtonSmall
import com.kirdev.pulsedeckmobile.core.ui.LiveDot
import com.kirdev.pulsedeckmobile.core.ui.enter
import com.kirdev.pulsedeckmobile.core.ui.pressScale
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.core.ui.AppIcons
import com.kirdev.pulsedeckmobile.core.ui.ScreenHeader
import com.kirdev.pulsedeckmobile.core.ui.TopTitle
import com.kirdev.pulsedeckmobile.feature.connection.ConnectionViewModel.Step

/** «Подключение к ПК» (approved mockups): QR first, found PCs, an address; then the code with the PC's fingerprint. */
@Composable
fun ConnectionScreen(viewModel: ConnectionViewModel, onBack: (() -> Unit)?, onPaired: () -> Unit) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    LaunchedEffect(step) { if (step is Step.Paired) onPaired() }

    Column(Modifier.fillMaxSize().background(Palette.Black).safeDrawingPadding()) {
        when (val current = step) {
            is Step.EnterCode -> EnterCode(current, viewModel::submit, viewModel::search)
            else -> {
                if (onBack != null) ScreenHeader("Подключение к ПК", onBack) else TopTitle("Подключение к ПК")
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    when (current) {
                        is Step.Probing -> Busy("Связываюсь с ${current.host}…")
                        Step.Paired -> Busy("Подключено")
                        else -> Choose(current, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun Choose(step: Step, viewModel: ConnectionViewModel) {
    var address by remember { mutableStateOf("") }
    val context = LocalContext.current
    Text("На ПК откройте PulseDeck → «Телефон» → «Показать код».", fontSize = 14.sp, color = Palette.Text2,
        modifier = Modifier.padding(start = 4.dp, bottom = 14.dp).enter(0))
    val qrSource = remember { MutableInteractionSource() }
    Row(Modifier.fillMaxWidth().enter(1).pressScale(qrSource).clip(RoundedCornerShape(28.dp)).background(Palette.Container)
        .clickable(qrSource, indication = androidx.compose.material3.ripple()) { QrScanner.scan(context, viewModel::pairFromQr, viewModel::showError) }
        .padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(Palette.onAccent.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Image, null, tint = Palette.accent, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text("Сканировать QR-код", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Palette.OnContainer)
            Text("быстрее всего, без поиска в сети", fontSize = 13.sp, color = Palette.OnContainer.copy(alpha = 0.85f))
        }
        Icon(AppIcons.Chevron, null, tint = Palette.OnContainer, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.height(12.dp))

    Column(Modifier.fillMaxWidth().enter(2).clip(RoundedCornerShape(28.dp)).background(Palette.Surface).padding(start = 6.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)) {
        val searching = step == Step.Searching
        Row(Modifier.padding(start = 12.dp, end = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            LiveDot(Palette.accent, searching, size = 10.dp)
            Spacer(Modifier.width(10.dp))
            Text(when (step) {
                Step.Searching -> "Ищу ПК в сети…"
                is Step.Found -> if (step.pcs.isEmpty()) "Поиск не запущен" else "Найдено: ${step.pcs.size}"
                is Step.Failed -> "Ничего не найдено"
                else -> "Поиск в сети"
            }, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text, modifier = Modifier.weight(1f))
            TextButton(onClick = if (searching) viewModel::cancel else viewModel::search) {
                Text(if (searching) "Стоп" else "Искать", color = Palette.accent, fontWeight = FontWeight.SemiBold)
            }
        }
        when (step) {
            is Step.Found -> for (pc in step.pcs) FoundRow(pc.name, if (pc.supported) "Wi-Fi · ${pc.host}" else "${pc.host} · обновите PulseDeck до 1.0",
                if (pc.supported) "Подключить" else null) { viewModel.choose(pc) }
            is Step.Failed -> Text(step.message, fontSize = 13.sp, color = Palette.Text2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            Step.Searching -> Text("Проверяю устройства в этой сети Wi-Fi.", fontSize = 13.sp, color = Palette.Text2,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            else -> {}
        }
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp).enter(3), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Palette.Border))
        Text("или по адресу", fontSize = 12.sp, color = Palette.Text3, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Palette.Border))
    }
    Row(Modifier.fillMaxWidth().enter(4), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(address, { address = it.take(40) }, singleLine = true, modifier = Modifier.weight(1f),
            label = { Text("IP-адрес ПК") }, placeholder = { Text("192.168.1.5", color = Palette.Text3) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (address.isNotBlank()) viewModel.connectManual(address) }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Palette.accent, unfocusedBorderColor = Palette.Outline,
                cursorColor = Palette.accent, focusedTextColor = Palette.Text, unfocusedTextColor = Palette.Text,
                focusedLabelColor = Palette.accent, unfocusedLabelColor = Palette.Text2))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.padding(top = 8.dp).height(56.dp).clip(RoundedCornerShape(16.dp))
            .background(if (address.isNotBlank()) Palette.Tonal else Palette.Disabled.copy(alpha = 0.12f))
            .clickable(enabled = address.isNotBlank()) { viewModel.connectManual(address) }.padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center) {
            Text("Найти", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (address.isNotBlank()) Palette.OnTonal else Palette.Disabled)
        }
    }
    Spacer(Modifier.height(18.dp))
    Hint(AppIcons.Usb, "Нет Wi-Fi? Подключите телефон кабелем: «Параметры разработчика» → «Отладка по USB», разрешите отладку " +
        "для этого ПК — PulseDeck сам направит связь через кабель.")
    Spacer(Modifier.height(10.dp))
    Hint(AppIcons.Lock, "Соединение шифруется. Новый ПК подключается только после ввода кода с его экрана.")
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun FoundRow(name: String, sub: String, action: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(enabled = action != null, onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Palette.Surface2), contentAlignment = Alignment.Center) {
            Icon(AppIcons.Monitor, null, tint = Palette.Text2, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, color = Palette.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, color = Palette.Text2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (action != null) Box(Modifier.height(32.dp).clip(RoundedCornerShape(10.dp)).background(Palette.accent).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center) {
            Text(action, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Palette.onAccent)
        }
    }
}

@Composable
private fun Hint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Palette.Text3, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 12.sp, color = Palette.Text2, lineHeight = 16.sp)
    }
}

/** Six cells, the PC's fingerprint to compare, a keypad of our own (no system keyboard over the cells). */
@Composable
private fun EnterCode(step: Step.EnterCode, onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var code by remember(step.host) { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) { IconButtonSmall(AppIcons.Back, "Выбрать другой ПК", onClick = onBack) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(72.dp).enter(0).clip(RoundedCornerShape(24.dp)).background(Palette.Container), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Monitor, null, tint = Palette.OnContainer, modifier = Modifier.size(36.dp))
            }
            Text(step.name, fontSize = 26.sp, color = Palette.Text, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp).enter(1))
            Text("Введите 6 цифр с экрана ПК", fontSize = 14.sp, color = Palette.Text2, modifier = Modifier.padding(top = 4.dp).enter(1))
            Row(Modifier.padding(top = 24.dp).enter(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (i in 0 until 6) CodeCell(code.getOrNull(i), current = i == code.length && !step.sending)
            }
            Column(Modifier.fillMaxWidth().padding(top = 24.dp).enter(3).clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Lock, null, tint = Palette.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Отпечаток ПК", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                }
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConnectionViewModel.shortFingerprint(step.fingerprint).split(' ').forEach { part ->
                        Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(Palette.Surface2), contentAlignment = Alignment.Center) {
                            Text(part, fontSize = 15.sp, fontFamily = FontFamily.Monospace, letterSpacing = 1.sp, color = Palette.Text)
                        }
                    }
                }
                Text("Сравните с экраном ПК. Если не совпадает — не продолжайте.", fontSize = 12.sp, color = Palette.Text2,
                    modifier = Modifier.padding(top = 10.dp))
            }
            step.error?.let { Text(it, fontSize = 13.sp, color = Palette.Error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp)) }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val enabled = key.isNotEmpty() && !step.sending
                        val source = remember { MutableInteractionSource() }
                        Box(Modifier.weight(1f).height(52.dp).pressScale(source).clip(RoundedCornerShape(18.dp))
                            .background(if (key.isEmpty()) androidx.compose.ui.graphics.Color.Transparent else Palette.Surface)
                            .clickable(source, indication = androidx.compose.material3.ripple(), enabled = enabled) {
                                code = if (key == "⌫") code.dropLast(1) else (code + key).take(6)
                                if (code.length == 6 && key != "⌫") onSubmit(code)
                            }.semantics { contentDescription = if (key == "⌫") "Стереть" else key }, contentAlignment = Alignment.Center) {
                            Text(key, fontSize = 22.sp, color = Palette.Text)
                        }
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
                .background(if (code.length == 6 && !step.sending) Palette.accent else Palette.Disabled.copy(alpha = 0.12f))
                .clickable(enabled = code.length == 6 && !step.sending) { onSubmit(code) }, contentAlignment = Alignment.Center) {
                Text(if (step.sending) "Подключаю…" else "Подключить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = if (code.length == 6 && !step.sending) Palette.onAccent else Palette.Disabled)
            }
        }
    }
}

@Composable
private fun CodeCell(digit: Char?, current: Boolean) {
    val blink = com.kirdev.pulsedeckmobile.core.ui.breathe(current, 1000)
    Box(Modifier.size(46.dp, 60.dp).clip(RoundedCornerShape(14.dp))
        .background(if (digit != null) Palette.Surface2 else androidx.compose.ui.graphics.Color.Transparent)
        .border(if (current) 2.dp else 1.dp, if (current) Palette.accent else if (digit != null) androidx.compose.ui.graphics.Color.Transparent else Palette.Outline,
            RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        if (digit != null) Text(digit.toString(), fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Palette.Text)
        else if (current) Box(Modifier.size(2.dp, 28.dp).background(Palette.accent.copy(alpha = blink)))
    }
}

@Composable
private fun Busy(text: String) {
    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Palette.accent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, fontSize = 15.sp, color = Palette.Text2)
        }
    }
}
