package com.doan.cicdmanager.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/** Các hàm hỗ trợ làm việc với JDBC. */
public final class SqlUtil {

    private SqlUtil() {
    }

    public static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    public static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Tạo mẫu tìm kiếm LIKE: "abc" thành "%abc%". */
    public static String like(String keyword) {
        return "%" + (keyword == null ? "" : keyword.trim()) + "%";
    }
}