package com.bayzed.scanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val format: String,
    val content: String,
    val parsedType: String,
    val timestamp: Long = System.currentTimeMillis()
)
