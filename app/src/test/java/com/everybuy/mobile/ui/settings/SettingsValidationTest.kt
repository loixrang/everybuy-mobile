package com.everybuy.mobile.ui.settings

import com.everybuy.mobile.data.CategoryDto
import com.everybuy.mobile.data.ShopSettingsDto
import com.everybuy.mobile.data.StaffInvitationDto
import com.everybuy.mobile.data.SupplierInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidationTest {
    @Test fun `shop details require a name and currency`() {
        assertEquals("Enter the shop name.", validateShopSettings(" ", "NGN"))
        assertEquals("Enter the currency code, for example NGN.", validateShopSettings("Everybuy", " "))
        assertNull(validateShopSettings("Everybuy", "NGN"))
    }

    @Test fun `catalog names are required and bounded`() {
        assertEquals("Enter a category name.", validateCatalogName("", "category"))
        assertEquals("Enter a brand name.", validateCatalogName("  ", "brand"))
        assertEquals("Keep the name to 120 characters or fewer.", validateCatalogName("a".repeat(121), "brand"))
        assertNull(validateCatalogName("Baby care", "category"))
    }

    @Test fun `supplier rules mirror the backend columns`() {
        assertEquals("Enter a supplier name.", validateSupplier("", ""))
        assertEquals("Supplier name is too long.", validateSupplier("a".repeat(256), ""))
        assertEquals("Phone number is too long.", validateSupplier("Ada", "1".repeat(101)))
        assertNull(validateSupplier("Ada", "0803"))
    }

    @Test fun `invites need a name and a plausible email`() {
        assertEquals("Enter the staff member’s name.", validateStaffInvite("", "ada@example.com"))
        assertEquals("Enter an email address.", validateStaffInvite("Ada", ""))
        assertEquals("Enter a valid email address.", validateStaffInvite("Ada", "ada"))
        assertNull(validateStaffInvite("Ada", "ada@example.com"))
    }

    @Test fun `profile name matches the name field rules`() {
        assertEquals("Enter your name.", validateProfileName(""))
        assertEquals("Keep your name to 120 characters or fewer.", validateProfileName("a".repeat(121)))
        assertNull(validateProfileName("Ada Obi"))
    }

    @Test fun `only pending invitations are shown`() {
        val rows = listOf(
            StaffInvitationDto(id = "1", status = "pending", createdAt = "2026-11-17T09:00:00Z"),
            StaffInvitationDto(id = "2", status = "accepted", createdAt = "2026-11-16T09:00:00Z"),
            StaffInvitationDto(id = "3", status = "processing", createdAt = "2026-11-18T09:00:00Z"),
        )
        assertEquals(listOf("3", "1"), pendingInvitations(rows).map { it.id })
    }

    @Test fun `shop values fall back when blank`() {
        assertEquals("—", shopValue(null))
        assertEquals("—", shopValue("  "))
        assertEquals("Lagos", shopValue("Lagos"))
    }

    @Test fun `shop edit input starts from the loaded row`() {
        val input = shopSettingsInput(ShopSettingsDto(id = "s1", name = "Everybuy", address = null, phone = "0803", currency = "NGN", timezone = "Africa/Lagos", receiptFooter = null))
        assertEquals("Everybuy", input.name)
        assertEquals("0803", input.phone)
        assertNull(input.address)
        assertEquals("Africa/Lagos", input.timezone)
    }

    @Test fun `category read shape matches the backend`() {
        assertEquals(true, CategoryDto(id = "c1", name = "Baby care").active)
    }

    @Test fun `supplier input omits a blank phone`() {
        val body = SupplierInput(name = "Ada", phone = null).toJson()
        assertTrue(body["phone"] == null)
        assertEquals("Ada", body["name"].toString().trim('"'))
    }
}
