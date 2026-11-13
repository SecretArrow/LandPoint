package com.landpoint.app.ui.owners

import com.landpoint.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Owner field validation, run without a device: these are the sentences the
 * form shows, so the test pins both the verdict and the message they carry.
 */
class OwnerValidationTest {

    @Test
    fun `a blank name is refused`() {
        assertEquals(R.string.error_owner_name_required, OwnerValidation.fullNameError("   "))
    }

    @Test
    fun `any real name passes`() {
        assertNull(OwnerValidation.fullNameError("Budi Santoso"))
    }

    @Test
    fun `nik is optional`() {
        assertNull(OwnerValidation.nikError(""))
    }

    @Test
    fun `a nik with letters is refused`() {
        assertEquals(R.string.error_owner_nik_digits, OwnerValidation.nikError("3201abcd5678900"))
    }

    @Test
    fun `a short nik is refused`() {
        assertEquals(R.string.error_owner_nik_length, OwnerValidation.nikError("3201234567890"))
    }

    @Test
    fun `a long nik is refused`() {
        assertEquals(R.string.error_owner_nik_length, OwnerValidation.nikError("32012345678900011"))
    }

    @Test
    fun `sixteen digits pass`() {
        assertNull(OwnerValidation.nikError("3201234567890001"))
    }

    @Test
    fun `phone is optional`() {
        assertNull(OwnerValidation.phoneError(""))
    }

    @Test
    fun `gibberish phone is refused`() {
        assertEquals(R.string.error_owner_phone, OwnerValidation.phoneError("call me"))
    }

    @Test
    fun `a diallable phone passes`() {
        assertNull(OwnerValidation.phoneError("+62 812-345 (678)"))
    }

    @Test
    fun `email is optional`() {
        assertNull(OwnerValidation.emailError(""))
    }

    @Test
    fun `an email without a domain is refused`() {
        assertEquals(R.string.error_owner_email, OwnerValidation.emailError("budi@"))
    }

    @Test
    fun `a working email passes`() {
        assertNull(OwnerValidation.emailError("budi@example.com"))
    }
}
