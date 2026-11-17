package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomersRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun `list and statement decode the backend envelopes`() {
        val customers = json.decodeFromString<List<CustomerListItemDto>>(
            """[{"id":"c1","name":"Ada","phone":"0803","address":null,"notes":null,"active":true,"createdAt":"2026-11-01T09:00:00.000Z","updatedAt":"2026-11-01T09:00:00.000Z","balance":"1500.00","lastSaleAt":"2026-11-17T10:00:00.000Z","totalPurchases":"2000.00"}]""",
        )
        assertEquals("1500.00", customers.single().balance)
        assertEquals("Ada", customers.single().name)

        val statement = json.decodeFromString<CustomerStatementDto>(
            """{"customer":{"id":"c1","name":"Ada","phone":null,"address":null,"notes":null,"active":true,"createdAt":"2026-11-01T09:00:00.000Z","updatedAt":"2026-11-01T09:00:00.000Z"},"totals":{"salesTotal":"2000.00","moneyReceived":"500.00","outstandingDebt":"1500.00"},"sales":[{"id":"s1","soldAt":"2026-11-17T10:00:00.000Z","totalAmount":"2000.00","paidAmount":"500.00","balanceDue":"1500.00","paymentStatus":"PART_PAID","items":[],"payments":[{"id":1,"saleId":"s1","amount":"500.00","method":"cash","reference":null,"recordedBy":"u1","requestId":"r1","paidAt":"2026-11-17T10:05:00.000Z"}]}]}""",
        )
        assertEquals("PART_PAID", statement.sales.single().paymentStatus)
        assertEquals("1500.00", statement.totals.outstandingDebt)
        assertEquals("cash", statement.sales.single().payments.single().method)
    }

    @Test fun `create sends only the provided customer fields`() = runTest {
        val api = FakeCustomersApi()
        val repository = CustomersRepository(api)

        repository.create(CreateCustomerInput(name = "Ada", phone = "0803", address = null, notes = null))

        val body = api.createBody!!
        assertEquals("Ada", body["name"]?.jsonPrimitive?.content)
        assertEquals("0803", body["phone"]?.jsonPrimitive?.content)
        assertNull(body["address"])
        assertNull(body["notes"])
    }

    @Test fun `update sends the patch fields including status`() = runTest {
        val api = FakeCustomersApi()
        val repository = CustomersRepository(api)

        repository.update("c1", UpdateCustomerInput(name = "Ada Obi", phone = null, active = false))

        assertEquals("c1", api.updatedId)
        val body = api.updateBody!!
        assertEquals("Ada Obi", body["name"]?.jsonPrimitive?.content)
        assertEquals("false", body["active"]?.jsonPrimitive?.content)
        assertNull(body["phone"])
    }

    @Test fun `repayment sends the verified request shape`() = runTest {
        val api = FakeCustomersApi()
        val repository = CustomersRepository(api)

        repository.repay(
            "c1",
            RepaymentInput(
                requestId = "00000000-0000-0000-0000-000000000002",
                saleId = "s1",
                amount = "250.50",
            ),
        )

        assertEquals("c1", api.repaidCustomerId)
        val body = api.repayBody!!
        assertEquals("00000000-0000-0000-0000-000000000002", body["requestId"]?.jsonPrimitive?.content)
        assertEquals("s1", body["saleId"]?.jsonPrimitive?.content)
        assertEquals("250.50", body["amount"]?.jsonPrimitive?.content)
    }

    private class FakeCustomersApi : CustomersApi {
        var createBody: JsonObject? = null
        var updateBody: JsonObject? = null
        var repayBody: JsonObject? = null
        var updatedId: String? = null
        var repaidCustomerId: String? = null

        override suspend fun customers() = emptyList<CustomerListItemDto>()
        override suspend fun customer(id: String) = listOf(CustomerDto(id = id))
        override suspend fun create(body: JsonObject): List<CustomerDto> {
            createBody = body
            return listOf(CustomerDto(id = "c1"))
        }
        override suspend fun update(id: String, body: JsonObject): List<CustomerDto> {
            updatedId = id
            updateBody = body
            return listOf(CustomerDto(id = id))
        }
        override suspend fun statement(id: String) = CustomerStatementDto(customer = CustomerDto(id = id))
        override suspend fun repay(id: String, body: JsonObject): RecordRepaymentResultDto {
            repaidCustomerId = id
            repayBody = body
            return RecordRepaymentResultDto(payment = PaymentDto(id = 1))
        }
    }
}
