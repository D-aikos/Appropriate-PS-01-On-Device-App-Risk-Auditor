package com.appropriate.riskauditor.core.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.appropriate.riskauditor.core.model.*
import com.appropriate.riskauditor.core.rules.CategoryMapper

/**
 * Android platform adapter for querying PackageManager metadata and permission states.
 * Respects honest visibility constraints and never fabricates signals.
 */
class PackageManagerAdapter(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Enumerates installed packages on the device.
     * Declares QUERY_ALL_PACKAGES for security auditing purposes.
     */
    fun getInstalledAppPackages(includeSystemApps: Boolean = false): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES

        val allPackages = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return allPackages.filter { pkgInfo ->
            val flags = pkgInfo.applicationInfo?.flags ?: 0
            val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            if (includeSystemApps) true else !isSystem
        }
    }

    /**
     * Normalizes raw PackageInfo into an AppInventoryRecord.
     */
    fun extractInventory(packageInfo: PackageInfo): AppInventoryRecord {
        val appInfo = packageInfo.applicationInfo
        val appLabel = appInfo?.loadLabel(packageManager)?.toString() ?: packageInfo.packageName
        val flags = appInfo?.flags ?: 0
        val isSystem = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 && (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        val targetSdk = appInfo?.targetSdkVersion
        val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val versionName = packageInfo.versionName
        val firstInstallTime = packageInfo.firstInstallTime.takeIf { it > 0 }
        val lastUpdateTime = packageInfo.lastUpdateTime.takeIf { it > 0 }

        val installerPackage = queryInstallerPackage(packageInfo.packageName)

        return AppInventoryRecord(
            packageName = packageInfo.packageName,
            appLabel = appLabel,
            versionName = versionName,
            versionCode = versionCode,
            targetSdk = targetSdk,
            isSystemApp = isSystem,
            installerPackage = installerPackage,
            firstInstallTimeEpochMillis = firstInstallTime,
            lastUpdateTimeEpochMillis = lastUpdateTime
        )
    }

    private val permissionProtectionCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Resolves manifest-declared permissions and their current runtime grant status.
     * Distinguishes declared vs granted states honestly.
     */
    fun extractPermissions(packageInfo: PackageInfo): List<PermissionAudit> {
        val requested = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(0)

        val audits = mutableListOf<PermissionAudit>()

        for (i in requested.indices) {
            val permName = requested[i]
            val isDeclared = true
            val category = CategoryMapper.mapPermission(permName)

            val isGrantedByFlag = if (i < flags.size) {
                (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                false
            }

            // Determine accurate GrantState
            val grantState = when {
                isGrantedByFlag -> GrantState.GRANTED
                isRuntimePermission(permName) -> GrantState.DENIED
                else -> GrantState.NOT_RUNTIME
            }

            val protectionLevel = resolveProtectionLevel(permName)

            audits.add(
                PermissionAudit(
                    name = permName,
                    isDeclared = isDeclared,
                    grantState = grantState,
                    category = category,
                    protectionLevel = protectionLevel
                )
            )
        }

        return audits
    }

    private fun resolveProtectionLevel(permissionName: String): String {
        return permissionProtectionCache.computeIfAbsent(permissionName) { name ->
            try {
                val permInfo = packageManager.getPermissionInfo(name, 0)
                val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    permInfo.protection
                } else {
                    @Suppress("DEPRECATION")
                    permInfo.protectionLevel and android.content.pm.PermissionInfo.PROTECTION_MASK_BASE
                }
                when (base) {
                    android.content.pm.PermissionInfo.PROTECTION_NORMAL -> "NORMAL"
                    android.content.pm.PermissionInfo.PROTECTION_DANGEROUS -> "DANGEROUS"
                    android.content.pm.PermissionInfo.PROTECTION_SIGNATURE -> "SIGNATURE"
                    else -> "OTHER"
                }
            } catch (e: PackageManager.NameNotFoundException) {
                "UNKNOWN"
            } catch (e: Exception) {
                "UNKNOWN"
            }
        }
    }

    private fun queryInstallerPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun isRuntimePermission(permissionName: String): Boolean {
        // High-level dangerous permission heuristic
        val perm = permissionName.uppercase()
        return perm.contains("SMS") ||
                perm.contains("CONTACTS") ||
                perm.contains("LOCATION") ||
                perm.contains("CAMERA") ||
                perm.contains("RECORD_AUDIO") ||
                perm.contains("CALL_LOG") ||
                perm.contains("READ_PHONE_STATE") ||
                perm.contains("READ_EXTERNAL_STORAGE") ||
                perm.contains("WRITE_EXTERNAL_STORAGE") ||
                perm.contains("READ_MEDIA_")
    }
}
