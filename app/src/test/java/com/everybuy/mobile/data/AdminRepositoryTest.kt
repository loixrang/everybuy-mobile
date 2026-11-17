package com.everybuy.mobile.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminRepositoryTest {
    @Test fun `shop update sends only the editable fields`() = runTest {
        val settings = FakeSettingsApi()
        val repository = repository(settings = settings)

        repository.updateShop(
            "s1",
            UpdateShopSettingsInput(name = " Everybuy ", address = null, phone = "0803", currency = "NGN", timezone = "Africa/Lagos", receiptFooter = null),
        )

        assertEquals("s1", settings.updatedId)
        val body = settings.body!!
        assertEquals(" Everybuy ", body["name"]?.jsonPrimitive?.content)
        assertEquals("0803", body["phone"]?.jsonPrimitive?.content)
        assertEquals("NGN", body["currency"]?.jsonPrimitive?.content)
        assertEquals("Africa/Lagos", body["timezone"]?.jsonPrimitive?.content)
        assertEquals("null", body["address"]?.jsonPrimitive?.content)
        assertEquals("null", body["receiptFooter"]?.jsonPrimitive?.content)
    }

    @Test fun `staff role and active updates send only provided fields`() = runTest {
        val staff = FakeStaffApi()
        val repository = repository(staff = staff)

        repository.updateStaff("st1", UpdateStaffInput(role = "Owner"))
        assertEquals("st1", staff.updatedId)
        assertEquals("Owner", staff.body!!["role"]?.jsonPrimitive?.content)
        assertNull(staff.body!!["active"])

        repository.updateStaff("st1", UpdateStaffInput(active = false))
        assertEquals("false", staff.body!!["active"]?.jsonPrimitive?.content)
        assertNull(staff.body!!["role"])
    }

    @Test fun `invite sends name and email`() = runTest {
        val staff = FakeStaffApi()
        repository(staff = staff).inviteStaff("Ada", "ada@example.com")
        assertEquals("Ada", staff.inviteBody!!["name"]?.jsonPrimitive?.content)
        assertEquals("ada@example.com", staff.inviteBody!!["email"]?.jsonPrimitive?.content)
    }

    @Test fun `catalog writes send the expected body`() = runTest {
        val catalog = FakeCatalogApi()
        val repository = repository(catalog = catalog)

        repository.createCategory("Baby care")
        repository.setCategoryActive("c1", false)
        repository.createBrand("Molfix")
        repository.setBrandActive("b1", false)

        assertEquals("Baby care", catalog.categoryBody!!["name"]?.jsonPrimitive?.content)
        assertEquals("false", catalog.categoryUpdate!!["active"]?.jsonPrimitive?.content)
        assertEquals("Molfix", catalog.brandBody!!["name"]?.jsonPrimitive?.content)
        assertEquals("false", catalog.brandUpdate!!["active"]?.jsonPrimitive?.content)
        assertEquals("c1", catalog.updatedCategoryId)
        assertEquals("b1", catalog.updatedBrandId)
    }

    @Test fun `supplier create and update use the backend routes`() = runTest {
        val suppliers = FakeSuppliersApi()
        val repository = repository(suppliers = suppliers)

        repository.createSupplier(SupplierInput(name = "Ada", phone = null))
        assertEquals("Ada", suppliers.createBody!!["name"]?.jsonPrimitive?.content)
        assertNull(suppliers.createBody!!["phone"])

        repository.updateSupplier(4, SupplierInput(name = "Ada", phone = "0803", active = false))
        assertEquals(4, suppliers.updatedId)
        assertEquals("0803", suppliers.updateBody!!["phone"]?.jsonPrimitive?.content)
        assertEquals("false", suppliers.updateBody!!["active"]?.jsonPrimitive?.content)
    }

    @Test fun `profile rename sends only the name`() = runTest {
        val profile = FakeProfileApi()
        repository(profile = profile).updateName("Ada Obi")
        assertEquals("Ada Obi", profile.body!!["name"]?.jsonPrimitive?.content)
    }

    @Test fun `staff update returns the first row`() = runTest {
        val staff = FakeStaffApi()
        val result = repository(staff = staff).updateStaff("st1", UpdateStaffInput(active = true))
        assertEquals("st1", result?.staffId)
        assertTrue(result?.active == true)
    }

    private fun repository(
        settings: FakeSettingsApi = FakeSettingsApi(),
        staff: FakeStaffApi = FakeStaffApi(),
        catalog: FakeCatalogApi = FakeCatalogApi(),
        suppliers: FakeSuppliersApi = FakeSuppliersApi(),
        profile: FakeProfileApi = FakeProfileApi(),
    ) = AdminRepository(settings, staff, catalog, FakeCatalogReadApi(), suppliers, profile)

    private class FakeSettingsApi : AdminSettingsApi {
        var updatedId: String? = null
        var body: JsonObject? = null
        override suspend fun shop() = ShopSettingsDto(id = "s1", name = "Everybuy", currency = "NGN", timezone = "Africa/Lagos")
        override suspend fun updateShop(id: String, body: JsonObject): ShopSettingsDto {
            updatedId = id
            this.body = body
            return ShopSettingsDto(id = id, name = "Everybuy", currency = "NGN", timezone = "Africa/Lagos")
        }
    }

    private class FakeStaffApi : AdminStaffApi {
        var updatedId: String? = null
        var body: JsonObject? = null
        var inviteBody: JsonObject? = null
        override suspend fun staff() = listOf(StaffMemberDto(staffId = "st1", name = "Ada", email = "ada@example.com"))
        override suspend fun update(staffId: String, body: JsonObject): List<StaffMemberDto> {
            updatedId = staffId
            this.body = body
            return listOf(StaffMemberDto(staffId = staffId, name = "Ada", email = "ada@example.com", role = "Owner", active = true))
        }
        override suspend fun invitations() = emptyList<StaffInvitationDto>()
        override suspend fun invite(body: JsonObject): InviteStaffResultDto {
            inviteBody = body
            return InviteStaffResultDto(message = "sent", email = "ada@example.com")
        }
        override suspend fun revoke(id: String) = StaffInvitationDto(id = id)
    }

    private class FakeCatalogReadApi : CatalogApi {
        override suspend fun categories() = listOf(CategoryDto(id = "c1", name = "Baby care"))
        override suspend fun brands() = listOf(BrandDto(id = "b1", name = "Molfix"))
    }

    private class FakeCatalogApi : AdminCatalogApi {
        var categoryBody: JsonObject? = null
        var categoryUpdate: JsonObject? = null
        var brandBody: JsonObject? = null
        var brandUpdate: JsonObject? = null
        var updatedCategoryId: String? = null
        var updatedBrandId: String? = null

        override suspend fun createCategory(body: JsonObject): CategoryDto {
            categoryBody = body
            return CategoryDto(id = "c1", name = "Baby care")
        }
        override suspend fun updateCategory(id: String, body: JsonObject): CategoryDto {
            updatedCategoryId = id
            categoryUpdate = body
            return CategoryDto(id = id, name = "Baby care", active = false)
        }
        override suspend fun createBrand(body: JsonObject): BrandDto {
            brandBody = body
            return BrandDto(id = "b1", name = "Molfix")
        }
        override suspend fun updateBrand(id: String, body: JsonObject): BrandDto {
            updatedBrandId = id
            brandUpdate = body
            return BrandDto(id = id, name = "Molfix", active = false)
        }
    }

    private class FakeSuppliersApi : AdminSuppliersApi {
        var createBody: JsonObject? = null
        var updateBody: JsonObject? = null
        var updatedId: Int? = null
        override suspend fun list() = emptyList<SupplierDto>()
        override suspend fun create(body: JsonObject): SupplierDto {
            createBody = body
            return SupplierDto(id = 4, name = "Ada")
        }
        override suspend fun update(id: Int, body: JsonObject): SupplierDto {
            updatedId = id
            updateBody = body
            return SupplierDto(id = id, name = "Ada")
        }
    }

    private class FakeProfileApi : ProfileApi {
        var body: JsonObject? = null
        override suspend fun updateName(body: JsonObject): JsonObject {
            this.body = body
            return body
        }
    }
}
