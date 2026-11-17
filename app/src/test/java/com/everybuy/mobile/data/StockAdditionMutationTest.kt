package com.everybuy.mobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockAdditionMutationTest {
    @Test fun `receipt uses pack unit and leaves optional supplier omitted`() {
        val body = StockAdditionMutation("request-1", "product-1", 24, "123.45", null).toJson()

        assertEquals("request-1", body["requestId"]?.toString()?.trim('"'))
        assertEquals("product-1", body["productId"]?.toString()?.trim('"'))
        assertEquals("Receipt", body["type"]?.toString()?.trim('"'))
        assertEquals("Pack", body["unit"]?.toString()?.trim('"'))
        assertEquals("24", body["quantity"]?.toString())
        assertEquals("123.45", body["unitCost"]?.toString()?.trim('"'))
        assertNull(body["supplierId"])
    }

    @Test fun `receipt includes supplier when selected`() {
        val body = StockAdditionMutation("request-2", "product-1", 1, "10", 42).toJson()

        assertEquals("42", body["supplierId"]?.toString())
    }
}
