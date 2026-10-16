package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.model.RiskCategory
import com.appropriate.riskauditor.core.rules.CategoryMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryMapperTest {

    @Test
    fun testSpecialAccessMapping() {
        assertEquals(
            RiskCategory.SPECIAL_ACCESS,
            CategoryMapper.mapPermission("android.permission.SYSTEM_ALERT_WINDOW")
        )
        assertEquals(
            RiskCategory.SPECIAL_ACCESS,
            CategoryMapper.mapPermission("android.permission.REQUEST_INSTALL_PACKAGES")
        )
        assertEquals(
            RiskCategory.SPECIAL_ACCESS,
            CategoryMapper.mapPermission("android.permission.BIND_ACCESSIBILITY_SERVICE")
        )
        assertEquals(
            RiskCategory.SPECIAL_ACCESS,
            CategoryMapper.mapPermission("android.permission.BIND_DEVICE_ADMIN")
        )
        assertEquals(
            RiskCategory.SPECIAL_ACCESS,
            CategoryMapper.mapPermission("android.permission.PACKAGE_USAGE_STATS")
        )
    }

    @Test
    fun testSmsMapping() {
        assertEquals(RiskCategory.SMS, CategoryMapper.mapPermission("android.permission.SEND_SMS"))
        assertEquals(RiskCategory.SMS, CategoryMapper.mapPermission("android.permission.READ_SMS"))
        assertEquals(RiskCategory.SMS, CategoryMapper.mapPermission("android.permission.RECEIVE_SMS"))
        assertEquals(RiskCategory.SMS, CategoryMapper.mapPermission("android.permission.RECEIVE_MMS"))
    }

    @Test
    fun testContactsAndCallsMapping() {
        assertEquals(
            RiskCategory.CONTACTS_AND_CALLS,
            CategoryMapper.mapPermission("android.permission.READ_CONTACTS")
        )
        assertEquals(
            RiskCategory.CONTACTS_AND_CALLS,
            CategoryMapper.mapPermission("android.permission.READ_CALL_LOG")
        )
        assertEquals(
            RiskCategory.CONTACTS_AND_CALLS,
            CategoryMapper.mapPermission("android.permission.GET_ACCOUNTS")
        )
    }

    @Test
    fun testCameraAndMicMapping() {
        assertEquals(
            RiskCategory.CAMERA_AND_MIC,
            CategoryMapper.mapPermission("android.permission.CAMERA")
        )
        assertEquals(
            RiskCategory.CAMERA_AND_MIC,
            CategoryMapper.mapPermission("android.permission.RECORD_AUDIO")
        )
    }

    @Test
    fun testLocationMapping() {
        assertEquals(
            RiskCategory.LOCATION,
            CategoryMapper.mapPermission("android.permission.ACCESS_FINE_LOCATION")
        )
        assertEquals(
            RiskCategory.LOCATION,
            CategoryMapper.mapPermission("android.permission.ACCESS_COARSE_LOCATION")
        )
        assertEquals(
            RiskCategory.LOCATION,
            CategoryMapper.mapPermission("android.permission.ACCESS_BACKGROUND_LOCATION")
        )
    }

    @Test
    fun testPhoneAndIdentityMapping() {
        assertEquals(
            RiskCategory.PHONE_AND_IDENTITY,
            CategoryMapper.mapPermission("android.permission.READ_PHONE_STATE")
        )
        assertEquals(
            RiskCategory.PHONE_AND_IDENTITY,
            CategoryMapper.mapPermission("android.permission.CALL_PHONE")
        )
    }

    @Test
    fun testStorageMapping() {
        assertEquals(
            RiskCategory.STORAGE_AND_MEDIA,
            CategoryMapper.mapPermission("android.permission.READ_EXTERNAL_STORAGE")
        )
        assertEquals(
            RiskCategory.STORAGE_AND_MEDIA,
            CategoryMapper.mapPermission("android.permission.MANAGE_EXTERNAL_STORAGE")
        )
        assertEquals(
            RiskCategory.STORAGE_AND_MEDIA,
            CategoryMapper.mapPermission("android.permission.READ_MEDIA_IMAGES")
        )
    }

    @Test
    fun testBackgroundAndPersistenceMapping() {
        assertEquals(
            RiskCategory.BACKGROUND_AND_PERSISTENCE,
            CategoryMapper.mapPermission("android.permission.RECEIVE_BOOT_COMPLETED")
        )
        assertEquals(
            RiskCategory.BACKGROUND_AND_PERSISTENCE,
            CategoryMapper.mapPermission("android.permission.FOREGROUND_SERVICE")
        )
    }

    @Test
    fun testNetworkMapping() {
        assertEquals(
            RiskCategory.NETWORK,
            CategoryMapper.mapPermission("android.permission.INTERNET")
        )
        assertEquals(
            RiskCategory.NETWORK,
            CategoryMapper.mapPermission("android.permission.ACCESS_NETWORK_STATE")
        )
    }
}
