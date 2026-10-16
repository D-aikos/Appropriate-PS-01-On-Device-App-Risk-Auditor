package com.appropriate.riskauditor.core.platform

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import com.appropriate.riskauditor.core.model.SpecialAccessAudit
import com.appropriate.riskauditor.core.model.SpecialAccessState
import com.appropriate.riskauditor.core.model.SpecialAccessType

/**
 * Snapshot of device-wide special access states computed ONCE per scan session.
 * Always produces exactly 6 SpecialAccessAudit entries per application.
 */
class SpecialAccessSnapshot(
    private val enabledA11yPackages: Set<String>,
    private val declaredA11yPackages: Set<String>,
    private val a11yQueryFailed: Boolean,
    private val activeAdminPackages: Set<String>,
    private val adminQueryFailed: Boolean,
    private val enabledListenerPackages: Set<String>,
    private val listenerQueryFailed: Boolean,
    private val appOpsManager: AppOpsManager?,
    private val devicePolicyManager: DevicePolicyManager?
) {

    fun isDeviceOrProfileOwner(packageName: String): Boolean {
        if (devicePolicyManager == null) return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                devicePolicyManager.isDeviceOwnerApp(packageName) || devicePolicyManager.isProfileOwnerApp(packageName)
            } else {
                @Suppress("DEPRECATION")
                devicePolicyManager.isDeviceOwnerApp(packageName)
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Resolves exactly 6 SpecialAccessAudit entries for a given PackageInfo.
     */
    fun auditsFor(info: PackageInfo): List<SpecialAccessAudit> {
        val packageName = info.packageName
        val declaredPerms = info.requestedPermissions?.toSet() ?: emptySet()
        val services = info.services ?: emptyArray()
        val receivers = info.receivers ?: emptyArray()
        val uid = info.applicationInfo?.uid ?: Process.myUid()

        val results = mutableListOf<SpecialAccessAudit>()

        // 1. Accessibility Service
        val a11yDeclared = packageName in declaredA11yPackages ||
                services.any { it.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE" }
        val a11yAudit = when {
            !a11yDeclared -> SpecialAccessAudit(
                type = SpecialAccessType.ACCESSIBILITY_SERVICE,
                state = SpecialAccessState.NOT_DECLARED,
                isDeclared = false,
                detail = "Not requested by app"
            )
            a11yQueryFailed -> SpecialAccessAudit(
                type = SpecialAccessType.ACCESSIBILITY_SERVICE,
                state = SpecialAccessState.UNKNOWN,
                isDeclared = true,
                detail = "Declared in manifest; could not verify enabled state"
            )
            packageName in enabledA11yPackages -> SpecialAccessAudit(
                type = SpecialAccessType.ACCESSIBILITY_SERVICE,
                state = SpecialAccessState.ENABLED,
                isDeclared = true,
                detail = "Listed in the phone's enabled accessibility services"
            )
            else -> SpecialAccessAudit(
                type = SpecialAccessType.ACCESSIBILITY_SERVICE,
                state = SpecialAccessState.DISABLED,
                isDeclared = true,
                detail = "Declared in manifest; currently turned off"
            )
        }
        results.add(a11yAudit)

        // 2. Active Device Administrator
        val adminDeclared = receivers.any { it.permission == "android.permission.BIND_DEVICE_ADMIN" }
        val adminAudit = when {
            !adminDeclared -> SpecialAccessAudit(
                type = SpecialAccessType.DEVICE_ADMIN,
                state = SpecialAccessState.NOT_DECLARED,
                isDeclared = false,
                detail = "Not requested by app"
            )
            adminQueryFailed -> SpecialAccessAudit(
                type = SpecialAccessType.DEVICE_ADMIN,
                state = SpecialAccessState.UNKNOWN,
                isDeclared = true,
                detail = "Declared device admin receiver; query failed"
            )
            packageName in activeAdminPackages -> SpecialAccessAudit(
                type = SpecialAccessType.DEVICE_ADMIN,
                state = SpecialAccessState.ENABLED,
                isDeclared = true,
                detail = "Actively registered as a Device Administrator"
            )
            else -> SpecialAccessAudit(
                type = SpecialAccessType.DEVICE_ADMIN,
                state = SpecialAccessState.DISABLED,
                isDeclared = true,
                detail = "Declared device admin receiver; not active"
            )
        }
        results.add(adminAudit)

        // 3. Notification Listener
        val listenerDeclared = services.any {
            it.permission == "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
        }
        val listenerAudit = when {
            !listenerDeclared -> SpecialAccessAudit(
                type = SpecialAccessType.NOTIFICATION_LISTENER,
                state = SpecialAccessState.NOT_DECLARED,
                isDeclared = false,
                detail = "Not requested by app"
            )
            listenerQueryFailed -> SpecialAccessAudit(
                type = SpecialAccessType.NOTIFICATION_LISTENER,
                state = SpecialAccessState.UNKNOWN,
                isDeclared = true,
                detail = "Declared notification listener; query failed"
            )
            packageName in enabledListenerPackages -> SpecialAccessAudit(
                type = SpecialAccessType.NOTIFICATION_LISTENER,
                state = SpecialAccessState.ENABLED,
                isDeclared = true,
                detail = "Enabled in system notification access settings"
            )
            else -> SpecialAccessAudit(
                type = SpecialAccessType.NOTIFICATION_LISTENER,
                state = SpecialAccessState.DISABLED,
                isDeclared = true,
                detail = "Declared notification listener service; turned off"
            )
        }
        results.add(listenerAudit)

        // 4. System Overlay
        val overlayDeclared = "android.permission.SYSTEM_ALERT_WINDOW" in declaredPerms
        results.add(
            checkAppOpCapability(
                type = SpecialAccessType.SYSTEM_ALERT_WINDOW,
                declared = overlayDeclared,
                opString = "android:system_alert_window",
                uid = uid,
                packageName = packageName
            )
        )

        // 5. Request Install Packages
        val installDeclared = "android.permission.REQUEST_INSTALL_PACKAGES" in declaredPerms
        results.add(
            checkAppOpCapability(
                type = SpecialAccessType.REQUEST_INSTALL_PACKAGES,
                declared = installDeclared,
                opString = "android:request_install_packages",
                uid = uid,
                packageName = packageName
            )
        )

        // 6. Usage Stats Access
        val usageDeclared = "android.permission.PACKAGE_USAGE_STATS" in declaredPerms
        results.add(
            checkAppOpCapability(
                type = SpecialAccessType.USAGE_STATS,
                declared = usageDeclared,
                opString = "android:get_usage_stats",
                uid = uid,
                packageName = packageName
            )
        )

        return results
    }

    private fun checkAppOpCapability(
        type: SpecialAccessType,
        declared: Boolean,
        opString: String,
        uid: Int,
        packageName: String
    ): SpecialAccessAudit {
        if (!declared) {
            return SpecialAccessAudit(
                type = type,
                state = SpecialAccessState.NOT_DECLARED,
                isDeclared = false,
                detail = "Not requested by app"
            )
        }

        if (appOpsManager == null) {
            return SpecialAccessAudit(
                type = type,
                state = SpecialAccessState.UNKNOWN,
                isDeclared = true,
                detail = "AppOps service unavailable on device"
            )
        }

        return try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(opString, uid, packageName)
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(opString, uid, packageName)
            }

            when (mode) {
                AppOpsManager.MODE_ALLOWED -> SpecialAccessAudit(
                    type = type,
                    state = SpecialAccessState.ENABLED,
                    isDeclared = true,
                    detail = "Turned on by user in system settings"
                )
                AppOpsManager.MODE_IGNORED, AppOpsManager.MODE_ERRORED -> SpecialAccessAudit(
                    type = type,
                    state = SpecialAccessState.DISABLED,
                    isDeclared = true,
                    detail = "Declared in manifest; currently disabled"
                )
                else -> SpecialAccessAudit(
                    type = type,
                    state = SpecialAccessState.UNKNOWN,
                    isDeclared = true,
                    detail = "Couldn't check; Android did not report whether it is turned on"
                )
            }
        } catch (e: Exception) {
            SpecialAccessAudit(
                type = type,
                state = SpecialAccessState.UNKNOWN,
                isDeclared = true,
                detail = "Couldn't check AppOps status (${e.javaClass.simpleName})"
            )
        }
    }
}

