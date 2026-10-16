package com.appropriate.riskauditor.core.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Provides safe shortcuts to Android system settings and uninstallation flows.
 *
 * Security Principle: The auditor never performs silent mutations, changes
 * permissions behind the user's back, or uninstalls apps without user mediation.
 * All operations route through standard Android user confirmation dialogs.
 */
object SystemNavigator {

    /**
     * Directs the user safely to the application's detailed system settings page.
     */
    fun openAppDetails(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Dispatches a safe system uninstallation request.
     * Android OS displays the standard confirmation prompt to the user.
     */
    fun promptUninstall(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens system Accessibility Settings screen.
     */
    fun openAccessibilitySettings(context: Context, fallbackPackageName: String? = null): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            if (fallbackPackageName != null) openAppDetails(context, fallbackPackageName) else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens system Notification Listener Access screen.
     */
    fun openNotificationListenerSettings(context: Context, fallbackPackageName: String? = null): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            if (fallbackPackageName != null) openAppDetails(context, fallbackPackageName) else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens system Overlay permission screen ("Display over other apps").
     */
    fun openOverlaySettings(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            openAppDetails(context, packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens system Unknown App Sources install settings screen.
     */
    fun openInstallSourcesSettings(context: Context, packageName: String): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            openAppDetails(context, packageName)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens Usage Access settings screen.
     */
    fun openUsageAccessSettings(context: Context, fallbackPackageName: String? = null): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            if (fallbackPackageName != null) openAppDetails(context, fallbackPackageName) else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Opens Device Admin security settings screen.
     */
    fun openDeviceAdminSettings(context: Context, fallbackPackageName: String? = null): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            if (fallbackPackageName != null) openAppDetails(context, fallbackPackageName) else false
        } catch (e: Exception) {
            false
        }
    }
}
