package com.everybuy.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.core.util.serverMessage
import com.everybuy.mobile.data.AdminRepository
import com.everybuy.mobile.data.BrandDto
import com.everybuy.mobile.data.CategoryDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(
        val categories: List<CategoryDto> = emptyList(),
        val brands: List<BrandDto> = emptyList(),
        val busy: Boolean = false,
        val busyId: String? = null,
        val created: Boolean = false,
        val actionError: String? = null,
        val notice: String? = null,
    ) : CatalogState
    data class Forbidden(val message: String) : CatalogState
    data object SessionExpired : CatalogState
    data class Failed(val message: String) : CatalogState
}

enum class CatalogKind(val label: String) { CATEGORIES("category"), BRANDS("brand") }

@HiltViewModel class CatalogViewModel @Inject constructor(private val repository: AdminRepository) : ViewModel() {
    private val _state = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        _state.value = CatalogState.Loading
        _state.value = try {
            val categories = repository.categories()
            val brands = repository.brands()
            CatalogState.Ready(categories = categories.sortedBy { it.name.lowercase() }, brands = brands.sortedBy { it.name.lowercase() })
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> CatalogState.SessionExpired
                403 -> CatalogState.Forbidden("Only an owner can manage categories and brands.")
                else -> CatalogState.Failed("Categories and brands could not be loaded (${e.code()}).")
            }
        }
        catch (_: java.io.IOException) { CatalogState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { CatalogState.Failed("Categories and brands could not be loaded. Try again.") }
    }

    fun add(kind: CatalogKind, name: String) {
        val current = _state.value as? CatalogState.Ready ?: return
        if (current.busy) return
        val validation = validateCatalogName(name, kind.label)
        if (validation != null) { _state.value = current.copy(actionError = validation, notice = null); return }
        _state.value = current.copy(busy = true, created = false, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                when (kind) {
                    CatalogKind.CATEGORIES -> {
                        val created = repository.createCategory(name.trim())
                        current.copy(busy = false, created = true, categories = (current.categories + created).sortedBy { it.name.lowercase() }, notice = "Category created.")
                    }
                    CatalogKind.BRANDS -> {
                        val created = repository.createBrand(name.trim())
                        current.copy(busy = false, created = true, brands = (current.brands + created).sortedBy { it.name.lowercase() }, notice = "Brand created.")
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) { failure(current, e, kind) }
            catch (_: java.io.IOException) { current.copy(busy = false, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(busy = false, actionError = "Unable to create ${kind.label}. Try again.") }
            _state.value = next
        }
    }

    fun toggle(kind: CatalogKind, id: String, active: Boolean) {
        val current = _state.value as? CatalogState.Ready ?: return
        if (current.busyId != null) return
        _state.value = current.copy(busyId = id, actionError = null, notice = null)
        viewModelScope.launch {
            val next = try {
                when (kind) {
                    CatalogKind.CATEGORIES -> {
                        val updated = repository.setCategoryActive(id, active)
                        current.copy(busyId = null, categories = current.categories.map { if (it.id == id) updated else it }, notice = "Category ${if (active) "reactivated" else "deactivated"}.")
                    }
                    CatalogKind.BRANDS -> {
                        val updated = repository.setBrandActive(id, active)
                        current.copy(busyId = null, brands = current.brands.map { if (it.id == id) updated else it }, notice = "Brand ${if (active) "reactivated" else "deactivated"}.")
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: retrofit2.HttpException) { failure(current.copy(busyId = null), e, kind) }
            catch (_: java.io.IOException) { current.copy(busyId = null, actionError = "Couldn’t connect. Check your internet and retry.") }
            catch (_: Exception) { current.copy(busyId = null, actionError = "Unable to update ${kind.label} status. Try again.") }
            _state.value = next
        }
    }

    fun clearFeedback() = updateReady { it.copy(actionError = null, notice = null, created = false) }

    private fun failure(current: CatalogState.Ready, e: retrofit2.HttpException, kind: CatalogKind): CatalogState = when (e.code()) {
        401 -> CatalogState.SessionExpired
        403 -> CatalogState.Forbidden("Only an owner can manage categories and brands.")
        409 -> current.copy(actionError = "A ${kind.label} with that name already exists.")
        else -> current.copy(actionError = e.serverMessage() ?: "Unable to save the ${kind.label} (${e.code()}).")
    }

    private fun updateReady(transform: (CatalogState.Ready) -> CatalogState.Ready) {
        val current = _state.value as? CatalogState.Ready ?: return
        _state.value = transform(current)
    }
}
