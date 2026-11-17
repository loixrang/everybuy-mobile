package com.everybuy.mobile.ui.customers

import com.everybuy.mobile.data.CustomerStatementDto
import com.everybuy.mobile.data.PaymentDto
import com.everybuy.mobile.data.StatementSaleDto
import java.math.BigDecimal

const val CUSTOMER_NAME_MAX = 120
const val CUSTOMER_PHONE_MAX = 40
const val CUSTOMER_ADDRESS_MAX = 200
const val CUSTOMER_NOTES_MAX = 500

private val repaymentPattern = Regex("""^(?=.*[1-9])\d{1,12}(\.\d{1,2})?$""")

fun validateCustomerName(value: String): String? = when {
    value.isBlank() -> "Enter the customer’s name."
    value.trim().length > CUSTOMER_NAME_MAX -> "Keep the name within $CUSTOMER_NAME_MAX characters."
    else -> null
}

fun validateCustomerPhone(value: String): String? =
    if (value.trim().length > CUSTOMER_PHONE_MAX) "Keep the phone number within $CUSTOMER_PHONE_MAX characters." else null

fun validateCustomerAddress(value: String): String? =
    if (value.trim().length > CUSTOMER_ADDRESS_MAX) "Keep the address within $CUSTOMER_ADDRESS_MAX characters." else null

fun validateCustomerNotes(value: String): String? =
    if (value.trim().length > CUSTOMER_NOTES_MAX) "Keep notes within $CUSTOMER_NOTES_MAX characters." else null

/** Mirrors the backend repayment rules before the request is sent. */
fun validateRepaymentAmount(amount: String, balanceDue: String): String? {
    val value = amount.trim()
    if (value.isEmpty()) return "Enter the repayment amount."
    if (!repaymentPattern.matches(value)) return "Amount must be greater than zero with at most 2 decimal places."
    val requested = value.toBigDecimalOrNull() ?: return "Enter a valid repayment amount."
    val balance = balanceDue.toBigDecimalOrNull() ?: BigDecimal.ZERO
    if (requested > balance) return "The repayment cannot exceed the outstanding balance."
    return null
}

fun outstandingStatementSales(statement: CustomerStatementDto): List<StatementSaleDto> =
    statement.sales.filter { (it.balanceDue.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO }

fun customerPaymentHistory(statement: CustomerStatementDto): List<PaymentDto> =
    statement.sales.flatMap { it.payments }.sortedByDescending { it.paidAt }

fun saleShortReference(id: String): String = "#${id.take(8)}"
