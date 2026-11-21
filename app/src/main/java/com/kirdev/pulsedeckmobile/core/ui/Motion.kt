package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * The motion of the approved mockups: cards come up one after another, charts draw themselves,
 * bars grow, «live» dots pulse, buttons sink when pressed. All of it stays still when animations
 * are off (Оформление → «Анимации», Android's own switch) or the phone runs «Облегчённый режим».
 */
object Motion {
    var on by mutableStateOf(true)

    /** Material 3 «emphasized» easing. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Fades the item in and lifts it 16 dp, [index] × 45 ms after the screen opens. */
fun Modifier.enter(index: Int = 0): Modifier = composed {
    if (!Motion.on) return@composed this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(10) * 45L)
        p.animateTo(1f, tween(450, easing = Motion.Emphasized))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 16.dp.toPx()
    }
}

/** 0 → 1 once, after [delayMs]: how much of a chart or a bar is drawn. 1 at once without motion. */
@Composable
fun appear(delayMs: Int = 200, durationMs: Int = 900): Float {
    if (!Motion.on) return 1f
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        p.animateTo(1f, tween(durationMs, easing = Motion.Emphasized))
    }
    return p.value
}

/** Sinks a little while pressed; pass the same [source] to the clickable. */
fun Modifier.pressScale(source: MutableInteractionSource): Modifier = composed {
    if (!Motion.on) return@composed this
    val pressed by source.collectIsPressedAsState()
    val k by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.94f else 1f, tween(160, easing = Motion.Emphasized),
        label = "press")
    graphicsLayer { scaleX = k; scaleY = k }
}

/** A status dot; [live] adds a ring that spreads out and fades, once every 1.6 s. */
@Composable
fun LiveDot(color: Color, live: Boolean, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (live && Motion.on) {
            val t by rememberInfiniteTransition(label = "live").animateFloat(0f, 1f,
                infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "ring")
            Canvas(Modifier.size(size)) {
                drawCircle(color.copy(alpha = 0.6f * (1f - t)), radius = this.size.minDimension / 2 * (1f + 1.2f * t))
            }
        }
        Canvas(Modifier.size(size)) { drawCircle(color) }
    }
}

/** 1 ↔ 0.35, for things that «breathe» (a value over its limit). 1 without motion. */
@Composable
fun breathe(on: Boolean, periodMs: Int = 1500): Float {
    if (!on || !Motion.on) return 1f
    val v by rememberInfiniteTransition(label = "breathe").animateFloat(1f, 0.35f,
        infiniteRepeatable(tween(periodMs / 2), RepeatMode.Reverse), label = "v")
    return v
}
