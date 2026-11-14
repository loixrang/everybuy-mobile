package com.everybuy.mobile.ui.inventory

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductDraftTest {
    @Test fun `valid product setup passes validation`() {
        val errors = validateProductDraft(
            ProductDraft(name = "Rice", brandId = "brand", categoryId = "category", packsPerBag = "10", retailPrice = "900", minRetailPrice = "800", bagPrice = "8000", minBagPrice = "7000"),
        )
        assertTrue(errors.isEmpty())
    }

    @Test fun `half bag requires paired prices and even packs per bag`() {
        val errors = validateProductDraft(
            ProductDraft(name = "Rice", brandId = "brand", categoryId = "category", packsPerBag = "9", retailPrice = "900", minRetailPrice = "800", bagPrice = "8000", minBagPrice = "7000", halfBagPrice = "450"),
        )
        assertTrue(errors.containsKey("minHalfBagPrice"))
        assertEquals("Half Bag requires an even number of packs per bag.", errors["packsPerBag"])
    }

    @Test fun `default prices cannot be below their minimums`() {
        val errors = validateProductDraft(
            ProductDraft(name = "Rice", brandId = "brand", categoryId = "category", packsPerBag = "10", retailPrice = "900", minRetailPrice = "950", bagPrice = "8000", minBagPrice = "7000"),
        )
        assertTrue(errors.containsKey("minRetailPrice"))
    }
}
