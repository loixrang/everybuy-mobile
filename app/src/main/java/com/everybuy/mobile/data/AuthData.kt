package com.everybuy.mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.secureStore by preferencesDataStore("everybuy_secure")
private val blobKey = stringPreferencesKey("cookie_blob")
private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

@Serializable data class SignInRequest(val email: String, val password: String)
@Serializable data class CodeRequest(val code: String)
@Serializable data class AuthUser(val id: String = "", val name: String = "", val email: String = "")
@Serializable data class AuthSession(val expiresAt: String = "")
@Serializable data class SessionPayload(val session: AuthSession, val user: AuthUser)
@Serializable data class SignInPayload(val redirect: Boolean = false, val token: String? = null, val user: AuthUser? = null, val twoFactorRedirect: Boolean = false)
@Serializable data class StaffProfile(val userId: String, val staffId: String, val name: String, val email: String, val role: String, val active: Boolean)

interface AuthApi {
    @retrofit2.http.POST("api/v1/auth/sign-in/email") suspend fun signIn(@retrofit2.http.Body body: SignInRequest): SignInPayload
    @retrofit2.http.GET("api/v1/auth/get-session") suspend fun getSession(): JsonElement?
    @retrofit2.http.POST("api/v1/auth/sign-out") suspend fun signOut(): JsonElement
    @retrofit2.http.POST("api/v1/auth/two-factor/verify-totp") suspend fun verifyTotp(@retrofit2.http.Body body: CodeRequest): SignInPayload
}

@Serializable data class ProductDto(
    val id: String, val name: String, val categoryId: String = "", val brandId: String = "",
    val packName: String = "", val size: String? = null, val packsPerBag: Int = 1,
    val piecesPerPack: Int? = null, val bagPrice: String = "0", val minBagPrice: String = "0",
    val halfBagPrice: String? = null, val minHalfBagPrice: String? = null,
    val retailPrice: String = "0", val minRetailPrice: String = "0", val costPerPack: String? = null,
    val stockPacks: Int = 0, val active: Boolean = true, val createdAt: String = "", val updatedAt: String = "",
)

interface ProductsApi {
    @retrofit2.http.GET("api/v1/products") suspend fun products(@retrofit2.http.Query("page") page: Int = 1, @retrofit2.http.Query("limit") limit: Int = 100): List<ProductDto>
    @retrofit2.http.GET("api/v1/products/{id}") suspend fun product(@retrofit2.http.Path("id") id: String): List<ProductDto>
    @retrofit2.http.POST("api/v1/products") suspend fun create(@retrofit2.http.Body body: JsonObject): List<ProductDto>
    @retrofit2.http.PATCH("api/v1/products/{id}") suspend fun update(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): List<ProductDto>
}
@Serializable data class CategoryDto(val id: String, val name: String, val active: Boolean = true)
@Serializable data class BrandDto(val id: String, val name: String, val active: Boolean = true)
@Serializable data class ProductInventory(val products: List<ProductDto>, val categories: List<CategoryDto>, val brands: List<BrandDto>)
data class ProductDetails(val product: ProductDto, val categoryName: String?, val brandName: String?)
data class ProductMutation(
    val name: String, val categoryId: String, val brandId: String, val packName: String,
    val size: String?, val packsPerBag: Int, val piecesPerPack: Int?,
    val bagPrice: String, val minBagPrice: String, val halfBagPrice: String?, val minHalfBagPrice: String?,
    val retailPrice: String, val minRetailPrice: String, val costPerPack: String?, val active: Boolean,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name)); put("categoryId", JsonPrimitive(categoryId)); put("brandId", JsonPrimitive(brandId))
        put("packName", JsonPrimitive(packName)); put("size", size?.let { JsonPrimitive(it) } ?: JsonNull)
        put("packsPerBag", JsonPrimitive(packsPerBag)); put("piecesPerPack", piecesPerPack?.let { JsonPrimitive(it) } ?: JsonNull)
        put("bagPrice", JsonPrimitive(bagPrice)); put("minBagPrice", JsonPrimitive(minBagPrice))
        put("halfBagPrice", halfBagPrice?.let { JsonPrimitive(it) } ?: JsonNull); put("minHalfBagPrice", minHalfBagPrice?.let { JsonPrimitive(it) } ?: JsonNull)
        put("retailPrice", JsonPrimitive(retailPrice)); put("minRetailPrice", JsonPrimitive(minRetailPrice))
        costPerPack?.let { put("costPerPack", JsonPrimitive(it)) }
        put("active", JsonPrimitive(active))
    }
}
interface CatalogApi {
    @retrofit2.http.GET("api/v1/categories") suspend fun categories(): List<CategoryDto>
    @retrofit2.http.GET("api/v1/brands") suspend fun brands(): List<BrandDto>
}

