package com.everybuy.mobile.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.CustomerDto
import com.everybuy.mobile.data.CustomersRepository
import com.everybuy.mobile.data.SaleDetailDto
import com.everybuy.mobile.data.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

sealed interface SaleDetailState {
    data object Loading : SaleDetailState
    data class Ready(val sale: SaleDetailDto, val customer: CustomerDto?) : SaleDetailState
    data object NotFound : SaleDetailState
    data object SessionExpired : SaleDetailState
    data class Failed(val message: String) : SaleDetailState
}

fun salePaidTotal(sale: SaleDetailDto): BigDecimal = sale.payments.fold(BigDecimal.ZERO) { total, payment -> total + (payment.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO) }

fun saleBalance(sale: SaleDetailDto): BigDecimal {
    val balance = (sale.itemsTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO) - salePaidTotal(sale)
    return if (balance > BigDecimal.ZERO) balance else BigDecimal.ZERO
}

fun saleStatus(sale: SaleDetailDto): String {
    val total = sale.itemsTotal.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val paid = salePaidTotal(sale)
    return when {
        paid <= BigDecimal.ZERO -> "UNPAID"
        paid >= total -> "PAID"
        else -> "PART_PAID"
    }
}

@HiltViewModel class SaleDetailViewModel @Inject constructor(
    private val repository: SalesRepository,
    private val customersRepository: CustomersRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SaleDetailState>(SaleDetailState.Loading)
    val state = _state.asStateFlow()

    fun load(id: String) = viewModelScope.launch {
        _state.value = SaleDetailState.Loading
        try {
            val sale = repository.detail(id)
            val customer = sale.customerId?.let {
                try { customersRepository.detail(it) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { null }
            }
            _state.value = SaleDetailState.Ready(sale, customer)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> SaleDetailState.SessionExpired
                404 -> SaleDetailState.NotFound
                else -> SaleDetailState.Failed("This sale could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = SaleDetailState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = SaleDetailState.Failed("This sale could not be loaded. Try again.") }
    }
}
