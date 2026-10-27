package com.doan.cicdmanager.service;

import com.doan.cicdmanager.model.User;

/** Lưu người dùng đang đăng nhập trong suốt thời gian chạy ứng dụng. */
public final class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
}