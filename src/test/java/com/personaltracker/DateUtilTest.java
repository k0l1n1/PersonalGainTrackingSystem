package com.personaltracker;

import com.personaltracker.util.DateUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateUtil Unit Tests")
public class DateUtilTest {

    @Test
    @DisplayName("Should format and parse relative dates correctly")
    void testRelativeDateParsing() {
        LocalDate today = LocalDate.now();
        assertEquals(today, DateUtil.parseDateOrNull("today"));
        assertEquals(today, DateUtil.parseDateOrNull("Today"));
        assertEquals(today, DateUtil.parseDateOrNull("TODAY"));

        assertEquals(today.minusDays(1), DateUtil.parseDateOrNull("yesterday"));
        assertEquals(today.minusDays(1), DateUtil.parseDateOrNull("Yesterday"));

        assertEquals(today.plusDays(1), DateUtil.parseDateOrNull("tomorrow"));
        assertEquals(today.plusDays(1), DateUtil.parseDateOrNull("Tomorrow"));
    }

    @Test
    @DisplayName("Should parse ISO and standard date formats")
    void testStandardDateParsing() {
        LocalDate expected = LocalDate.of(2026, 10, 21);
        assertEquals(expected, DateUtil.parseDateOrNull("2026-10-21"));
        assertEquals(expected, DateUtil.parseDateOrNull("Oct 21, 2026"));

        assertNull(DateUtil.parseDateOrNull("invalid-date-string"));
        assertNull(DateUtil.parseDateOrNull(""));
        assertNull(DateUtil.parseDateOrNull(null));
    }

    @Test
    @DisplayName("Should format duration correctly in hours and minutes")
    void testFormatDuration() {
        assertEquals("45m", DateUtil.formatDuration(45));
        assertEquals("1h", DateUtil.formatDuration(60));
        assertEquals("1h 15m", DateUtil.formatDuration(75));
        assertEquals("2h", DateUtil.formatDuration(120));
        assertEquals("2h 30m", DateUtil.formatDuration(150));
    }
}
