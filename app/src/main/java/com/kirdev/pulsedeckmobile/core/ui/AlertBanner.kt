package com.kirdev.pulsedeckmobile.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Tone
import kotlinx.coroutines.delay

/**
 * A warning while the app is open (approved «Уведомления в приложении»): slides down from the top,
 * the bell swings once, a thin bar shows how long it stays; «Открыть» goes to Мониторинг.
 */
@Composable
fun AlertBanner(app: PulseDeskApp) {
    val banner by app.alerts.banner.collectAsStateWithLifecycle()
    val shown = banner
    AnimatedVisibility(shown != null, modifier = Modifier.statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        enter = slideInVertically(tween(450, easing = Motion.Emphasized)) { -it * 2 },
        exit = slideOutVertically(tween(300)) { -it * 2 } + fadeOut(tween(300))) {
        val b = shown ?: return@AnimatedVisibility
        val life = remember(b.at) { Animatable(1f) }
        val swing = remember(b.at) { Animatable(0f) }
        LaunchedEffect(b.at) {
            if (Motion.on) for (x in listOf(-12f, 10f, -6f, 0f)) swing.animateTo(x, tween(80))
        }
        LaunchedEffect(b.at) {
            if (Motion.on) life.animateTo(0f, tween(LIFE_MS, easing = LinearEasing)) else delay(LIFE_MS.toLong())
            app.alerts.dismissBanner()
        }
        val tone = Tone.Amber
        Column(Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(tone.bg)) {
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).graphicsLayer { rotationZ = swing.value }.clip(CircleShape).background(tone.fg), contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Bell, null, tint = tone.bg, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(b.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tone.fg, modifier = Modifier.weight(1f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (b.key != "offline") Box(Modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(tone.fg)
                    .clickable { app.debugOpen.value = "monitoring"; app.alerts.dismissBanner() }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Открыть", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = tone.bg)
                }
                IconButtonSmall(AppIcons.Close, "Скрыть", size = 40.dp, onClick = { app.alerts.dismissBanner() })
            }
            val ink = tone.fg
            Canvas(Modifier.fillMaxWidth().height(3.dp)) { drawRect(ink, size = Size(size.width * life.value, size.height)) }
        }
    }
}

private const val LIFE_MS = 6000
