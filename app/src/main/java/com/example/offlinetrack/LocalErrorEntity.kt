package com.example.offlinetrack

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_errors")
data class LocalErrorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val exceptionMessage: String,
    val stackTrace: String,
    val threadName: String
)
