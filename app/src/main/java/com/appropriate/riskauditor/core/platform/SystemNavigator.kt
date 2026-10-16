package com.appropriate.riskauditor.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Provides safe shortcuts to Android system settings and uninstallation flows.
 *
 * Security Principle: The auditor never performs silent mutations, changes
 * permissions behind the user's back, or uninstalls apps without user mediation.
 * All operations route through the standard Android user confirmation dialogs.
 */
object SystemNavigator {

    /**
     * Directs the user safely to the application's detailed system settings page
     * where permissions, notifications, and battery optimizations can be managed.
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
     * The Android OS displays the standard confirmation prompt to the user.
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
     * Opens the Special App Access settings page.
     */
    fun openSpecialAccessSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
}
