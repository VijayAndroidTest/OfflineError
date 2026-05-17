package com.example.offlinetrack

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalErrorDao {
    // REMOVED 'suspend': This stops KSP from misinterpreting the bytecode signature
    @Insert
    fun insertError(error: LocalErrorEntity): Long

    @Query("SELECT * FROM local_errors ORDER BY timestamp DESC")
    fun getAllErrorsFlow(): Flow<List<LocalErrorEntity>>

    // REMOVED 'suspend'
    @Query("DELETE FROM local_errors")
    fun clearAllErrors()
}