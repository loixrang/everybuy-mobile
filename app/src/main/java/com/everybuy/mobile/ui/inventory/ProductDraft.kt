package com.everybuy.mobile.ui.inventory

import java.math.BigDecimal

data class ProductDraft(
    val name: String = "", val brandId: String = "", val categoryId: String = "",
    val packName: String = "Small Pack", val size: String = "", val piecesPerPack: String = "",
    val packsPerBag: String = "", val retailPrice: String = "", val minRetailPrice: String = "",
    val bagPrice: String = "", val minBagPrice: String = "", val halfBagPrice: String = "",
    val minHalfBagPrice: String = "", val costPerPack: String = "", val active: Boolean = true,
)

fun validateProductDraft(draft: ProductDraft): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (draft.name.trim().isEmpty()) errors["name"] = "Product name is required."
    if (draft.brandId.isBlank()) errors["brandId"] = "Choose a brand."
    if (draft.categoryId.isBlank()) errors["categoryId"] = "Choose a category."
    if (draft.packName !in listOf("Small Pack", "Twin Pack", "Jumbo Pack", "Wipes", "Pad", "Tissue")) errors["packName"] = "Choose a product pack type."
    val packs = draft.packsPerBag.toIntOrNull()
    if (packs == null || packs <= 0) errors["packsPerBag"] = "Enter a positive whole number of packs."
    if (draft.piecesPerPack.isNotBlank() && (draft.piecesPerPack.toIntOrNull() ?: 0) <= 0) errors["piecesPerPack"] = "Pieces per pack must be a positive whole number."
    validatePrice(draft.retailPrice, "retailPrice", "Pack price", errors)
    validatePrice(draft.bagPrice, "bagPrice", "Bag price", errors)
    validateMinimum(draft.minRetailPrice, "minRetailPrice", "Minimum pack price", draft.retailPrice, errors)
    validateMinimum(draft.minBagPrice, "minBagPrice", "Minimum bag price", draft.bagPrice, errors)
    val hasHalfValue = draft.halfBagPrice.isNotBlank() || draft.minHalfBagPrice.isNotBlank()
    if (hasHalfValue) {
        validatePrice(draft.halfBagPrice, "halfBagPrice", "Half-bag price", errors)
        validateMinimum(draft.minHalfBagPrice, "minHalfBagPrice", "Minimum half-bag price", draft.halfBagPrice, errors)
        if (packs != null && packs % 2 != 0) errors["packsPerBag"] = "Half Bag requires an even number of packs per bag."
    }
    if (draft.costPerPack.isNotBlank() && !Regex("^(?=.*[1-9])\\d{1,10}(\\.\\d{1,4})?$").matches(draft.costPerPack.trim())) {
        errors["costPerPack"] = "Enter a positive acquisition cost with up to 4 decimal places."
    }
    return errors
}

private fun validatePrice(value: String, field: String, label: String, errors: MutableMap<String, String>): BigDecimal? {
    val parsed = value.trim().toBigDecimalOrNull()
    if (parsed == null || parsed <= BigDecimal.ZERO) {
        errors[field] = "$label must be greater than zero."
        return null
    }
    return parsed
}

private fun validateMinimum(value: String, field: String, label: String, defaultValue: String, errors: MutableMap<String, String>) {
    val minimum = value.trim().toBigDecimalOrNull()
    val price = defaultValue.trim().toBigDecimalOrNull()
    if (minimum == null || minimum <= BigDecimal.ZERO || price == null || minimum > price) {
        errors[field] = "$label must be positive and no higher than the default price."
    }
}
