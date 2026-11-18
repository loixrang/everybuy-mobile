package com.everybuy.mobile.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.search.DebouncedFuzzySearch
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.StockMovementDto
import com.everybuy.mobile.data.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StockMovementsState {
    data object Loading : StockMovementsState
    data class Ready(
        val movements: List<StockMovementDto>,
        val products: List<ProductDto>,
        val page: Int,
        val total: Int,
        val query: String = "",
        val searchResults: List<StockMovementDto> = movements,
        val searching: Boolean = false,
        val loadingMore: Boolean = false,
        val moreError: String? = null,
    ) : StockMovementsState
    data object SessionExpired : StockMovementsState
    data class Failed(val message: String) : StockMovementsState
}

@HiltViewModel class StockMovementsViewModel @Inject constructor(private val repository: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockMovementsState>(StockMovementsState.Loading)
    val state = _state.asStateFlow()
    private val fuzzySearch = DebouncedFuzzySearch<StockMovementDto>(viewModelScope)
    private var query = ""
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        fuzzySearch.cancel()
        _state.value = StockMovementsState.Loading
        try {
            val page = repository.movements(1)
            val products = try { repository.products() } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
            val ready = StockMovementsState.Ready(
                movements = page.data,
                products = products,
                page = page.page,
                total = page.total,
                query = query,
                searching = query.isNotBlank(),
            )
            _state.value = ready
            searchMovements(ready)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) { _state.value = if (e.code() == 401) StockMovementsState.SessionExpired else StockMovementsState.Failed("Stock movements could not be loaded (${e.code()}).") }
        catch (e: java.io.IOException) { _state.value = StockMovementsState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = StockMovementsState.Failed("Stock movements could not be loaded. Try again.") }
    }

    fun loadMore() = viewModelScope.launch {
        val current = _state.value as? StockMovementsState.Ready ?: return@launch
        if (current.loadingMore || current.movements.size >= current.total) return@launch
        _state.value = current.copy(loadingMore = true, moreError = null)
        try {
            val next = repository.movements(current.page + 1)
            val updated = current.copy(
                movements = current.movements + next.data,
                page = next.page,
                searchResults = current.movements + next.data,
                searching = current.query.isNotBlank(),
                loadingMore = false,
            )
            _state.value = updated
            searchMovements(updated)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) _state.value = StockMovementsState.SessionExpired
            else _state.value = current.copy(loadingMore = false, moreError = "More movements could not be loaded (${e.code()}).")
        } catch (e: java.io.IOException) { _state.value = current.copy(loadingMore = false, moreError = "Couldn’t connect to load more movements.") }
        catch (_: Exception) { _state.value = current.copy(loadingMore = false, moreError = "More movements could not be loaded.") }
    }

    fun setQuery(value: String) {
        query = value
        val current = _state.value as? StockMovementsState.Ready ?: return
        val updated = current.copy(query = value, searching = value.isNotBlank())
        _state.value = updated
        searchMovements(updated)
    }

    private fun searchMovements(current: StockMovementsState.Ready) {
        val products = current.products.associateBy { it.id }
        fuzzySearch.search(
            items = current.movements,
            query = current.query,
            searchableText = { movement ->
                val product = products[movement.productId]
                listOf(
                    movement.id,
                    movement.productId,
                    product?.name.orEmpty(),
                    product?.size.orEmpty(),
                    product?.packName.orEmpty(),
                    product?.categoryId.orEmpty(),
                    product?.brandId.orEmpty(),
                    movement.type,
                    movement.changePacks.toString(),
                    movement.supplierId?.toString().orEmpty(),
                    movement.saleItemId.orEmpty(),
                    movement.requestId.orEmpty(),
                    movement.costPerPack.orEmpty(),
                    movement.note.orEmpty(),
                    movement.createdBy,
                    movement.createdAt,
                ).joinToString(" ")
            },
        ) { results ->
            val latest = _state.value as? StockMovementsState.Ready ?: return@search
            if (latest.movements === current.movements && latest.query == current.query) {
                _state.value = latest.copy(searchResults = results, searching = false)
            }
        }
    }
}
