package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.util.DBConnection;
import com.doan.cicdmanager.util.DateUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MaintenanceDAO {

    /**
     * Đánh dấu FAILED các lần chạy và bước còn ở trạng thái RUNNING/WAITING
     * do ứng dụng bị đóng đột ngột. Trả về số lần chạy đã xử lý.
     */
    public int recoverInterruptedRuns() throws SQLException {
        String now = DateUtil.toDb(DateUtil.now());
        String stepSql = "UPDATE step_logs SET status = 'FAILED', finished_at = ?, "
                + "exit_code = COALESCE(exit_code, -1), "
                + "log_text = IFNULL(log_text, '') || char(10) || "
                + "'[Bị gián đoạn] Ứng dụng đóng đột ngột khi bước này đang chạy.' "
                + "WHERE status = 'RUNNING'";
        String runSql = "UPDATE pipeline_runs SET status = 'FAILED', finished_at = ?, "
                + "result_message = 'Bị gián đoạn do ứng dụng đóng đột ngột.' "
                + "WHERE status IN ('RUNNING', 'WAITING')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement steps = conn.prepareStatement(stepSql);
                 PreparedStatement runs = conn.prepareStatement(runSql)) {
                steps.setString(1, now);
                steps.executeUpdate();
                runs.setString(1, now);
                int fixed = runs.executeUpdate();
                conn.commit();
                return fixed;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}