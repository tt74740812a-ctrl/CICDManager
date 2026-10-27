package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.model.Pipeline;
import com.doan.cicdmanager.model.RunMode;
import com.doan.cicdmanager.util.DBConnection;
import com.doan.cicdmanager.util.DateUtil;
import com.doan.cicdmanager.util.SqlUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PipelineDAO {

    private static final String BASE_SELECT =
            "SELECT p.id, p.project_id, p.name, p.description, p.run_mode, p.created_at, "
            + "pr.name AS project_name "
            + "FROM pipelines p JOIN projects pr ON pr.id = p.project_id";

    public List<Pipeline> findAll() throws SQLException {
        return queryList(BASE_SELECT + " ORDER BY p.id DESC");
    }

    /** Tìm theo tên pipeline, mô tả hoặc tên dự án. */
    public List<Pipeline> search(String keyword) throws SQLException {
        String sql = BASE_SELECT
                + " WHERE p.name LIKE ? OR IFNULL(p.description, '') LIKE ? OR pr.name LIKE ?"
                + " ORDER BY p.id DESC";
        String like = SqlUtil.like(keyword);
        return queryList(sql, like, like, like);
    }

    public List<Pipeline> findByProjectId(int projectId) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.project_id = ? ORDER BY p.id DESC";
        List<Pipeline> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Pipeline findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Kiểm tra trùng tên trong cùng một dự án. Khi thêm mới truyền excludeId = -1. */
    public boolean existsByName(int projectId, String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM pipelines WHERE project_id = ? AND LOWER(name) = LOWER(?) AND id <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setString(2, name);
            ps.setInt(3, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public int insert(Pipeline p) throws SQLException {
        String sql = "INSERT INTO pipelines (project_id, name, description, run_mode, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getProjectId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription());
            ps.setString(4, p.getRunMode().name());
            ps.setString(5, DateUtil.toDb(DateUtil.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không lấy được id của pipeline vừa thêm.");
        }
    }

    public boolean update(Pipeline p) throws SQLException {
        String sql = "UPDATE pipelines SET project_id = ?, name = ?, description = ?, run_mode = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getProjectId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getDescription());
            ps.setString(4, p.getRunMode().name());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Xóa pipeline. Các bước và lịch sử chạy của pipeline bị xóa theo (ON DELETE CASCADE). */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM pipelines WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM pipelines";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<Pipeline> queryList(String sql, String... params) throws SQLException {
        List<Pipeline> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setString(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private Pipeline map(ResultSet rs) throws SQLException {
        Pipeline p = new Pipeline();
        p.setId(rs.getInt("id"));
        p.setProjectId(rs.getInt("project_id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setRunMode(RunMode.valueOf(rs.getString("run_mode")));
        p.setCreatedAt(DateUtil.fromDb(rs.getString("created_at")));
        p.setProjectName(rs.getString("project_name"));
        return p;
    }
}