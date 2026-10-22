package com.personaltracker;

import com.personaltracker.model.Workout;
import com.personaltracker.util.HealthCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Health and Fitness Calculations Unit Tests")
public class HealthCalculatorTest {

    @Test
    @DisplayName("Should accurately calculate BMI for normal weight")
    void testCalculateBmiNormal() {
        // 70kg, 175cm -> BMI = 70 / (1.75 * 1.75) = 22.86 -> 22.9
        double bmi = HealthCalculator.calculateBmi(70.0, 175.0);
        assertEquals(22.9, bmi, 0.05);
        assertEquals("Normal weight", HealthCalculator.getBmiCategory(bmi));
    }

    @Test
    @DisplayName("Should categorize Underweight and Overweight correctly")
    void testBmiCategories() {
        double underweightBmi = HealthCalculator.calculateBmi(48.0, 170.0);
        assertEquals("Underweight", HealthCalculator.getBmiCategory(underweightBmi));

        double overweightBmi = HealthCalculator.calculateBmi(85.0, 172.0);
        assertEquals("Overweight", HealthCalculator.getBmiCategory(overweightBmi));

        double obeseBmi = HealthCalculator.calculateBmi(100.0, 175.0);
        assertEquals("Obesity Class I", HealthCalculator.getBmiCategory(obeseBmi));
    }

    @Test
    @DisplayName("Should return 0 BMI for invalid weight or height")
    void testInvalidBmi() {
        assertEquals(0.0, HealthCalculator.calculateBmi(0, 175.0));
        assertEquals(0.0, HealthCalculator.calculateBmi(70.0, 0));
        assertEquals(0.0, HealthCalculator.calculateBmi(-10, -5));
    }

    @Test
    @DisplayName("Should calculate sleep duration crossing midnight")
    void testSleepDurationCrossingMidnight() {
        OffsetDateTime bedtime = OffsetDateTime.parse("2026-10-12T23:30:00Z");
        OffsetDateTime wakeTime = OffsetDateTime.parse("2026-10-13T07:15:00Z");

        int durationMins = HealthCalculator.calculateSleepDurationMinutes(bedtime, wakeTime);
        assertEquals(465, durationMins); // 7 hours 45 mins = 465 mins
    }

    @Test
    @DisplayName("Should calculate goal progress percentage with bounds [0, 100]")
    void testGoalProgress() {
        assertEquals(50, HealthCalculator.calculateGoalProgress(50.0, 100.0));
        assertEquals(100, HealthCalculator.calculateGoalProgress(120.0, 100.0));
        assertEquals(0, HealthCalculator.calculateGoalProgress(0.0, 50.0));
        assertEquals(75, HealthCalculator.calculateGoalProgress(75.0, 100.0));
    }

    @Test
    @DisplayName("Should estimate workout calories burned using MET")
    void testEstimateCaloriesBurned() {
        // 60 minutes Strength session for 75kg athlete
        int strengthCal = HealthCalculator.estimateCaloriesBurned("Strength", 60, 75.0);
        assertTrue(strengthCal > 300 && strengthCal < 450);

        // HIIT should burn more than Yoga
        int hiitCal = HealthCalculator.estimateCaloriesBurned("HIIT", 45, 75.0);
        int yogaCal = HealthCalculator.estimateCaloriesBurned("Yoga", 45, 75.0);
        assertTrue(hiitCal > yogaCal);

        // Zero or negative duration should return 0
        assertEquals(0, HealthCalculator.estimateCaloriesBurned("Cardio", 0, 75.0));
        assertEquals(0, HealthCalculator.estimateCaloriesBurned("Cardio", -10, 75.0));

        // Intensity levels: Vigorous > Moderate > Light
        int lightCal = HealthCalculator.estimateCaloriesBurned("Cardio", "Light", 40, 75.0, null);
        int modCal = HealthCalculator.estimateCaloriesBurned("Cardio", "Moderate", 40, 75.0, null);
        int vigCal = HealthCalculator.estimateCaloriesBurned("Cardio", "Vigorous", 40, 75.0, null);
        assertTrue(vigCal > modCal);
        assertTrue(modCal > lightCal);

        // Strength volume load: workout with heavy compound exercises burns more than bodyweight alone
        List<com.personaltracker.model.WorkoutExercise> exercises = new ArrayList<>();
        exercises.add(new com.personaltracker.model.WorkoutExercise("Bench Press", 4, 10, 80.0)); // 3200kg volume
        exercises.add(new com.personaltracker.model.WorkoutExercise("Squat", 4, 8, 100.0));       // 3200kg volume
        int volumeStrengthCal = HealthCalculator.estimateCaloriesBurned("Strength", "Moderate", 45, 75.0, exercises);
        assertTrue(volumeStrengthCal > 250);
    }

    @Test
    @DisplayName("Should calculate continuous daily workout streak")
    void testCalculateWorkoutStreak() {
        LocalDate today = LocalDate.now();
        List<Workout> workouts = new ArrayList<>();

        Workout w1 = new Workout("Session 1", "Cardio", today, 30, 250);
        Workout w2 = new Workout("Session 2", "Strength", today.minusDays(1), 45, 350);
        Workout w3 = new Workout("Session 3", "HIIT", today.minusDays(2), 25, 220);

        workouts.add(w1);
        workouts.add(w2);
        workouts.add(w3);

        int streak = HealthCalculator.calculateWorkoutStreak(workouts);
        assertEquals(3, streak);
    }
}
