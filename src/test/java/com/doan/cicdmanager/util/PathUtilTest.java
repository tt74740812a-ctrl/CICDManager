package com.doan.cicdmanager.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PathUtilTest {

    @Test
    public void acceptsAbsolutePath() {
        assertEquals("D:\\DoAn\\App", PathUtil.requireAbsolutePath("D:\\DoAn\\App", "Đường dẫn", 260));
    }

    @Test
    public void normalizesPath() {
        assertEquals("D:\\App", PathUtil.requireAbsolutePath("D:\\DoAn\\..\\App", "Đường dẫn", 260));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsRelativePath() {
        PathUtil.requireAbsolutePath("DoAn\\App", "Đường dẫn", 260);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankPath() {
        PathUtil.requireAbsolutePath("  ", "Đường dẫn", 260);
    }
}