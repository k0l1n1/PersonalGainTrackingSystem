package com.personaltracker.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Date and Time formatting utility.
 */
public class DateUtil {

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    public static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        LocalDate today = LocalDate.now();
        if (date.isEqual(today)) return "Today";
        if (date.isEqual(today.minusDays(1))) return "Yesterday";
        if (date.isEqual(today.plusDays(1))) return "Tomorrow";
        return date.format(DATE_FORMAT);
    }

    public static String formatIsoDate(LocalDate date) {
        if (date == null) return "";
        return date.format(ISO_DATE_FORMAT);
    }

    public static String formatTime(LocalTime time) {
        if (time == null) return "";
        return time.format(TIME_FORMAT);
    }

    public static String formatDateTime(OffsetDateTime dt) {
        if (dt == null) return "";
        return dt.format(DATE_TIME_FORMAT);
    }

    public static String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        }
        int hours = minutes / 60;
        int remMins = minutes % 60;
        if (remMins == 0) {
            return hours + "h";
        }
        return hours + "h " + remMins + "m";
    }

    public static LocalDate parseDateOrNull(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(str.trim(), ISO_DATE_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.trim(), DATE_FORMAT);
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
