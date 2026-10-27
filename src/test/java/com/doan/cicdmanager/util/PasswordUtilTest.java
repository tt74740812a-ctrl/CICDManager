package com.doan.cicdmanager.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PasswordUtilTest {

    @Test
    public void hashIsNotPlainText() {
        String hash = PasswordUtil.hash("Admin@123");
        assertNotEquals("Admin@123", hash);
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    public void verifyAcceptsCorrectPasswordOnly() {
        String hash = PasswordUtil.hash("Admin@123");
        assertTrue(PasswordUtil.verify("Admin@123", hash));
        assertFalse(PasswordUtil.verify("sai-mat-khau", hash));
    }

    @Test
    public void samePasswordGivesDifferentHashes() {
        assertNotEquals(PasswordUtil.hash("abc"), PasswordUtil.hash("abc"));
    }

    @Test
    public void verifyHandlesNullAndBrokenHash() {
        assertFalse(PasswordUtil.verify(null, "x"));
        assertFalse(PasswordUtil.verify("abc", null));
        assertFalse(PasswordUtil.verify("abc", "x"));
        assertFalse(PasswordUtil.verify("abc", "not-a-hash"));
    }
}