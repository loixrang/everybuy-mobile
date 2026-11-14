package com.everybuy.mobile.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    data class Ready(val movements: List<StockMovementDto>, val products: List<ProductDto>, val page: Int, val total: Int, val loadingMore: Boolean = false, val moreError: String? = null) : StockMovementsState
    data object SessionExpired : StockMovementsState
    data class Failed(val message: String) : StockMovementsState
}

@HiltViewModel class StockMovementsViewModel @Inject constructor(private val repository: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockMovementsState>(StockMovementsState.Loading)
    val state = _state.asStateFlow()
    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = StockMovementsState.Loading
        try {
            val page = repository.movements(1)
            val products = try { repository.products() } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
            _state.value = StockMovementsState.Ready(page.data, products, page.page, page.total)
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
            _state.value = current.copy(movements = current.movements + next.data, page = next.page, loadingMore = false)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) _state.value = StockMovementsState.SessionExpired
            else _state.value = current.copy(loadingMore = false, moreError = "More movements could not be loaded (${e.code()}).")
        } catch (e: java.io.IOException) { _state.value = current.copy(loadingMore = false, moreError = "Couldn’t connect to load more movements.") }
        catch (_: Exception) { _state.value = current.copy(loadingMore = false, moreError = "More movements could not be loaded.") }
    }
}
