package com.everybuy.mobile.ui.auth

import com.everybuy.mobile.data.AcceptInviteInput
import com.everybuy.mobile.data.PasswordResetInput
import com.everybuy.mobile.data.ResetPasswordInput
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRecoveryTest {
    @Test fun `parses reset password deep links`() {
        val link = parseAuthDeepLink("everybuy://reset-password?token=abc123") as AuthDeepLink.ResetPassword
        assertEquals("abc123", link.token)
        assertNull(link.error)
    }

    @Test fun `parses reset error deep links`() {
        val link = parseAuthDeepLink("everybuy://reset-password?error=INVALID_TOKEN") as AuthDeepLink.ResetPassword
        assertNull(link.token)
        assertEquals("INVALID_TOKEN", link.error)
    }

    @Test fun `parses invite deep links`() {
        val link = parseAuthDeepLink("everybuy://invite/accept?token=deadbeef") as AuthDeepLink.AcceptInvite
        assertEquals("deadbeef", link.token)
    }

    @Test fun `rejects unrelated deep links`() {
        assertNull(parseAuthDeepLink(null))
        assertNull(parseAuthDeepLink("https://example.com/reset-password?token=abc"))
        assertNull(parseAuthDeepLink("everybuy://invite/unknown?token=abc"))
        assertNull(parseAuthDeepLink("everybuy://something"))
    }

    @Test fun `decodes query values`() {
        val link = parseAuthDeepLink("everybuy://reset-password?token=abc%2F123") as AuthDeepLink.ResetPassword
        assertEquals("abc/123", link.token)
    }

    @Test fun `validates the new password rules`() {
        assertEquals("Choose a password between 12 and 128 characters.", validateNewPassword("short", "short"))
        assertEquals("Choose a password between 12 and 128 characters.", validateNewPassword("a".repeat(129), "a".repeat(129)))
        assertEquals("The passwords do not match.", validateNewPassword("correct horse", "correct horsE"))
        assertNull(validateNewPassword("correct horse", "correct horse"))
    }

    @Test fun `password reset input trims the email`() {
        val body = PasswordResetInput("  ada@example.com ", "everybuy://reset-password").toJson()
        assertEquals("ada@example.com", body["email"]?.jsonPrimitive?.content)
        assertEquals("everybuy://reset-password", body["redirectTo"]?.jsonPrimitive?.content)
    }

    @Test fun `reset and invite inputs match the Better Auth bodies`() {
        val reset = ResetPasswordInput("token-1", "a-long-password").toJson()
        assertEquals("token-1", reset["token"]?.jsonPrimitive?.content)
        assertEquals("a-long-password", reset["newPassword"]?.jsonPrimitive?.content)

        val invite = AcceptInviteInput("token-2", "another-password").toJson()
        assertEquals("token-2", invite["token"]?.jsonPrimitive?.content)
        assertEquals("another-password", invite["password"]?.jsonPrimitive?.content)
        assertTrue(invite.size == 2)
    }
}
