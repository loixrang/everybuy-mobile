package com.everybuy.mobile.ui.stocktakes

import com.everybuy.mobile.data.StockTakeItemDto

/** Matches the backend `findStockTakesSchema` default and maximum page size. */
const val STOCK_TAKE_PAGE_SIZE = 20

/** Matches the backend `createStockTakeSchema` limit. */
const val MAX_STOCK_TAKE_NAME_LENGTH = 100

/** Largest value PostgreSQL `integer` accepts for a count. */
const val MAX_COUNTED_PACKS = 2_147_483_647

data class StockTakeCountValidation(val countedPacks: Int? = null, val message: String? = null)

fun validateStockTakeName(value: String): String? {
    val trimmed = value.trim()
    return when {
        trimmed.isEmpty() -> "Give this stocktake a name, for example “October stock count”."
        trimmed.length > MAX_STOCK_TAKE_NAME_LENGTH -> "The name must be $MAX_STOCK_TAKE_NAME_LENGTH characters or fewer."
        else -> null
    }
}

fun parseWholeNumber(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.any { !it.isDigit() }) return null
    return trimmed.toIntOrNull()
}

/**
 * Validates the bag/loose-pack inputs used by the count dialog and converts
 * them to the single pack total the backend accepts as a JSON number.
 */
fun validateStockTakeCount(bags: String, loosePacks: String, packsPerBag: Int): StockTakeCountValidation {
    val bagCount = parseWholeNumber(bags)
        ?: return StockTakeCountValidation(message = "Enter a whole number of bags (0 or more).")
    val looseCount = parseWholeNumber(loosePacks)
        ?: return StockTakeCountValidation(message = "Enter a whole number of loose packs (0 or more).")
    if (packsPerBag <= 0) {
        return StockTakeCountValidation(message = "This product has no valid bag conversion. Ask an owner to fix it.")
    }
    val total = bagCount.toLong() * packsPerBag + looseCount
    if (total > MAX_COUNTED_PACKS) {
        return StockTakeCountValidation(message = "The total count must be ${"%,d".format(MAX_COUNTED_PACKS)} packs or fewer.")
    }
    return StockTakeCountValidation(countedPacks = total.toInt())
}

fun stockTakeStatusLabel(status: String): String = when (status) {
    "Draft" -> "In progress"
    "Finalized" -> "Approved"
    "Cancelled" -> "Cancelled"
    else -> status
}

fun isDraftStockTake(status: String): Boolean = status == "Draft"

fun differenceFromExpected(item: StockTakeItemDto): Int? =
    item.countedPacks?.let { it - item.expectedPacks }

fun formatPackDifference(difference: Int): String {
    if (difference == 0) return "0"
    val sign = if (difference > 0) "+" else "-"
    val count = kotlin.math.abs(difference)
    return "$sign$count ${if (count == 1) "pack" else "packs"}"
}

fun stockTakeProgress(countedProducts: Int, totalProducts: Int): Int {
    if (totalProducts <= 0) return 0
    return ((countedProducts.toDouble() / totalProducts) * 100).toInt().coerceIn(0, 100)
}

fun countedByLabel(countedBy: String?, currentUserId: String?): String = when {
    countedBy.isNullOrBlank() -> "—"
    countedBy == currentUserId -> "You"
    else -> "Staff #${countedBy.take(8)}"
}
