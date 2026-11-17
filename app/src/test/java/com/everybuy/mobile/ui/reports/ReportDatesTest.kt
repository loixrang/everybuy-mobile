package com.everybuy.mobile.ui.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportDatesTest {
    @Test fun `validates report months`() {
        assertTrue(isReportMonth("2026-11"))
        assertTrue(isReportMonth(" 2026-01 "))
        assertFalse(isReportMonth("2026-13"))
        assertFalse(isReportMonth("2026-00"))
        assertFalse(isReportMonth("0000-05"))
        assertFalse(isReportMonth("Nov 2026"))
    }

    @Test fun `shifts across year boundaries`() {
        assertEquals("2025-12", shiftMonth("2026-01", -1))
        assertEquals("2027-01", shiftMonth("2026-12", 1))
        assertEquals("2026-11", shiftMonth("2026-11", 0))
    }

    @Test fun `shift keeps unparseable input`() {
        assertEquals("nope", shiftMonth("nope", 1))
    }

    @Test fun `formats month labels`() {
        assertEquals("November 2026", formatMonthLabel("2026-11"))
        assertEquals("January 2027", formatMonthLabel("2027-01"))
        assertEquals("bad", formatMonthLabel("bad"))
    }
}
