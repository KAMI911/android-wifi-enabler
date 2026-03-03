package com.kami911.wifienabler.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val trigger: String,
    val action: String,   // "enable" or "disable"
    val success: Boolean,
    val message: String
)
