package com.doan.cicdmanager.util;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Cung cấp kết nối tới database SQLite của ứng dụng.
 * Mỗi lần gọi getConnection() trả về một kết nối mới.
 * Nơi gọi phải đóng kết nối, nên dùng try-with-resources.
 */
public final class DBConnection {

    private static final String DB_FILE = "data/cicd.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            throw new SQLException("Không tìm thấy file database: " + file.getAbsolutePath()
                    + ". Hãy chạy DatabaseInitializer trước.");
        }

        Connection conn = DriverManager.getConnection(URL);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    public static String getDatabasePath() {
        return new File(DB_FILE).getAbsolutePath();
    }
}