@Serializable data class OverviewSalesToday(val count: Int = 0, val total: String = "0")
@Serializable data class OverviewLowStock(val productId: String, val name: String, val packsAvailable: Int)
@Serializable data class OverviewCustomer(val id: String? = null, val name: String? = null)
@Serializable data class OverviewRecentSale(val id: String, val reference: String, val soldAt: String, val customer: OverviewCustomer? = null, val itemsTotal: String, val amountPaid: String, val balance: String)
@Serializable data class DailyOverview(val salesToday: OverviewSalesToday, val paymentsReceived: String, val customerBalances: String, val lowStock: List<OverviewLowStock> = emptyList(), val recentSales: List<OverviewRecentSale> = emptyList())

interface ReportsApi { @retrofit2.http.GET("api/v1/reports/overview") suspend fun dailyOverview(): DailyOverview }
interface MeApi { @retrofit2.http.GET("api/v1/me") suspend fun me(): List<StaffProfile> }

@Serializable data class StockMovementDto(
    val id: String, val productId: String, val type: String, val changePacks: Int,
    val supplierId: Int? = null, val saleItemId: String? = null, val requestId: String? = null,
    val costPerPack: String? = null, val note: String? = null, val createdBy: String = "", val createdAt: String,
)
@Serializable data class StockMovementsPageDto(val data: List<StockMovementDto>, val page: Int, val limit: Int, val total: Int)
@Serializable data class SupplierDto(val id: Int, val name: String, val phone: String? = null, val active: Boolean = true)
@Serializable data class UpdatedProductStockDto(val id: String, val stockPacks: Int)
@Serializable data class StockMutationResultDto(val updateProduct: List<UpdatedProductStockDto> = emptyList(), val replayed: Boolean = false)
interface StockApi {
    @retrofit2.http.GET("api/v1/stock/movements") suspend fun movements(@retrofit2.http.Query("page") page: Int = 1, @retrofit2.http.Query("limit") limit: Int = 50): StockMovementsPageDto
    @retrofit2.http.POST("api/v1/stock/additions") suspend fun addStock(@retrofit2.http.Body body: JsonObject): StockMutationResultDto
}
interface SuppliersApi { @retrofit2.http.GET("api/v1/suppliers") suspend fun suppliers(): List<SupplierDto> }

@Singleton
class EncryptedCookieJar @Inject constructor(@ApplicationContext private val context: Context) : CookieJar {
    private data class CookieEntry(val originKey: String, val cookie: Cookie)
    private val cookies = mutableListOf<CookieEntry>()
    private val lock = Any()

    init { synchronized(lock) { cookies.addAll(readCookies()) } }
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) = synchronized(lock) {
        val originKey = originKey(url)
        this.cookies.removeAll { old -> old.originKey == originKey && cookies.any { it.name == old.cookie.name && it.domain == old.cookie.domain && it.path == old.cookie.path } }
        this.cookies.addAll(cookies.filter { it.expiresAt > System.currentTimeMillis() }.map { CookieEntry(originKey, it) })
        persist()
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        cookies.removeAll { it.cookie.expiresAt <= System.currentTimeMillis() }
        persist()
        val originKey = originKey(url)
        cookies.filter { it.originKey == originKey && it.cookie.matches(url) }.map { it.cookie }
    }
    fun clear() = synchronized(lock) { cookies.clear(); persist() }

    private fun persist() = runBlocking {
        val payload = json.encodeToString(cookies.filter { it.cookie.expiresAt > System.currentTimeMillis() }.map { entry ->
            entry.cookie.let { StoredCookie(entry.originKey, it.name, it.value, it.domain, it.path, it.expiresAt, it.secure, it.httpOnly, it.hostOnly) }
        })
        context.secureStore.edit { it[blobKey] = encrypt(payload.toByteArray()).joinToString("") { b -> "%02x".format(b) } }
    }
    private fun readCookies(): List<Cookie> = runBlocking {
        val value = context.secureStore.data.first()[blobKey] ?: return@runBlocking emptyList()
        runCatching {
            val bytes = value.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            val decoded = json.decodeFromString<List<StoredCookie>>(String(decrypt(bytes)))
            decoded.mapNotNull { c -> runCatching {
                val cookie = Cookie.Builder().name(c.name).value(c.value).apply { if (c.hostOnly) hostOnlyDomain(c.domain) else domain(c.domain) }.path(c.path).expiresAt(c.expiresAt).apply { if (c.secure) secure(); if (c.httpOnly) httpOnly() }.build()
                CookieEntry(c.originKey, cookie)
            }.getOrNull() }
                .filter { it.originKey.isNotBlank() && it.cookie.expiresAt > System.currentTimeMillis() }
        }.getOrElse { context.secureStore.edit { it.remove(blobKey) }; emptyList() }
    }
    private fun encrypt(input: ByteArray): ByteArray { val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key()); val iv = cipher.iv; return iv + cipher.doFinal(input) }
    private fun decrypt(input: ByteArray): ByteArray { val cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, input.copyOfRange(0, 12))); return cipher.doFinal(input.copyOfRange(12, input.size)) }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("everybuy.cookie.v1", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("everybuy.cookie.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }
    private fun originKey(url: HttpUrl) = "${url.scheme}://${url.host}:${url.port}"
}

@Serializable private data class StoredCookie(val originKey: String = "", val name: String, val value: String, val domain: String, val path: String, val expiresAt: Long, val secure: Boolean, val httpOnly: Boolean, val hostOnly: Boolean)

class OriginInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = chain.proceed(chain.request().newBuilder().header("Origin", "everybuy://app").header("Accept", "application/json").build())
}

