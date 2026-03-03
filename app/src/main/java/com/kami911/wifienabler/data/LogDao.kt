package com.kami911.wifienabler.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {

    @Query("SELECT * FROM log_entries ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<LogEntry>>

    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("DELETE FROM log_entries")
    suspend fun clearAll()

    @Query("DELETE FROM log_entries WHERE timestamp < :threshold")
    suspend fun deleteOlderThan(threshold: Long)

    @Query("SELECT COUNT(*) FROM log_entries")
    suspend fun count(): Int
}
