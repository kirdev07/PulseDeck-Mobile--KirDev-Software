package com.kirdev.pulsedeckmobile.monitoring

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.kirdev.pulsedeckmobile.MainActivity
import com.kirdev.pulsedeckmobile.PulseDeskApp
import com.kirdev.pulsedeckmobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notifications of the approved «Уведомления» mockup: a warning with «Процессы» and «Тише на 1 ч»,
 * the pending shutdown with its countdown and «Отменить», and the downloaded update.
 */
object Notices {
    private const val ALERTS = "alerts"
    private const val POWER = "power"
    private const val UPDATES = "updates"
    const val POWER_ID = 3001
    const val UPDATE_ID = 3002

    /** «Тише на 1 ч»: until this moment (elapsed realtime) warnings stay silent. */
    @Volatile var quietUntil = 0L

    val quiet: Boolean get() = SystemClock.elapsedRealtime() < quietUntil

    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)

    private fun channel(context: Context, id: String, name: String, text: String, importance: Int = NotificationManager.IMPORTANCE_DEFAULT) {
        manager(context).createNotificationChannel(NotificationChannel(id, name, importance).apply { description = text })
    }

    /** Opens the app on [route] (a whitelisted screen) — works in release builds too. */
    fun openApp(context: Context, route: String?, request: Int): PendingIntent =
        PendingIntent.getActivity(context, request,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP).apply { if (route != null) putExtra(EXTRA_ROUTE, route) },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun action(context: Context, what: String, id: Int): PendingIntent =
        PendingIntent.getBroadcast(context, id, Intent(context, NoticeReceiver::class.java).setAction(what).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    fun alert(context: Context, title: String, text: String, key: String) {
        if (!AlertCenter.allowed(context)) return
        channel(context, ALERTS, "Предупреждения", "Нагрузка ПК выше порога, мало места, нет связи с ПК.")
        val id = 2000 + key.hashCode().mod(1000)
        val builder = NotificationCompat.Builder(context, ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context, if (key == "offline") null else "monitoring", id))
            .setAutoCancel(true)
        if (key != "offline") {
            builder.addAction(0, "Процессы", openApp(context, "processes", id + 500))
            builder.addAction(0, "Тише на 1 ч", action(context, ACTION_QUIET, id))
        }
        manager(context).notify(id, builder.build())
    }

    /** The pending shutdown or restart: a live countdown and «Отменить» that tells the PC. */
    fun power(context: Context, restart: Boolean, seconds: Int) {
        if (!AlertCenter.allowed(context)) return
        channel(context, POWER, "Выключение ПК", "Таймер выключения или перезагрузки ПК с кнопкой «Отменить».")
        val what = if (restart) "Перезагрузка" else "Выключение"
        manager(context).notify(POWER_ID, NotificationCompat.Builder(context, POWER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$what ПК")
            .setContentText("Несохранённые документы могут потеряться")
            .setWhen(System.currentTimeMillis() + seconds * 1000L)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setTimeoutAfter(seconds * 1000L + 5_000L)
            .setOngoing(true)
            .setContentIntent(openApp(context, "control", POWER_ID))
            .addAction(0, "Отменить", action(context, ACTION_CANCEL_POWER, POWER_ID))
            .build())
    }

    fun clearPower(context: Context) = manager(context).cancel(POWER_ID)

    fun update(context: Context, version: String) {
        if (!AlertCenter.allowed(context)) return
        channel(context, UPDATES, "Обновления", "Новая версия PulseDesk скачана.", NotificationManager.IMPORTANCE_LOW)
        manager(context).notify(UPDATE_ID, NotificationCompat.Builder(context, UPDATES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("PulseDesk $version скачан")
            .setContentText("Нажмите, чтобы установить")
            .setContentIntent(openApp(context, null, UPDATE_ID))
            .setAutoCancel(true)
            .build())
    }

    const val EXTRA_ROUTE = "route"
    private const val EXTRA_ID = "id"
    private const val ACTION_QUIET = "com.kirdev.pulsedeckmobile.QUIET"
    private const val ACTION_CANCEL_POWER = "com.kirdev.pulsedeckmobile.CANCEL_POWER"

    /** Screens a notification may open. */
    val ROUTES = setOf("monitoring", "processes", "control", "home")

    internal fun handle(context: Context, intent: Intent, pending: BroadcastReceiver.PendingResult) {
        val id = intent.getIntExtra(EXTRA_ID, 0)
        when (intent.action) {
            ACTION_QUIET -> {
                quietUntil = SystemClock.elapsedRealtime() + 60 * 60 * 1000L
                manager(context).cancel(id)
                pending.finish()
            }
            ACTION_CANCEL_POWER -> {
                manager(context).cancel(POWER_ID)
                val app = context.applicationContext as PulseDeskApp
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try { runCatching { app.api.cancelPower() } } finally { pending.finish() }
                }
            }
            else -> pending.finish()
        }
    }
}

class NoticeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Notices.handle(context, intent, goAsync())
    }
}
