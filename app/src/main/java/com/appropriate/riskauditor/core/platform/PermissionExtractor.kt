package com.appropriate.riskauditor.core.platform

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.PermissionAudit
import com.appropriate.riskauditor.core.model.ProtectionLevel
import com.appropriate.riskauditor.core.rules.CategoryMapper
import java.util.concurrent.ConcurrentHashMap

/**
 * Extracts manifest-declared permissions, cached protection levels, and runtime grant states.
 * Respects declared != granted rules and caches PermissionInfo device-wide during a scan.
 */
class PermissionExtractor(private val pm: PackageManager) {

    // Cache protection level queries per permission name across the entire scan session
    private val protectionCache = ConcurrentHashMap<String, ProtectionLevel>()

    fun extract(info: PackageInfo): List<PermissionAudit> {
        val requested = info.requestedPermissions ?: return emptyList()
        val flags = info.requestedPermissionsFlags

        val audits = mutableListOf<PermissionAudit>()
        val seenNames = mutableSetOf<String>()

        for (i in requested.indices) {
            val name = requested[i]
            if (!seenNames.add(name)) continue // Deduplicate repeated declarations

            val flagBit: Boolean? = if (flags != null && i < flags.size) {
                (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                null
            }

            val protection = resolveProtection(name)
            val isNewerApi = isNewerThanDeviceApi(name)
            val grantState = mapGrant(flagBit, protection, isNewerApi)
            val category = CategoryMapper.mapPermission(name)

            audits.add(
                PermissionAudit(
                    name = name,
                    simpleName = name.substringAfterLast('.'),
                    isDeclared = true,
                    grantState = grantState,
                    category = category,
                    protectionLevel = protection.displayName(),
                    protection = protection
                )
            )
        }

        return audits
    }

    private fun resolveProtection(name: String): ProtectionLevel {
        return protectionCache.getOrPut(name) {
            val shortName = name.substringAfterLast('.')
            if (shortName in SPECIAL_PERMISSIONS || name in SPECIAL_PERMISSIONS) {
                return@getOrPut ProtectionLevel.SPECIAL
            }

            try {
                val permInfo = pm.getPermissionInfo(name, 0)
                val base = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    permInfo.protection
                } else {
                    @Suppress("DEPRECATION")
                    permInfo.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE
                }

                when (base) {
                    PermissionInfo.PROTECTION_DANGEROUS -> ProtectionLevel.DANGEROUS
                    PermissionInfo.PROTECTION_SIGNATURE -> ProtectionLevel.SIGNATURE
                    PermissionInfo.PROTECTION_NORMAL -> ProtectionLevel.NORMAL
                    else -> ProtectionLevel.NORMAL
                }
            } catch (e: PackageManager.NameNotFoundException) {
                ProtectionLevel.UNKNOWN
            } catch (e: Exception) {
                ProtectionLevel.UNKNOWN
            }
        }
    }

    private fun isNewerThanDeviceApi(name: String): Boolean {
        val shortName = name.substringAfterLast('.')
        val minSdk = NEWER_API_PERMISSIONS[shortName] ?: NEWER_API_PERMISSIONS[name] ?: return false
        return Build.VERSION.SDK_INT < minSdk
    }

    companion object {
        val SPECIAL_PERMISSIONS = setOf(
            "SYSTEM_ALERT_WINDOW",
            "REQUEST_INSTALL_PACKAGES",
            "PACKAGE_USAGE_STATS",
            "MANAGE_EXTERNAL_STORAGE",
            "WRITE_SETTINGS",
            "BIND_ACCESSIBILITY_SERVICE",
            "BIND_DEVICE_ADMIN",
            "BIND_NOTIFICATION_LISTENER_SERVICE",
            "SCHEDULE_EXACT_ALARM",
            "USE_FULL_SCREEN_INTENT",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.PACKAGE_USAGE_STATS",
            "android.permission.MANAGE_EXTERNAL_STORAGE",
            "android.permission.WRITE_SETTINGS",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.BIND_DEVICE_ADMIN",
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "android.permission.SCHEDULE_EXACT_ALARM",
            "android.permission.USE_FULL_SCREEN_INTENT"
        )

        val NEWER_API_PERMISSIONS = mapOf(
            "POST_NOTIFICATIONS" to 33,
            "READ_MEDIA_IMAGES" to 33,
            "READ_MEDIA_VIDEO" to 33,
            "READ_MEDIA_AUDIO" to 33,
            "NEARBY_WIFI_DEVICES" to 33,
            "READ_MEDIA_VISUAL_USER_SELECTED" to 34
        )

        /**
         * Pure grant mapping function per Implementation Plan Section 6 Task D2-2.
         */
        fun mapGrant(
            flagBit: Boolean?,
            protection: ProtectionLevel,
            isNewerApi: Boolean = false
        ): GrantState {
            if (isNewerApi) {
                return GrantState.NOT_SUPPORTED
            }

            if (flagBit == true) {
                return GrantState.GRANTED
            }

            if (protection == ProtectionLevel.SPECIAL) {
                return GrantState.NOT_RUNTIME
            }

            if (flagBit == false) {
                return GrantState.DENIED
            }

            return GrantState.UNKNOWN
        }
    }
}
