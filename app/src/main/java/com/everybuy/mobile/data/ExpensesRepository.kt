package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class ExpenseDto(
    val id: String,
    val requestId: String = "",
    val title: String = "",
    val category: String = "",
    val amount: String = "0",
    val notes: String? = null,
    val spentAt: String = "",
    val recordedBy: String = "",
    val createdAt: String = "",
)

data class CreateExpenseInput(
    val requestId: String,
    val title: String,
    val category: String,
    val amount: String,
    val notes: String? = null,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("requestId", JsonPrimitive(requestId))
        put("title", JsonPrimitive(title))
        put("category", JsonPrimitive(category))
        put("amount", JsonPrimitive(amount))
        notes?.let { put("notes", JsonPrimitive(it)) }
    }
}

interface ExpensesApi {
    @retrofit2.http.GET("api/v1/expenses") suspend fun expenses(): List<ExpenseDto>
    @retrofit2.http.POST("api/v1/expenses") suspend fun create(@retrofit2.http.Body body: JsonObject): ExpenseDto
}

@Singleton class ExpensesRepository @Inject constructor(private val api: ExpensesApi) {
    suspend fun list(): List<ExpenseDto> = api.expenses()
    suspend fun create(input: CreateExpenseInput): ExpenseDto = api.create(input.toJson())
}
