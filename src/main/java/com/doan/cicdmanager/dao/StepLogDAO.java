package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.model.RunStatus;
import com.doan.cicdmanager.model.StepLog;
import com.doan.cicdmanager.model.StepType;
import com.doan.cicdmanager.util.DBConnection;
import com.doan.cicdmanager.util.DateUtil;
import com.doan.cicdmanager.util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StepLogDAO {

    private static final String BASE_SELECT =
            "SELECT l.id, l.run_id, l.step_type, l.status, l.started_at, l.finished_at, "
            + "l.exit_code, l.log_text, p.name AS pipeline_name "
            + "FROM step_logs l "
            + "JOIN pipeline_runs r ON r.id = l.run_id "
            + "JOIN pipelines p ON p.id = r.pipeline_id";

    /** Tạo log cho một bước, trả về id. */
    public int insert(StepLog log) throws SQLException {
        String sql = "INSERT INTO step_logs (run_id, step_type, status, started_at, finished_at, exit_code, log_text) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getRunId());
            ps.setString(2, log.getStepType().name());
            ps.setString(3, log.getStatus().name());
            ps.setString(4, DateUtil.toDb(log.getStartedAt()));
            ps.setString(5, DateUtil.toDb(log.getFinishedAt()));
            SqlUtil.setNullableInt(ps, 6, log.getExitCode());
            ps.setString(7, log.getLogText());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không lấy được id của log vừa tạo.");
        }
    }

    /** Cập nhật log của bước. Tham số nào truyền null thì giữ nguyên giá trị cũ. */
    public boolean update(int logId, RunStatus status, LocalDateTime startedAt, LocalDateTime finishedAt,
                          Integer exitCode, String logText) throws SQLException {
        String sql = "UPDATE step_logs SET status = ?, "
                + "started_at = COALESCE(?, started_at), "
                + "finished_at = COALESCE(?, finished_at), "
                + "exit_code = COALESCE(?, exit_code), "
                + "log_text = COALESCE(?, log_text) "
                + "WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, DateUtil.toDb(startedAt));
            ps.setString(3, DateUtil.toDb(finishedAt));
            SqlUtil.setNullableInt(ps, 4, exitCode);
            ps.setString(5, logText);
            ps.setInt(6, logId);
            return ps.executeUpdate() > 0;
        }
    }

    /** Lấy log các bước của một lần chạy, theo thứ tự thời gian tạo. */
    public List<StepLog> findByRunId(int runId) throws SQLException {
        String sql = BASE_SELECT + " WHERE l.run_id = ? ORDER BY l.id";
        List<StepLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Tìm trong nội dung log, loại bước hoặc tên pipeline. */
    public List<StepLog> search(String keyword) throws SQLException {
        String sql = BASE_SELECT
                + " WHERE (IFNULL(l.log_text, '') LIKE ? OR l.step_type LIKE ? OR p.name LIKE ?)"
                + " ORDER BY l.id DESC";
        List<StepLog> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = SqlUtil.like(keyword);
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private StepLog map(ResultSet rs) throws SQLException {
        StepLog l = new StepLog();
        l.setId(rs.getInt("id"));
        l.setRunId(rs.getInt("run_id"));
        l.setStepType(StepType.valueOf(rs.getString("step_type")));
        l.setStatus(RunStatus.valueOf(rs.getString("status")));
        l.setStartedAt(DateUtil.fromDb(rs.getString("started_at")));
        l.setFinishedAt(DateUtil.fromDb(rs.getString("finished_at")));
        l.setExitCode(SqlUtil.getNullableInt(rs, "exit_code"));
        l.setLogText(rs.getString("log_text"));
        l.setPipelineName(rs.getString("pipeline_name"));
        return l;
    }
}