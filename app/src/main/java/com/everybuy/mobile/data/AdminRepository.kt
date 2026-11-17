package com.everybuy.mobile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Serializable data class ShopSettingsDto(
    val id: String,
    val name: String = "",
    val address: String? = null,
    val phone: String? = null,
    val currency: String = "NGN",
    val timezone: String = "Africa/Lagos",
    val receiptFooter: String? = null,
)

data class UpdateShopSettingsInput(
    val name: String,
    val address: String?,
    val phone: String?,
    val currency: String,
    val timezone: String,
    val receiptFooter: String?,
) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        put("address", address?.let { JsonPrimitive(it) } ?: JsonNull)
        put("phone", phone?.let { JsonPrimitive(it) } ?: JsonNull)
        put("currency", JsonPrimitive(currency))
        put("timezone", JsonPrimitive(timezone))
        put("receiptFooter", receiptFooter?.let { JsonPrimitive(it) } ?: JsonNull)
    }
}

@Serializable data class StaffMemberDto(
    val staffId: String,
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "Staff",
    val active: Boolean = true,
    val createdAt: String = "",
)

@Serializable data class StaffInvitationDto(
    val id: String,
    val name: String = "",
    val email: String = "",
    val status: String = "pending",
    val createdBy: String = "",
    val expiresAt: String = "",
    val acceptedAt: String? = null,
    val createdAt: String = "",
) {
    val pending: Boolean get() = status == "pending" || status == "processing"
}

@Serializable data class InviteStaffResultDto(val message: String = "", val email: String = "", val expiresAt: String = "")

data class UpdateStaffInput(val role: String? = null, val active: Boolean? = null) {
    fun toJson(): JsonObject = buildJsonObject {
        role?.let { put("role", JsonPrimitive(it)) }
        active?.let { put("active", JsonPrimitive(it)) }
    }
}

data class InviteStaffInput(val name: String, val email: String) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        put("email", JsonPrimitive(email))
    }
}

data class NamedInput(val name: String) {
    fun toJson(): JsonObject = buildJsonObject { put("name", JsonPrimitive(name)) }
}

data class ActiveInput(val active: Boolean) {
    fun toJson(): JsonObject = buildJsonObject { put("active", JsonPrimitive(active)) }
}

data class SupplierInput(val name: String, val phone: String?, val active: Boolean? = null) {
    fun toJson(): JsonObject = buildJsonObject {
        put("name", JsonPrimitive(name))
        phone?.let { put("phone", JsonPrimitive(it)) }
        active?.let { put("active", JsonPrimitive(it)) }
    }
}

interface AdminSettingsApi {
    @retrofit2.http.GET("api/v1/settings/shop") suspend fun shop(): ShopSettingsDto
    @retrofit2.http.PATCH("api/v1/settings/shop/{id}") suspend fun updateShop(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): ShopSettingsDto
}

interface AdminStaffApi {
    @retrofit2.http.GET("api/v1/staff") suspend fun staff(): List<StaffMemberDto>
    @retrofit2.http.PATCH("api/v1/staff/{staffId}") suspend fun update(@retrofit2.http.Path("staffId") staffId: String, @retrofit2.http.Body body: JsonObject): List<StaffMemberDto>
    @retrofit2.http.GET("api/v1/staff/invitations") suspend fun invitations(): List<StaffInvitationDto>
    @retrofit2.http.POST("api/v1/staff/invite") suspend fun invite(@retrofit2.http.Body body: JsonObject): InviteStaffResultDto
    @retrofit2.http.DELETE("api/v1/staff/invitations/{id}") suspend fun revoke(@retrofit2.http.Path("id") id: String): StaffInvitationDto
}

interface AdminCatalogApi {
    @retrofit2.http.POST("api/v1/categories") suspend fun createCategory(@retrofit2.http.Body body: JsonObject): CategoryDto
    @retrofit2.http.PATCH("api/v1/categories/{id}") suspend fun updateCategory(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): CategoryDto
    @retrofit2.http.POST("api/v1/brands") suspend fun createBrand(@retrofit2.http.Body body: JsonObject): BrandDto
    @retrofit2.http.PATCH("api/v1/brands/{id}") suspend fun updateBrand(@retrofit2.http.Path("id") id: String, @retrofit2.http.Body body: JsonObject): BrandDto
}

interface AdminSuppliersApi {
    @retrofit2.http.GET("api/v1/suppliers") suspend fun list(): List<SupplierDto>
    @retrofit2.http.POST("api/v1/suppliers/add") suspend fun create(@retrofit2.http.Body body: JsonObject): SupplierDto
    @retrofit2.http.PATCH("api/v1/suppliers/update/{id}") suspend fun update(@retrofit2.http.Path("id") id: Int, @retrofit2.http.Body body: JsonObject): SupplierDto
}

interface ProfileApi {
    @retrofit2.http.PATCH("api/v1/me") suspend fun updateName(@retrofit2.http.Body body: JsonObject): JsonObject
}

@Singleton class AdminRepository @Inject constructor(
    private val settingsApi: AdminSettingsApi,
    private val staffApi: AdminStaffApi,
    private val catalogApi: AdminCatalogApi,
    private val catalogReadApi: CatalogApi,
    private val suppliersApi: AdminSuppliersApi,
    private val profileApi: ProfileApi,
) {
    suspend fun shop(): ShopSettingsDto = settingsApi.shop()
    suspend fun updateShop(id: String, input: UpdateShopSettingsInput): ShopSettingsDto = settingsApi.updateShop(id, input.toJson())
    suspend fun updateName(name: String) { profileApi.updateName(NamedInput(name).toJson()) }

    suspend fun staff(): List<StaffMemberDto> = staffApi.staff()
    suspend fun updateStaff(staffId: String, input: UpdateStaffInput): StaffMemberDto? =
        staffApi.update(staffId, input.toJson()).firstOrNull()
    suspend fun invitations(): List<StaffInvitationDto> = staffApi.invitations()
    suspend fun inviteStaff(name: String, email: String): InviteStaffResultDto = staffApi.invite(InviteStaffInput(name, email).toJson())
    suspend fun revokeInvitation(id: String): StaffInvitationDto = staffApi.revoke(id)

    suspend fun categories(): List<CategoryDto> = catalogReadApi.categories()
    suspend fun brands(): List<BrandDto> = catalogReadApi.brands()
    suspend fun createCategory(name: String): CategoryDto = catalogApi.createCategory(NamedInput(name).toJson())
    suspend fun setCategoryActive(id: String, active: Boolean): CategoryDto = catalogApi.updateCategory(id, ActiveInput(active).toJson())
    suspend fun createBrand(name: String): BrandDto = catalogApi.createBrand(NamedInput(name).toJson())
    suspend fun setBrandActive(id: String, active: Boolean): BrandDto = catalogApi.updateBrand(id, ActiveInput(active).toJson())

    suspend fun suppliers(): List<SupplierDto> = suppliersApi.list()
    suspend fun createSupplier(input: SupplierInput): SupplierDto = suppliersApi.create(input.toJson())
    suspend fun updateSupplier(id: Int, input: SupplierInput): SupplierDto = suppliersApi.update(id, input.toJson())
}
