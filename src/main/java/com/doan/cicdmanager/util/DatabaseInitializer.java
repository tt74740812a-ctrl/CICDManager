package com.doan.cicdmanager.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Tạo file database SQLite, các bảng và dữ liệu mẫu.
 * Chạy bằng Run File. Chạy nhiều lần vẫn an toàn.
 */
public class DatabaseInitializer {

    private static final String DB_DIR = "data";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/cicd.db";
    private static final String[] TABLES = {
        "users", "projects", "pipelines", "pipeline_steps", "pipeline_runs", "step_logs"
    };

    public static void main(String[] args) {
        File dir = new File(DB_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            System.out.println("Không tạo được thư mục: " + dir.getAbsolutePath());
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {

            st.execute("PRAGMA foreign_keys = ON");

            for (String sql : readStatements("/db/schema.sql")) {
                st.execute(sql);
            }
            for (String sql : readStatements("/db/sample_data.sql")) {
                st.execute(sql);
            }

            System.out.println("Đã tạo database tại: " + new File(DB_DIR, "cicd.db").getAbsolutePath());
            for (String table : TABLES) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    System.out.println("  - " + table + ": " + rs.getInt(1) + " dòng");
                }
            }
        } catch (SQLException e) {
            System.out.println("Lỗi database: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Lỗi đọc file SQL: " + e.getMessage());
        }
    }

    /** Đọc file SQL trong resources, bỏ dòng chú thích, tách theo dấu chấm phẩy. */
    private static List<String> readStatements(String resourcePath) throws IOException {
        List<String> result = new ArrayList<>();
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Không tìm thấy file: " + resourcePath);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("--")) {
                    continue;
                }
                sb.append(line).append('\n');
            }
            for (String part : sb.toString().split(";")) {
                String sql = part.trim();
                if (!sql.isEmpty()) {
                    result.add(sql);
                }
            }
        }
        return result;
    }
}