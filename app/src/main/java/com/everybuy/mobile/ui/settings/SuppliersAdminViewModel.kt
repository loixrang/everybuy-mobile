package com.everybuy.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.AdminRepository
import com.everybuy.mobile.data.SupplierDto
import com.everybuy.mobile.data.SupplierInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SuppliersAdminState {
    data object Loading : SuppliersAdminState
    data class Ready(
        val suppliers: List<SupplierDto> = emptyList(),
        val saving: Boolean = false,
        val saved: Boolean = false,
        val busyId: Int? = null,
        val actionError: String? = null,
        val notice: String? = null,
    ) : SuppliersAdminState
    data class Forbidden(val message: String) : SuppliersAdminState
    data object SessionExpired : SuppliersAdminState
    data class Failed(val message: String) : SuppliersAdminState
}

@HiltViewModel class SuppliersAdminViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow<SuppliersAdminState>(SuppliersAdminState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = SuppliersAdminState.Loading
        _state.value = try {
            SuppliersAdminState.Ready(repository.suppliers().sortedWith(compareByDescending<SupplierDto> { it.active }.thenBy { it.name.lowercase() }))
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> SuppliersAdminState.SessionExpired
                403 -> SuppliersAdminState.Forbidden("Only an owner can manage suppliers.")
                else -> SuppliersAdminState.Failed("Suppliers could not be loaded (${e.code()}).")
            }
        }
        catch (_: java.io.IOException) { SuppliersAdminState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { SuppliersAdminState.Failed("Suppliers could not be loaded. Try again.") }
    }

    fun save(existing: SupplierDto?, name: String, phone: String) {
        val current = _state.value as? SuppliersAdminState.Ready ?: return
        if (current.saving) return
        val validation = validateSupplier(name, phone)
        if (validation != null) { _state.value = current.copy(actionError = validation, notice = null); return }
        _state.value = current.copy(saving = true, saved = false, actionError = null, notice = null)
        viewModelScope.launch {
            val input = SupplierInput(name = name.trim(), phone = phone.trim().ifEmpty { null })
            val next = try {
                if (existing == null) {
                    val created = repository.createSupplier(input)
                    current.copy(saving = false, saved = true, suppliers = (current.suppliers + created).sortedWith(compareByDescending<SupplierDto> { it.active }.thenBy { it.name.lowercase() }), notice = "Supplier added.")
                } else {
                    val updated = repository.updateSupplier(existing.id, input)
                    current.copy(saving = false, saved = true, suppliers = current.suppliers.map { if (it.id == existing.id) updated else it }, notice = "Supplier details saved.")
                }
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> SuppliersAdminState.SessionExpired
                    403 -> SuppliersAdminState.Forbidden("Only an owner can manage suppliers.")
                    409 -> current.copy(saving = false, actionError = e.serverMessage() ?: "A supplier with that name already exists.")
                    else -> current.copy(saving = false, actionError = e.serverMessage() ?: "Unable to save supplier (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(saving = false, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(saving = false, actionError = "Unable to save supplier. Try again.") }
            _state.value = next
        }
    }

    fun toggle(supplier: SupplierDto) {
        val current = _state.value as? SuppliersAdminState.Ready ?: return
        if (current.busyId != null) return
        _state.value = current.copy(busyId = supplier.id, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                val updated = repository.updateSupplier(supplier.id, SupplierInput(name = supplier.name, phone = supplier.phone, active = !supplier.active))
                current.copy(busyId = null, suppliers = current.suppliers.map { if (it.id == supplier.id) updated else it }, notice = "${updated.name} ${if (updated.active) "reactivated" else "deactivated"}.")
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) {
                when (e.code()) {
                    401 -> SuppliersAdminState.SessionExpired
                    403 -> SuppliersAdminState.Forbidden("Only an owner can manage suppliers.")
                    else -> current.copy(busyId = null, actionError = e.serverMessage() ?: "Unable to update supplier status (${e.code()}).")
                }
            }
            catch (_: java.io.IOException) { current.copy(busyId = null, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(busyId = null, actionError = "Unable to update supplier status. Try again.") }
            _state.value = next
        }
    }

    fun clearFeedback() = updateReady { it.copy(actionError = null, notice = null, saved = false) }

    private fun updateReady(transform: (SuppliersAdminState.Ready) -> SuppliersAdminState.Ready) {
        val current = _state.value as? SuppliersAdminState.Ready ?: return
        _state.value = transform(current)
    }
}
