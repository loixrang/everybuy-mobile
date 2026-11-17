package com.everybuy.mobile.ui.sales

import com.everybuy.mobile.data.ProductDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SaleValidationTest {
    private val rice = ProductDto(
        id = "p1",
        name = "Rice",
        packName = "Small Pack",
        packsPerBag = 24,
        bagPrice = "24000.00",
        minBagPrice = "22000.00",
        halfBagPrice = "12500.00",
        minHalfBagPrice = "11500.00",
        retailPrice = "1000.00",
        minRetailPrice = "900.00",
        stockPacks = 60,
    )

    @Test fun `unit prices default to the configured selling prices`() {
        assertEquals("1000.00", saleUnitPrice(rice, "Pack"))
        assertEquals("24000.00", saleUnitPrice(rice, "Bag"))
        assertEquals("12500.00", saleUnitPrice(rice, "Half Bag"))
        assertEquals("900.00", saleUnitMinimumPrice(rice, "Pack"))
        assertEquals("22000.00", saleUnitMinimumPrice(rice, "Bag"))
        assertEquals("11500.00", saleUnitMinimumPrice(rice, "Half Bag"))
    }

    @Test fun `units convert to pack quantities using the saved pack size`() {
        assertEquals(1, salePacksPerUnit(rice, "Pack"))
        assertEquals(24, salePacksPerUnit(rice, "Bag"))
        assertEquals(12, salePacksPerUnit(rice, "Half Bag"))
        assertEquals(60, maxSaleQuantity(rice, "Pack"))
        assertEquals(2, maxSaleQuantity(rice, "Bag"))
        assertEquals(5, maxSaleQuantity(rice, "Half Bag"))
    }

    @Test fun `half bags stay unavailable for an odd pack count`() {
        val odd = rice.copy(packsPerBag = 25)

        assertFalse(isSaleUnitAvailable(odd, "Half Bag"))
        assertEquals(listOf("Pack", "Bag"), availableSaleUnits(odd))
    }

    @Test fun `lines and totals multiply the agreed unit price`() {
        val lines = buildSaleLines(
            listOf(
                SaleLineDraft(productId = "p1", unit = "Bag", quantity = 2, unitPrice = "23000.00"),
                SaleLineDraft(productId = "p1", unit = "Pack", quantity = 3, unitPrice = "950.00"),
            ),
            mapOf("p1" to rice),
        )

        assertEquals(48L, lines[0].packsRequired)
        assertEquals(BigDecimal("46000.00"), lines[0].lineTotal)
        assertEquals(BigDecimal("48850.00"), saleTotal(lines))
    }

    @Test fun `submit validation mirrors the backend sale rules`() {
        val lines = buildSaleLines(
            listOf(SaleLineDraft(productId = "p1", unit = "Pack", quantity = 2, unitPrice = "1000.00")),
            mapOf("p1" to rice),
        )

        assertEquals("Add at least one item to record a sale.", validateSaleForSubmit(emptyList(), "0", null))
        assertEquals("Select a customer for a sale with an unpaid balance.", validateSaleForSubmit(lines, "500", null))
        assertNotNull(validateSaleForSubmit(lines, "5000", "c1"))
        assertNotNull(validateSaleForSubmit(lines, "1.234", "c1"))
        assertNull(validateSaleForSubmit(lines, "2000", null))
        assertNull(validateSaleForSubmit(lines, "500", "c1"))
    }

    @Test fun `submit validation rejects prices below the minimum`() {
        val lines = buildSaleLines(
            listOf(SaleLineDraft(productId = "p1", unit = "Bag", quantity = 1, unitPrice = "21000.00")),
            mapOf("p1" to rice),
        )

        val message = validateSaleForSubmit(lines, "21000", null)
        assertTrue(message!!.contains("minimum"))
    }

    @Test fun `submit validation rejects quantities beyond available stock`() {
        val lines = buildSaleLines(
            listOf(SaleLineDraft(productId = "p1", unit = "Bag", quantity = 3, unitPrice = "24000.00")),
            mapOf("p1" to rice),
        )

        val message = validateSaleForSubmit(lines, "72000", null)
        assertTrue(message!!.contains("Not enough stock"))
    }

    @Test fun `payment labels normalise the backend statuses`() {
        assertEquals("Paid", paymentStatusLabel(normalisedPaymentStatus("PAID")))
        assertEquals("Part paid", paymentStatusLabel(normalisedPaymentStatus("PART_PAID")))
        assertEquals("Part paid", paymentStatusLabel(normalisedPaymentStatus("PARTIAL")))
        assertEquals("Unpaid", paymentStatusLabel(normalisedPaymentStatus("UNPAID")))
        assertEquals("Bank transfer", paymentMethodLabel("transfer"))
        assertEquals("POS", paymentMethodLabel("pos"))
        assertEquals("Cash", paymentMethodLabel("cash"))
    }
}
