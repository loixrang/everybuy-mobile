package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class StockTakeDto(
    val id: String,
    val name: String = "",
    val status: String = "Draft",
    val reference: String = "",
    val createdBy: String = "",
    val finalizedBy: String? = null,
    val createdAt: String? = null,
    val finalizedAt: String? = null,
)

@Serializable data class StockTakeItemDto(
    val id: String,
    val productId: String,
    val productName: String = "",
    val packName: String = "",
    val size: String? = null,
    val packsPerBag: Int = 0,
    val costPerPack: String? = null,
    val expectedPacks: Int = 0,
    val stockAtCountPacks: Int? = null,
    val countedPacks: Int? = null,
    val countedBy: String? = null,
    val countedAt: String? = null,
)

@Serializable data class StockTakeDetailDto(
    val id: String,
    val name: String = "",
    val status: String = "Draft",
    val reference: String = "",
    val createdBy: String = "",
    val finalizedBy: String? = null,
    val createdAt: String? = null,
    val finalizedAt: String? = null,
    val items: List<StockTakeItemDto> = emptyList(),
    val totalProducts: Int = 0,
    val countedProducts: Int = 0,
)

@Serializable data class StockTakePageDto(
    val data: List<StockTakeDto> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
)

@Serializable data class StockTakeCountResultDto(
    val id: String,
    val stockTakeId: String = "",
    val productId: String = "",
    val expectedPacks: Int = 0,
    val stockAtCountPacks: Int? = null,
    val countedPacks: Int? = null,
    val countedBy: String? = null,
    val countedAt: String? = null,
)

interface StockTakesApi {
    @retrofit2.http.GET("stock-takes") suspend fun list(
        @retrofit2.http.Query("page") page: Int = 1,
        @retrofit2.http.Query("limit") limit: Int = 20,
    ): StockTakePageDto

    @retrofit2.http.GET("stock-takes/{id}") suspend fun detail(
        @retrofit2.http.Path("id") id: String,
    ): StockTakeDetailDto

    @retrofit2.http.POST("stock-takes") suspend fun create(
        @retrofit2.http.Body body: JsonObject,
    ): StockTakeDto

    @retrofit2.http.PATCH("stock-takes/{id}/items/{itemId}/count") suspend fun recordCount(
        @retrofit2.http.Path("id") id: String,
        @retrofit2.http.Path("itemId") itemId: String,
        @retrofit2.http.Body body: JsonObject,
    ): StockTakeCountResultDto

    @retrofit2.http.POST("stock-takes/{id}/finalize") suspend fun finalize(
        @retrofit2.http.Path("id") id: String,
    ): StockTakeDto

    @retrofit2.http.POST("stock-takes/{id}/cancel") suspend fun cancel(
        @retrofit2.http.Path("id") id: String,
    ): StockTakeDto
}

@Singleton class StockTakesRepository @Inject constructor(private val api: StockTakesApi) {
    suspend fun list(page: Int, limit: Int = 20): StockTakePageDto = api.list(page, limit)

    suspend fun detail(id: String): StockTakeDetailDto = api.detail(id)

    suspend fun create(name: String): StockTakeDto = api.create(buildJsonObject {
        put("name", JsonPrimitive(name.trim()))
    })

    suspend fun recordCount(id: String, itemId: String, countedPacks: Int): StockTakeCountResultDto =
        api.recordCount(id, itemId, buildJsonObject { put("countedPacks", JsonPrimitive(countedPacks)) })

    suspend fun finalize(id: String): StockTakeDto = api.finalize(id)

    suspend fun cancel(id: String): StockTakeDto = api.cancel(id)
}
