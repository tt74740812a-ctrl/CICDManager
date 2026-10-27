package com.doan.cicdmanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ValidationUtilTest {

    @Test
    public void isBlankDetectsEmptyValues() {
        assertTrue(ValidationUtil.isBlank(null));
        assertTrue(ValidationUtil.isBlank("   "));
        assertFalse(ValidationUtil.isBlank("abc"));
    }

    @Test
    public void requireTextTrimsSpaces() {
        assertEquals("abc", ValidationUtil.requireText("  abc  ", "Tên", 10));
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireTextRejectsBlank() {
        ValidationUtil.requireText("   ", "Tên", 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requireTextRejectsTooLong() {
        ValidationUtil.requireText("abcdef", "Tên", 3);
    }

    @Test
    public void optionalTextAllowsEmpty() {
        assertEquals("", ValidationUtil.optionalText(null, "Mô tả", 10));
    }
}