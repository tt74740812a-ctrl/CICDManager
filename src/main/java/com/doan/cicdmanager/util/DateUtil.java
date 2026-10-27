package com.doan.cicdmanager.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Chuyển đổi ngày giờ giữa Java (LocalDateTime) và database (chuỗi TEXT). */
public final class DateUtil {

    private static final DateTimeFormatter DB_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private DateUtil() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now().withNano(0);
    }

    public static String toDb(LocalDateTime time) {
        return time == null ? null : time.format(DB_FORMAT);
    }

    public static LocalDateTime fromDb(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DB_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String toDisplay(LocalDateTime time) {
        return time == null ? "" : time.format(DISPLAY_FORMAT);
    }
}