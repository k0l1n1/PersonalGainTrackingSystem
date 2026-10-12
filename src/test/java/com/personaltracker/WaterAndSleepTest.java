package com.personaltracker;

import com.personaltracker.model.SleepRecord;
import com.personaltracker.model.WaterIntake;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Water and Sleep Calculations Unit Tests")
public class WaterAndSleepTest {

    @Test
    @DisplayName("Should sum water intakes accurately")
    void testWaterIntakeSum() {
        LocalDate today = LocalDate.now();
        List<WaterIntake> logs = Arrays.asList(
                new WaterIntake(250, today, LocalTime.of(8, 0)),
                new WaterIntake(500, today, LocalTime.of(11, 30)),
                new WaterIntake(750, today, LocalTime.of(15, 0)),
                new WaterIntake(500, today, LocalTime.of(19, 0))
        );

        int totalMl = logs.stream().mapToInt(WaterIntake::getAmountMl).sum();
        assertEquals(2000, totalMl);
    }

    @Test
    @DisplayName("Should calculate sleep hours and formatted duration")
    void testSleepRecordDuration() {
        LocalDate date = LocalDate.now();
        OffsetDateTime bed = OffsetDateTime.parse("2026-10-12T22:30:00Z");
        OffsetDateTime wake = OffsetDateTime.parse("2026-10-13T06:30:00Z"); // 8 hours

        SleepRecord record = new SleepRecord(date, bed, wake, 5, "Deep sleep");
        assertEquals(480, record.getDurationMinutes());
        assertEquals(8.0, record.getDurationHours());
    }
}
