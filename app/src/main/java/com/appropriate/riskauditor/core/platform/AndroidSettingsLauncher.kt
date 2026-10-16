package com.appropriate.riskauditor.core.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Platform implementation of settings shortcuts and user-mediated uninstallation.
 * Ensures all operations navigate through Android's system confirmation dialogs.
 */
class AndroidSettingsLauncher(private val context: Context) {

    private fun safeStart(intent: Intent, fallbackPackage: String? = null): Boolean {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            if (fallbackPackage != null) {
                openAppDetails(fallbackPackage)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun openAppDetails(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        return safeStart(intent)
    }

    fun promptUninstall(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
        }
        return safeStart(intent)
    }

    fun openAccessibilitySettings(fallbackPackage: String? = null): Boolean {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        return safeStart(intent, fallbackPackage)
    }

    fun openNotificationListenerSettings(fallbackPackage: String? = null): Boolean {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        return safeStart(intent, fallbackPackage)
    }

    fun openOverlaySettings(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:$packageName")
        }
        return safeStart(intent, fallbackPackage = packageName)
    }

    fun openInstallSourcesSettings(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:$packageName")
        }
        return safeStart(intent, fallbackPackage = packageName)
    }

    fun openUsageAccessSettings(fallbackPackage: String? = null): Boolean {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        return safeStart(intent, fallbackPackage)
    }

    fun openDeviceAdminSettings(fallbackPackage: String? = null): Boolean {
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        return safeStart(intent, fallbackPackage)
    }
}
