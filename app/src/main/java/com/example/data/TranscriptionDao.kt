package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptionDao {
    @Query("SELECT * FROM transcriptions ORDER BY timestamp DESC")
    fun getAllTranscriptions(): Flow<List<TranscriptionRecord>>

    @Query("SELECT * FROM transcriptions WHERE type = :type ORDER BY timestamp DESC")
    fun getTranscriptionsByType(type: String): Flow<List<TranscriptionRecord>>

    @Query("SELECT * FROM transcriptions WHERE content LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTranscriptions(query: String): Flow<List<TranscriptionRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: TranscriptionRecord): Long

    @Delete
    suspend fun delete(record: TranscriptionRecord)

    @Query("DELETE FROM transcriptions")
    suspend fun clearAll()
}
