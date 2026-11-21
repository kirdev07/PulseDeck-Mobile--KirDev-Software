package com.kirdev.pulsedeckmobile.data.local

import android.content.Context
import com.kirdev.pulsedeckmobile.monitoring.Limit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Prefs(
    /** «color» (Material 3) or «mono» (the black-and-white look of PulseDesk 1.0). */
    val style: String = "color",
    /** «dark», «light» or «system». */
    val mode: String = "dark",
    /** purple / blue / green / orange / pink, or «wallpaper» (Material You, Android 12+). */
    val accentKey: String = "purple",
    /** Pure black background in the dark theme (OLED screens). */
    val amoled: Boolean = false,
    /** Wave and screen transitions; the system «remove animations» setting also turns them off. */
    val animations: Boolean = true,
    val compact: Boolean = false,
    /** Seconds between metric updates while the app is on screen (1–10, the PC's limits). */
    val intervalSeconds: Double = 1.5,
    /** Media card in the shade and on the lock screen while something plays on the PC. */
    val mediaNotification: Boolean = true,
    /** Phone volume keys change the PC volume while a track is playing. */
    val remoteVolume: Boolean = true,
    /** The seek bar as a wave while a track plays (as in Android / One UI). */
    val waveSeek: Boolean = true,
    /** «Предупреждения»: a notice when a value stays past its limit. */
    val alerts: Boolean = true,
    /** …also as a system notification. */
    val notify: Boolean = false,
    val alertOffline: Boolean = true,
    val limits: Map<Limit, Int> = Limit.entries.associateWith { it.default },
) {
    fun limit(key: Limit): Int = (limits[key] ?: key.default).coerceIn(key.range)
}

/** Non-secret preferences. Tokens live in [PairingStore], encrypted with the Android Keystore. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _prefs = MutableStateFlow(read())
    val values: StateFlow<Prefs> = _prefs.asStateFlow()

    fun update(change: (Prefs) -> Prefs) {
        val next = change(_prefs.value).let { it.copy(intervalSeconds = it.intervalSeconds.coerceIn(1.0, 10.0)) }
        prefs.edit()
            .putString("style", next.style)
            .putString("mode", next.mode)
            .putString("accent_key", next.accentKey)
            .putBoolean("amoled", next.amoled)
            .putBoolean("animations", next.animations)
            .putBoolean("compact", next.compact)
            .putFloat("interval", next.intervalSeconds.toFloat())
            .putBoolean("media_notification", next.mediaNotification)
            .putBoolean("remote_volume", next.remoteVolume)
            .putBoolean("wave_seek", next.waveSeek)
            .putBoolean("alerts", next.alerts)
            .putBoolean("notify", next.notify)
            .putBoolean("alert_offline", next.alertOffline)
            .apply {
                for (key in Limit.entries) putInt("limit_${key.name}", next.limit(key))
            }
            .apply()
        _prefs.value = next
    }

    private fun read() = Prefs(
        style = prefs.getString("style", null) ?: "color",
        mode = prefs.getString("mode", null) ?: "dark",
        accentKey = prefs.getString("accent_key", null) ?: "purple",
        // 1.0.x had «AMOLED» and «Graphite» themes: AMOLED carries over as the black background.
        amoled = if (prefs.contains("amoled")) prefs.getBoolean("amoled", false) else prefs.getString("theme", "AMOLED") == "AMOLED",
        animations = prefs.getBoolean("animations", true),
        compact = prefs.getBoolean("compact", false),
        intervalSeconds = prefs.getFloat("interval", 1.5f).toDouble(),
        mediaNotification = prefs.getBoolean("media_notification", true),
        remoteVolume = prefs.getBoolean("remote_volume", true),
        waveSeek = prefs.getBoolean("wave_seek", true),
        alerts = prefs.getBoolean("alerts", true),
        notify = prefs.getBoolean("notify", false),
        alertOffline = prefs.getBoolean("alert_offline", true),
        limits = Limit.entries.associateWith { prefs.getInt("limit_${it.name}", it.default) },
    )
}
