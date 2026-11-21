package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.data.model.ConnectionState

/**
 * «Нет связи с ПК» (approved mockup): what to check, the countdown to the next try, «Повторить сейчас»
 * and «Выбрать другой ПК». Shown where a screen has nothing to show without the PC.
 */
@Composable
fun NoConnection(state: ConnectionState, onRetry: () -> Unit, onChoose: () -> Unit, modifier: Modifier = Modifier) {
    val name = when (state) {
        is ConnectionState.Connecting -> state.pc.name
        is ConnectionState.Reconnecting -> state.pc.name
        is ConnectionState.CertificateChanged -> state.pc.name
        else -> null
    }
    Column(modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Phone · · · PC; the gap blinks, once a shake says «no answer».
        val shake = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            if (Motion.on) for (x in listOf(-3f, 3f, -2f, 1f, 0f)) shake.animateTo(x, tween(90))
        }
        Row(Modifier.graphicsLayer { translationX = shake.value * density }, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Surface), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Devices, null, tint = Palette.Text2, modifier = Modifier.size(34.dp))
            }
            Row(Modifier.padding(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { i -> GapDot(i) }
            }
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Surface), contentAlignment = Alignment.Center) {
                Icon(AppIcons.Monitor, null, tint = Palette.Text2, modifier = Modifier.size(34.dp))
            }
        }
        Text(if (state is ConnectionState.Connecting) "Подключение…" else "Нет связи с ПК", fontSize = 26.sp, color = Palette.Text,
            modifier = Modifier.padding(top = 28.dp))
        Text(when (state) {
            is ConnectionState.CertificateChanged -> "Сертификат ${state.pc.name} изменился. Подключите этот ПК заново."
            ConnectionState.NotPaired -> "ПК не выбран. Подключите его по QR-коду или по адресу."
            ConnectionState.Revoked -> "ПК отозвал доступ этого телефона. Подключите его заново."
            else -> "${name ?: "ПК"} не отвечает. Проверьте, что ПК включён, PulseDeck запущен и телефон в той же сети или подключён по USB."
        }, fontSize = 14.sp, color = Palette.Text2, textAlign = TextAlign.Center, lineHeight = 20.sp, modifier = Modifier.padding(top = 8.dp))
        if (state is ConnectionState.Reconnecting) {
            Row(Modifier.padding(top = 24.dp).clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Countdown(state.inSeconds)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Пробую снова через ${state.inSeconds} с", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
                    Text(state.reason, fontSize = 12.sp, color = Palette.Text2)
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        if (state !is ConnectionState.CertificateChanged && state != ConnectionState.NotPaired && state != ConnectionState.Revoked) {
            Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Palette.accent).clickable(onClick = onRetry),
                contentAlignment = Alignment.Center) {
                Text("Повторить сейчас", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.onAccent)
            }
            Spacer(Modifier.height(8.dp))
        }
        Box(Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).border(1.dp, Palette.Outline, RoundedCornerShape(26.dp))
            .clickable(onClick = onChoose), contentAlignment = Alignment.Center) {
            Text(if (name == null) "Подключить ПК" else "Выбрать другой ПК", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.accent)
        }
    }
}

@Composable
private fun GapDot(index: Int) {
    val color = Palette.Error
    if (!Motion.on) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = 0.6f)))
        return
    }
    val t by rememberInfiniteTransition(label = "gap").animateFloat(0.25f, 1f,
        infiniteRepeatable(tween(700, delayMillis = index * 200, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Box(Modifier.size(6.dp).clip(CircleShape).background(color.copy(alpha = t)))
}

/** A ring that empties over the seconds left before the next try. */
@Composable
private fun Countdown(seconds: Int) {
    val ring = Palette.accent
    val track = Palette.Track
    val p = remember(seconds) { Animatable(1f) }
    LaunchedEffect(seconds) { if (Motion.on) p.animateTo(0.0f, tween(1000, easing = LinearEasing)) }
    val start = (seconds.coerceAtMost(30) / 30f)
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = 4.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
            val sweep = 360f * (start - (1f / 30f) * (1f - p.value)).coerceIn(0f, 1f)
            drawArc(ring, -90f, sweep, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Text("$seconds", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text)
    }
}
