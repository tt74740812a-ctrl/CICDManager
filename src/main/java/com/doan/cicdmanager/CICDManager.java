package com.doan.cicdmanager;

import com.doan.cicdmanager.dao.MaintenanceDAO;
import com.doan.cicdmanager.service.AuthService;
import com.doan.cicdmanager.service.ServiceException;
import com.doan.cicdmanager.ui.LoginFrame;
import com.doan.cicdmanager.util.DBConnection;
import com.doan.cicdmanager.util.DatabaseInitializer;
import java.io.File;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class CICDManager {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Không áp dụng được giao diện hệ thống thì dùng giao diện mặc định của Java
        }

        // Tự tạo database và dữ liệu mẫu nếu chưa có
        if (!new File(DBConnection.getDatabasePath()).exists()) {
            DatabaseInitializer.main(new String[0]);
        }

        try {
            new AuthService().ensureDefaultUsers();
        } catch (ServiceException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Lỗi khởi động", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Xử lý các lần chạy bị gián đoạn do lần trước ứng dụng đóng đột ngột
        try {
            int fixed = new MaintenanceDAO().recoverInterruptedRuns();
            if (fixed > 0) {
                System.out.println("Đã đánh dấu " + fixed + " lần chạy bị gián đoạn là FAILED.");
            }
        } catch (SQLException e) {
            System.err.println("Không thể khôi phục các lần chạy dở dang: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}