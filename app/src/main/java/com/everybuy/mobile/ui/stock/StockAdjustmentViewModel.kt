package com.everybuy.mobile.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.StockAdjustmentMutation
import com.everybuy.mobile.data.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface StockAdjustmentState {
    data object Idle : StockAdjustmentState
    data object Saving : StockAdjustmentState
    data class Failed(val message: String) : StockAdjustmentState
    data object Saved : StockAdjustmentState
    data object SessionExpired : StockAdjustmentState
}

fun validateStockAdjustment(product: ProductDto, quantityInput: String, direction: String, noteInput: String): String? {
    val quantity = quantityInput.trim().toIntOrNull()
    val note = noteInput.trim()
    if (quantity == null) return "Enter a whole number of packs."
    if (quantity !in 1..1_000_000) return "Quantity must be between 1 and 1,000,000 packs."
    if (note.isEmpty()) return "A reason is required and is recorded in movement history."
    if (note.length > 500) return "Reason must be at most 500 characters."
    if (direction !in setOf("Add", "Sub")) return "Choose whether to add or remove stock."
    if (direction == "Add" && product.costPerPack == null) return "This product has no recorded acquisition cost. Add its known cost per pack in product setup before increasing stock."
    if (direction == "Sub" && quantity > product.stockPacks) return "Not enough stock for this removal. Reload the product and try again."
    return null
}

@HiltViewModel
class StockAdjustmentViewModel @Inject constructor(private val repository: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockAdjustmentState>(StockAdjustmentState.Idle)
    val state = _state.asStateFlow()
    private var payloadKey: String? = null
    private var requestId: String? = null

    fun reset() {
        payloadKey = null
        requestId = null
        _state.value = StockAdjustmentState.Idle
    }

    fun submit(product: ProductDto, quantityInput: String, direction: String, noteInput: String) = viewModelScope.launch {
        val quantity = quantityInput.trim().toIntOrNull()
        val note = noteInput.trim()
        val error = validateStockAdjustment(product, quantityInput, direction, noteInput)
        if (error != null) { _state.value = StockAdjustmentState.Failed(error); return@launch }
        val validQuantity = quantity ?: return@launch
        val key = "${product.id}|$quantity|$direction|$note"
        if (payloadKey != key) { payloadKey = key; requestId = UUID.randomUUID().toString() }
        val stableRequestId = requestId ?: UUID.randomUUID().toString().also { requestId = it; payloadKey = key }
        _state.value = StockAdjustmentState.Saving
        try {
            val result = repository.adjust(StockAdjustmentMutation(stableRequestId, product.id, validQuantity, direction, note))
            if (result.updateProduct.none { it.id == product.id }) throw IllegalStateException("The server did not return the updated stock balance.")
            _state.value = StockAdjustmentState.Saved
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> StockAdjustmentState.SessionExpired
                403 -> StockAdjustmentState.Failed("Only owners can adjust stock.")
                404 -> StockAdjustmentState.Failed("The selected product no longer exists. Reload and try again.")
                409 -> StockAdjustmentState.Failed(if (direction == "Sub") "Not enough stock for this removal. The balance may have changed; reload and try again." else "Stock changed before this adjustment. Reload and retry.")
                else -> StockAdjustmentState.Failed("Stock could not be adjusted (${e.code()}). Retry the same adjustment if the connection was interrupted.")
            }
        } catch (e: java.io.IOException) {
            _state.value = StockAdjustmentState.Failed("Couldn’t connect. Retry the same adjustment to avoid applying it twice.")
        } catch (_: Exception) {
            _state.value = StockAdjustmentState.Failed("Stock could not be adjusted. Retry the same adjustment if needed.")
        }
    }
}
