package com.everybuy.mobile.ui.stocktakes

import com.everybuy.mobile.data.StockTakeItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StockTakeValidationTest {
    @Test fun `name is required and capped at the backend limit`() {
        assertNotNull(validateStockTakeName("   "))
        assertNotNull(validateStockTakeName("x".repeat(MAX_STOCK_TAKE_NAME_LENGTH + 1)))
        assertNull(validateStockTakeName("October stock count"))
    }

    @Test fun `bags and loose packs convert to a single pack count`() {
        val result = validateStockTakeCount(bags = "12", loosePacks = "3", packsPerBag = 24)

        assertEquals(291, result.countedPacks)
        assertNull(result.message)
    }

    @Test fun `count rejects malformed input and invalid bag conversions`() {
        assertNotNull(validateStockTakeCount("", "0", 24).message)
        assertNotNull(validateStockTakeCount("1.5", "0", 24).message)
        assertNotNull(validateStockTakeCount("0", "0", 0).message)
    }

    @Test fun `count caps totals at the PostgreSQL integer maximum`() {
        val result = validateStockTakeCount(MAX_COUNTED_PACKS.toString(), "1", 1)

        assertNull(result.countedPacks)
        assertNotNull(result.message)
    }

    @Test fun `difference and progress match the approved count state`() {
        val item = StockTakeItemDto(id = "i1", productId = "p1", expectedPacks = 50, countedPacks = 45)

        assertEquals(-5, differenceFromExpected(item))
        assertEquals("-5 packs", formatPackDifference(-5))
        assertEquals("+1 pack", formatPackDifference(1))
        assertEquals("0", formatPackDifference(0))
        assertEquals(50, stockTakeProgress(5, 10))
        assertEquals(0, stockTakeProgress(0, 0))
    }

    @Test fun `status labels distinguish approved and in-progress stocktakes`() {
        assertEquals("In progress", stockTakeStatusLabel("Draft"))
        assertEquals("Approved", stockTakeStatusLabel("Finalized"))
        assertEquals("Cancelled", stockTakeStatusLabel("Cancelled"))
        assertEquals("You", countedByLabel("user-1", "user-1"))
        assertEquals("Staff #12345678", countedByLabel("1234567890", "someone-else"))
    }
}
