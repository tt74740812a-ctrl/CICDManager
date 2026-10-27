package com.doan.cicdmanager.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class PasswordPolicyTest {

    @Test
    public void acceptsStrongPassword() {
        assertNull(PasswordPolicy.validate("Matkhau2026", "admin"));
    }

    @Test
    public void rejectsBlankPassword() {
        assertNotNull(PasswordPolicy.validate(null, "admin"));
        assertNotNull(PasswordPolicy.validate("", "admin"));
    }

    @Test
    public void rejectsShortPassword() {
        assertNotNull(PasswordPolicy.validate("Ab1", "admin"));
    }

    @Test
    public void requiresBothLettersAndDigits() {
        assertNotNull(PasswordPolicy.validate("abcdefghij", "admin"));
        assertNotNull(PasswordPolicy.validate("1234567890", "admin"));
    }

    @Test
    public void rejectsPasswordContainingUsername() {
        assertNotNull(PasswordPolicy.validate("Admin12345", "admin"));
    }

    @Test
    public void rejectsTooLongPassword() {
        StringBuilder sb = new StringBuilder("a1");
        while (sb.length() <= PasswordPolicy.MAX_LENGTH) {
            sb.append('x');
        }
        assertNotNull(PasswordPolicy.validate(sb.toString(), "admin"));
    }
}