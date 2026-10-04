package com.calendario.app.sync

import android.content.Context
import android.util.Log
import com.calendario.app.data.AppDatabase
import com.calendario.app.data.EventEntity
import com.calendario.app.notify.NotificationHelper
import com.calendario.app.util.Prefs
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Sync con codice condiviso: collection calendars/{CODE}/events.
 * Senza google-services.json va in modalità solo-locale (firebaseDisponibile=false).
 */
object SyncRepository {

    private var listener: ListenerRegistration? = null
    private val _status = MutableStateFlow("Non connesso")
    val status: StateFlow<String> = _status

    var firebaseDisponibile = true

    fun currentCode(c: Context): String = Prefs.getCode(c)

    fun connect(context: Context, code: String) {
        val clean = code.trim().lowercase()
        if (clean.isEmpty()) { _status.value = "Inserisci un codice"; return }
        Prefs.setCode(context, clean)
        startListening(context, clean)
    }

    fun startListening(context: Context, code: String) {
        listener?.remove()
        if (code.isEmpty()) { _status.value = "Non connesso (solo locale)"; return }
        try {
            val auth = Firebase.auth
            if (auth.currentUser == null) {
                auth.signInAnonymously()
                    .addOnSuccessListener { attach(context, code) }
                    .addOnFailureListener {
                        firebaseDisponibile = false
                        _status.value = "Offline: Firebase non configurato, uso locale"
                    }
            } else attach(context, code)
        } catch (e: Exception) {
            Log.w("Sync", "Firebase assente, modo locale", e)
            firebaseDisponibile = false
            _status.value = "Offline: Firebase non configurato, uso locale"
        }
    }

    private fun attach(context: Context, code: String) {
        try {
            _status.value = "Connessione a \"$code\"…"
            listener = Firebase.firestore
                .collection("calendars").document(code)
                .collection("events")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        _status.value = "Errore sync: ${err.message}"
                        return@addSnapshotListener
                    }
                    if (snap == null) return@addSnapshotListener
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppDatabase.get(context)
                        val list = snap.documents.mapNotNull { d ->
                            try {
                                EventEntity(
                                    id = d.id,
                                    title = d.getString("title") ?: "",
                                    description = d.getString("description") ?: "",
                                    startMillis = d.getLong("startMillis") ?: 0L,
                                    colorIndex = (d.getLong("colorIndex") ?: 4L).toInt(),
                                    reminderMinutes = (d.getLong("reminderMinutes") ?: 0L).toInt(),
                                    calendarCode = code,
                                    updatedAt = d.getLong("updatedAt") ?: 0L,
                                    firestoreId = d.id
                                )
                            } catch (_: Exception) { null }
                        }
                        db.eventDao().deleteByCode(code)
                        if (list.isNotEmpty()) db.eventDao().upsertAll(list)
                        // riprogramma notifiche locali per il futuro
                        val future = list.filter { it.startMillis > System.currentTimeMillis() }
                        NotificationHelper.rescheduleAll(context, future)
                    }
                    _status.value = "Connesso a \"$code\" • ${snap.size()} attività"
                }
        } catch (e: Exception) {
            firebaseDisponibile = false
            _status.value = "Offline: uso locale"
        }
    }

    fun pushEvent(context: Context, e: EventEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.get(context).eventDao().upsert(e)
            NotificationHelper.schedule(context, e)
        }
        if (e.calendarCode.isEmpty()) return
        try {
            Firebase.firestore.collection("calendars").document(e.calendarCode)
                .collection("events").document(e.id)
                .set(
                    mapOf(
                        "title" to e.title,
                        "description" to e.description,
                        "startMillis" to e.startMillis,
                        "colorIndex" to e.colorIndex,
                        "reminderMinutes" to e.reminderMinutes,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
        } catch (_: Exception) { /* resta locale */ }
    }

    fun deleteEvent(context: Context, e: EventEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.get(context).eventDao().deleteById(e.id)
            NotificationHelper.cancel(context, e)
        }
        if (e.calendarCode.isEmpty()) return
        try {
            Firebase.firestore.collection("calendars").document(e.calendarCode)
                .collection("events").document(e.id).delete()
        } catch (_: Exception) { }
    }
}
