package com.doan.cicdmanager.util;

/** Kiểm tra dữ liệu nhập. Lỗi được ném ra kèm thông báo tiếng Việt để hiển thị cho người dùng. */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /** Bắt buộc có giá trị, không vượt quá độ dài cho phép. Trả về chuỗi đã cắt khoảng trắng đầu cuối. */
    public static String requireText(String value, String fieldName, int maxLength) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " không được để trống.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " không được dài quá " + maxLength + " ký tự.");
        }
        return trimmed;
    }

    /** Không bắt buộc, nhưng nếu có thì không vượt quá độ dài cho phép. Trả về "" nếu để trống. */
    public static String optionalText(String value, String fieldName, int maxLength) {
        if (isBlank(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " không được dài quá " + maxLength + " ký tự.");
        }
        return trimmed;
    }
}