package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Repeats [block] every [intervalMs] only while this screen is shown and the app is started.
 * Leaving the screen or minimising the app stops it; nothing keeps running in the background.
 * Used only where the PC has no push channel (events, process list).
 */
@Composable
fun PollWhileVisible(intervalMs: Long, vararg keys: Any?, block: suspend () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val current = rememberUpdatedState(block)
    LaunchedEffect(lifecycle, *keys) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                runCatching { current.value() }
                delay(intervalMs)
            }
        }
    }
}
