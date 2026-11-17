package com.everybuy.mobile.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.CreateCustomerInput
import com.everybuy.mobile.data.CustomersRepository
import com.everybuy.mobile.data.UpdateCustomerInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDraft(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
    val active: Boolean = true,
)

sealed interface CustomerFormState {
    data object Loading : CustomerFormState
    data class Ready(
        val draft: CustomerDraft,
        val editing: Boolean,
        val errors: Map<String, String> = emptyMap(),
        val formError: String? = null,
        val saving: Boolean = false,
        val sessionExpired: Boolean = false,
    ) : CustomerFormState
    data class Failed(val message: String) : CustomerFormState
    data class Saved(val customerId: String) : CustomerFormState
}

@HiltViewModel class CustomerFormViewModel @Inject constructor(private val repository: CustomersRepository) : ViewModel() {
    private val _state = MutableStateFlow<CustomerFormState>(CustomerFormState.Loading)
    val state = _state.asStateFlow()

    fun load(id: String?) = viewModelScope.launch {
        if (id == null) {
            _state.value = CustomerFormState.Ready(CustomerDraft(), editing = false)
            return@launch
        }
        _state.value = CustomerFormState.Loading
        try {
            val customer = repository.detail(id)
                ?: run { _state.value = CustomerFormState.Failed("Customer not found."); return@launch }
            _state.value = CustomerFormState.Ready(
                CustomerDraft(
                    name = customer.name,
                    phone = customer.phone.orEmpty(),
                    address = customer.address.orEmpty(),
                    notes = customer.notes.orEmpty(),
                    active = customer.active,
                ),
                editing = true,
            )
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            _state.value = when (e.code()) {
                401 -> CustomerFormState.Failed("Your session has expired. Sign in again.")
                403 -> CustomerFormState.Failed("This account does not have staff access to customers.")
                404 -> CustomerFormState.Failed("Customer not found.")
                else -> CustomerFormState.Failed("The customer form could not be loaded (${e.code()}).")
            }
        }
        catch (e: java.io.IOException) { _state.value = CustomerFormState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = CustomerFormState.Failed("The customer form could not be loaded. Try again.") }
    }

    fun updateDraft(transform: (CustomerDraft) -> CustomerDraft) {
        val current = _state.value as? CustomerFormState.Ready ?: return
        _state.value = current.copy(draft = transform(current.draft), errors = emptyMap(), formError = null)
    }

    fun save(id: String?) = viewModelScope.launch {
        val current = _state.value as? CustomerFormState.Ready ?: return@launch
        val draft = current.draft
        val errors = buildMap {
            validateCustomerName(draft.name)?.let { put("name", it) }
            validateCustomerPhone(draft.phone)?.let { put("phone", it) }
            validateCustomerAddress(draft.address)?.let { put("address", it) }
            validateCustomerNotes(draft.notes)?.let { put("notes", it) }
        }
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return@launch
        }

        val name = draft.name.trim()
        val phone = draft.phone.trim().ifEmpty { null }
        val address = draft.address.trim().ifEmpty { null }
        val notes = draft.notes.trim().ifEmpty { null }

        _state.value = current.copy(saving = true, formError = null, errors = emptyMap())
        try {
            val saved = if (id == null) {
                repository.create(CreateCustomerInput(name = name, phone = phone, address = address, notes = notes))
            } else {
                repository.update(
                    id,
                    UpdateCustomerInput(name = name, phone = phone, address = address, notes = notes, active = draft.active),
                )
            }
            _state.value = CustomerFormState.Saved(saved.id)
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> e.serverMessage() ?: "The customer details were rejected. Check the values and try again."
                401 -> "Your session has expired."
                403 -> "This account does not have staff access to customers."
                404 -> "This customer no longer exists. Reload customers and try again."
                409 -> "A customer with this name already exists."
                429 -> "Too many requests. Wait a moment and try again."
                else -> e.serverMessage() ?: "The customer could not be saved (${e.code()})."
            }
            _state.value = current.copy(saving = false, formError = message, sessionExpired = e.code() == 401)
        }
        catch (e: java.io.IOException) { _state.value = current.copy(saving = false, formError = "Couldn’t connect. Retry to save the customer.") }
        catch (_: Exception) { _state.value = current.copy(saving = false, formError = "The customer could not be saved. Try again.") }
    }
}
