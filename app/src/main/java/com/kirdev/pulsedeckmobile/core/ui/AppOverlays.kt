package com.kirdev.pulsedeckmobile.core.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.kirdev.pulsedeckmobile.core.utils.Format
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.core.theme.Palette
import com.kirdev.pulsedeckmobile.data.update.UpdateState

/** Things that belong to the whole app, drawn above every screen. */
@Composable
fun AppOverlays(app: PulseDeskApp) {
    LocalNetworkGate(app)
    AlertBanner(app)
    UpdatePrompt(app)
}

/**
 * Android 17 blocks every connection to the local network (search, Wi-Fi link to the PC)
 * for apps targeting SDK 37 until the user allows «Nearby devices». Asked once on start;
 * while it is missing a banner explains why nothing connects. USB (127.0.0.1) still works.
 */
object LocalNetwork {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    fun required() = Build.VERSION.SDK_INT >= 37

    fun granted(context: Context) =
        !required() || context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun LocalNetworkGate(app: PulseDeskApp) {
    if (!LocalNetwork.required()) return
    val context = LocalContext.current
    var granted by remember { mutableStateOf(LocalNetwork.granted(context)) }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (ok) app.connection.restart()
    }
    // Back from system settings: the user may have allowed it there.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = LocalNetwork.granted(context)
        if (now && !granted) app.connection.restart()
        granted = now
    }
    LaunchedEffect(Unit) {
        if (!granted && !asked) {
            asked = true
            launcher.launch(LocalNetwork.PERMISSION)
        }
    }
    if (granted) return
    Column(Modifier.statusBarsPadding().padding(12.dp)) {
        PanelCard(Modifier.fillMaxWidth()) {
            Text("Нет доступа к локальной сети", fontSize = 15.sp, color = Palette.Text)
            Text("Android 17 не даёт приложениям связываться с устройствами в вашей сети без разрешения " +
                "«Устройства поблизости». Без него PulseDesk не найдёт ПК и не подключится по Wi-Fi (по USB-кабелю — работает).",
                fontSize = 12.sp, color = Palette.Text2, lineHeight = 17.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }) { Text("Настройки", color = Palette.Text2) }
                TextButton(onClick = { launcher.launch(LocalNetwork.PERMISSION) }) { Text("Разрешить", color = Palette.accent) }
            }
        }
    }
}

/**
 * «Обновление» (approved mockup): a sheet from below — what is new, the download as a wave,
 * «Установить» lights up once the file is here. A new version downloads by itself (Updater.checkAutomatically).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun UpdatePrompt(app: PulseDeskApp) {
    val state by app.updater.state.collectAsStateWithLifecycle()
    var dismissed by rememberSaveable { mutableStateOf("") }
    val (release, progress, ready) = when (val s = state) {
        is UpdateState.Downloading -> Triple(s.release, s.fraction, null)
        is UpdateState.Ready -> Triple(s.release, 1f, s)
        else -> return
    }
    if (dismissed == release.version) return
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = { dismissed = release.version }, containerColor = Palette.Surface,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = Palette.Outline) }) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Palette.Container), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(AppIcons.Download, null, tint = Palette.OnContainer, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Новая версия ${release.version}", fontSize = 22.sp, color = Palette.Text)
                    Text("у вас ${app.updater.current}" + if (release.size > 0) " · ${Format.bytes(release.size)}" else "",
                        fontSize = 13.sp, color = Palette.Text2)
                }
            }
            val notes = release.notes.lines().map { it.trim().trimStart('-', '*', '•', ' ').replace("**", "").replace("`", "") }
                .filter { it.isNotBlank() && !it.startsWith("#") }.take(5)
            if (notes.isNotEmpty()) {
                Text("Что нового", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Palette.accent, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
                notes.forEach { line ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(Palette.accent))
                        Spacer(Modifier.width(10.dp))
                        Text(line, fontSize = 14.sp, color = Palette.Text, lineHeight = 19.sp)
                    }
                }
            }
            Row(Modifier.padding(top = 22.dp, bottom = 8.dp)) {
                Text(if (ready == null) "Загружаю…" else "Готово к установке", fontSize = 13.sp, color = Palette.Text, modifier = Modifier.weight(1f))
                Text(if (release.size > 0) "${Format.bytes((release.size * progress).toLong())} из ${Format.bytes(release.size)} · ${(progress * 100).toInt()}%"
                    else "${(progress * 100).toInt()}%", fontSize = 13.sp, color = Palette.Text2)
            }
            com.kirdev.pulsedeckmobile.feature.media.SquigglyProgress(progress, playing = ready == null, motion = Motion.on,
                modifier = Modifier.fillMaxWidth())
            Text(if (ready == null) "Загрузка идёт в фоне — это окно можно закрыть."
                else "Нажмите «Установить» и подтвердите в окне Android — пара с ПК и настройки сохранятся.",
                fontSize = 12.sp, color = Palette.Text2, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, Palette.Outline, RoundedCornerShape(20.dp))
                    .clickable { dismissed = release.version }.padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                    Text("Позже", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Palette.accent)
                }
                Box(Modifier.height(40.dp).clip(RoundedCornerShape(20.dp))
                    .background(if (ready != null) Palette.accent else Palette.Disabled.copy(alpha = 0.12f))
                    .clickable(enabled = ready != null) { ready?.let { app.updater.install(it.file) } }.padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Установить", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (ready != null) Palette.onAccent else Palette.Disabled)
                }
            }
        }
    }
}
