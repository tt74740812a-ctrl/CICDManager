package com.doan.cicdmanager.dao;

import com.doan.cicdmanager.model.Project;
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

public class ProjectDAO {

    private static final String COLUMNS = "id, name, description, path, created_by, created_at";

    public List<Project> findAll() throws SQLException {
        return queryList("SELECT " + COLUMNS + " FROM projects ORDER BY id DESC");
    }

    /** Tìm theo tên, mô tả hoặc đường dẫn. */
    public List<Project> search(String keyword) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM projects "
                + "WHERE name LIKE ? OR IFNULL(description, '') LIKE ? OR path LIKE ? ORDER BY id DESC";
        String like = SqlUtil.like(keyword);
        return queryList(sql, like, like, like);
    }

    public Project findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM projects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Kiểm tra trùng tên. Khi thêm mới truyền excludeId = -1, khi sửa truyền id đang sửa. */
    public boolean existsByName(String name, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM projects WHERE LOWER(name) = LOWER(?) AND id <> ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    public int insert(Project p) throws SQLException {
        String sql = "INSERT INTO projects (name, description, path, created_by, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getPath());
            SqlUtil.setNullableInt(ps, 4, p.getCreatedBy());
            ps.setString(5, DateUtil.toDb(DateUtil.now()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Không lấy được id của dự án vừa thêm.");
        }
    }

    public boolean update(Project p) throws SQLException {
        String sql = "UPDATE projects SET name = ?, description = ?, path = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getName());
            ps.setString(2, p.getDescription());
            ps.setString(3, p.getPath());
            ps.setInt(4, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    /** Xóa dự án. Các pipeline, lịch sử chạy và log của dự án bị xóa theo (ON DELETE CASCADE). */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM projects";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private List<Project> queryList(String sql, String... params) throws SQLException {
        List<Project> list = new ArrayList<>();
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

    private Project map(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setPath(rs.getString("path"));
        p.setCreatedBy(SqlUtil.getNullableInt(rs, "created_by"));
        p.setCreatedAt(DateUtil.fromDb(rs.getString("created_at")));
        return p;
    }
}