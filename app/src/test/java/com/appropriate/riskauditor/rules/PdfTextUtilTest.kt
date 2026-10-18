package com.appropriate.riskauditor.rules

import com.appropriate.riskauditor.core.report.PdfTextUtil
import org.junit.Assert.*
import org.junit.Test

class PdfTextUtilTest {

    @Test
    fun testSanitizeStripsControlCharactersExceptSpace() {
        val input = "Hello\u0000\u0007\u001F World \t\n!"
        val result = PdfTextUtil.sanitize(input)
        assertEquals("Hello World !", result)
    }

    @Test
    fun testSanitizeStripsBidiOverrideCharacters() {
        // U+202E is RIGHT-TO-LEFT OVERRIDE (RLO)
        // U+202A is LEFT-TO-RIGHT EMBEDDING (LRE)
        // U+2066 is LEFT-TO-RIGHT ISOLATE (LRI)
        val maliciousLabel = "\u202Eevil_app\u202C\u2066legit\u2069"
        val result = PdfTextUtil.sanitize(maliciousLabel)
        assertEquals("evil_applegit", result)
        assertFalse(result.contains("\u202E"))
        assertFalse(result.contains("\u2066"))
        assertFalse(result.contains("\u2069"))
    }

    @Test
    fun testCapLabelTruncatesAtSixtyChars() {
        val shortLabel = "My Super App"
        assertEquals(shortLabel, PdfTextUtil.capLabel(shortLabel, 60))

        val exact60 = "A".repeat(60)
        assertEquals(exact60, PdfTextUtil.capLabel(exact60, 60))

        val longLabel = "B".repeat(80)
        val capped = PdfTextUtil.capLabel(longLabel, 60)
        assertEquals(60, capped.length)
        assertTrue(capped.endsWith("..."))
        assertEquals("B".repeat(57) + "...", capped)
    }
}
