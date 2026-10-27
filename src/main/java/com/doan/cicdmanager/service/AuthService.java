package com.doan.cicdmanager.service;

import com.doan.cicdmanager.dao.UserDAO;
import com.doan.cicdmanager.model.Role;
import com.doan.cicdmanager.model.User;
import com.doan.cicdmanager.util.PasswordPolicy;
import com.doan.cicdmanager.util.PasswordUtil;
import com.doan.cicdmanager.util.ValidationUtil;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MILLIS = 30_000;

    /** Dùng chung cho mọi đối tượng AuthService để việc khóa tạm không bị mất khi mở lại màn hình. */
    private static final Map<String, Attempt> ATTEMPTS = new HashMap<>();

    private final UserDAO userDAO = new UserDAO();

    private static class Attempt {
        int failed;
        long lockedUntil;
    }

    /** Tạo tài khoản mẫu nếu bảng users đang trống. */
    public void ensureDefaultUsers() throws ServiceException {
        try {
            if (userDAO.count() > 0) {
                return;
            }
            createUser("admin", "Admin@123", "Quản trị viên", Role.ADMIN);
            createUser("user", "User@123", "Người dùng", Role.USER);
        } catch (SQLException e) {
            throw new ServiceException("Không thể khởi tạo tài khoản mẫu: " + e.getMessage(), e);
        }
    }

    private void createUser(String username, String password, String fullName, Role role) throws SQLException {
        User u = new User();
        u.setUsername(username);
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setFullName(fullName);
        u.setRole(role);
        userDAO.insert(u);
    }

    /** Đăng nhập. Thành công thì lưu vào SessionManager và trả về người dùng. */
    public User login(String username, String password) throws ServiceException {
        if (ValidationUtil.isBlank(username) || password == null || password.isEmpty()) {
            throw new ServiceException("Vui lòng nhập tên đăng nhập và mật khẩu.");
        }
        String key = "login:" + username.trim().toLowerCase();

        synchronized (ATTEMPTS) {
            Attempt attempt = ATTEMPTS.computeIfAbsent(key, k -> new Attempt());
            checkNotLocked(attempt);

            User user;
            try {
                user = userDAO.findByUsername(username.trim());
            } catch (SQLException e) {
                throw new ServiceException("Không thể truy cập cơ sở dữ liệu: " + e.getMessage(), e);
            }

            boolean ok = user != null && PasswordUtil.verify(password, user.getPasswordHash());
            if (!ok) {
                registerFailure(attempt);
                throw new ServiceException("Tên đăng nhập hoặc mật khẩu không đúng.");
            }

            ATTEMPTS.remove(key);
            user.setPasswordHash(null); // không giữ hash trong phiên làm việc
            SessionManager.login(user);
            return user;
        }
    }

    /** Đổi mật khẩu của người dùng đang đăng nhập. */
    public void changePassword(String currentPassword, String newPassword, String confirmPassword)
            throws ServiceException {
        User sessionUser = SessionManager.getCurrentUser();
        if (sessionUser == null) {
            throw new ServiceException("Bạn cần đăng nhập để đổi mật khẩu.");
        }
        if (currentPassword == null || currentPassword.isEmpty()) {
            throw new ServiceException("Vui lòng nhập mật khẩu hiện tại.");
        }
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            throw new ServiceException("Mật khẩu mới và phần nhập lại không khớp.");
        }
        String key = "password:" + sessionUser.getUsername().toLowerCase();

        synchronized (ATTEMPTS) {
            Attempt attempt = ATTEMPTS.computeIfAbsent(key, k -> new Attempt());
            checkNotLocked(attempt);

            try {
                User stored = userDAO.findByUsername(sessionUser.getUsername());
                if (stored == null) {
                    throw new ServiceException("Không tìm thấy tài khoản của bạn.");
                }
                if (!PasswordUtil.verify(currentPassword, stored.getPasswordHash())) {
                    registerFailure(attempt);
                    throw new ServiceException("Mật khẩu hiện tại không đúng.");
                }

                String policyError = PasswordPolicy.validate(newPassword, stored.getUsername());
                if (policyError != null) {
                    throw new ServiceException(policyError);
                }
                if (currentPassword.equals(newPassword)) {
                    throw new ServiceException("Mật khẩu mới phải khác mật khẩu hiện tại.");
                }

                if (!userDAO.updatePassword(stored.getId(), PasswordUtil.hash(newPassword))) {
                    throw new ServiceException("Không thể cập nhật mật khẩu. Vui lòng thử lại.");
                }
                ATTEMPTS.remove(key);
            } catch (SQLException e) {
                throw new ServiceException("Không thể đổi mật khẩu: " + e.getMessage(), e);
            }
        }
    }

    public void logout() {
        SessionManager.logout();
    }

    private void checkNotLocked(Attempt attempt) throws ServiceException {
        long now = System.currentTimeMillis();
        if (attempt.lockedUntil > now) {
            long seconds = (attempt.lockedUntil - now + 999) / 1000;
            throw new ServiceException("Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau " + seconds + " giây.");
        }
    }

    private void registerFailure(Attempt attempt) {
        attempt.failed++;
        if (attempt.failed >= MAX_FAILED_ATTEMPTS) {
            attempt.failed = 0;
            attempt.lockedUntil = System.currentTimeMillis() + LOCK_MILLIS;
        }
    }
}