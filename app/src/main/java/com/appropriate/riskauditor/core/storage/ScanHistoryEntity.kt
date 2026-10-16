package com.appropriate.riskauditor.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey
    val scanId: String,
    val timestampEpochMillis: Long,
    val totalAppsScanned: Int,
    val userAppsScanned: Int,
    val systemAppsScanned: Int,
    val criticalCount: Int,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val averageRiskScore: Int
)
