package com.everybuy.mobile.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.ProductDetails
import com.everybuy.mobile.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductDetailState {
    data object Loading : ProductDetailState
    data class Ready(val details: ProductDetails) : ProductDetailState
    data object NotFound : ProductDetailState
    data object SessionExpired : ProductDetailState
    data class Failed(val message: String) : ProductDetailState
}

@HiltViewModel class ProductDetailViewModel @Inject constructor(private val repository: ProductsRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProductDetailState>(ProductDetailState.Loading)
    val state = _state.asStateFlow()
    fun load(id: String) = viewModelScope.launch {
        _state.value = ProductDetailState.Loading
        _state.value = try { repository.getById(id)?.let(ProductDetailState::Ready) ?: ProductDetailState.NotFound }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) ProductDetailState.SessionExpired else ProductDetailState.Failed("Product could not be loaded (${e.code()}).")
        }
        catch (e: java.io.IOException) { ProductDetailState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { ProductDetailState.Failed("Product could not be loaded. Try again.") }
    }
}
