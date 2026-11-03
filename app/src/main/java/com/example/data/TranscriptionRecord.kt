package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val speaker: String,
    val content: String,
    val type: String, // "LIVE", "VOICE_NOTE", "CONVERSATION"
    val timestamp: Long = System.currentTimeMillis()
)
