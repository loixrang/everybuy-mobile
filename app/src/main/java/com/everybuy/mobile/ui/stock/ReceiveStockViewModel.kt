package com.everybuy.mobile.ui.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.StockAdditionMutation
import com.everybuy.mobile.data.StockRepository
import com.everybuy.mobile.data.SupplierDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ReceiveStockDraft(val productId: String = "", val supplierId: Int? = null, val bags: String = "0", val loosePacks: String = "0", val unitCost: String = "")

sealed interface ReceiveStockState {
    data object Loading : ReceiveStockState
    data class Ready(
        val products: List<ProductDto>, val suppliers: List<SupplierDto>, val suppliersWarning: String?, val draft: ReceiveStockDraft,
        val errors: Map<String, String> = emptyMap(), val error: String? = null, val saving: Boolean = false,
    ) : ReceiveStockState
    data class Success(val productName: String, val totalPacks: Int, val newStockPacks: Int, val replayed: Boolean) : ReceiveStockState
    data object SessionExpired : ReceiveStockState
    data class Failed(val message: String) : ReceiveStockState
}

@HiltViewModel class ReceiveStockViewModel @Inject constructor(private val repository: StockRepository) : ViewModel() {
    private val _state = MutableStateFlow<ReceiveStockState>(ReceiveStockState.Loading)
    val state = _state.asStateFlow()
    private var lastPayloadKey: String? = null
    private var lastRequestId: String? = null

    fun load(initialProductId: String?) = viewModelScope.launch {
        _state.value = ReceiveStockState.Loading
        try {
            val products = repository.products().filter { it.active }
            val suppliers = try { repository.suppliers() } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
            val initialId = initialProductId?.takeIf { id -> products.any { it.id == id } } ?: products.firstOrNull()?.id.orEmpty()
            _state.value = ReceiveStockState.Ready(products, suppliers, null, ReceiveStockDraft(productId = initialId))
            if (suppliers.isEmpty()) {
                val current = _state.value as ReceiveStockState.Ready
                _state.value = current.copy(suppliersWarning = "The supplier list is unavailable. You can still record this receipt without a supplier.")
            }
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) { _state.value = if (e.code() == 401) ReceiveStockState.SessionExpired else ReceiveStockState.Failed("Products could not be loaded (${e.code()}).") }
        catch (e: java.io.IOException) { _state.value = ReceiveStockState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = ReceiveStockState.Failed("Products could not be loaded. Try again.") }
    }

    fun updateDraft(transform: (ReceiveStockDraft) -> ReceiveStockDraft) = _state.update { current ->
        if (current is ReceiveStockState.Ready) current.copy(draft = transform(current.draft), errors = emptyMap(), error = null) else current
    }

    fun submit() = viewModelScope.launch {
        val current = _state.value as? ReceiveStockState.Ready ?: return@launch
        val product = current.products.firstOrNull { it.id == current.draft.productId }
        val (quantity, errors) = validateStockReceive(product, current.draft.bags, current.draft.loosePacks, current.draft.unitCost)
        if (errors.isNotEmpty() || quantity == null || product == null) { _state.value = current.copy(errors = errors); return@launch }
        val cost = current.draft.unitCost.trim()
        val payloadKey = listOf(product.id, quantity.totalPacks, cost, current.draft.supplierId ?: "").joinToString("|")
        val requestId = if (payloadKey == lastPayloadKey) lastRequestId!! else UUID.randomUUID().toString().also { lastPayloadKey = payloadKey; lastRequestId = it }
        _state.value = current.copy(saving = true, errors = emptyMap(), error = null)
        try {
            val result = repository.receive(StockAdditionMutation(requestId, product.id, quantity.totalPacks, cost, current.draft.supplierId))
            val updated = result.updateProduct.firstOrNull { it.id == product.id } ?: throw IllegalStateException("The server did not return the updated stock balance.")
            _state.value = ReceiveStockState.Success(product.name, quantity.totalPacks, updated.stockPacks, result.replayed)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "The receipt details were rejected. Check the quantity and cost."
                401 -> { _state.value = ReceiveStockState.SessionExpired; return@launch }
                403 -> "This account cannot receive stock."
                404 -> "The selected product or supplier could not be found."
                409 -> "Stock changed before this receipt was applied. Refresh products and try again."
                else -> "The receipt could not be saved (${e.code()})."
            }
            _state.value = current.copy(saving = false, error = message)
        } catch (e: java.io.IOException) { _state.value = current.copy(saving = false, error = "Couldn’t connect. Retry the same receipt to avoid recording it twice.") }
        catch (_: Exception) { _state.value = current.copy(saving = false, error = "The receipt could not be saved. Try again.") }
    }

    fun receiveAnother() {
        val current = _state.value as? ReceiveStockState.Success ?: return
        lastPayloadKey = null
        lastRequestId = null
        viewModelScope.launch { load(null) }
    }
}
