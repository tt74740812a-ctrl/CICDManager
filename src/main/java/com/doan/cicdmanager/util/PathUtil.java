package com.doan.cicdmanager.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class PathUtil {

    private PathUtil() {
    }

    /** Bắt buộc là đường dẫn đầy đủ và hợp lệ. Trả về đường dẫn đã chuẩn hóa. */
    public static String requireAbsolutePath(String raw, String fieldName, int maxLength) {
        String text = ValidationUtil.requireText(raw, fieldName, maxLength);
        try {
            Path path = Paths.get(text);
            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(fieldName
                        + " phải là đường dẫn đầy đủ, ví dụ D:\\DoAn\\MyApp.");
            }
            return path.normalize().toString();
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(fieldName + " không hợp lệ.");
        }
    }
}