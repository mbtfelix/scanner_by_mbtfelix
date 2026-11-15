package com.bayzed.scanner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanItem>> = scanDao.getAllScans()
        .flowOn(Dispatchers.IO)

    suspend fun insert(scan: ScanItem) = withContext(Dispatchers.IO) {
        scanDao.insertScan(scan)
        scanDao.trimOldScans()
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        scanDao.deleteScanById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        scanDao.clearAllScans()
    }
}
