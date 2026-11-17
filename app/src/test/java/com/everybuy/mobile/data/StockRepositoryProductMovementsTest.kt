package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class StockRepositoryProductMovementsTest {
    @Test fun `walks pages and keeps only the requested product`() = runTest {
        val api = FakeStockApi(
            mapOf(
                1 to StockMovementsPageDto(
                    data = listOf(movement("m1", "p1", "2026-11-17T09:00:00.000Z"), movement("m2", "p2", "2026-11-17T10:00:00.000Z")),
                    page = 1,
                    limit = 2,
                    total = 3,
                ),
                2 to StockMovementsPageDto(
                    data = listOf(movement("m3", "p1", "2026-11-18T09:00:00.000Z")),
                    page = 2,
                    limit = 2,
                    total = 3,
                ),
            ),
        )
        val repository = StockRepository(api, FakeSuppliersApi(), ProductsRepository(FakeProductsApi(), FakeCatalogApi()))

        val result = repository.movementsForProduct("p1")

        assertEquals(listOf("m3", "m1"), result.map { it.id })
        assertEquals(listOf(1, 2), api.requestedPages)
    }

    @Test fun `stops on a short page`() = runTest {
        val api = FakeStockApi(
            mapOf(1 to StockMovementsPageDto(data = listOf(movement("m1", "p1", "2026-11-17T09:00:00.000Z")), page = 1, limit = 50, total = 90)),
        )
        val repository = StockRepository(api, FakeSuppliersApi(), ProductsRepository(FakeProductsApi(), FakeCatalogApi()))

        assertEquals(1, repository.movementsForProduct("p1").size)
        assertEquals(listOf(1), api.requestedPages)
    }

    private fun movement(id: String, productId: String, createdAt: String) = StockMovementDto(
        id = id,
        productId = productId,
        type = "Receipt",
        changePacks = 5,
        createdBy = "u1",
        createdAt = createdAt,
    )

    private class FakeStockApi(private val pages: Map<Int, StockMovementsPageDto>) : StockApi {
        val requestedPages = mutableListOf<Int>()
        override suspend fun movements(page: Int, limit: Int): StockMovementsPageDto {
            requestedPages += page
            return pages[page] ?: StockMovementsPageDto(emptyList(), page, limit, 0)
        }
        override suspend fun movement(id: String) = StockMovementDto(id = id, productId = "p1", type = "Receipt", changePacks = 1, createdBy = "u1", createdAt = "")
        override suspend fun addStock(body: JsonObject) = StockMutationResultDto()
        override suspend fun adjustStock(body: JsonObject) = StockMutationResultDto()
    }

    private class FakeSuppliersApi : SuppliersApi {
        override suspend fun suppliers() = emptyList<SupplierDto>()
    }

    private class FakeProductsApi : ProductsApi {
        override suspend fun products(page: Int, limit: Int) = emptyList<ProductDto>()
        override suspend fun product(id: String) = emptyList<ProductDto>()
        override suspend fun create(body: JsonObject) = emptyList<ProductDto>()
        override suspend fun update(id: String, body: JsonObject) = emptyList<ProductDto>()
    }

    private class FakeCatalogApi : CatalogApi {
        override suspend fun categories() = emptyList<CategoryDto>()
        override suspend fun brands() = emptyList<BrandDto>()
    }
}
