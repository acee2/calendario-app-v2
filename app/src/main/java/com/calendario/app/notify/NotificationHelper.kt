package com.calendario.app.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.calendario.app.MainActivity
import com.calendario.app.R
import com.calendario.app.data.EventEntity
import com.calendario.app.util.ColorUtils
import java.text.SimpleDateFormat
import java.util.*

object NotificationHelper {
    const val CH = "attivita"

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH, "Attività", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun triggerTime(e: EventEntity): Long {
        if (e.reminderMinutes < 0) return -1 // nessuno
        return e.startMillis - e.reminderMinutes * 60_000L
    }

    fun schedule(c: Context, e: EventEntity) {
        ensureChannel(c)
        val at = triggerTime(e)
        if (at < 0) return
        // Se l'ora è già passata ma l'evento è futuro prossimo, notifica comunque tra 2s? No: salta.
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val i = Intent(c, AlarmReceiver::class.java).apply {
            putExtra("id", e.id); putExtra("title", e.title)
            putExtra("desc", e.description); putExtra("color", e.colorIndex)
            putExtra("start", e.startMillis)
        }
        val pi = PendingIntent.getBroadcast(
            c, e.id.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.coerceAtLeast(System.currentTimeMillis() + 1000), pi) }
        catch (_: SecurityException) { }
    }

    fun cancel(c: Context, e: EventEntity) {
        val am = c.getSystemService(AlarmManager::class.java) ?: return
        val pi = PendingIntent.getBroadcast(
            c, e.id.hashCode(), Intent(c, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }

    fun rescheduleAll(c: Context, list: List<EventEntity>) {
        list.forEach { schedule(c, it) }
    }

    fun showNow(c: Context, title: String, desc: String, colorIndex: Int, start: Long, notifId: Int) {
        ensureChannel(c)
        val fmt = SimpleDateFormat("dd MMM HH:mm", Locale.ITALIAN)
        val open = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle(title)
            .setContentText("${fmt.format(Date(start))} • $desc")
            .setColor(ColorUtils.resolve(c, colorIndex))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.notify(notifId, n)
    }
}
