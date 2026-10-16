package com.appropriate.riskauditor.core.platform

import android.content.pm.PackageManager
import android.os.Build
import com.appropriate.riskauditor.core.model.InstallSource

/**
 * Resolves the installer source for an application into Play Store, Other App Store,
 * Sideloaded, or Unknown.
 *
 * Security Note:
 * Sideloaded apps lack store-side verification and represent a higher risk profile.
 * Note on False Positives: MDM/enterprise-distributed packages or apps restored from OEM
 * backups may lack store installer package tags and appear as SIDELOADED.
 */
object InstallSourceResolver {

    const val PLAY_STORE_PACKAGE = "com.android.vending"

    val KNOWN_OTHER_STORES = setOf(
        "com.sec.android.app.samsungapps",  // Samsung Galaxy Store
        "com.amazon.venezia",               // Amazon Appstore
        "com.huawei.appmarket",             // Huawei AppGallery
        "com.xiaomi.mipicks",               // Xiaomi GetApps
        "com.xiaomi.market",                // Xiaomi Market
        "com.heytap.market",                // Oppo / Realme App Market
        "com.oppo.market",                  // Oppo Market
        "org.fdroid.fdroid"                 // F-Droid Open Source Repository
    )

    val KNOWN_SYSTEM_INSTALLERS = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.android.shell"
    )

    /**
     * Queries PackageManager for installing package and classifies the install source.
     */
    fun resolve(pm: PackageManager, packageName: String, isSystem: Boolean): Pair<InstallSource, String?> {
        val installerPackage = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            return Pair(InstallSource.UNKNOWN, null)
        }

        val classification = classify(installerPackage, isSystem)
        return Pair(classification, installerPackage)
    }

    /**
     * Pure classification function without Android framework dependencies for testability.
     */
    fun classify(installer: String?, isSystem: Boolean): InstallSource {
        if (installer == PLAY_STORE_PACKAGE) {
            return InstallSource.PLAY_STORE
        }

        if (installer in KNOWN_OTHER_STORES) {
            return InstallSource.OTHER_STORE
        }

        if (isSystem && installer == null) {
            // Preinstalled system application without installer package
            return InstallSource.OTHER_STORE
        }

        // Sideloaded: package installer, shell, browser, file manager, or untracked origin
        return InstallSource.SIDELOADED
    }
}
