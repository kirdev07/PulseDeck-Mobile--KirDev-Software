package com.kirdev.pulsedeckmobile.data.local

import android.content.Context
import com.kirdev.pulsedeckmobile.core.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Prefs(
    val theme: AppTheme = AppTheme.AMOLED,
    val accent: Int = 0,
    /** Wave and screen transitions; the system «remove animations» setting also turns them off. */
    val animations: Boolean = true,
    val compact: Boolean = false,
    /** Seconds between metric updates while the app is on screen (1–10, the PC's limits). */
    val intervalSeconds: Double = 1.5,
    /** Media card in the shade and on the lock screen while something plays on the PC. */
    val mediaNotification: Boolean = true,
    /** Phone volume keys change the PC volume while a track is playing. */
    val remoteVolume: Boolean = true,
)

/** Non-secret preferences. Tokens live in [PairingStore], encrypted with the Android Keystore. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _prefs = MutableStateFlow(read())
    val values: StateFlow<Prefs> = _prefs.asStateFlow()

    fun update(change: (Prefs) -> Prefs) {
        val next = change(_prefs.value).let { it.copy(intervalSeconds = it.intervalSeconds.coerceIn(1.0, 10.0)) }
        prefs.edit()
            .putString("theme", next.theme.name)
            .putInt("accent", next.accent)
            .putBoolean("animations", next.animations)
            .putBoolean("compact", next.compact)
            .putFloat("interval", next.intervalSeconds.toFloat())
            .putBoolean("media_notification", next.mediaNotification)
            .putBoolean("remote_volume", next.remoteVolume)
            .apply()
        _prefs.value = next
    }

    private fun read() = Prefs(
        theme = runCatching { AppTheme.valueOf(prefs.getString("theme", null) ?: "") }.getOrDefault(AppTheme.AMOLED),
        accent = prefs.getInt("accent", 0),
        animations = prefs.getBoolean("animations", true),
        compact = prefs.getBoolean("compact", false),
        intervalSeconds = prefs.getFloat("interval", 1.5f).toDouble(),
        mediaNotification = prefs.getBoolean("media_notification", true),
        remoteVolume = prefs.getBoolean("remote_volume", true),
    )
}
