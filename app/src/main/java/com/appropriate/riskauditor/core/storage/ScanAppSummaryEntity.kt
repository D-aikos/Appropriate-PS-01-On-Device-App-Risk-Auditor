package com.appropriate.riskauditor.core.storage

import androidx.room.Entity

@Entity(
    tableName = "scan_app_summary",
    primaryKeys = ["scanId", "packageName"]
)
data class ScanAppSummaryEntity(
    val scanId: String,
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val riskScore: Int,
    val riskLevel: String,
    val findingsCount: Int,
    val topFindingTitle: String?,
    val isSystemApp: Boolean
)
