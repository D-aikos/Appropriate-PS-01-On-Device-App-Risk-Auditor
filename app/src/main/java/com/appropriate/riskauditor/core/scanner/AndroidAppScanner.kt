package com.appropriate.riskauditor.core.scanner

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.platform.InstallSourceResolver
import com.appropriate.riskauditor.core.platform.InstalledPackagesReader
import com.appropriate.riskauditor.core.platform.PermissionExtractor
import com.appropriate.riskauditor.core.platform.SpecialAccessCollector
import com.appropriate.riskauditor.core.rules.RiskScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

sealed interface ScanEvent {
    data class Progress(val done: Int, val total: Int, val currentLabel: String) : ScanEvent
    data class AppScanned(val report: AppRiskReport) : ScanEvent
    data class Warning(val message: String) : ScanEvent
}

/**
 * On-device Android application scanner adhering strictly to Section 6 Task D2-4:
 * 1. Takes a single device-wide special access snapshot up front.
 * 2. Emits progressive events over Dispatchers.Default with cooperative cancellation.
 * 3. Never logs or leaks package names in warning messages.
 * 4. Audits own package safely.
 */
class AndroidAppScanner(
    private val context: Context,
    private val packagesReader: InstalledPackagesReader = InstalledPackagesReader(context),
    private val specialAccessCollector: SpecialAccessCollector = SpecialAccessCollector(context),
    private val permissionExtractor: PermissionExtractor = PermissionExtractor(context.packageManager)
) {
    private val packageManager: PackageManager = context.packageManager

    fun scan(includeSystemApps: Boolean = false): Flow<ScanEvent> = flow {
        val scanTimestamp = System.currentTimeMillis()

        // 1. Read all packages with bulk-query fallback
        val warnings = mutableListOf<String>()
        val packages = packagesReader.readAll { warning ->
            warnings.add(warning)
        }

        for (w in warnings) {
            emit(ScanEvent.Warning(w))
        }

        val filteredPackages = packages.filter { pkgInfo ->
            val isSystem = InstalledPackagesReader.isSystem(pkgInfo.applicationInfo)
            if (includeSystemApps) true else !isSystem
        }

        val total = filteredPackages.size
        if (total == 0) return@flow

        // 2. Compute SpecialAccessSnapshot ONCE for the whole scan
        val specialAccessSnapshot = specialAccessCollector.snapshot()

        // 3. Process each application cooperatively
        for (i in filteredPackages.indices) {
            currentCoroutineContext().ensureActive()

            val pkgInfo = filteredPackages[i]
            val label = resolveAppLabel(pkgInfo)

            emit(ScanEvent.Progress(done = i + 1, total = total, currentLabel = label))

            try {
                val report = auditSinglePackage(pkgInfo, specialAccessSnapshot, scanTimestamp)
                emit(ScanEvent.AppScanned(report))
            } catch (e: Exception) {
                // Rule: Never log package names, app lists, or scan results.
                // Wrap per-app work in try/catch, add a warning, continue.
                emit(ScanEvent.Warning("Could not fully audit an application"))
            }
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Inspects a single package on demand (e.g. for install monitoring or single-app re-audit).
     */
    fun scanOne(packageName: String): AppRiskReport? {
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES

        val pkgInfo: PackageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags)
            }
        } catch (e: Exception) {
            return null
        }

        val snapshot = specialAccessCollector.snapshot()
        return auditSinglePackage(pkgInfo, snapshot, System.currentTimeMillis())
    }

    private fun auditSinglePackage(
        pkgInfo: PackageInfo,
        specialAccessSnapshot: com.appropriate.riskauditor.core.platform.SpecialAccessSnapshot,
        timestamp: Long
    ): AppRiskReport {
        val appInfo = pkgInfo.applicationInfo
        val label = resolveAppLabel(pkgInfo)
        val isSystem = InstalledPackagesReader.isSystem(appInfo)
        val targetSdk = appInfo?.targetSdkVersion
        val versionCode = PackageInfoCompat.getLongVersionCode(pkgInfo)
        val versionName = pkgInfo.versionName
        val firstInstallTime = pkgInfo.firstInstallTime.takeIf { it > 0 }
        val lastUpdateTime = pkgInfo.lastUpdateTime.takeIf { it > 0 }

        val (installSource, installerPkg) = InstallSourceResolver.resolve(
            packageManager,
            pkgInfo.packageName,
            isSystem
        )

        val inventory = AppInventoryRecord(
            packageName = pkgInfo.packageName,
            appLabel = label,
            versionName = versionName,
            versionCode = versionCode,
            targetSdk = targetSdk,
            isSystemApp = isSystem,
            installerPackage = installerPkg,
            firstInstallTimeEpochMillis = firstInstallTime,
            lastUpdateTimeEpochMillis = lastUpdateTime
        )

        val permissions = permissionExtractor.extract(pkgInfo)
        val specialAccess = specialAccessSnapshot.auditsFor(pkgInfo)

        return RiskScoringEngine.evaluate(
            inventory = inventory,
            permissions = permissions,
            specialAccess = specialAccess,
            scannedAtEpochMillis = timestamp
        )
    }

    private fun resolveAppLabel(pkgInfo: PackageInfo): String {
        val raw = try {
            pkgInfo.applicationInfo?.loadLabel(packageManager)?.toString()
        } catch (e: Exception) {
            null
        } ?: pkgInfo.packageName

        // Truncate cleanly to 120 chars max to prevent UI buffer overflows
        return if (raw.length > 120) raw.take(117) + "..." else raw
    }
}
