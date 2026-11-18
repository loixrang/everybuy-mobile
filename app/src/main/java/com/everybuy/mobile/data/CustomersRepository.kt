package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class CustomerDto(
    val id: String,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable data class CustomerListItemDto(
    val id: String,
    val name: String = "",
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val active: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
    val balance: String = "0",
    val lastSaleAt: String? = null,
    val totalPurchases: String = "0",
)

@Serializable data class StatementSaleDto(
    val id: String,
    val soldAt: String = "",
    val totalAmount: String = "0",
    val paidAmount: String = "0",
    val balanceDue: String = "0",
    val paymentStatus: String = "UNPAID",
    val items: List<SaleItemDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
)

@Serializable data class CustomerStatementTotalsDto(
    val salesTotal: String = "0",
    val moneyReceived: String = "0",
    val outstandingDebt: String = "0",
)

@Serializable data class CustomerStatementDto(
    val customer: CustomerDto,
    val totals: CustomerStatementTotalsDto = CustomerStatementTotalsDto(),
    val sales: List<StatementSaleDto> = emptyList(),
)

@Serializable data class RecordRepaymentResultDto(
    val payment: PaymentDto,
    val saleId: String = "",
    val totalAmount: String = "0",
    val paidAmount: String = "0",
    val balanceDue: String = "0",
)

data class CreateCustomerInput(val name: String, val phone: String? = null, val address: String? = null, val notes: String? = null) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        phone?.let { put("phone", JsonPrimitive(it)) }
        address?.let { put("address", JsonPrimitive(it)) }
        notes?.let { put("notes", JsonPrimitive(it)) }
    }
}

data class UpdateCustomerInput(
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val active: Boolean? = null,
) {
    fun toJson(): JsonObject = buildJsonObject {
        name?.let { put("name", JsonPrimitive(it)) }
        phone?.let { put("phone", JsonPrimitive(it)) }
        address?.let { put("address", JsonPrimitive(it)) }
        notes?.let { put("notes", JsonPrimitive(it)) }
        active?.let { put("active", JsonPrimitive(it)) }
    }
}

data class RepaymentInput(val requestId: String, val saleId: String, val amount: String) {
    fun toJson(): JsonObject = buildJsonObject {
        put("requestId", JsonPrimitive(requestId))
        put("saleId", JsonPrimitive(saleId))
        put("amount", JsonPrimitive(amount))
    }
}

interface CustomersApi {
    @retrofit2.http.GET("customers") suspend fun customers(): List<CustomerListItemDto>
    @retrofit2.http.GET("customers/{id}") suspend fun customer(@retrofit2.http.Path("id") id: String): List<CustomerDto>
    @retrofit2.http.POST("customers") suspend fun create(@retrofit2.http.Body body: JsonObject): List<CustomerDto>
    @retrofit2.http.PATCH("customers/update/{id}") suspend fun update(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): List<CustomerDto>
    @retrofit2.http.GET("customers/{id}/statement") suspend fun statement(@retrofit2.http.Path("id") id: String): CustomerStatementDto
    @retrofit2.http.POST("customers/{id}/repayments") suspend fun repay(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): RecordRepaymentResultDto
}

@Singleton class CustomersRepository @Inject constructor(private val api: CustomersApi) {
    suspend fun list(): List<CustomerListItemDto> = api.customers()
    suspend fun active(): List<CustomerListItemDto> = api.customers().filter { it.active }
    suspend fun detail(id: String): CustomerDto? = api.customer(id).firstOrNull()
    suspend fun create(input: CreateCustomerInput): CustomerDto =
        api.create(input.toJson()).firstOrNull() ?: throw IllegalStateException("The server did not return the created customer.")
    suspend fun update(id: String, input: UpdateCustomerInput): CustomerDto =
        api.update(id, input.toJson()).firstOrNull() ?: throw IllegalStateException("The server did not return the updated customer.")
    suspend fun statement(id: String): CustomerStatementDto = api.statement(id)
    suspend fun repay(customerId: String, input: RepaymentInput): RecordRepaymentResultDto = api.repay(customerId, input.toJson())
}
