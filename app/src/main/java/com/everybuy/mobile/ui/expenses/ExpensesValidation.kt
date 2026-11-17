package com.everybuy.mobile.ui.expenses

import com.everybuy.mobile.data.ExpenseDto
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

const val EXPENSE_TITLE_MAX = 120
const val EXPENSE_CATEGORY_MAX = 80
const val EXPENSE_NOTES_MAX = 500

private val amountPattern = Regex("""^(?=.*[1-9])\d{1,12}(\.\d{1,2})?$""")

val suggestedExpenseCategories = listOf("Stock purchase", "Transport", "Rent", "Utilities", "Salaries", "Packaging", "Other")

/** Mirrors the backend expense rules so failures are caught before the request. */
fun validateExpense(title: String, category: String, amount: String, notes: String): String? {
    if (title.isBlank()) return "Enter what the expense was for."
    if (title.trim().length > EXPENSE_TITLE_MAX) return "Keep the title to $EXPENSE_TITLE_MAX characters or fewer."
    if (category.isBlank()) return "Enter a category."
    if (category.trim().length > EXPENSE_CATEGORY_MAX) return "Keep the category to $EXPENSE_CATEGORY_MAX characters or fewer."
    if (!amountPattern.matches(amount.trim())) return "Enter an amount greater than zero with at most 2 decimal places."
    if (notes.trim().length > EXPENSE_NOTES_MAX) return "Keep the notes to $EXPENSE_NOTES_MAX characters or fewer."
    return null
}

fun lagosMonthOf(isoDateTime: String): String? = runCatching {
    YearMonth.from(Instant.parse(isoDateTime).atZone(ZoneId.of("Africa/Lagos"))).toString()
}.getOrNull()

fun expenseTotal(expenses: List<ExpenseDto>, month: String? = null): BigDecimal = expenses.fold(BigDecimal.ZERO) { total, expense ->
    if (month != null && lagosMonthOf(expense.spentAt) != month) total
    else total + (expense.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
}
