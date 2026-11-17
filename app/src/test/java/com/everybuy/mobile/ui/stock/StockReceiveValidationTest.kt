package com.everybuy.mobile.ui.stock

import com.everybuy.mobile.data.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockReceiveValidationTest {
    private val product = ProductDto(id = "p1", name = "Rice", packsPerBag = 12)

    @Test fun `bags and loose packs are converted into pack quantity`() {
        val (quantity, errors) = validateStockReceive(product, "2", "3", "125.50")

        assertTrue(errors.isEmpty())
        assertEquals(27, quantity?.totalPacks)
    }

    @Test fun `rejects zero quantity and malformed cost`() {
        val (quantity, errors) = validateStockReceive(product, "0", "0", "0")

        assertNull(quantity)
        assertNotNull(errors["quantity"])
        assertNotNull(errors["unitCost"])
    }

    @Test fun `caps receipt quantity at one million packs`() {
        val (_, errors) = validateStockReceive(product, "83334", "0", "1")

        assertTrue(errors["quantity"]?.contains("1,000,000") == true)
    }
}
