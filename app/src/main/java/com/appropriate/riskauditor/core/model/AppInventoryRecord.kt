package com.appropriate.riskauditor.core.model

/**
 * Normalized static inventory metadata for an audited application.
 */
data class AppInventoryRecord(
    val packageName: String,
    val appLabel: String,
    val versionName: String?,
    val versionCode: Long?,
    val targetSdk: Int?,
    val isSystemApp: Boolean,
    val installerPackage: String?,
    val firstInstallTimeEpochMillis: Long?,
    val lastUpdateTimeEpochMillis: Long?
) {
    val isSideloaded: Boolean
        get() {
            if (isSystemApp) return false
            if (installerPackage == null) return true
            val knownStores = listOf(
                "com.android.vending", // Google Play Store
                "com.amazon.venezia",   // Amazon Appstore
                "com.sec.android.app.samsungapps", // Samsung Galaxy Store
                "com.huawei.appmarket" // Huawei AppGallery
            )
            return installerPackage !in knownStores
        }
}
