package com.kirdev.pulsedeckmobile.feature.remote

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kirdev.pulsedeckmobile.data.remote.PcApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A screenshot of the PC's main monitor (mss: 1 = main monitor, 0 = all of them together). */
suspend fun takeScreenshot(api: PcApi): ImageBitmap? {
    val bytes = api.screenshot(1, 1600)
    return withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
}

/** Full screen on black; a tap anywhere closes it. */
@Composable
fun ScreenshotViewer(image: ImageBitmap, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Image(image, "Скриншот ПК", contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().background(Color.Black).clickable(onClick = onClose))
    }
}
