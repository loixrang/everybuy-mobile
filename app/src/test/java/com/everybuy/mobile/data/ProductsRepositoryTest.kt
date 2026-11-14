package com.everybuy.mobile.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductsRepositoryTest {
    @Test fun `inventory shows active products and excludes archived rows`() = runBlocking {
        val api = object : ProductsApi {
            override suspend fun products(page: Int, limit: Int) = listOf(
                ProductDto(id = "active", name = "Rice", active = true),
                ProductDto(id = "archived", name = "Old rice", active = false),
            )
            override suspend fun product(id: String) = emptyList<ProductDto>()
            override suspend fun create(body: kotlinx.serialization.json.JsonObject) = emptyList<ProductDto>()
            override suspend fun update(id: String, body: kotlinx.serialization.json.JsonObject) = emptyList<ProductDto>()
        }
        val catalog = object : CatalogApi {
            override suspend fun categories() = listOf(CategoryDto("c1", "Food", active = true))
            override suspend fun brands() = listOf(BrandDto("b1", "Legacy", active = false))
        }

        val inventory = ProductsRepository(api, catalog).load()

        assertEquals(listOf("active"), inventory.products.map { it.id })
        assertEquals(listOf("Food"), inventory.categories.map { it.name })
        assertEquals(emptyList<String>(), inventory.brands.map { it.name })
    }
}
