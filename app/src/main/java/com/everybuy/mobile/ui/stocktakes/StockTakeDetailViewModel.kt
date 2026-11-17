package com.everybuy.mobile.ui.stocktakes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.StockTakeDetailDto
import com.everybuy.mobile.data.StockTakesRepository
import com.everybuy.mobile.core.util.serverMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StockTakeDetailState {
    data object Loading : StockTakeDetailState
    data class Ready(
        val detail: StockTakeDetailDto,
        val countingItemId: String? = null,
        val countSaving: Boolean = false,
        val countError: String? = null,
        val actionBusy: Boolean = false,
        val actionError: String? = null,
    ) : StockTakeDetailState
    data object SessionExpired : StockTakeDetailState
    data class Failed(val message: String) : StockTakeDetailState
}

@HiltViewModel class StockTakeDetailViewModel @Inject constructor(private val repository: StockTakesRepository) : ViewModel() {
    private val _state = MutableStateFlow<StockTakeDetailState>(StockTakeDetailState.Loading)
    val state = _state.asStateFlow()
    private var stockTakeId: String = ""

    fun load(id: String) = viewModelScope.launch {
        stockTakeId = id
        _state.value = StockTakeDetailState.Loading
        try {
            _state.value = StockTakeDetailState.Ready(repository.detail(id))
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> StockTakeDetailState.SessionExpired
                404 -> StockTakeDetailState.Failed("This stocktake could not be found. It may have been removed.")
                403 -> StockTakeDetailState.Failed("This account cannot view stocktakes.")
                else -> StockTakeDetailState.Failed("This stocktake could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = StockTakeDetailState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = StockTakeDetailState.Failed("This stocktake could not be loaded. Try again.") }
    }

    fun startCount(itemId: String) {
        val current = _state.value as? StockTakeDetailState.Ready ?: return
        _state.value = current.copy(countingItemId = itemId, countError = null)
    }

    fun dismissCount() {
        val current = _state.value as? StockTakeDetailState.Ready ?: return
        if (!current.countSaving) _state.value = current.copy(countingItemId = null, countError = null)
    }

    fun recordCount(itemId: String, bags: String, loosePacks: String) = viewModelScope.launch {
        val current = _state.value as? StockTakeDetailState.Ready ?: return@launch
        val item = current.detail.items.firstOrNull { it.id == itemId } ?: return@launch
        val validation = validateStockTakeCount(bags, loosePacks, item.packsPerBag)
        val countedPacks = validation.countedPacks
        if (countedPacks == null) {
            _state.value = current.copy(countError = validation.message)
            return@launch
        }
        _state.value = current.copy(countSaving = true, countError = null)
        try {
            repository.recordCount(current.detail.id, itemId, countedPacks)
            refreshSilently()
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = StockTakeDetailState.SessionExpired; return@launch }
            val message = when (e.code()) {
                400 -> e.serverMessage() ?: "Check the count and try again."
                403 -> "This account cannot record counts."
                404 -> "This stocktake item is no longer available. Reload and try again."
                409 -> "This stocktake is no longer in progress. Reload to see its current status."
                else -> "The count could not be saved (${e.code()})."
            }
            _state.value = current.copy(countSaving = false, countError = message)
        }
        catch (e: java.io.IOException) { _state.value = current.copy(countSaving = false, countError = "Couldn’t connect. Retry to save this count.") }
        catch (_: Exception) { _state.value = current.copy(countSaving = false, countError = "The count could not be saved. Try again.") }
    }

    fun finalize() = runAction(finalize = true)

    fun cancel() = runAction(finalize = false)

    private fun runAction(finalize: Boolean) = viewModelScope.launch {
        val current = _state.value as? StockTakeDetailState.Ready ?: return@launch
        if (current.detail.id != stockTakeId) return@launch
        _state.value = current.copy(actionBusy = true, actionError = null)
        try {
            if (finalize) repository.finalize(current.detail.id) else repository.cancel(current.detail.id)
            refreshSilently()
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = StockTakeDetailState.SessionExpired; return@launch }
            val server = e.serverMessage()
            val message = when (e.code()) {
                400 -> listOfNotNull(server, "Count the remaining products, then approve again.").joinToString(" ")
                403 -> if (finalize) "Only an owner can approve or cancel a stocktake." else "Only an owner can approve or cancel a stocktake."
                404 -> "This stocktake is no longer available. Reload and try again."
                409 -> if (finalize && server?.startsWith("Record the acquisition cost per pack") == true) {
                    "$server Enter the known cost in product setup, then reload this stocktake."
                } else "This stocktake is no longer in progress. Reload to see its current status."
                429 -> "Too many requests. Wait a moment and try again."
                else -> server ?: "Everybuy could not complete this action right now. Try again shortly."
            }
            _state.value = current.copy(actionBusy = false, actionError = message)
        }
        catch (e: java.io.IOException) { _state.value = current.copy(actionBusy = false, actionError = "Couldn’t connect. Check your internet and try again.") }
        catch (_: Exception) { _state.value = current.copy(actionBusy = false, actionError = "Everybuy could not complete this action right now. Try again shortly.") }
    }

    fun clearActionError() {
        val current = _state.value as? StockTakeDetailState.Ready ?: return
        if (current.actionError != null) _state.value = current.copy(actionError = null)
    }

    private suspend fun refreshSilently() {
        val current = _state.value as? StockTakeDetailState.Ready
        val detail = repository.detail(stockTakeId)
        _state.value = StockTakeDetailState.Ready(detail, actionError = current?.actionError)
    }
}
