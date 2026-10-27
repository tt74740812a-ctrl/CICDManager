package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.model.PipelineStep;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PipelineStepDAO {

    /** Lấy các bước của một pipeline, đã sắp xếp theo thứ tự thực hiện. */
    public List<PipelineStep> findByPipelineId(int pipelineId) throws SQLException {
        String sql = "SELECT id, pipeline_id, step_type, step_order, enabled, deploy_dir "
                + "FROM pipeline_steps WHERE pipeline_id = ? ORDER BY step_order";
        List<PipelineStep> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pipelineId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PipelineStep s = new PipelineStep();
                    s.setId(rs.getInt("id"));
                    s.setPipelineId(rs.getInt("pipeline_id"));
                    s.setStepType(StepType.valueOf(rs.getString("step_type")));
                    s.setStepOrder(rs.getInt("step_order"));
                    s.setEnabled(rs.getInt("enabled") == 1);
                    s.setDeployDir(rs.getString("deploy_dir"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    /**
     * Thay toàn bộ các bước của pipeline bằng danh sách mới (xóa cũ, thêm mới).
     * Thực hiện trong một giao dịch: nếu lỗi thì khôi phục lại như cũ.
     */
    public void replaceSteps(int pipelineId, List<PipelineStep> steps) throws SQLException {
        String deleteSql = "DELETE FROM pipeline_steps WHERE pipeline_id = ?";
        String insertSql = "INSERT INTO pipeline_steps (pipeline_id, step_type, step_order, enabled, deploy_dir) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement del = conn.prepareStatement(deleteSql)) {
                    del.setInt(1, pipelineId);
                    del.executeUpdate();
                }
                try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                    for (PipelineStep s : steps) {
                        ins.setInt(1, pipelineId);
                        ins.setString(2, s.getStepType().name());
                        ins.setInt(3, s.getStepOrder());
                        ins.setInt(4, s.isEnabled() ? 1 : 0);
                        ins.setString(5, s.getDeployDir());
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}