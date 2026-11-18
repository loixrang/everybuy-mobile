package com.everybuy.mobile.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchTest {
    private val productName = "Softcare Premium Small Size 2"

    @Test
    fun matchesWordsInAnyOrderAndIgnoresRepeatedSpaces() {
        assertTrue(matchesSearch(productName, "softcare small"))
        assertTrue(matchesSearch(productName, "small softcare"))
        assertTrue(matchesSearch(productName, "SOFTCARE   small"))
    }

    @Test
    fun matchesPartialWordsAndExactNumericWords() {
        assertTrue(matchesSearch(productName, "size 2"))
        assertTrue(matchesSearch(productName, "prem 2"))
        assertFalse(matchesSearch(productName, "softcare small 3"))
    }

    @Test
    fun matchesProductWordsInAnyOrderWithPartialTokens() {
        assertTrue(matchesSearch("Kisskids Small Size 2", "kiss small siz 2"))
        assertFalse(matchesSearch("Kisskids Small Size 3", "kiss small siz 2"))
    }

    @Test
    fun stripsAccentsAndTreatsBlankQueryAsMatchAll() {
        assertTrue(matchesSearch("Café Size 2", "cafe"))
        assertTrue(matchesSearch(productName, "  "))
    }

    @Test
    fun requiresEveryQueryWord() {
        assertFalse(matchesSearch(productName, "softcare large"))
    }
}
