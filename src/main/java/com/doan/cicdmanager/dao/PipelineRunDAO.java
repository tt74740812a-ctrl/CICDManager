package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.model.PipelineRun;
import com.doan.cicdmanager.model.RunStatus;
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

public class PipelineRunDAO {

    private static final String BASE_SELECT =
            "SELECT r.id, r.pipeline_id, r.triggered_by, r.status, r.started_at, r.finished_at, "
            + "r.result_message, p.name AS pipeline_name, pr.name AS project_name "
            + "FROM pipeline_runs r "
            + "JOIN pipelines p ON p.id = r.pipeline_id "
            + "JOIN projects pr ON pr.id = p.project_id";

    /** Tạo một lần chạy mới, trả về id. */
    public int insert(PipelineRun run) throws SQLException {
        String sql = "INSERT INTO pipeline_runs (pipeline_id, triggered_by, status, started_at, finished_at, result_message) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, run.getPipelineId());
            SqlUtil.setNullableInt(ps, 2, run.getTriggeredBy());
            ps.setString(3, run.getStatus().name());
            ps.setString(4, DateUtil.toDb(run.getStartedAt()));
            ps.setString(5, DateUtil.toDb(run.getFinishedAt()));
            ps.setString(6, run.getResultMessage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không lấy được id của lần chạy vừa tạo.");
        }
    }

    /** Cập nhật trạng thái. Tham số nào truyền null thì giữ nguyên giá trị cũ. */
    public boolean updateStatus(int runId, RunStatus status, LocalDateTime startedAt,
                                LocalDateTime finishedAt, String resultMessage) throws SQLException {
        String sql = "UPDATE pipeline_runs SET status = ?, "
                + "started_at = COALESCE(?, started_at), "
                + "finished_at = COALESCE(?, finished_at), "
                + "result_message = COALESCE(?, result_message) "
                + "WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, DateUtil.toDb(startedAt));
            ps.setString(3, DateUtil.toDb(finishedAt));
            ps.setString(4, resultMessage);
            ps.setInt(5, runId);
            return ps.executeUpdate() > 0;
        }
    }

    public PipelineRun findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE r.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Lấy các lần chạy gần nhất (dùng cho Dashboard). */
    public List<PipelineRun> findRecent(int limit) throws SQLException {
        String sql = BASE_SELECT + " ORDER BY r.id DESC LIMIT ?";
        List<PipelineRun> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /** Tìm theo từ khóa (tên pipeline, tên dự án, kết quả) và lọc theo trạng thái. status = null nghĩa là tất cả. */
    public List<PipelineRun> search(String keyword, RunStatus status) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        sql.append(" WHERE (p.name LIKE ? OR pr.name LIKE ? OR IFNULL(r.result_message, '') LIKE ?)");
        if (status != null) {
            sql.append(" AND r.status = ?");
        }
        sql.append(" ORDER BY r.id DESC");

        List<PipelineRun> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            String like = SqlUtil.like(keyword);
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            if (status != null) {
                ps.setString(4, status.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM pipeline_runs";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public int countByStatus(RunStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pipeline_runs WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private PipelineRun map(ResultSet rs) throws SQLException {
        PipelineRun r = new PipelineRun();
        r.setId(rs.getInt("id"));
        r.setPipelineId(rs.getInt("pipeline_id"));
        r.setTriggeredBy(SqlUtil.getNullableInt(rs, "triggered_by"));
        r.setStatus(RunStatus.valueOf(rs.getString("status")));
        r.setStartedAt(DateUtil.fromDb(rs.getString("started_at")));
        r.setFinishedAt(DateUtil.fromDb(rs.getString("finished_at")));
        r.setResultMessage(rs.getString("result_message"));
        r.setPipelineName(rs.getString("pipeline_name"));
        r.setProjectName(rs.getString("project_name"));
        return r;
    }
}