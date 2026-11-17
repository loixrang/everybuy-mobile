package com.everybuy.mobile.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.CreateSaleInput
import com.everybuy.mobile.data.CreateSaleResultDto
import com.everybuy.mobile.data.CustomerListItemDto
import com.everybuy.mobile.data.CustomersRepository
import com.everybuy.mobile.data.ProductDto
import com.everybuy.mobile.data.ProductsRepository
import com.everybuy.mobile.data.SaleLineInput
import com.everybuy.mobile.data.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface RecordSaleState {
    data object Loading : RecordSaleState
    data class Ready(
        val products: List<ProductDto>,
        val customers: List<CustomerListItemDto>,
        val customerWarning: String? = null,
        val lines: List<SaleLineDraft> = emptyList(),
        val search: String = "",
        val customerId: String = "",
        val amountPaid: String = "",
        val method: String = "cash",
        val reference: String = "",
        val error: String? = null,
        val submitting: Boolean = false,
        val success: CreateSaleResultDto? = null,
    ) : RecordSaleState
    data object SessionExpired : RecordSaleState
    data class Failed(val message: String) : RecordSaleState
}

@HiltViewModel class RecordSaleViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val productsRepository: ProductsRepository,
    private val customersRepository: CustomersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<RecordSaleState>(RecordSaleState.Loading)
    val state = _state.asStateFlow()
    private var payloadKey: String? = null
    private var requestId: String? = null

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = RecordSaleState.Loading
        try {
            coroutineScope {
                val products = async { productsRepository.load().products }
                val customers = async { runCatching { customersRepository.active() } }
                val customerResult = customers.await()
                _state.value = RecordSaleState.Ready(
                    products = products.await(),
                    customers = customerResult.getOrDefault(emptyList()),
                    customerWarning = if (customerResult.isFailure) "Customers could not be loaded. Cash sales can still be recorded." else null,
                )
            }
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = if (e.code() == 401) RecordSaleState.SessionExpired else RecordSaleState.Failed("The sale screen could not be loaded (${e.code()}).")
        }
        catch (e: java.io.IOException) { _state.value = RecordSaleState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { RecordSaleState.Failed("The sale screen could not be loaded. Try again.") }
    }

    fun updateSearch(value: String) = updateReady { it.copy(search = value) }

    fun setCustomer(customerId: String) = updateReady { it.copy(customerId = customerId, error = null) }

    fun setAmountPaid(value: String) = updateReady { it.copy(amountPaid = value, error = null) }

    fun setMethod(method: String) = updateReady { it.copy(method = method, error = null) }

    fun setReference(value: String) = updateReady { it.copy(reference = value.take(200), error = null) }

    fun addProduct(productId: String) = updateReady { current ->
        val product = current.products.firstOrNull { it.id == productId } ?: return@updateReady current
        if (product.stockPacks < 1) return@updateReady current.copy(error = "${product.name} has no stock available.", search = "")
        if (current.lines.size >= MAX_SALE_LINES) return@updateReady current.copy(error = "A sale can contain at most $MAX_SALE_LINES lines.", search = "")
        val existing = current.lines.firstOrNull { it.productId == productId }
        if (existing != null) {
            val max = maxSaleQuantity(product, existing.unit)
            if (existing.quantity >= max) return@updateReady current.copy(search = "")
            current.copy(lines = current.lines.map { if (it === existing) it.copy(quantity = (it.quantity + 1).coerceAtMost(max)) else it }, search = "", error = null)
        } else {
            current.copy(
                lines = current.lines + SaleLineDraft(productId = product.id, unit = "Pack", quantity = 1, unitPrice = product.retailPrice),
                search = "",
                error = null,
            )
        }
    }

    fun updateQuantity(index: Int, value: String) = updateReady { current ->
        val line = current.lines.getOrNull(index) ?: return@updateReady current
        val product = current.products.firstOrNull { it.id == line.productId } ?: return@updateReady current
        val quantity = value.toIntOrNull() ?: return@updateReady current.copy(lines = current.lines.toMutableList().also { it[index] = line.copy(quantity = 1) })
        val max = maxSaleQuantity(product, line.unit).coerceAtLeast(1)
        current.copy(lines = current.lines.toMutableList().also { it[index] = line.copy(quantity = quantity.coerceIn(1, max)) }, error = null)
    }

    fun updateUnit(index: Int, unit: String) = updateReady { current ->
        val line = current.lines.getOrNull(index) ?: return@updateReady current
        val product = current.products.firstOrNull { it.id == line.productId } ?: return@updateReady current
        if (!isSaleUnitAvailable(product, unit)) return@updateReady current
        val max = maxSaleQuantity(product, unit).coerceAtLeast(1)
        current.copy(lines = current.lines.toMutableList().also { it[index] = line.copy(unit = unit, quantity = line.quantity.coerceAtMost(max), unitPrice = saleUnitPrice(product, unit)) }, error = null)
    }

    fun updateUnitPrice(index: Int, value: String) = updateReady { current ->
        val line = current.lines.getOrNull(index) ?: return@updateReady current
        current.copy(lines = current.lines.toMutableList().also { it[index] = line.copy(unitPrice = value) }, error = null)
    }

    fun removeLine(index: Int) = updateReady { current ->
        current.copy(lines = current.lines.filterIndexed { i, _ -> i != index }, error = null)
    }

    fun submit() = viewModelScope.launch {
        val current = _state.value as? RecordSaleState.Ready ?: return@launch
        val byId = current.products.associateBy { it.id }
        val lines = buildSaleLines(current.lines, byId)
        val error = validateSaleForSubmit(lines, current.amountPaid, current.customerId)
        if (error != null) { _state.value = current.copy(error = error); return@launch }
        val amountPaid = current.amountPaid.trim().ifBlank { "0" }
        val method = if (amountPaid.toBigDecimalOrNull() == java.math.BigDecimal.ZERO) "cash" else current.method
        val reference = if (amountPaid.toBigDecimalOrNull() == java.math.BigDecimal.ZERO) null else current.reference.trim().ifBlank { null }
        val sortedLines = lines.sortedWith(compareBy({ it.draft.productId }, { it.draft.unit }))
        val key = sortedLines.joinToString("|") { "${it.draft.productId}:${it.draft.unit}:${it.draft.quantity}:${it.draft.unitPrice}" } +
            "|${current.customerId}|$amountPaid|$method|${reference.orEmpty()}"
        if (payloadKey != key) { payloadKey = key; requestId = UUID.randomUUID().toString() }
        val stableRequestId = requestId ?: UUID.randomUUID().toString().also { requestId = it; payloadKey = key }
        _state.value = current.copy(submitting = true, error = null)
        try {
            val result = salesRepository.create(
                CreateSaleInput(
                    requestId = stableRequestId,
                    customerId = current.customerId.ifBlank { null },
                    amountPaid = amountPaid,
                    method = method,
                    reference = reference,
                    items = sortedLines.map { SaleLineInput(it.draft.productId, it.draft.unit, it.draft.quantity, it.draft.unitPrice.trim()) },
                ),
            )
            payloadKey = null
            requestId = null
            _state.value = current.copy(submitting = false, success = result)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = RecordSaleState.SessionExpired; return@launch }
            val message = when (e.code()) {
                400 -> e.serverMessage() ?: "The sale details were rejected. Check the quantities and prices."
                403 -> "Only active staff and owners can record sales."
                404 -> "A product or customer in this sale no longer exists. Reload and try again."
                409 -> e.serverMessage() ?: "Stock or customer details changed before this sale was recorded. Reload and retry."
                else -> e.serverMessage() ?: "The sale could not be recorded (${e.code()})."
            }
            _state.value = current.copy(submitting = false, error = message)
        }
        catch (e: java.io.IOException) { _state.value = current.copy(submitting = false, error = "Couldn’t connect. Retry this sale to avoid recording it twice.") }
        catch (_: Exception) { _state.value = current.copy(submitting = false, error = "The sale could not be recorded. Try again.") }
    }

    fun recordAnother() {
        payloadKey = null
        requestId = null
        load()
    }

    private fun updateReady(transform: (RecordSaleState.Ready) -> RecordSaleState.Ready) {
        val current = _state.value as? RecordSaleState.Ready ?: return
        _state.value = transform(current)
    }
}
