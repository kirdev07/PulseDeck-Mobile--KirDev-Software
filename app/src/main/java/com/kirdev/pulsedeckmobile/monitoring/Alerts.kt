package com.kirdev.pulsedeckmobile.monitoring

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.kirdev.pulsedeckmobile.MainActivity
import com.kirdev.pulsedeckmobile.R
import com.kirdev.pulsedeckmobile.data.local.AppSettings
import com.kirdev.pulsedeckmobile.data.local.Prefs
import com.kirdev.pulsedeckmobile.data.model.ConnectionState
import com.kirdev.pulsedeckmobile.data.model.Metrics
import com.kirdev.pulsedeckmobile.data.remote.PcConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Warning thresholds: «over» values warn at or above the limit, the others at or below it. */
enum class Limit(val title: String, val over: Boolean, val default: Int) {
    CPU("Процессор", true, 90), GPU("Видеокарта", true, 90), RAM("Память", true, 90), VRAM("Видеопамять", true, 95),
    DISK("Свободно на диске", false, 10), BATTERY("Батарея ПК", false, 20);

    val range: IntRange get() = if (over) 50..100 else 5..50
}

/**
 * «Предупреждения» (the same rules as PulseDeck Panel and the web panel): a value that stays past
 * its limit for 10 s raises one notice; it is armed again once the value is 5 points back.
 */
class AlertMonitor {
    private val since = mutableMapOf<Limit, Long>()
    private val active = mutableSetOf<Limit>()

    val activeLimits: Set<Limit> get() = active.toSet()

    /** Texts of new warnings (empty most of the time). [nowMs]: a monotonic clock. */
    fun check(m: Metrics, prefs: Prefs, nowMs: Long): List<Pair<Limit, String>> {
        val disk = m.disks.minByOrNull { if (it.total > 0) it.free.toDouble() / it.total else 1.0 }
        val values = mapOf(
            Limit.CPU to m.cpu.load, Limit.GPU to m.gpu.load, Limit.RAM to m.ram.percent, Limit.VRAM to m.vramPercent,
            Limit.DISK to disk?.takeIf { it.total > 0 }?.let { 100.0 * it.free / it.total },
            Limit.BATTERY to m.battery?.takeIf { !it.plugged }?.percent,
        )
        val fresh = mutableListOf<Pair<Limit, String>>()
        for (key in Limit.entries) {
            val limit = prefs.limit(key)
            val value = values[key]
            val bad = prefs.alerts && value != null && (if (key.over) value >= limit else value <= limit)
            val fine = value == null || (if (key.over) value < limit - 5 else value > limit + 5)
            if (!bad) since.remove(key) else if (key !in since) since[key] = nowMs
            val held = since[key]?.let { nowMs - it >= HOLD_MS } ?: false
            if (key !in active && held) {
                active += key
                val v = value!!.roundToInt()
                fresh += key to when (key) {
                    Limit.DISK -> "Мало места: свободно $v% на ${disk?.name?.trimEnd('\\') ?: "диске"}"
                    Limit.BATTERY -> "Батарея ПК: $v%, подключите зарядку"
                    else -> "${key.title}: $v% (порог $limit%)"
                }
            } else if (key in active && (fine || !prefs.alerts)) {
                active -= key
            }
        }
        return fresh
    }

    fun reset() {
        since.clear()
        active.clear()
    }

    companion object {
        const val HOLD_MS = 10_000L
    }
}

/**
 * Watches the metrics wherever they come from: while the app is open, and while the music card
 * keeps the link in the background (then the PC sends media only, so only «нет связи» can come).
 */
class AlertCenter(
    private val context: Context,
    private val connection: PcConnection,
    private val settings: AppSettings,
    private val visible: StateFlow<Boolean>,
    scope: CoroutineScope,
) {
    private val monitor = AlertMonitor()
    private val _alerts = MutableStateFlow<Set<Limit>>(emptySet())
    val alerts: StateFlow<Set<Limit>> = _alerts.asStateFlow()
    private var pcName = ""
    private var offlineJob: Job? = null

    init {
        scope.launch {
            connection.metrics.collect { m ->
                if (m == null) return@collect
                for ((key, text) in monitor.check(m, settings.values.value, SystemClock.elapsedRealtime())) notice(text, key.name)
                _alerts.value = monitor.activeLimits
            }
        }
        scope.launch {
            var online = false
            connection.state.collect { s ->
                when {
                    s is ConnectionState.Connected -> {
                        pcName = s.pcName
                        online = true
                        offlineJob?.cancel()
                    }
                    // Only a lost network link (the countdown updates every second: started once).
                    s is ConnectionState.Reconnecting || s is ConnectionState.Connecting -> if (online) {
                        online = false
                        monitor.reset()
                        _alerts.value = emptySet()
                        offlineJob?.cancel()
                        offlineJob = launch {
                            delay(30_000)
                            if (settings.values.value.alertOffline) {
                                notice("Нет связи с ПК${if (pcName.isNotBlank()) " $pcName" else ""}. Он выключен или вне сети?", "offline")
                            }
                        }
                    }
                    else -> {
                        online = false
                        monitor.reset()
                        _alerts.value = emptySet()
                        offlineJob?.cancel()
                    }
                }
            }
        }
    }

    private fun notice(text: String, key: String) {
        if (visible.value) Toast.makeText(context, text, Toast.LENGTH_LONG).show()
        if (settings.values.value.notify) show(context, "PulseDeck · ${pcName.ifBlank { "ПК" }}", text, key)
    }

    companion object {
        private const val CHANNEL = "alerts"

        fun allowed(context: Context): Boolean = Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        fun show(context: Context, title: String, text: String, key: String) {
            if (!allowed(context)) return
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Предупреждения", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Нагрузка ПК выше порога, мало места, нет связи с ПК."
                },
            )
            val open = PendingIntent.getActivity(context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            manager.notify(2000 + key.hashCode().mod(1000), NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build())
        }
    }
}
