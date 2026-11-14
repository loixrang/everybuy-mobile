package com.everybuy.mobile.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException

@Singleton class ProductsRepository @Inject constructor(private val api: ProductsApi, private val catalogApi: CatalogApi) {
    suspend fun categories(): List<CategoryDto> = catalogApi.categories().filter { it.active }
    suspend fun brands(): List<BrandDto> = catalogApi.brands().filter { it.active }

    suspend fun load(): ProductInventory = coroutineScope {
        val products = async { api.products().filter { it.active } }
        val categories = async { optionalList { catalogApi.categories() } }
        val brands = async { optionalList { catalogApi.brands() } }
        ProductInventory(products.await(), categories.await().filter { it.active }, brands.await().filter { it.active })
    }

    suspend fun getById(id: String): ProductDetails? = coroutineScope {
        val product = api.product(id).firstOrNull() ?: return@coroutineScope null
        val categories = async { optionalList { catalogApi.categories() } }.await()
        val brands = async { optionalList { catalogApi.brands() } }.await()
        ProductDetails(product, categories.firstOrNull { it.id == product.categoryId }?.name, brands.firstOrNull { it.id == product.brandId }?.name)
    }

    suspend fun save(id: String?, input: ProductMutation): ProductDto {
        val result = if (id == null) api.create(input.toJson()) else api.update(id, input.toJson())
        return result.firstOrNull() ?: throw IllegalStateException("The server did not return the saved product.")
    }

    private suspend fun <T> optionalList(block: suspend () -> List<T>): List<T> = try { block() }
    catch (e: CancellationException) { throw e }
    catch (_: Exception) { emptyList() }
}
