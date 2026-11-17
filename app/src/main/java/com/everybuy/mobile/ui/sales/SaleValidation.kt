package com.everybuy.mobile.ui.sales

import com.everybuy.mobile.data.ProductDto
import java.math.BigDecimal

const val MAX_SALE_LINES = 100
const val MAX_SALE_QUANTITY = 1_000_000
private val pricePattern = Regex("""^\d{1,12}(\.\d{1,2})?$""")

data class SaleLineDraft(
    val productId: String,
    val unit: String = "Pack",
    val quantity: Int = 1,
    val unitPrice: String = "",
)

data class SaleLineView(
    val draft: SaleLineDraft,
    val product: ProductDto,
    val lineTotal: BigDecimal,
    val packsRequired: Long,
)

fun saleUnitPrice(product: ProductDto, unit: String): String = when (unit) {
    "Bag" -> product.bagPrice
    "Half Bag" -> product.halfBagPrice ?: ""
    else -> product.retailPrice
}

fun saleUnitMinimumPrice(product: ProductDto, unit: String): String = when (unit) {
    "Bag" -> product.minBagPrice
    "Half Bag" -> product.minHalfBagPrice ?: ""
    else -> product.minRetailPrice
}

fun salePacksPerUnit(product: ProductDto, unit: String): Int = when (unit) {
    "Bag" -> product.packsPerBag
    "Half Bag" -> if (product.packsPerBag % 2 == 0) product.packsPerBag / 2 else 0
    else -> 1
}

fun isSaleUnitAvailable(product: ProductDto, unit: String): Boolean = when (unit) {
    "Bag" -> product.bagPrice.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true && product.packsPerBag > 0
    "Half Bag" -> product.halfBagPrice?.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true &&
        product.minHalfBagPrice?.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true &&
        product.packsPerBag > 0 && product.packsPerBag % 2 == 0
    "Pack" -> product.retailPrice.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
    else -> false
}

fun availableSaleUnits(product: ProductDto): List<String> =
    listOf("Pack", "Bag", "Half Bag").filter { isSaleUnitAvailable(product, it) }

fun maxSaleQuantity(product: ProductDto, unit: String): Int {
    val packsPerUnit = salePacksPerUnit(product, unit)
    if (packsPerUnit <= 0) return 0
    return (product.stockPacks / packsPerUnit).coerceIn(0, MAX_SALE_QUANTITY)
}

fun buildSaleLines(lines: List<SaleLineDraft>, products: Map<String, ProductDto>): List<SaleLineView> = lines.mapNotNull { line ->
    val product = products[line.productId] ?: return@mapNotNull null
    val price = line.unitPrice.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val packs = salePacksPerUnit(product, line.unit).toLong() * line.quantity
    SaleLineView(line, product, price.multiply(BigDecimal(line.quantity)), packs)
}

fun saleTotal(lines: List<SaleLineView>): BigDecimal = lines.fold(BigDecimal.ZERO) { total, line -> total + line.lineTotal }

fun isAmountFormatValid(value: String): Boolean = value.isBlank() || pricePattern.matches(value.trim())

/** Mirrors the backend sale rules so failures are caught before the request. */
fun validateSaleForSubmit(
    lines: List<SaleLineView>,
    amountPaid: String,
    customerId: String?,
): String? {
    if (lines.isEmpty()) return "Add at least one item to record a sale."
    if (lines.size > MAX_SALE_LINES) return "A sale can contain at most $MAX_SALE_LINES lines."
    if (lines.any { it.draft.quantity !in 1..MAX_SALE_QUANTITY }) return "Every item needs a whole quantity of at least 1."
    if (!isAmountFormatValid(amountPaid)) return "Amount paid must be a whole Naira amount with at most 2 decimal places."

    val demandedByProduct = mutableMapOf<String, Long>()
    for (line in lines) {
        if (!isSaleUnitAvailable(line.product, line.draft.unit)) return "${line.product.name} cannot be sold by ${line.draft.unit}."
        val price = line.draft.unitPrice.toBigDecimalOrNull()
        val minimum = saleUnitMinimumPrice(line.product, line.draft.unit).toBigDecimalOrNull()
        if (price == null || !pricePattern.matches(line.draft.unitPrice.trim())) return "Enter a valid price for ${line.product.name}."
        if (minimum != null && price < minimum) return "The price for ${line.product.name} is below its configured minimum of ₦${saleUnitMinimumPrice(line.product, line.draft.unit)}."
        demandedByProduct[line.product.id] = (demandedByProduct[line.product.id] ?: 0L) + line.packsRequired
    }
    for ((productId, demanded) in demandedByProduct) {
        val product = lines.first { it.product.id == productId }.product
        if (demanded > product.stockPacks) return "Not enough stock for ${product.name}: $demanded packs requested, ${product.stockPacks} available."
    }

    val paid = (if (amountPaid.isBlank()) "0" else amountPaid.trim()).toBigDecimalOrNull() ?: return "Amount paid must be a valid amount."
    val total = saleTotal(lines)
    if (paid > total) return "Amount paid cannot exceed the sale total."
    if (paid < total && customerId.isNullOrBlank()) return "Select a customer for a sale with an unpaid balance."
    return null
}

fun paymentStatusLabel(status: String): String = when (status) {
    "PAID" -> "Paid"
    "PARTIAL", "PART_PAID" -> "Part paid"
    else -> "Unpaid"
}

fun paymentMethodLabel(method: String): String = when (method) {
    "transfer" -> "Bank transfer"
    "pos" -> "POS"
    else -> "Cash"
}

fun normalisedPaymentStatus(status: String): String = when (status) {
    "PAID" -> "PAID"
    "PARTIAL", "PART_PAID" -> "PART_PAID"
    else -> "UNPAID"
}
