package com.everybuy.mobile.core.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {
    @Test
    fun formatNairaOmitsZeroDecimalsAndKeepsNonZeroDecimals() {
        val whole = formatNaira("21000")
        val fractional = formatNaira("21000.50")
        val cents = formatNaira("21000.25")

        assertTrue(whole.contains("21,000"))
        assertFalse(whole.contains("."))
        assertTrue(fractional.endsWith(".5"))
        assertTrue(cents.endsWith(".25"))
    }
}
