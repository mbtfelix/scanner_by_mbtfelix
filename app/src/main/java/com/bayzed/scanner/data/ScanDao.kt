package com.bayzed.scanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanItem): Long

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clearAllScans()

    @Query("DELETE FROM scans WHERE id NOT IN (SELECT id FROM scans ORDER BY timestamp DESC LIMIT 100)")
    suspend fun trimOldScans()
}
