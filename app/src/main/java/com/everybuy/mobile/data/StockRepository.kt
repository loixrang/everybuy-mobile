package com.everybuy.mobile.data

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import javax.inject.Inject
import javax.inject.Singleton

data class StockAdditionMutation(
    val requestId: String,
    val productId: String,
    val quantity: Int,
    val unitCost: String,
    val supplierId: Int?,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("requestId", JsonPrimitive(requestId))
        put("productId", JsonPrimitive(productId))
        put("type", JsonPrimitive("Receipt"))
        put("unit", JsonPrimitive("Pack"))
        put("quantity", JsonPrimitive(quantity))
        put("unitCost", JsonPrimitive(unitCost))
        supplierId?.let { put("supplierId", JsonPrimitive(it)) }
    }
}

data class StockAdjustmentMutation(
    val requestId: String,
    val productId: String,
    val quantity: Int,
    val changePack: String,
    val note: String,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("requestId", JsonPrimitive(requestId))
        put("productId", JsonPrimitive(productId))
        put("type", JsonPrimitive("Adjustment"))
        put("unit", JsonPrimitive("Pack"))
        put("quantity", JsonPrimitive(quantity))
        put("changePack", JsonPrimitive(changePack))
        put("note", JsonPrimitive(note))
    }
}

@Singleton class StockRepository @Inject constructor(private val api: StockApi, private val suppliersApi: SuppliersApi, private val productsRepository: ProductsRepository) {
    suspend fun movements(page: Int): StockMovementsPageDto = api.movements(page = page, limit = 50)

    /**
     * Walks the movement pages the same way the web client does, then keeps the
     * product's ledger. The backend only paginates, so a product filter has to
     * be applied after loading.
     */
    suspend fun movementsForProduct(productId: String, maxPages: Int = 20): List<StockMovementDto> {
        val collected = mutableListOf<StockMovementDto>()
        var page = 1
        while (page <= maxPages) {
            val response = api.movements(page = page, limit = 50)
            collected += response.data
            val pageSize = response.limit.takeIf { it > 0 } ?: 50
            if (response.data.isEmpty() || response.data.size < pageSize || collected.size >= response.total) break
            page++
        }
        return collected.filter { it.productId == productId }.sortedByDescending { it.createdAt }
    }
    suspend fun movement(id: String): StockMovementDto = api.movement(id)
    suspend fun suppliers(): List<SupplierDto> = suppliersApi.suppliers().filter { it.active }
    suspend fun products(): List<ProductDto> = productsRepository.load().products
    suspend fun receive(input: StockAdditionMutation): StockMutationResultDto {
        val body = input.toJson()
        return withIdempotentReplay { api.addStock(body) }
    }
    suspend fun adjust(input: StockAdjustmentMutation): StockMutationResultDto {
        val body = input.toJson()
        return withIdempotentReplay { api.adjustStock(body) }
    }
}
