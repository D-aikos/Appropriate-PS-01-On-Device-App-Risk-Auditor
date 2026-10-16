package com.appropriate.riskauditor.core.platform

import com.appropriate.riskauditor.core.model.GrantState
import com.appropriate.riskauditor.core.model.ProtectionLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionExtractorTest {

    @Test
    fun testGrantedBitTrue() {
        val state = PermissionExtractor.mapGrant(
            flagBit = true,
            protection = ProtectionLevel.DANGEROUS
        )
        assertEquals(GrantState.GRANTED, state)
    }

    @Test
    fun testSpecialProtectionNotRuntime() {
        val state = PermissionExtractor.mapGrant(
            flagBit = false,
            protection = ProtectionLevel.SPECIAL
        )
        assertEquals(GrantState.NOT_RUNTIME, state)
    }

    @Test
    fun testDangerousProtectionDeniedWhenNotGranted() {
        val state = PermissionExtractor.mapGrant(
            flagBit = false,
            protection = ProtectionLevel.DANGEROUS
        )
        assertEquals(GrantState.DENIED, state)
    }

    @Test
    fun testNormalProtectionDeniedWhenNotGranted() {
        val state = PermissionExtractor.mapGrant(
            flagBit = false,
            protection = ProtectionLevel.NORMAL
        )
        assertEquals(GrantState.DENIED, state)
    }

    @Test
    fun testFlagsUnavailableIsUnknown() {
        val state = PermissionExtractor.mapGrant(
            flagBit = null,
            protection = ProtectionLevel.DANGEROUS
        )
        assertEquals(GrantState.UNKNOWN, state)
    }

    @Test
    fun testNewerApiIsNotSupported() {
        val state = PermissionExtractor.mapGrant(
            flagBit = false,
            protection = ProtectionLevel.DANGEROUS,
            isNewerApi = true
        )
        assertEquals(GrantState.NOT_SUPPORTED, state)
    }
}
