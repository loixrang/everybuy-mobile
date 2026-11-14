package com.everybuy.mobile.data

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthModelsTest {
    private val parser = Json { ignoreUnknownKeys = true }

    @Test fun `sign in keeps only the session user and detects two factor`() {
        val login = parser.decodeFromString<SignInPayload>("""{"redirect":false,"token":"ignored","user":{"id":"u1","name":"Ada","email":"ada@example.com","emailVerified":true}}""")
        assertEquals("ada@example.com", login.user?.email)
        assertTrue(!login.twoFactorRedirect)

        val twoFactor = parser.decodeFromString<SignInPayload>("""{"twoFactorRedirect":true,"twoFactorMethods":["totp"]}""")
        assertTrue(twoFactor.twoFactorRedirect)
    }

    @Test fun `session accepts extra Better Auth fields`() {
        val session = parser.decodeFromString<SessionPayload>("""{"session":{"id":"s1","expiresAt":"2026-11-15T00:00:00.000Z","token":"opaque"},"user":{"id":"u1","name":"Ada","email":"ada@example.com","role":"owner"}}""")
        assertEquals("u1", session.user.id)
        assertEquals("2026-11-15T00:00:00.000Z", session.session.expiresAt)
    }

    @Test fun `product preserves backend decimal prices and pack stock`() {
        val product = parser.decodeFromString<ProductDto>("""{"id":"p1","name":"Rice","packName":"Small Pack","bagPrice":"12500.50","retailPrice":"900.00","stockPacks":27,"active":true}""")
        assertEquals("12500.50", product.bagPrice)
        assertEquals("900.00", product.retailPrice)
        assertEquals(27, product.stockPacks)
    }

    @Test fun `daily overview matches reports response and preserves decimal totals`() {
        val report = parser.decodeFromString<DailyOverview>("""{"salesToday":{"count":2,"total":"7500.00"},"paymentsReceived":"5000.00","customerBalances":"2500.00","lowStock":[{"productId":"p1","name":"Rice","packsAvailable":4}],"recentSales":[{"id":"s1","reference":"SL-0001","soldAt":"2026-11-14T10:00:00.000Z","customer":null,"itemsTotal":"7500.00","amountPaid":"5000.00","balance":"2500.00"}]}""")
        assertEquals(2, report.salesToday.count)
        assertEquals("7500.00", report.salesToday.total)
        assertEquals("2500.00", report.recentSales.single().balance)
        assertEquals(4, report.lowStock.single().packsAvailable)
    }
}
