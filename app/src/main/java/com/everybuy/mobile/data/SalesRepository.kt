package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class SaleCustomerDto(val id: String? = null, val name: String? = null)

@Serializable data class SalesListRowDto(
    val id: String,
    val reference: String = "",
    val createdAt: String = "",
    val customer: SaleCustomerDto? = null,
    val itemsTotal: String = "0",
    val amountPaid: String = "0",
    val balance: String = "0",
    val status: String = "UNPAID",
)

@Serializable data class SalesPageDto(
    val data: List<SalesListRowDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 1,
)

@Serializable data class SalesSummaryDto(
    val salesTotal: String = "0",
    val salesCount: Int = 0,
    val amountPaid: String = "0",
    val outstanding: String = "0",
)

@Serializable data class SaleDto(
    val id: String,
    val saleNumber: String = "",
    val customerId: String? = null,
    val itemsTotal: String = "0",
    val requestId: String = "",
    val createdBy: String? = null,
    val soldAt: String = "",
)

@Serializable data class SaleItemDto(
    val id: String,
    val saleId: String = "",
    val productId: String = "",
    val productName: String = "",
    val unit: String = "Pack",
    val quantity: Int = 0,
    val costPerPack: String? = null,
    val packsPerUnit: Int = 0,
    val unitPrice: String = "0",
    val lineTotal: String = "0",
)

@Serializable data class PaymentDto(
    val id: Int,
    val saleId: String = "",
    val amount: String = "0",
    val method: String = "cash",
    val reference: String? = null,
    val recordedBy: String? = null,
    val requestId: String = "",
    val paidAt: String = "",
)

@Serializable data class SaleDetailDto(
    val id: String,
    val saleNumber: String = "",
    val customerId: String? = null,
    val itemsTotal: String = "0",
    val requestId: String = "",
    val createdBy: String? = null,
    val soldAt: String = "",
    val items: List<SaleItemDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
)

@Serializable data class CreateSaleResultDto(
    val sale: SaleDto,
    val items: List<SaleItemDto> = emptyList(),
    val payment: List<PaymentDto> = emptyList(),
    val updatedProduct: List<UpdatedProductStockDto> = emptyList(),
    val stockMovement: List<StockMovementDto> = emptyList(),
    val replayed: Boolean = false,
)

data class SaleLineInput(val productId: String, val unit: String, val quantity: Int, val unitPrice: String? = null)

data class CreateSaleInput(
    val requestId: String,
    val customerId: String?,
    val amountPaid: String,
    val method: String,
    val reference: String?,
    val items: List<SaleLineInput>,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("requestId", JsonPrimitive(requestId))
        customerId?.let { put("customerId", JsonPrimitive(it)) }
        put("amountPaid", JsonPrimitive(amountPaid))
        put("method", JsonPrimitive(method))
        reference?.let { put("reference", JsonPrimitive(it)) }
        put("items", kotlinx.serialization.json.buildJsonArray {
            items.forEach { item ->
                add(buildJsonObject {
                    put("productId", JsonPrimitive(item.productId))
                    put("unit", JsonPrimitive(item.unit))
                    put("quantity", JsonPrimitive(item.quantity))
                    item.unitPrice?.let { put("unitPrice", JsonPrimitive(it)) }
                })
            }
        })
    }
}

interface SalesApi {
    @retrofit2.http.GET("sales") suspend fun sales(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 20,
        @retrofit2.http.Query("from") from: String? = null,
        @retrofit2.http.Query("to") to: String? = null,
        @retrofit2.http.Query("q") q: String? = null,
        @retrofit2.http.Query("paymentStatus") paymentStatus: String? = null,
    ): SalesPageDto

    @retrofit2.http.GET("sales/summary") suspend fun summary(
        @retrofit2.http.Query("from") from: String? = null,
        @retrofit2.http.Query("to") to: String? = null,
    ): SalesSummaryDto

    @retrofit2.http.GET("sales/{id}") suspend fun sale(@retrofit2.http.Path("id") id: String): SaleDetailDto

    @retrofit2.http.POST("sales") suspend fun create(@retrofit2.http.Body body: JsonObject): CreateSaleResultDto
}

@Singleton class SalesRepository @Inject constructor(private val api: SalesApi) {
    suspend fun list(page: Int, limit: Int = 20, from: String? = null, to: String? = null, query: String? = null, paymentStatus: String? = null): SalesPageDto =
        api.sales(page, limit, from, to, query, paymentStatus)

    suspend fun summary(from: String? = null, to: String? = null): SalesSummaryDto = api.summary(from, to)

    suspend fun detail(id: String): SaleDetailDto = api.sale(id)

    suspend fun create(input: CreateSaleInput): CreateSaleResultDto {
        val body = input.toJson()
        return withIdempotentReplay { api.create(body) }
    }
}
