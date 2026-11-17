package com.everybuy.mobile.ui.stock

import com.everybuy.mobile.data.ProductDto
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StockAdjustmentValidationTest {
    private val product = ProductDto(id = "p1", name = "Rice", stockPacks = 10, costPerPack = "100")

    @Test fun `accepts a valid owner adjustment`() {
        assertNull(validateStockAdjustment(product, "5", "Sub", "Damaged packs"))
    }

    @Test fun `requires reason and prevents stock from going negative`() {
        assertNotNull(validateStockAdjustment(product, "1", "Add", "  "))
        assertNotNull(validateStockAdjustment(product, "11", "Sub", "Damaged packs"))
    }

    @Test fun `increase requires an acquisition cost`() {
        val noCost = product.copy(costPerPack = null)
        assertNotNull(validateStockAdjustment(noCost, "2", "Add", "Opening correction"))
        assertNull(validateStockAdjustment(noCost, "2", "Sub", "Damaged packs"))
    }
}
