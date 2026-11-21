package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.clip

private val Container = Color(0xFF4F378B)
private val Ink = Color(0xFFEADDFF)
private val Accent = Color(0xFFD0BCFF)
private val EMPHASIZED = CubicBezierEasing(0.2f, 0f, 0f, 1f)

/**
 * Launch animation over the first screen: the logo «PC and phone» in its purple circle (as on the
 * system splash screen) — a pulse runs across the monitor, the phone bounces — then the logo grows
 * and fades while the app shows through. About 0.8 s, once per cold start; a tap skips it.
 * If the PC is not there yet, it stays up to 4 s more with the real steps of the link
 * ([stage]: 0 — looking for the PC, 1 — linked, waiting for the first numbers, 2 — ready).
 */
@Composable
fun LaunchIntro(enabled: Boolean, background: Color, stage: Int = 2, pcName: String? = null, onDone: () -> Unit) {
    if (!enabled) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    val pulse = remember { Animatable(0f) }
    val bounce = remember { Animatable(0f) }
    val leave = remember { Animatable(0f) }
    var skipped by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }
    val stageNow by androidx.compose.runtime.rememberUpdatedState(stage)
    LaunchedEffect(skipped) {
        if (skipped) {
            leave.animateTo(1f, tween(160, easing = LinearEasing))
            onDone()
            return@LaunchedEffect
        }
        launch { bounce.animateTo(1f, tween(420, delayMillis = 120, easing = LinearEasing)) }
        pulse.animateTo(1f, tween(480, easing = EMPHASIZED))
        if (stageNow < 2) {
            waiting = true
            kotlinx.coroutines.withTimeoutOrNull(4_000) {
                androidx.compose.runtime.snapshotFlow { stageNow }.first { it >= 2 }
                kotlinx.coroutines.delay(350)            // the last step is seen ticking
            }
        }
        leave.animateTo(1f, tween(320, easing = EMPHASIZED))
        onDone()
    }
    val logo = remember { LogoPaths() }
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = 1f - leave.value }
            .background(background)
            .pointerInput(Unit) { detectTapGestures { skipped = true } },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(160.dp).graphicsLayer {
            val k = 1f + 0.18f * leave.value
            scaleX = k
            scaleY = k
        }) {
            // the circle, then the logo on the 48-unit grid of the icon
            drawCircle(Container)
            val unit = size.minDimension / 48f
            scale(unit, unit, Offset.Zero) {
                val stroke = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(logo.monitor, Ink, style = stroke)
                drawLine(Ink, Offset(16f, 35f), Offset(26f, 35f), 3f, StrokeCap.Round)
                // the pulse is drawn again from left to right
                val drawn = logo.pulsePart(pulse.value)
                drawPath(drawn, Ink, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                // the phone hops once: up and back
                val hop = kotlin.math.sin(bounce.value * Math.PI).toFloat() * -2.2f
                translate(0f, hop) {
                    drawRoundRect(Accent, Offset(29f, 19f), Size(12f, 20f), CornerRadius(3.5f))
                }
            }
        }
        // The name comes up under the logo while the pulse runs (approved «Загрузка» mockup).
        androidx.compose.foundation.layout.Column(
            Modifier.padding(top = 250.dp).graphicsLayer {
                alpha = pulse.value * (1f - leave.value)
                translationY = (1f - pulse.value) * 8.dp.toPx()
            },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            androidx.compose.material3.Text("PulseDesk", fontSize = 32.sp, fontWeight = FontWeight.Medium, color = com.kirdev.pulsedeckmobile.core.theme.Palette.Text)
            androidx.compose.material3.Text("пульт вашего ПК", fontSize = 14.sp, color = com.kirdev.pulsedeckmobile.core.theme.Palette.Text2)
        }
        if (waiting) ConnectingSteps(stage, pcName, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp)
            .graphicsLayer { alpha = 1f - leave.value })
    }
}

/** «Подключаюсь к …» with the steps that really happened (approved «Загрузка» mockup). */
@Composable
private fun ConnectingSteps(stage: Int, pcName: String?, modifier: Modifier) {
    val p = com.kirdev.pulsedeckmobile.core.theme.Palette
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(400, easing = EMPHASIZED)) }
    androidx.compose.foundation.layout.Column(modifier.graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 12.dp.toPx() },
        horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.material3.CircularProgressIndicator(color = p.accent, strokeWidth = 4.dp, modifier = Modifier.size(40.dp))
        androidx.compose.material3.Text(if (pcName != null) "Подключаюсь к $pcName" else "Подключаюсь к ПК", fontSize = 15.sp, color = p.Text,
            modifier = Modifier.padding(top = 14.dp))
        androidx.compose.foundation.layout.Row(Modifier.padding(top = 12.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            listOf("Ищу ПК", "Связь есть", "Получаю данные").forEachIndexed { i, label ->
                val done = stage > i
                val now = stage == i
                val blink = breathe(now, 1200)
                androidx.compose.foundation.layout.Row(
                    Modifier.height(30.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(15.dp))
                        .background(if (done) p.Tonal else p.Surface).padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background((if (done) p.OnTonal else p.Text2).copy(alpha = if (now) blink else 1f)))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
                    androidx.compose.material3.Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (done) p.OnTonal else p.Text2)
                }
            }
        }
    }
}

private class LogoPaths {
    val monitor = Path().apply {
        addRoundRect(androidx.compose.ui.geometry.RoundRect(8f, 12f, 34f, 30f, CornerRadius(4f)))
    }
    private val pulse = Path().apply {
        moveTo(12f, 22f); lineTo(16f, 22f); lineTo(18f, 18f); lineTo(21f, 26f); lineTo(23f, 22f); lineTo(26f, 22f)
    }
    private val measure = PathMeasure().apply { setPath(pulse, false) }
    private val part = Path()

    fun pulsePart(fraction: Float): Path {
        part.reset()
        measure.getSegment(0f, measure.length * fraction.coerceIn(0f, 1f), part, true)
        return part
    }
}
