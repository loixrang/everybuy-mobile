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

sealed interface StockMovementDetailState {
    data object Loading : StockMovementDetailState
    data class Ready(val movement: StockMovementDto, val product: ProductDto?) : StockMovementDetailState
    data object NotFound : StockMovementDetailState
    data object SessionExpired : StockMovementDetailState
    data class Failed(val message: String) : StockMovementDetailState
}

@HiltViewModel class StockMovementDetailViewModel @Inject constructor(private val repository: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockMovementDetailState>(StockMovementDetailState.Loading)
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _state.value = StockMovementDetailState.Loading
        try {
            val movement = repository.movement(id)
            val product = try { repository.products().firstOrNull { it.id == movement.productId } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            _state.value = StockMovementDetailState.Ready(movement, product)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> StockMovementDetailState.SessionExpired
                404 -> StockMovementDetailState.NotFound
                else -> StockMovementDetailState.Failed("This movement could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = StockMovementDetailState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = StockMovementDetailState.Failed("This movement could not be loaded. Try again.") }
    }
}
