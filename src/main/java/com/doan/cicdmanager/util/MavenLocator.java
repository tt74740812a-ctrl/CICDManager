package com.doan.cicdmanager.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Tìm file chạy Maven (mvn) trên máy theo thứ tự:
 * 1. Khai báo trong data/config.properties (khóa maven.path)
 * 2. Biến môi trường MAVEN_HOME hoặc M2_HOME
 * 3. Thư mục trong biến môi trường PATH
 */
public final class MavenLocator {

    private static final String CONFIG_FILE = "data/config.properties";
    private static final String[] NAMES = {"mvn.cmd", "mvn.bat", "mvn"};

    private MavenLocator() {
    }

    /** Trả về đường dẫn đầy đủ của mvn, hoặc null nếu không tìm thấy. */
    public static String find() {
        String configured = fromConfig();
        if (configured != null) {
            return configured;
        }

        for (String env : new String[]{"MAVEN_HOME", "M2_HOME"}) {
            String home = System.getenv(env);
            if (home != null && !home.trim().isEmpty()) {
                try {
                    String found = inDirectory(Paths.get(home.trim(), "bin"));
                    if (found != null) {
                        return found;
                    }
                } catch (InvalidPathException e) {
                    // bỏ qua giá trị không hợp lệ
                }
            }
        }

        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(File.pathSeparator)) {
                if (dir.trim().isEmpty()) {
                    continue;
                }
                try {
                    String found = inDirectory(Paths.get(dir.trim()));
                    if (found != null) {
                        return found;
                    }
                } catch (InvalidPathException e) {
                    // bỏ qua mục không hợp lệ trong PATH
                }
            }
        }
        return null;
    }

    private static String fromConfig() {
        File file = new File(CONFIG_FILE);
        if (!file.isFile()) {
            return null;
        }
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        } catch (IOException e) {
            return null;
        }
        String value = props.getProperty("maven.path");
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            Path p = Paths.get(value.trim());
            Path name = p.getFileName();
            if (name == null) {
                return null;
            }
            String lower = name.toString().toLowerCase();
            boolean allowedName = lower.equals("mvn") || lower.equals("mvn.cmd") || lower.equals("mvn.bat");
            return allowedName && Files.isRegularFile(p) ? p.toAbsolutePath().toString() : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private static String inDirectory(Path dir) {
        for (String name : NAMES) {
            Path candidate = dir.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        return null;
    }
}