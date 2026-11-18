package com.everybuy.mobile.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.search.DebouncedFuzzySearch
import com.everybuy.mobile.data.ProductInventory
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InventoryState {
    data object Loading : InventoryState
    data class Ready(
        val inventory: ProductInventory,
        val query: String = "",
        val searchResults: List<ProductDto> = inventory.products,
        val searching: Boolean = false,
    ) : InventoryState
    data class Failed(val message: String) : InventoryState
    data object Unauthorized : InventoryState
}

@HiltViewModel class InventoryViewModel @Inject constructor(private val repository: ProductsRepository) : ViewModel() {
    private val _state = MutableStateFlow<InventoryState>(InventoryState.Loading)
    val state = _state.asStateFlow()
    private val fuzzySearch = DebouncedFuzzySearch<ProductDto>(viewModelScope)
    init { refresh() }
    fun refresh() = viewModelScope.launch {
        fuzzySearch.cancel()
        _state.value = InventoryState.Loading
        _state.value = try { InventoryState.Ready(repository.load()) }
        catch (e: retrofit2.HttpException) { if (e.code() == 401) InventoryState.Unauthorized else InventoryState.Failed("Inventory could not be loaded (${e.code()}).") }
        catch (e: java.io.IOException) { InventoryState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { InventoryState.Failed("Inventory could not be loaded. Try again.") }
    }

    fun setQuery(value: String) {
        val current = _state.value as? InventoryState.Ready ?: return
        _state.value = current.copy(query = value, searching = value.isNotBlank())
        val inventory = current.inventory
        val categories = inventory.categories.associate { it.id to it.name }
        val brands = inventory.brands.associate { it.id to it.name }
        fuzzySearch.search(
            items = inventory.products,
            query = value,
            searchableText = { product ->
                listOf(
                    product.name,
                    brands[product.brandId].orEmpty(),
                    categories[product.categoryId].orEmpty(),
                    product.packName,
                    product.size.orEmpty(),
                    product.packsPerBag.toString(),
                    product.piecesPerPack?.toString().orEmpty(),
                ).joinToString(" ")
            },
        ) { results ->
            val latest = _state.value as? InventoryState.Ready ?: return@search
            if (latest.inventory === inventory && latest.query == value) {
                _state.value = latest.copy(searchResults = results, searching = false)
            }
        }
    }
}
