package com.appropriate.riskauditor.core.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

/**
 * Robust reader of installed packages with DeadObjectException and TransactionTooLargeException
 * fallbacks, ensuring inventory queries never crash the audit scan.
 */
class InstalledPackagesReader(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Enumerates all installed packages on the device.
     * Uses GET_PERMISSIONS, GET_RECEIVERS, GET_SERVICES.
     * Handles API 33+ PackageInfoFlags and falls back to individual package querying
     * if the bulk transaction exceeds the binder buffer limit.
     */
    fun readAll(onWarning: ((String) -> Unit)? = null): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        } catch (e: RuntimeException) {
            // Transaction too large or DeadObjectException: fallback to per-package interrogation
            onWarning?.invoke("Bulk package query exceeded binder limit; falling back to individual queries")
            fallbackReadPerPackage(flags)
        } catch (e: Exception) {
            onWarning?.invoke("Could not query all packages: ${e.message}")
            emptyList()
        }
    }

    private fun fallbackReadPerPackage(flags: Int): List<PackageInfo> {
        val results = mutableListOf<PackageInfo>()
        val apps = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }
        } catch (e: Exception) {
            return emptyList()
        }

        for (app in apps) {
            try {
                val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(app.packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(app.packageName, flags)
                }
                results.add(pkgInfo)
            } catch (e: Exception) {
                // Ignore missing package or transient binder error per-app
            }
        }
        return results
    }

    companion object {
        /**
         * Checks if an application is a genuine pre-installed system app according to AGENTS.md:
         * (flags and FLAG_SYSTEM) != 0 && (flags and FLAG_UPDATED_SYSTEM_APP) == 0
         */
        fun isSystem(appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val flags = appInfo.flags
            val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            return isSystem && !isUpdatedSystem
        }
    }
}
