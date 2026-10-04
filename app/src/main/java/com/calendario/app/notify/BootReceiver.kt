package com.calendario.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.calendario.app.data.AppDatabase
import com.calendario.app.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val code = Prefs.getCode(c)
            if (code.isEmpty()) return@launch
            val list = AppDatabase.get(c).eventDao().listByCode(code)
                .filter { it.startMillis > System.currentTimeMillis() }
            NotificationHelper.rescheduleAll(c, list)
        }
    }
}
