package com.kirdev.pulsedeckmobile.core.utils

import java.util.Locale
import kotlin.math.roundToInt

object Format {
    const val UNKNOWN = "—"

    fun percent(value: Double?): String = value?.let { "${it.roundToInt()}%" } ?: UNKNOWN

    /** 312 GB, 1.4 TB, 820 MB — compact, for the bottom bar. */
    fun bytes(value: Long?): String {
        if (value == null || value < 0) return UNKNOWN
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = value.toDouble()
        var unit = 0
        while (size >= 1000 && unit < units.lastIndex) {
            size /= 1024
            unit++
        }
        return if (size >= 100 || unit == 0) "${size.roundToInt()} ${units[unit]}"
        else String.format(Locale.US, "%.1f %s", size, units[unit])
    }

    /** Network rate per second: 980 B/s, 1.2 MB/s. */
    fun speed(bytesPerSecond: Double?): String = bytesPerSecond?.let { bytes(it.toLong()) + "/s" } ?: UNKNOWN

    /** 3:28, 1:02:05. */
    fun time(seconds: Double): String {
        val total = seconds.coerceAtLeast(0.0).toInt()
        val h = total / 3600
        val m = total % 3600 / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    }

    /** Fraction 0..1 for a progress bar, 0 when unknown. */
    fun fraction(percent: Double?): Float = ((percent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)

    /** 3 д 4 ч, 5 ч 12 мин, 42 мин. */
    fun uptime(seconds: Long?): String {
        if (seconds == null || seconds < 0) return UNKNOWN
        val days = seconds / 86_400
        val hours = seconds % 86_400 / 3600
        val minutes = seconds % 3600 / 60
        return when {
            days > 0 -> "$days д $hours ч"
            hours > 0 -> "$hours ч $minutes мин"
            else -> "$minutes мин"
        }
    }

    /** 1,5 с; 3 с. */
    fun seconds(value: Double): String =
        (if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value).replace('.', ',')) + " с"
}