@Module @InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton fun client(jar: EncryptedCookieJar): okhttp3.OkHttpClient {
        val builder = okhttp3.OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(OriginInterceptor())
        if (com.everybuy.mobile.BuildConfig.DEBUG) {
            val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
                level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
                redactHeader("Authorization")
            }
            builder.addInterceptor(logging)
        }
        return builder.build()
    }
    @Provides @Singleton fun authApi(client: okhttp3.OkHttpClient): AuthApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(AuthApi::class.java)
    @Provides @Singleton fun productsApi(client: okhttp3.OkHttpClient): ProductsApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(ProductsApi::class.java)
    @Provides @Singleton fun catalogApi(client: okhttp3.OkHttpClient): CatalogApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(CatalogApi::class.java)
    @Provides @Singleton fun reportsApi(client: okhttp3.OkHttpClient): ReportsApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(ReportsApi::class.java)
    @Provides @Singleton fun meApi(client: okhttp3.OkHttpClient): MeApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(MeApi::class.java)
    @Provides @Singleton fun stockApi(client: okhttp3.OkHttpClient): StockApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(StockApi::class.java)
    @Provides @Singleton fun suppliersApi(client: okhttp3.OkHttpClient): SuppliersApi = Retrofit.Builder().baseUrl(com.everybuy.mobile.BuildConfig.API_BASE_URL).client(client).addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(SuppliersApi::class.java)
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val user: AuthUser, val staff: StaffProfile) : AuthState
    data class Error(val message: String, val retrySession: Boolean = false, val twoFactor: Boolean = false) : AuthState
    data object TwoFactor : AuthState
    data object NoStaffAccess : AuthState
}

@Singleton class AuthRepository @Inject constructor(private val api: AuthApi, private val meApi: MeApi, private val jar: EncryptedCookieJar) {
    suspend fun restore(): AuthState = try {
        val payload = api.getSession()
        if (payload == null || payload is kotlinx.serialization.json.JsonNull) { jar.clear(); AuthState.SignedOut }
        else { val parsed = json.decodeFromJsonElement<SessionPayload>(payload); resolveStaff(parsed.user) }
    } catch (e: java.io.IOException) { AuthState.Error("Can’t connect. Your saved session is still available to retry.", retrySession = true) }
      catch (e: retrofit2.HttpException) { AuthState.Error("Session check failed (${e.code()}). Retry when the service is available.", retrySession = true) }
      catch (e: kotlinx.coroutines.CancellationException) { throw e }
      catch (e: Exception) { AuthState.Error("Couldn’t read the session response. Retry to continue.", retrySession = true) }
    suspend fun signIn(email: String, password: String): AuthState = try {
        val result = api.signIn(SignInRequest(email.trim(), password))
        if (result.twoFactorRedirect) AuthState.TwoFactor else result.user?.let { resolveStaff(it) } ?: AuthState.Error("The server returned an incomplete sign-in response.")
    } catch (e: retrofit2.HttpException) { AuthState.Error("Sign-in failed (${e.code()}). Check your details and try again.") }
      catch (e: java.io.IOException) { AuthState.Error("Can’t connect. Check your internet and try again.") }
      catch (e: kotlinx.coroutines.CancellationException) { throw e }
    suspend fun verifyTotp(code: String): AuthState = try {
        val result = api.verifyTotp(CodeRequest(code.trim()))
        result.user?.let { resolveStaff(it) } ?: AuthState.Error("The server returned an incomplete verification response.")
    } catch (e: retrofit2.HttpException) { AuthState.Error("Verification failed (${e.code()}). Check the code and try again.", twoFactor = true) }
      catch (e: java.io.IOException) { AuthState.Error("Can’t connect. Check your internet and try again.", twoFactor = true) }
      catch (e: kotlinx.coroutines.CancellationException) { throw e }
    private suspend fun resolveStaff(user: AuthUser): AuthState = try {
        val staff = meApi.me().firstOrNull() ?: return AuthState.NoStaffAccess
        if (!staff.active) AuthState.NoStaffAccess else AuthState.SignedIn(user, staff)
    } catch (e: retrofit2.HttpException) {
        when (e.code()) {
            401 -> { jar.clear(); AuthState.SignedOut }
            403 -> AuthState.NoStaffAccess
            else -> AuthState.Error("Your account could not be verified (${e.code()}). Retry your session.", retrySession = true)
        }
    } catch (e: java.io.IOException) {
        AuthState.Error("Can’t connect to verify your staff access. Your saved session is available to retry.", retrySession = true)
    } catch (e: kotlinx.coroutines.CancellationException) { throw e }
      catch (e: Exception) { AuthState.Error("Your staff profile could not be read. Retry your session.", retrySession = true) }
    suspend fun signOut() {
        try { api.signOut() } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (_: Exception) { }
        finally { jar.clear() }
    }
}
