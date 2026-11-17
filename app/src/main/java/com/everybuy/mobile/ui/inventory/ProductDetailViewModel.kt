package com.everybuy.mobile.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.ProductDetails
import com.everybuy.mobile.data.ProductsRepository
import com.everybuy.mobile.data.StockMovementDto
import com.everybuy.mobile.data.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductDetailState {
    data object Loading : ProductDetailState
    data class Ready(
        val details: ProductDetails,
        val recentMovements: List<StockMovementDto> = emptyList(),
        val movementsUnavailable: Boolean = false,
    ) : ProductDetailState
    data object NotFound : ProductDetailState
    data object SessionExpired : ProductDetailState
    data class Failed(val message: String) : ProductDetailState
}

@HiltViewModel class ProductDetailViewModel @Inject constructor(
    private val repository: ProductsRepository,
    private val stockRepository: StockRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ProductDetailState>(ProductDetailState.Loading)
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _state.value = ProductDetailState.Loading
        val detailsResult = runCatching { repository.getById(id) }
        detailsResult.exceptionOrNull()?.let { throwable ->
            _state.value = when (throwable) {
                is CancellationException -> throw throwable
                is retrofit2.HttpException -> if (throwable.code() == 401) ProductDetailState.SessionExpired else ProductDetailState.Failed("Product could not be loaded (${throwable.code()}).")
                is java.io.IOException -> ProductDetailState.Failed("Couldn’t connect. Check your internet and retry.")
                else -> ProductDetailState.Failed("Product could not be loaded. Try again.")
            }
            return@launch
        }
        val details = detailsResult.getOrNull()
        if (details == null) { _state.value = ProductDetailState.NotFound; return@launch }

        val movements = async {
            runCatching { stockRepository.movementsForProduct(id).take(5) }
        }
        val recent = movements.await()
        _state.value = ProductDetailState.Ready(
            details = details,
            recentMovements = recent.getOrDefault(emptyList()),
            movementsUnavailable = recent.isFailure,
        )
    }
}
