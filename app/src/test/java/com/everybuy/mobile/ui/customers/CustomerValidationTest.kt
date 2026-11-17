package com.everybuy.mobile.ui.customers

import com.everybuy.mobile.data.CustomerDto
import com.everybuy.mobile.data.CustomerStatementDto
import com.everybuy.mobile.data.CustomerStatementTotalsDto
import com.everybuy.mobile.data.PaymentDto
import com.everybuy.mobile.data.StatementSaleDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerValidationTest {
    @Test fun `customer name is required and length capped`() {
        assertNotNull(validateCustomerName("   "))
        assertNotNull(validateCustomerName("x".repeat(CUSTOMER_NAME_MAX + 1)))
        assertNull(validateCustomerName("  Itoro Essien  "))
    }

    @Test fun `optional contact fields respect the web limits`() {
        assertNull(validateCustomerPhone("0803 000 0000"))
        assertNotNull(validateCustomerPhone("1".repeat(CUSTOMER_PHONE_MAX + 1)))
        assertNotNull(validateCustomerAddress("x".repeat(CUSTOMER_ADDRESS_MAX + 1)))
        assertNotNull(validateCustomerNotes("x".repeat(CUSTOMER_NOTES_MAX + 1)))
    }

    @Test fun `repayment amount must be positive with at most two decimals`() {
        assertNotNull(validateRepaymentAmount("", "1000.00"))
        assertNotNull(validateRepaymentAmount("0", "1000.00"))
        assertNotNull(validateRepaymentAmount("0.00", "1000.00"))
        assertNotNull(validateRepaymentAmount("-5", "1000.00"))
        assertNotNull(validateRepaymentAmount("10.999", "1000.00"))
        assertNotNull(validateRepaymentAmount("1500", "1000.00"))
        assertNull(validateRepaymentAmount("1000", "1000.00"))
        assertNull(validateRepaymentAmount("250.50", "1000.00"))
    }

    @Test fun `statement helpers separate outstanding sales and payment history`() {
        val paid = StatementSaleDto(id = "s1", totalAmount = "1000.00", paidAmount = "1000.00", balanceDue = "0.00", paymentStatus = "PAID")
        val open = StatementSaleDto(
            id = "s2",
            totalAmount = "2000.00",
            paidAmount = "500.00",
            balanceDue = "1500.00",
            paymentStatus = "PART_PAID",
            payments = listOf(
                PaymentDto(id = 1, saleId = "s2", amount = "500.00", method = "cash", paidAt = "2026-11-17T10:00:00.000Z"),
            ),
        )
        val statement = CustomerStatementDto(
            customer = CustomerDto(id = "c1", name = "Ada"),
            totals = CustomerStatementTotalsDto(salesTotal = "3000.00", moneyReceived = "1500.00", outstandingDebt = "1500.00"),
            sales = listOf(open, paid),
        )

        assertEquals(listOf("s2"), outstandingStatementSales(statement).map { it.id })
        assertEquals(1, customerPaymentHistory(statement).size)
        assertEquals("#s2", saleShortReference("s2"))
        assertTrue(customerPaymentHistory(statement).single().amount == "500.00")
    }
}
