package com.everybuy.mobile.ui.expenses

import com.everybuy.mobile.data.ExpenseDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class ExpenseValidationTest {
    @Test fun `accepts a valid expense`() {
        assertNull(validateExpense("Diesel for generator", "Utilities", "12500.50", "Filled the tank"))
    }

    @Test fun `rejects missing title and category`() {
        assertEquals("Enter what the expense was for.", validateExpense("  ", "Transport", "100", ""))
        assertEquals("Enter a category.", validateExpense("Fuel", " ", "100", ""))
    }

    @Test fun `rejects zero and over-precise amounts`() {
        assertEquals("Enter an amount greater than zero with at most 2 decimal places.", validateExpense("Fuel", "Transport", "0", ""))
        assertEquals("Enter an amount greater than zero with at most 2 decimal places.", validateExpense("Fuel", "Transport", "0.00", ""))
        assertEquals("Enter an amount greater than zero with at most 2 decimal places.", validateExpense("Fuel", "Transport", "10.555", ""))
        assertEquals("Enter an amount greater than zero with at most 2 decimal places.", validateExpense("Fuel", "Transport", "-5", ""))
    }

    @Test fun `enforces backend length limits`() {
        assertEquals("Keep the title to 120 characters or fewer.", validateExpense("a".repeat(121), "Transport", "100", ""))
        assertEquals("Keep the category to 80 characters or fewer.", validateExpense("Fuel", "b".repeat(81), "100", ""))
        assertEquals("Keep the notes to 500 characters or fewer.", validateExpense("Fuel", "Transport", "100", "c".repeat(501)))
    }

    @Test fun `totals filter by Lagos month`() {
        val expenses = listOf(
            ExpenseDto(id = "1", amount = "100.50", spentAt = "2026-11-17T09:00:00.000Z"),
            ExpenseDto(id = "2", amount = "200.25", spentAt = "2026-11-30T23:30:00.000Z"),
            ExpenseDto(id = "3", amount = "50", spentAt = "2026-12-01T00:30:00.000Z"),
        )
        assertEquals(BigDecimal("100.50"), expenseTotal(expenses, "2026-11"))
        assertEquals(BigDecimal("250.25"), expenseTotal(expenses, "2026-12"))
        assertEquals(BigDecimal("350.75"), expenseTotal(expenses))
        assertEquals("2026-12", lagosMonthOf("2026-11-30T23:30:00.000Z"))
    }
}
