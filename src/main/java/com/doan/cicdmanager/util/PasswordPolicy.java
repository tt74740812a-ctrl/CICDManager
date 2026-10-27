package com.doan.cicdmanager.util;

import java.nio.charset.StandardCharsets;

/** Quy tắc đặt mật khẩu. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    /** BCrypt chỉ dùng tối đa 72 byte đầu của mật khẩu. */
    private static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    /** Trả về thông báo lỗi tiếng Việt nếu mật khẩu không đạt, hoặc null nếu hợp lệ. */
    public static String validate(String password, String username) {
        if (password == null || password.isEmpty()) {
            return "Mật khẩu mới không được để trống.";
        }
        if (password.length() < MIN_LENGTH) {
            return "Mật khẩu mới phải có ít nhất " + MIN_LENGTH + " ký tự.";
        }
        if (password.length() > MAX_LENGTH || password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            return "Mật khẩu mới quá dài (tối đa " + MAX_LENGTH + " ký tự).";
        }
        if (!password.equals(password.trim())) {
            return "Mật khẩu mới không được bắt đầu hoặc kết thúc bằng khoảng trắng.";
        }

        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            return "Mật khẩu mới phải chứa cả chữ cái và chữ số.";
        }

        if (username != null && !username.isEmpty()
                && password.toLowerCase().contains(username.toLowerCase())) {
            return "Mật khẩu mới không được chứa tên đăng nhập.";
        }
        return null;
    }
}