/**
 * Creates device-wide SpecialAccessSnapshot once per scan.
 */
class SpecialAccessCollector(private val context: Context) {

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    private val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    private val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager

    fun snapshot(): SpecialAccessSnapshot {
        // 1. Accessibility Snapshot
        var a11yFailed = false
        val enabledA11y = mutableSetOf<String>()
        val declaredA11y = mutableSetOf<String>()

        try {
            accessibilityManager?.let { am ->
                val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK) ?: emptyList()
                for (svc in enabledServices) {
                    svc.resolveInfo?.serviceInfo?.packageName?.let { enabledA11y.add(it) }
                }

                val installedServices = am.installedAccessibilityServiceList ?: emptyList()
                for (svc in installedServices) {
                    svc.resolveInfo?.serviceInfo?.packageName?.let { declaredA11y.add(it) }
                }
            }

            // Fallback to Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES string
            val rawSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
            for (part in rawSetting.split(":")) {
                val pkg = part.substringBefore("/")
                if (pkg.isNotBlank()) enabledA11y.add(pkg)
            }
        } catch (e: Exception) {
            a11yFailed = true
        }

        // 2. Device Admin Snapshot
        var adminFailed = false
        val activeAdmins = mutableSetOf<String>()
        try {
            devicePolicyManager?.activeAdmins?.forEach { adminComponent ->
                activeAdmins.add(adminComponent.packageName)
            }
        } catch (e: Exception) {
            adminFailed = true
        }

        // 3. Notification Listener Snapshot
        var listenerFailed = false
        val enabledListeners = mutableSetOf<String>()
        try {
            val listeners = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledListeners.addAll(listeners)
        } catch (e: Exception) {
            listenerFailed = true
        }

        return SpecialAccessSnapshot(
            enabledA11yPackages = enabledA11y,
            declaredA11yPackages = declaredA11y,
            a11yQueryFailed = a11yFailed,
            activeAdminPackages = activeAdmins,
            adminQueryFailed = adminFailed,
            enabledListenerPackages = enabledListeners,
            listenerQueryFailed = listenerFailed,
            appOpsManager = appOpsManager,
            devicePolicyManager = devicePolicyManager
        )
    }
}
