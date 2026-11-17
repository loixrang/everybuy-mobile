package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StockTakeRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `stocktake page and detail decode the backend envelopes`() {
        val page = json.decodeFromString<StockTakePageDto>(
            """{"data":[{"id":"st-1","name":"October count","status":"Draft","reference":"ST-20261030-ABC123","createdBy":"u1","finalizedBy":null,"createdAt":"2026-10-30T09:00:00.000Z","finalizedAt":null}],"page":1,"limit":20,"total":1}""",
        )
        assertEquals(1, page.total)
        assertEquals("ST-20261030-ABC123", page.data.single().reference)

        val detail = json.decodeFromString<StockTakeDetailDto>(
            """{"id":"st-1","name":"October count","status":"Draft","reference":"ST-20261030-ABC123","createdBy":"u1","finalizedBy":null,"createdAt":null,"finalizedAt":null,"totalProducts":1,"countedProducts":1,"items":[{"id":"i1","productId":"p1","productName":"Rice","packName":"Small Pack","size":null,"packsPerBag":24,"costPerPack":"100.0000","expectedPacks":48,"stockAtCountPacks":48,"countedPacks":47,"countedBy":"u1","countedAt":"2026-10-30T09:05:00.000Z"}]}""",
        )
        assertEquals(1, detail.countedProducts)
        assertEquals(47, detail.items.single().countedPacks)
        assertNull(detail.items.single().size)
    }

    @Test fun `create trims the name and sends the verified request shape`() = runTest {
        val api = FakeStockTakesApi()
        val repository = StockTakesRepository(api)

        repository.create("  October count  ")

        assertEquals("October count", api.createBody?.get("name")?.toString()?.trim('"'))
    }

    @Test fun `record count sends countedPacks as a JSON number`() = runTest {
        val api = FakeStockTakesApi()
        val repository = StockTakesRepository(api)

        repository.recordCount("st-1", "i1", 291)

        assertEquals("st-1", api.countStockTakeId)
        assertEquals("i1", api.countItemId)
        assertEquals("291", api.countBody?.get("countedPacks")?.toString())
    }

    @Test fun `finalize and cancel call their owner endpoints`() = runTest {
        val api = FakeStockTakesApi()
        val repository = StockTakesRepository(api)

        repository.finalize("st-1")
        repository.cancel("st-2")

        assertEquals("st-1", api.finalizedId)
        assertEquals("st-2", api.cancelledId)
    }

    private class FakeStockTakesApi : StockTakesApi {
        var createBody: JsonObject? = null
        var countBody: JsonObject? = null
        var countStockTakeId: String? = null
        var countItemId: String? = null
        var finalizedId: String? = null
        var cancelledId: String? = null

        override suspend fun list(page: Int, limit: Int) = StockTakePageDto()
        override suspend fun detail(id: String) = StockTakeDetailDto(id = id)
        override suspend fun create(body: JsonObject): StockTakeDto {
            createBody = body
            return StockTakeDto(id = "st-1")
        }
        override suspend fun recordCount(id: String, itemId: String, body: JsonObject): StockTakeCountResultDto {
            countStockTakeId = id
            countItemId = itemId
            countBody = body
            return StockTakeCountResultDto(id = itemId)
        }
        override suspend fun finalize(id: String): StockTakeDto {
            finalizedId = id
            return StockTakeDto(id = id, status = "Finalized")
        }
        override suspend fun cancel(id: String): StockTakeDto {
            cancelledId = id
            return StockTakeDto(id = id, status = "Cancelled")
        }
    }
}
