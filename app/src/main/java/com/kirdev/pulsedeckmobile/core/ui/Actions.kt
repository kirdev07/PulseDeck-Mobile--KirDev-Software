package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** One snackbar for the whole app: results and errors of actions on the PC. */
val LocalMessages = staticCompositionLocalOf { SnackbarHostState() }

/** Runs a PC action off the UI thread and reports the PC's answer (or its error) in the snackbar. */
class ActionRunner(private val scope: CoroutineScope, private val messages: SnackbarHostState) {
    fun run(done: String? = null, block: suspend () -> String?) {
        scope.launch {
            val text = try {
                block()?.takeIf { it.isNotBlank() } ?: done
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: "Ошибка."
            }
            if (text != null) {
                messages.currentSnackbarData?.dismiss()
                messages.showSnackbar(text)
            }
        }
    }

    fun say(text: String) {
        scope.launch {
            messages.currentSnackbarData?.dismiss()
            messages.showSnackbar(text)
        }
    }
}

@Composable
fun rememberActions(): ActionRunner {
    val scope = rememberCoroutineScope()
    val messages = LocalMessages.current
    return remember(scope, messages) { ActionRunner(scope, messages) }
}
