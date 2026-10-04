package com.calendario.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE calendarCode = :code ORDER BY startMillis ASC")
    fun observeByCode(code: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE calendarCode = :code ORDER BY startMillis ASC")
    suspend fun listByCode(code: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events WHERE calendarCode = :code")
    suspend fun deleteByCode(code: String)
}
