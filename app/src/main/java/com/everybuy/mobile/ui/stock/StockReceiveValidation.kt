package com.everybuy.mobile.ui.stock

import com.everybuy.mobile.data.ProductDto

data class StockReceiveQuantity(val bags: Int, val loosePacks: Int, val totalPacks: Int)

fun validateStockReceive(product: ProductDto?, bagsValue: String, looseValue: String, unitCost: String): Pair<StockReceiveQuantity?, Map<String, String>> {
    val errors = mutableMapOf<String, String>()
    if (product == null) errors["product"] = "Choose a product."
    val bags = bagsValue.toIntOrNull()
    if (bags == null || bags < 0) errors["bags"] = "Enter zero or a positive whole number of bags."
    val loose = looseValue.toIntOrNull()
    if (loose == null || loose < 0) errors["loosePacks"] = "Enter zero or a positive whole number of loose packs."
    val cost = unitCost.trim()
    if (!Regex("^(?=.*[1-9])\\d{1,12}(\\.\\d{1,2})?$").matches(cost)) errors["unitCost"] = "Enter a positive cost per pack with up to 2 decimal places."
    if (product != null && bags != null && loose != null && bags >= 0 && loose >= 0) {
        val total = bags.toLong() * product.packsPerBag.toLong() + loose
        if (total <= 0) errors["quantity"] = "Receive at least one bag or loose pack."
        if (total > 1_000_000L) errors["quantity"] = "The total cannot exceed 1,000,000 packs."
        if (total in 1..1_000_000) return StockReceiveQuantity(bags, loose, total.toInt()) to errors
    }
    return null to errors
}
