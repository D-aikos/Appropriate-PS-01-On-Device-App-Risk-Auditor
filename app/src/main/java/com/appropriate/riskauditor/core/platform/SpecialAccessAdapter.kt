package com.appropriate.riskauditor.core.platform

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.appropriate.riskauditor.core.model.SpecialAccessAudit
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.model.SpecialAccessType

/**
 * Android platform adapter querying special access capabilities (Accessibility,
 * Device Admin, Notification Listener, Overlay, Install Packages, Usage Stats).
 * Accurately differentiates declared capability from active/enabled state.
 */
class SpecialAccessAdapter(private val context: Context) {

    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager

    fun auditSpecialAccess(packageInfo: PackageInfo): List<SpecialAccessAudit> {
        val packageName = packageInfo.packageName
        val declaredPermissions = packageInfo.requestedPermissions?.toSet() ?: emptySet()
        val results = mutableListOf<SpecialAccessAudit>()

        // 1. Accessibility Service Check
        results.add(checkAccessibilityService(packageInfo))

        // 2. Active Device Administrator Check
        results.add(checkDeviceAdmin(packageInfo))

        // 3. Notification Listener Check
        results.add(checkNotificationListener(packageInfo))

        // 4. System Overlay (Draw Over Other Apps)
        results.add(checkSystemOverlay(packageName, declaredPermissions))

        // 5. Request Install Packages (Unknown Sources)
        results.add(checkInstallPackages(packageName, declaredPermissions))

        // 6. Usage Stats Access
        results.add(checkUsageStats(packageInfo, declaredPermissions))

        return results
    }

    private fun checkAccessibilityService(packageInfo: PackageInfo): SpecialAccessAudit {
        val packageName = packageInfo.packageName
        val services = packageInfo.services ?: emptyArray()
        val hasAccessibilityDeclaration = services.any { svc ->
            svc.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE"
        }

        val enabledServices = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
        } catch (e: Exception) {
            ""
        }

        val isActivelyEnabled = enabledServices.split(":").any { componentStr ->
            componentStr.startsWith("$packageName/") || componentStr == packageName
        }

