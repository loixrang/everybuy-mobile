package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SalesRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `sales page and detail decode the backend envelopes`() {
        val page = json.decodeFromString<SalesPageDto>(
            """{"data":[{"id":"s1","reference":"SL-20261117-ABC123","createdAt":"2026-11-17T10:00:00.000Z","customer":{"id":"c1","name":"Ada"},"itemsTotal":"2400.00","amountPaid":"1000.00","balance":"1400.00","status":"PARTIAL"}],"page":1,"limit":20,"total":1,"totalPages":1}""",
        )
        assertEquals(1, page.total)
        assertEquals("Ada", page.data.single().customer?.name)
        assertEquals("PARTIAL", page.data.single().status)

        val sale = json.decodeFromString<SaleDetailDto>(
            """{"id":"s1","saleNumber":"SL-20261117-ABC123","customerId":"c1","itemsTotal":"2400.00","requestId":"r1","createdBy":"u1","soldAt":"2026-11-17T10:00:00.000Z","items":[{"id":"i1","saleId":"s1","productId":"p1","productName":"Rice","unit":"Bag","quantity":1,"costPerPack":"800.0000","packsPerUnit":24,"unitPrice":"2400.00","lineTotal":"2400.00"}],"payments":[{"id":1,"saleId":"s1","amount":"1000.00","method":"transfer","paidAt":"2026-11-17T10:05:00.000Z"}]}""",
        )
        assertEquals("Bag", sale.items.single().unit)
        assertEquals("transfer", sale.payments.single().method)
        assertNull(sale.payments.single().reference)
    }

    @Test fun `create sends the verified request shape`() = runTest {
        val api = FakeSalesApi()
        val repository = SalesRepository(api)

        repository.create(
            CreateSaleInput(
                requestId = "00000000-0000-0000-0000-000000000001",
                customerId = "c1",
                amountPaid = "1000.00",
                method = "transfer",
                reference = "TRX-1",
                items = listOf(
                    SaleLineInput(productId = "p1", unit = "Bag", quantity = 2, unitPrice = "24000.00"),
                    SaleLineInput(productId = "p2", unit = "Pack", quantity = 3),
                ),
            ),
        )

        val body = api.createBody!!
        assertEquals("00000000-0000-0000-0000-000000000001", body["requestId"]?.jsonPrimitive?.content)
        assertEquals("c1", body["customerId"]?.jsonPrimitive?.content)
        assertEquals("1000.00", body["amountPaid"]?.jsonPrimitive?.content)
        assertEquals("transfer", body["method"]?.jsonPrimitive?.content)
        assertEquals("TRX-1", body["reference"]?.jsonPrimitive?.content)

        val items = body["items"]!!.jsonArray
        assertEquals(2, items.size)
        assertEquals("p1", items[0].jsonObject["productId"]?.jsonPrimitive?.content)
        assertEquals("2", items[0].jsonObject["quantity"]?.jsonPrimitive?.content)
        assertEquals("24000.00", items[0].jsonObject["unitPrice"]?.jsonPrimitive?.content)
        assertEquals("p2", items[1].jsonObject["productId"]?.jsonPrimitive?.content)
        assertNull(items[1].jsonObject["unitPrice"])
    }

    @Test fun `create omits payment fields when nothing is paid`() = runTest {
        val api = FakeSalesApi()
        val repository = SalesRepository(api)

        repository.create(
            CreateSaleInput(
                requestId = "00000000-0000-0000-0000-000000000002",
                customerId = null,
                amountPaid = "0",
                method = "cash",
                reference = null,
                items = listOf(SaleLineInput(productId = "p1", unit = "Pack", quantity = 1)),
            ),
        )

        val body = api.createBody!!
        assertNull(body["customerId"])
        assertNull(body["reference"])
        assertEquals("0", body["amountPaid"]?.jsonPrimitive?.content)
    }

    @Test fun `list forwards filters to the query parameters`() = runTest {
        val api = FakeSalesApi()
        val repository = SalesRepository(api)

        repository.list(page = 2, from = "2026-11-01", to = "2026-11-17", query = "Ada", paymentStatus = "UNPAID")

        assertEquals(2, api.lastPage)
        assertEquals("2026-11-01", api.lastFrom)
        assertEquals("2026-11-17", api.lastTo)
        assertEquals("Ada", api.lastQuery)
        assertEquals("UNPAID", api.lastStatus)
    }

    private class FakeSalesApi : SalesApi {
        var createBody: JsonObject? = null
        var lastPage: Int? = null
        var lastFrom: String? = null
        var lastTo: String? = null
        var lastQuery: String? = null
        var lastStatus: String? = null

        override suspend fun sales(page: Int, limit: Int, from: String?, to: String?, q: String?, paymentStatus: String?): SalesPageDto {
            lastPage = page
            lastFrom = from
            lastTo = to
            lastQuery = q
            lastStatus = paymentStatus
            return SalesPageDto()
        }

        override suspend fun summary(from: String?, to: String?) = SalesSummaryDto()

        override suspend fun sale(id: String) = SaleDetailDto(id = id)

        override suspend fun create(body: JsonObject): CreateSaleResultDto {
            createBody = body
            return CreateSaleResultDto(sale = SaleDto(id = "s1"))
        }
    }
}
