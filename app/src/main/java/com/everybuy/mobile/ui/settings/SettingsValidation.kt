package com.everybuy.mobile.ui.settings

import com.everybuy.mobile.data.ShopSettingsDto
import com.everybuy.mobile.data.StaffInvitationDto

const val SHOP_NAME_MAX = 120
const val CATALOG_NAME_MAX = 120
const val SUPPLIER_NAME_MAX = 255
const val SUPPLIER_PHONE_MAX = 100
const val PROFILE_NAME_MAX = 120

private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

fun validateShopSettings(name: String, currency: String): String? {
    if (name.trim().isEmpty()) return "Enter the shop name."
    if (name.trim().length > SHOP_NAME_MAX) return "Keep the shop name to $SHOP_NAME_MAX characters or fewer."
    if (currency.trim().isEmpty()) return "Enter the currency code, for example NGN."
    return null
}

fun validateCatalogName(name: String, noun: String): String? {
    if (name.trim().isEmpty()) return "Enter a $noun name."
    if (name.trim().length > CATALOG_NAME_MAX) return "Keep the name to $CATALOG_NAME_MAX characters or fewer."
    return null
}

fun validateSupplier(name: String, phone: String): String? {
    if (name.trim().isEmpty()) return "Enter a supplier name."
    if (name.trim().length > SUPPLIER_NAME_MAX) return "Supplier name is too long."
    if (phone.trim().length > SUPPLIER_PHONE_MAX) return "Phone number is too long."
    return null
}

fun validateProfileName(name: String): String? {
    if (name.trim().isEmpty()) return "Enter your name."
    if (name.trim().length > PROFILE_NAME_MAX) return "Keep your name to $PROFILE_NAME_MAX characters or fewer."
    return null
}

fun validateStaffInvite(name: String, email: String): String? {
    if (name.trim().isEmpty()) return "Enter the staff member’s name."
    if (name.trim().length > PROFILE_NAME_MAX) return "Keep the name to $PROFILE_NAME_MAX characters or fewer."
    if (email.trim().isEmpty()) return "Enter an email address."
    if (!emailPattern.matches(email.trim())) return "Enter a valid email address."
    return null
}

fun pendingInvitations(invitations: List<StaffInvitationDto>): List<StaffInvitationDto> =
    invitations.filter { it.pending }.sortedByDescending { it.createdAt }

/** Reads the first-present value so a null shop row still renders safely. */
fun shopValue(value: String?, fallback: String = "—"): String = value?.takeIf { it.isNotBlank() } ?: fallback

fun shopSettingsInput(shop: ShopSettingsDto): com.everybuy.mobile.data.UpdateShopSettingsInput =
    com.everybuy.mobile.data.UpdateShopSettingsInput(
        name = shop.name,
        address = shop.address,
        phone = shop.phone,
        currency = shop.currency,
        timezone = shop.timezone,
        receiptFooter = shop.receiptFooter,
    )
