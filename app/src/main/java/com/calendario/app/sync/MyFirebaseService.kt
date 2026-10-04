package com.calendario.app.sync

import com.calendario.app.notify.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Push FCM (lato "Entrambe"): il mittente invia data-message con title/desc/start/color. */
class MyFirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(m: RemoteMessage) {
        val d = m.data
        if (d.isEmpty() && m.notification != null) {
            NotificationHelper.showNow(
                this, m.notification!!.title ?: "Attività",
                m.notification!!.body ?: "", 4,
                System.currentTimeMillis(), System.currentTimeMillis().toInt()
            )
            return
        }
        if (d.isNotEmpty()) {
            NotificationHelper.showNow(
                this,
                d["title"] ?: "Attività",
                d["desc"] ?: "",
                (d["color"]?.toIntOrNull() ?: 4),
                (d["start"]?.toLongOrNull() ?: System.currentTimeMillis()),
                (d["id"] ?: System.currentTimeMillis().toString()).hashCode()
            )
        }
    }
}
