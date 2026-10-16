package com.appropriate.riskauditor.core.platform

import com.appropriate.riskauditor.core.model.InstallSource
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallSourceResolverTest {

    @Test
    fun testPlayStore() {
        assertEquals(
            InstallSource.PLAY_STORE,
            InstallSourceResolver.classify("com.android.vending", isSystem = false)
        )
    }

    @Test
    fun testKnownOtherStores() {
        val stores = listOf(
            "com.sec.android.app.samsungapps",
            "com.amazon.venezia",
            "com.huawei.appmarket",
            "com.xiaomi.mipicks",
            "com.xiaomi.market",
            "com.heytap.market",
            "com.oppo.market",
            "org.fdroid.fdroid"
        )
        for (store in stores) {
            assertEquals(
                InstallSource.OTHER_STORE,
                InstallSourceResolver.classify(store, isSystem = false)
            )
        }
    }

    @Test
    fun testPreinstalledSystemAppWithNullInstaller() {
        assertEquals(
            InstallSource.OTHER_STORE,
            InstallSourceResolver.classify(null, isSystem = true)
        )
    }

    @Test
    fun testUserAppWithNullInstallerIsSideloaded() {
        assertEquals(
            InstallSource.SIDELOADED,
            InstallSourceResolver.classify(null, isSystem = false)
        )
    }

    @Test
    fun testPackageInstallerAndBrowsersAreSideloaded() {
        val sideloaders = listOf(
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.android.shell",
            "com.android.chrome",
            "com.google.android.apps.nbu.files"
        )
        for (installer in sideloaders) {
            assertEquals(
                InstallSource.SIDELOADED,
                InstallSourceResolver.classify(installer, isSystem = false)
            )
        }
    }
}
