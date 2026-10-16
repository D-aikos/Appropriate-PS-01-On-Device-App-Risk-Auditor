package com.appropriate.riskauditor.core.storage

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScanHistory(scanHistory: ScanHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppSummaries(summaries: List<ScanAppSummaryEntity>)

    @Query("SELECT * FROM scan_history ORDER BY timestampEpochMillis DESC")
    fun getAllScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE scanId = :scanId LIMIT 1")
    suspend fun getScanById(scanId: String): ScanHistoryEntity?

    @Query("SELECT * FROM scan_app_summary WHERE scanId = :scanId ORDER BY riskScore DESC")
    fun getAppsForScan(scanId: String): Flow<List<ScanAppSummaryEntity>>

    @Query("DELETE FROM scan_history WHERE scanId = :scanId")
    suspend fun deleteScan(scanId: String)

    @Query("DELETE FROM scan_app_summary WHERE scanId = :scanId")
    suspend fun deleteAppSummariesForScan(scanId: String)

    @Transaction
    suspend fun deleteScanAndApps(scanId: String) {
        deleteAppSummariesForScan(scanId)
        deleteScan(scanId)
    }

    @Query("DELETE FROM scan_history")
    suspend fun clearAllHistory()
}
