package com.everybuy.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StockAdjustmentMutationTest {
    @Test fun `adjustment includes backend type unit direction and required reason`() {
        val body = StockAdjustmentMutation("request-1", "product-1", 8, "Sub", "Damaged packs").toJson()

        assertEquals("request-1", body["requestId"]?.toString()?.trim('"'))
        assertEquals("product-1", body["productId"]?.toString()?.trim('"'))
        assertEquals("Adjustment", body["type"]?.toString()?.trim('"'))
        assertEquals("Pack", body["unit"]?.toString()?.trim('"'))
        assertEquals("8", body["quantity"]?.toString())
        assertEquals("Sub", body["changePack"]?.toString()?.trim('"'))
        assertEquals("Damaged packs", body["note"]?.toString()?.trim('"'))
    }
}
