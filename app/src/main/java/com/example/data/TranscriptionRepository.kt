package com.example.data

import kotlinx.coroutines.flow.Flow

class TranscriptionRepository(private val dao: TranscriptionDao) {
    val allTranscriptions: Flow<List<TranscriptionRecord>> = dao.getAllTranscriptions()

    fun getByType(type: String): Flow<List<TranscriptionRecord>> = dao.getTranscriptionsByType(type)

    fun search(query: String): Flow<List<TranscriptionRecord>> = dao.searchTranscriptions(query)

    suspend fun save(record: TranscriptionRecord): Long = dao.insert(record)

    suspend fun delete(record: TranscriptionRecord) = dao.delete(record)

    suspend fun clearAll() = dao.clearAll()
}
