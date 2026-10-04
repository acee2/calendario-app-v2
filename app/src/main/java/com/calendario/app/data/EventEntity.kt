package com.calendario.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val startMillis: Long = 0L,
    val colorIndex: Int = 4,
    val reminderMinutes: Int = 0, // 0=ora esatta, -1=nessuno, 15/60/1440
    val calendarCode: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val firestoreId: String = "" // id documento remoto, uguale a id
)
