package com.doan.cicdmanager.service;

/** Lỗi nghiệp vụ, thông báo bên trong là câu tiếng Việt có thể hiện thẳng cho người dùng. */
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}