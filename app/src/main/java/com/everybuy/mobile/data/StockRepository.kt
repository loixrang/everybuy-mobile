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

@Singleton class StockRepository @Inject constructor(private val api: StockApi, private val suppliersApi: SuppliersApi, private val productsRepository: ProductsRepository) {
    suspend fun movements(page: Int): StockMovementsPageDto = api.movements(page = page, limit = 50)
    suspend fun suppliers(): List<SupplierDto> = suppliersApi.suppliers().filter { it.active }
    suspend fun products(): List<ProductDto> = productsRepository.load().products
    suspend fun receive(input: StockAdditionMutation): StockMutationResultDto = api.addStock(input.toJson())
}
