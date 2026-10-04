package com.calendario.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        NotificationHelper.showNow(
            c,
            i.getStringExtra("title") ?: "Attività",
            i.getStringExtra("desc") ?: "",
            i.getIntExtra("color", 4),
            i.getLongExtra("start", System.currentTimeMillis()),
            (i.getStringExtra("id") ?: "x").hashCode()
        )
    }
}
