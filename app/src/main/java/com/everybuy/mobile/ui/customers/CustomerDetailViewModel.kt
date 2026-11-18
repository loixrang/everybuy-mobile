package com.everybuy.mobile.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.CustomerStatementDto
import com.everybuy.mobile.data.CustomersRepository
import com.everybuy.mobile.data.RepaymentInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RepaymentSuccess(val amount: String, val saleId: String)

sealed interface CustomerDetailState {
    data object Loading : CustomerDetailState
    data class Ready(
        val statement: CustomerStatementDto,
        val submitting: Boolean = false,
        val repaymentError: String? = null,
        val success: RepaymentSuccess? = null,
        val statementRefreshWarning: String? = null,
    ) : CustomerDetailState
    data object NotFound : CustomerDetailState
    data object SessionExpired : CustomerDetailState
    data class Failed(val message: String) : CustomerDetailState
}

@HiltViewModel class CustomerDetailViewModel @Inject constructor(private val repository: CustomersRepository) : ViewModel() {
    private val _state = MutableStateFlow<CustomerDetailState>(CustomerDetailState.Loading)
    val state = _state.asStateFlow()
    private var customerId: String = ""
    private var payloadKey: String? = null
    private var requestId: String? = null

    fun load(id: String) = viewModelScope.launch {
        customerId = id
        _state.value = CustomerDetailState.Loading
        try {
            _state.value = CustomerDetailState.Ready(repository.statement(id))
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> CustomerDetailState.SessionExpired
                403 -> CustomerDetailState.Failed("This account does not have staff access to customer statements.")
                404 -> CustomerDetailState.NotFound
                else -> CustomerDetailState.Failed("The customer statement could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = CustomerDetailState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = CustomerDetailState.Failed("The customer statement could not be loaded. Try again.") }
    }

    fun repay(saleId: String, amount: String) = viewModelScope.launch {
        val current = _state.value as? CustomerDetailState.Ready ?: return@launch
        val sale = current.statement.sales.firstOrNull { it.id == saleId } ?: return@launch
        val validation = validateRepaymentAmount(amount, sale.balanceDue)
        if (validation != null) {
            _state.value = current.copy(repaymentError = validation)
            return@launch
        }
        val value = amount.trim()
        val key = "$saleId|$value"
        if (payloadKey != key) {
            payloadKey = key
            requestId = UUID.randomUUID().toString()
        }
        val stableRequestId = requestId ?: UUID.randomUUID().toString().also { requestId = it; payloadKey = key }
        _state.value = current.copy(submitting = true, repaymentError = null)
        try {
            repository.repay(customerId, RepaymentInput(requestId = stableRequestId, saleId = saleId, amount = value))
            payloadKey = null
            requestId = null
            val success = RepaymentSuccess(amount = value, saleId = saleId)
            val statement = try {
                repository.statement(customerId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.value = current.copy(
                    submitting = false,
                    success = success,
                    statementRefreshWarning = "The repayment was recorded, but the statement could not refresh. Reopen this customer to see current balances.",
                )
                return@launch
            }
            _state.value = CustomerDetailState.Ready(
                statement = statement,
                success = success,
            )
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            if (e.code() == 401) { _state.value = CustomerDetailState.SessionExpired; return@launch }
            val message = when (e.code()) {
                400 -> e.serverMessage() ?: "The repayment was rejected. Check the amount and try again."
                403 -> "This account does not have staff access to record repayments."
                404 -> "This customer or sale no longer exists. Reload the statement and try again."
                409 -> e.serverMessage() ?: "This sale no longer has an outstanding balance. Reload the statement."
                429 -> "Too many requests. Wait a moment and try again."
                else -> e.serverMessage() ?: "The repayment could not be recorded (${e.code()})."
            }
            _state.value = current.copy(submitting = false, repaymentError = message)
        }
        catch (e: java.io.IOException) {
            _state.value = current.copy(
                submitting = false,
                repaymentError = "Couldn’t connect. Retry this repayment to avoid recording it twice.",
            )
        }
        catch (_: Exception) {
            _state.value = current.copy(submitting = false, repaymentError = "The repayment could not be recorded. Try again.")
        }
    }

    fun clearRepaymentError() {
        val current = _state.value as? CustomerDetailState.Ready ?: return
        if (current.repaymentError != null) _state.value = current.copy(repaymentError = null)
    }

    fun dismissSuccess() {
        val current = _state.value as? CustomerDetailState.Ready ?: return
        if (current.success != null) _state.value = current.copy(success = null)
    }
}