        val state = when {
            isActivelyEnabled -> SpecialAccessState.ENABLED
            hasAccessibilityDeclaration -> SpecialAccessState.DISABLED
            else -> SpecialAccessState.NOT_DECLARED
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.ACCESSIBILITY_SERVICE,
            state = state,
            isDeclared = hasAccessibilityDeclaration,
            detail = if (isActivelyEnabled) "Accessibility service component is actively enabled in System Settings" else null
        )
    }

    private fun checkDeviceAdmin(packageInfo: PackageInfo): SpecialAccessAudit {
        val packageName = packageInfo.packageName
        val receivers = packageInfo.receivers ?: emptyArray()
        val hasAdminDeclaration = receivers.any { rcvr ->
            rcvr.permission == "android.permission.BIND_DEVICE_ADMIN"
        }

        var isActivelyAdmin = false
        try {
            val activeAdmins = devicePolicyManager?.activeAdmins ?: emptyList()
            isActivelyAdmin = activeAdmins.any { it.packageName == packageName }
        } catch (e: Exception) {
            // Permission/policy boundary
        }

        val state = when {
            isActivelyAdmin -> SpecialAccessState.ENABLED
            hasAdminDeclaration -> SpecialAccessState.DISABLED
            else -> SpecialAccessState.NOT_DECLARED
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.DEVICE_ADMIN,
            state = state,
            isDeclared = hasAdminDeclaration,
            detail = if (isActivelyAdmin) "Active Device Administrator privileges enabled" else null
        )
    }

    private fun checkNotificationListener(packageInfo: PackageInfo): SpecialAccessAudit {
        val packageName = packageInfo.packageName
        val services = packageInfo.services ?: emptyArray()
        val hasNotificationDeclaration = services.any { svc ->
            svc.permission == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        }

        val enabledListeners = try {
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: ""
        } catch (e: Exception) {
            ""
        }

        val isActivelyEnabled = enabledListeners.split(":").any { componentStr ->
            componentStr.startsWith("$packageName/") || componentStr == packageName
        }

        val state = when {
            isActivelyEnabled -> SpecialAccessState.ENABLED
            hasNotificationDeclaration -> SpecialAccessState.DISABLED
            else -> SpecialAccessState.NOT_DECLARED
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.NOTIFICATION_LISTENER,
            state = state,
            isDeclared = hasNotificationDeclaration,
            detail = if (isActivelyEnabled) "Notification listener service enabled" else null
        )
    }

    private fun checkSystemOverlay(packageName: String, declaredPermissions: Set<String>): SpecialAccessAudit {
        val isDeclared = "android.permission.SYSTEM_ALERT_WINDOW" in declaredPermissions

        val isGranted = if (packageName == context.packageName) {
            Settings.canDrawOverlays(context)
        } else {
            // For other applications, check AppOpsManager OPSTR_SYSTEM_ALERT_WINDOW
            checkAppOp(packageName, AppOpsManager.OPSTR_SYSTEM_ALERT_WINDOW)
        }

        val state = when {
            isGranted == true -> SpecialAccessState.ENABLED
            isGranted == false && isDeclared -> SpecialAccessState.DISABLED
            !isDeclared -> SpecialAccessState.NOT_DECLARED
            else -> SpecialAccessState.UNKNOWN
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.SYSTEM_ALERT_WINDOW,
            state = state,
            isDeclared = isDeclared,
            detail = if (state == SpecialAccessState.ENABLED) "Can display overlay windows over other applications" else null
        )
    }

    private fun checkInstallPackages(packageName: String, declaredPermissions: Set<String>): SpecialAccessAudit {
        val isDeclared = "android.permission.REQUEST_INSTALL_PACKAGES" in declaredPermissions

        val isGranted = if (packageName == context.packageName) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else {
                null
            }
        } else {
            // For third-party apps, check AppOpsManager OP_REQUEST_INSTALL_PACKAGES
            checkAppOp(packageName, "android:request_install_packages")
        }

        val state = when {
            isGranted == true -> SpecialAccessState.ENABLED
            isGranted == false && isDeclared -> SpecialAccessState.DISABLED
            !isDeclared -> SpecialAccessState.NOT_DECLARED
            else -> SpecialAccessState.UNKNOWN
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.REQUEST_INSTALL_PACKAGES,
            state = state,
            isDeclared = isDeclared,
            detail = if (state == SpecialAccessState.ENABLED) "Permission to install unknown APKs is granted" else null
        )
    }

    private fun checkUsageStats(packageInfo: PackageInfo, declaredPermissions: Set<String>): SpecialAccessAudit {
        val isDeclared = "android.permission.PACKAGE_USAGE_STATS" in declaredPermissions
        val isGranted = checkAppOp(packageInfo.packageName, AppOpsManager.OPSTR_GET_USAGE_STATS)

        val state = when {
            isGranted == true -> SpecialAccessState.ENABLED
            isGranted == false && isDeclared -> SpecialAccessState.DISABLED
            !isDeclared -> SpecialAccessState.NOT_DECLARED
            else -> SpecialAccessState.UNKNOWN
        }

        return SpecialAccessAudit(
            type = SpecialAccessType.USAGE_STATS,
            state = state,
            isDeclared = isDeclared,
            detail = if (state == SpecialAccessState.ENABLED) "Granted permission to access application usage statistics" else null
        )
    }

    private fun checkAppOp(packageName: String, opStr: String): Boolean? {
        if (appOpsManager == null) return null
        return try {
            val uid = context.packageManager.getPackageUid(packageName, 0)
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(opStr, uid, packageName)
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(opStr, uid, packageName)
            }
            when (mode) {
                AppOpsManager.MODE_ALLOWED -> true
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> false
                else -> null // MODE_DEFAULT or unhandled => UNKNOWN (Unknown is not safe)
            }
        } catch (e: Exception) {
            null
        }
    }
}
