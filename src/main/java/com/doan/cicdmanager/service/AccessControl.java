package com.doan.cicdmanager.service;

/** Kiểm tra quyền ở lớp nghiệp vụ. */
public final class AccessControl {

    private AccessControl() {
    }

    public static void requireAdmin() throws ServiceException {
        if (!SessionManager.isAdmin()) {
            throw new ServiceException("Bạn không có quyền thực hiện thao tác này. Chỉ quản trị viên được phép.");
        }
    }
}