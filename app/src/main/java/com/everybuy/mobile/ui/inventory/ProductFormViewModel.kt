package com.everybuy.mobile.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.everybuy.mobile.data.BrandDto
import com.everybuy.mobile.data.CategoryDto
import com.everybuy.mobile.data.ProductMutation
import com.everybuy.mobile.data.ProductsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductFormState {
    data object Loading : ProductFormState
    data class Ready(
        val draft: ProductDraft,
        val categories: List<CategoryDto>,
        val brands: List<BrandDto>,
        val editing: Boolean,
        val errors: Map<String, String> = emptyMap(),
        val formError: String? = null,
        val saving: Boolean = false,
        val sessionExpired: Boolean = false,
    ) : ProductFormState
    data class Failed(val message: String) : ProductFormState
    data class Saved(val productId: String) : ProductFormState
}

@HiltViewModel class ProductFormViewModel @Inject constructor(private val repository: ProductsRepository) : ViewModel() {
    private val _state = MutableStateFlow<ProductFormState>(ProductFormState.Loading)
    val state = _state.asStateFlow()
    private var initialCostPerPack: String? = null

    fun load(id: String?) = viewModelScope.launch {
        _state.value = ProductFormState.Loading
        try {
            val categories = repository.categories()
            val brands = repository.brands()
            if (id == null) {
                _state.value = ProductFormState.Ready(ProductDraft(categoryId = categories.firstOrNull()?.id.orEmpty(), brandId = brands.firstOrNull()?.id.orEmpty()), categories, brands, editing = false)
            } else {
                val details = repository.getById(id) ?: run { _state.value = ProductFormState.Failed("Product not found."); return@launch }
                val product = details.product
                initialCostPerPack = product.costPerPack
                _state.value = ProductFormState.Ready(
                    ProductDraft(
                        name = product.name, brandId = product.brandId, categoryId = product.categoryId,
                        packName = product.packName, size = product.size.orEmpty(), piecesPerPack = product.piecesPerPack?.toString().orEmpty(),
                        packsPerBag = product.packsPerBag.toString(), retailPrice = product.retailPrice, minRetailPrice = product.minRetailPrice,
                        bagPrice = product.bagPrice, minBagPrice = product.minBagPrice, halfBagPrice = product.halfBagPrice.orEmpty(),
                        minHalfBagPrice = product.minHalfBagPrice.orEmpty(), costPerPack = product.costPerPack.orEmpty(), active = product.active,
                    ), categories, brands, editing = true,
                )
            }
        } catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) { _state.value = ProductFormState.Failed(if (e.code() == 403) "Only an owner can create or edit products." else "Product form could not be loaded (${e.code()}).") }
        catch (e: java.io.IOException) { _state.value = ProductFormState.Failed("Couldn’t connect. Check your internet and retry.") }
        catch (_: Exception) { _state.value = ProductFormState.Failed("Product form could not be loaded. Try again.") }
    }

    fun updateDraft(transform: (ProductDraft) -> ProductDraft) = _state.update { current ->
        if (current is ProductFormState.Ready) current.copy(draft = transform(current.draft), errors = emptyMap(), formError = null) else current
    }

    fun save(id: String?) = viewModelScope.launch {
        val current = _state.value as? ProductFormState.Ready ?: return@launch
        val errors = validateProductDraft(current.draft)
        if (errors.isNotEmpty()) { _state.value = current.copy(errors = errors); return@launch }
        _state.value = current.copy(saving = true, formError = null, errors = emptyMap())
        val draft = current.draft
        val cost = draft.costPerPack.trim().takeIf { it.isNotEmpty() && (id == null || it != initialCostPerPack.orEmpty()) }
        val mutation = ProductMutation(
            name = draft.name.trim(), categoryId = draft.categoryId, brandId = draft.brandId,
            packName = draft.packName, size = draft.size.trim().ifEmpty { null },
            packsPerBag = draft.packsPerBag.toInt(), piecesPerPack = draft.piecesPerPack.trim().toIntOrNull(),
            bagPrice = normalizeDefaultPrice(draft.bagPrice), minBagPrice = draft.minBagPrice.trim(),
            halfBagPrice = draft.halfBagPrice.trim().takeIf { it.isNotEmpty() }?.let(::normalizeDefaultPrice), minHalfBagPrice = draft.minHalfBagPrice.trim().ifEmpty { null },
            retailPrice = normalizeDefaultPrice(draft.retailPrice), minRetailPrice = draft.minRetailPrice.trim(), costPerPack = cost, active = draft.active,
        )
        try { _state.value = ProductFormState.Saved(repository.save(id, mutation).id) }
        catch (e: CancellationException) { throw e }
        catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "The product details were rejected. Check the values and try again."
                401 -> "Your session has expired."
                403 -> "Only a shop owner can create or edit products."
                else -> "Product could not be saved (${e.code()})."
            }
            _state.value = current.copy(saving = false, formError = message, sessionExpired = e.code() == 401)
        } catch (e: java.io.IOException) { _state.value = current.copy(saving = false, formError = "Couldn’t connect. Retry to save the product.") }
        catch (_: Exception) { _state.value = current.copy(saving = false, formError = "Product could not be saved. Try again.") }
    }

    private fun normalizeDefaultPrice(value: String): String = value.trim().toBigDecimal().stripTrailingZeros().toPlainString()
}
