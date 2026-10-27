package com.doan.cicdmanager.util;

import org.mindrot.jbcrypt.BCrypt;

/** Hash và kiểm tra mật khẩu bằng BCrypt. Không bao giờ lưu mật khẩu thuần. */
public final class PasswordUtil {

    private static final int COST = 12;

    private PasswordUtil() {
    }

    public static String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(COST));
    }

    public static boolean verify(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (RuntimeException e) {
            return false;
        }
    }
}