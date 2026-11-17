package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpensesRepositoryTest {
    @Test fun `create sends the request id and trimmed optional notes`() = runTest {
        val api = FakeExpensesApi()
        val repository = ExpensesRepository(api)

        repository.create(
            CreateExpenseInput(
                requestId = "00000000-0000-0000-0000-000000000003",
                title = "Diesel",
                category = "Utilities",
                amount = "12500.50",
                notes = "Generator",
            ),
        )

        val body = api.createBody!!
        assertEquals("00000000-0000-0000-0000-000000000003", body["requestId"]?.jsonPrimitive?.content)
        assertEquals("Diesel", body["title"]?.jsonPrimitive?.content)
        assertEquals("Utilities", body["category"]?.jsonPrimitive?.content)
        assertEquals("12500.50", body["amount"]?.jsonPrimitive?.content)
        assertEquals("Generator", body["notes"]?.jsonPrimitive?.content)
    }

    @Test fun `create omits notes when not provided`() = runTest {
        val api = FakeExpensesApi()
        ExpensesRepository(api).create(
            CreateExpenseInput(requestId = "r1", title = "Diesel", category = "Utilities", amount = "100"),
        )
        assertNull(api.createBody!!["notes"])
    }

    @Test fun `list decodes the expense envelope`() = runTest {
        val repository = ExpensesRepository(FakeExpensesApi())
        val expenses = repository.list()
        assertEquals("Diesel", expenses.single().title)
        assertEquals("12500.50", expenses.single().amount)
    }

    private class FakeExpensesApi : ExpensesApi {
        var createBody: JsonObject? = null

        override suspend fun expenses() = listOf(
            ExpenseDto(id = "e1", title = "Diesel", category = "Utilities", amount = "12500.50", spentAt = "2026-11-17T09:00:00.000Z"),
        )

        override suspend fun create(body: JsonObject): ExpenseDto {
            createBody = body
            return ExpenseDto(id = "e2", title = "Diesel")
        }
    }
}
