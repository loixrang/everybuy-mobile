package com.everybuy.mobile.data

import com.everybuy.mobile.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

class ApiConfigurationTest {
    private val apiBaseUrl = BuildConfig.API_BASE_URL.toHttpUrl()

    @Test
    fun usesDeployedHttpsApiRoot() {
        assertEquals("https", apiBaseUrl.scheme)
        assertEquals("api.everybuy.loixrang.com", apiBaseUrl.host)
        assertEquals("/api/v1/", apiBaseUrl.encodedPath)
    }

    @Test
    fun everyRetrofitEndpointResolvesUnderOneApiPrefix() {
        val apiTypes = listOf(
            AuthApi::class.java,
            ProductsApi::class.java,
            CatalogApi::class.java,
            ReportsApi::class.java,
            MeApi::class.java,
            StockApi::class.java,
            SuppliersApi::class.java,
            SalesApi::class.java,
            CustomersApi::class.java,
            StockTakesApi::class.java,
            ExpensesApi::class.java,
            AuditApi::class.java,
            AdminSettingsApi::class.java,
            AdminStaffApi::class.java,
            AdminCatalogApi::class.java,
            AdminSuppliersApi::class.java,
            ProfileApi::class.java,
        )

        val endpointPaths = apiTypes.flatMap { apiType ->
            apiType.declaredMethods.mapNotNull { method ->
                method.getAnnotation(GET::class.java)?.value
                    ?: method.getAnnotation(POST::class.java)?.value
                    ?: method.getAnnotation(PATCH::class.java)?.value
                    ?: method.getAnnotation(DELETE::class.java)?.value
            }
        }

        assertTrue("Expected Retrofit endpoints to be checked", endpointPaths.isNotEmpty())
        endpointPaths.forEach { path ->
            assertFalse("Endpoint must be relative: $path", path.startsWith('/'))
            assertFalse("Endpoint must not repeat api/v1: $path", path.startsWith("api/v1/"))
            val requestUrl = requireNotNull(apiBaseUrl.resolve(path))
            assertEquals("https", requestUrl.scheme)
            assertEquals("api.everybuy.loixrang.com", requestUrl.host)
            assertTrue(requestUrl.encodedPath.startsWith("/api/v1/"))
            assertFalse(requestUrl.encodedPath.startsWith("/api/v1/api/v1/"))
        }

        val sessionPath = requireNotNull(
            AuthApi::class.java.declaredMethods
                .single { it.name == "getSession" }
                .getAnnotation(GET::class.java),
        ).value
        assertEquals(
            "https://api.everybuy.loixrang.com/api/v1/auth/get-session",
            apiBaseUrl.resolve(sessionPath).toString(),
        )
    }
}
