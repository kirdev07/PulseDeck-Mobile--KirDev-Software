package com.kirdev.pulsedeckmobile.core.ui

import android.view.Display
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs

/**
 * «Частота кадров» (Оформление): «auto» = as the system decides (lists scroll at the screen's
 * rate), «eco» = 60 per second and the window asks for 60 Hz (less battery on 90/120 Hz phones),
 * «max» = the display's top rate (90/120/144 Hz) for the smoothest scrolling and animations.
 */
object Frames {
    /** Redraws per second the animations aim for; 0 = every frame of the display. */
    var limit by mutableIntStateOf(0)
        private set

    /** The fastest refresh rate this display offers at its current resolution, Hz. */
    fun maxRate(display: Display?): Int =
        display?.let { d -> sameSize(d).maxOfOrNull { it.refreshRate }?.toInt() } ?: 60

    /** Applies a choice: the animation limit and the display mode the window prefers. */
    fun apply(window: Window?, display: Display?, choice: String) {
        limit = if (choice == "eco") 60 else 0
        if (window == null || display == null) return
        val modes = sameSize(display)
        val mode = when (choice) {
            "max" -> modes.maxByOrNull { it.refreshRate }
            "eco" -> modes.minByOrNull { abs(it.refreshRate - 60f) }
            else -> null                                  // auto: no preference, the system decides
        }
        val id = mode?.modeId ?: 0
        val rate = mode?.refreshRate ?: 0f
        // Both: the mode for Android 11+, the rate for skins that only look at the old field.
        if (window.attributes.preferredDisplayModeId != id || window.attributes.preferredRefreshRate != rate) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = id
                preferredRefreshRate = rate
            }
        }
    }

    private fun sameSize(display: Display): List<Display.Mode> {
        val current = display.mode
        return display.supportedModes.filter {
            it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight
        }
    }
}

/**
 * Animation time in ms (wrapped every hour: a Float of many hours would make motion jitter),
 * advanced on display frames but at most [Frames.limit] times a second. Stands still while
 * [running] is false.
 */
@Composable
fun rememberFrameTime(running: Boolean): State<Float> {
    val ms = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var start = -1L
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (start < 0) start = now - (ms.floatValue * 1_000_000).toLong()
                val minGap = if (Frames.limit > 0) 900_000_000L / Frames.limit else 0L    // 10 % slack for vsync jitter
                if (now - last >= minGap) {
                    last = now
                    ms.floatValue = (((now - start) / 1_000_000L) % 3_600_000L).toFloat()
                }
            }
        }
    }
    return ms
}
