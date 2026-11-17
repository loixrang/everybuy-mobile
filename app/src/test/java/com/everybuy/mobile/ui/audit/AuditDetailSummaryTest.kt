package com.everybuy.mobile.ui.audit

import com.everybuy.mobile.data.AuditEntryDto
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class AuditDetailSummaryTest {
    @Test fun `summarises title and formatted amount`() {
        val entry = AuditEntryDto(
            detail = buildJsonObject {
                put("title", "Diesel for generator")
                put("amount", "12500.50")
            },
        )
        assertEquals("Diesel for generator · ₦12,500.50", auditDetailSummary(entry))
    }

    @Test fun `falls back to the available field`() {
        assertEquals("Transport", auditDetailSummary(AuditEntryDto(detail = buildJsonObject { put("title", "Transport") })))
        assertEquals("₦1,000.00", auditDetailSummary(AuditEntryDto(detail = buildJsonObject { put("amount", "1000") })))
    }

    @Test fun `handles missing and unrecognised detail`() {
        assertEquals("—", auditDetailSummary(AuditEntryDto()))
        assertEquals("foo, bar", auditDetailSummary(AuditEntryDto(detail = buildJsonObject { put("foo", "1"); put("bar", "2") })))
    }
}
