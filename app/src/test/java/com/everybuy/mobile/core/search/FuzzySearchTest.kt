package com.everybuy.mobile.core.search

import org.junit.Assert.assertEquals
import org.junit.Test

class FuzzySearchTest {
    private data class Product(val name: String, val size: String)

    @Test
    fun `matches reordered partial words and ranks strongest result first`() {
        val products = listOf(
            Product("Kisskids", "Small Size 2"),
            Product("Kisskids", "Small Size 2"),
            Product("Kisskids", "Smally Size 2"),
        )

        assertEquals(
            listOf(products[0], products[1], products[2]),
            fuzzySearch(products, "kiss small siz 2") { "${it.name} ${it.size}" },
        )
    }

    @Test
    fun `numbers match exactly`() {
        val products = listOf(Product("Kisskids", "Small Size 3"), Product("Kisskids", "Small Size 24"))

        assertEquals(emptyList<Product>(), fuzzySearch(products, "kiss small siz 2") { "${it.name} ${it.size}" })
    }

    @Test
    fun `matches a typo in a long word`() {
        assertEquals(
            listOf("Molfix baby Wipes"),
            fuzzySearch(listOf("Molfix baby Wipes"), "molfx wipes") { it },
        )
    }

    @Test
    fun `blank query returns every item in original order`() {
        val items = listOf("Third", "First", "Second")

        assertEquals(items, fuzzySearch(items, "  ") { it })
    }

    @Test
    fun `nonsense query returns no items`() {
        assertEquals(emptyList<String>(), fuzzySearch(listOf("Molfix baby Wipes"), "qzxv asdf") { it })
    }

    @Test
    fun `normalizes accents punctuation and letter digit boundaries`() {
        assertEquals(
            listOf("Café-size2"),
            fuzzySearch(listOf("Café-size2", "Cafe-size3"), "cafe size2") { it },
        )
    }
}
