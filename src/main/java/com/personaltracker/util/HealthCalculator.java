package com.personaltracker.util;

import com.personaltracker.model.Workout;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Health, Fitness, and Statistical calculations.
 */
public class HealthCalculator {

    /**
     * Calculates Body Mass Index (BMI) using weight in kg and height in cm.
     * Formula: weight_kg / (height_m ^ 2)
     * BMI is an informational indicator.
     */
    public static double calculateBmi(double weightKg, double heightCm) {
        if (weightKg <= 0 || heightCm <= 0) {
            return 0.0;
        }
        double heightMeters = heightCm / 100.0;
        double bmi = weightKg / (heightMeters * heightMeters);
        return Math.round(bmi * 10.0) / 10.0;
    }

    /**
     * Returns the World Health Organization BMI classification category.
     */
    public static String getBmiCategory(double bmi) {
        if (bmi <= 0) return "Unknown";
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25.0) return "Normal weight";
        if (bmi < 30.0) return "Overweight";
        if (bmi < 35.0) return "Obesity Class I";
        if (bmi < 40.0) return "Obesity Class II";
        return "Obesity Class III";
    }

    /**
     * Computes the duration in minutes between bedtime and wake time.
     * Accurately handles crossing midnight (e.g. 23:00 to 07:00).
     */
    public static int calculateSleepDurationMinutes(OffsetDateTime bedtime, OffsetDateTime wakeTime) {
        if (bedtime == null || wakeTime == null) return 0;
        long minutes = Duration.between(bedtime, wakeTime).toMinutes();
        if (minutes < 0) {
            // In case bedtime is after wakeTime on same calendar date without day adjustment
            minutes += 24 * 60;
        }
        return (int) Math.max(0, minutes);
    }

    /**
     * Calculates the consecutive daily workout streak leading up to today (or yesterday).
     */
    public static int calculateWorkoutStreak(List<Workout> workouts) {
        if (workouts == null || workouts.isEmpty()) return 0;

        Set<LocalDate> workoutDates = new HashSet<>();
        for (Workout w : workouts) {
            if ("COMPLETED".equalsIgnoreCase(w.getStatus()) && w.getWorkoutDate() != null) {
                workoutDates.add(w.getWorkoutDate());
            }
        }

        if (workoutDates.isEmpty()) return 0;

        LocalDate today = LocalDate.now();
        int streak = 0;

        // Check if user worked out today or if streak starts yesterday
        LocalDate checkDate = today;
        if (!workoutDates.contains(today)) {
            checkDate = today.minusDays(1);
            if (!workoutDates.contains(checkDate)) {
                return 0;
            }
        }

        while (workoutDates.contains(checkDate)) {
            streak++;
            checkDate = checkDate.minusDays(1);
        }

        return streak;
    }

    /**
     * Calculates percentage completion of a goal.
     */
    public static int calculateGoalProgress(double current, double target) {
        if (target <= 0) return 0;
        int pct = (int) Math.round((current / target) * 100);
        return Math.min(100, Math.max(0, pct));
    }

    /**
     * Calculates estimated calories burned based on exercise type, duration, and body weight.
     * Uses standard MET (Metabolic Equivalent of Task) values.
     */
    public static int estimateCaloriesBurned(String workoutType, int durationMinutes, double userWeightKg) {
        if (durationMinutes <= 0) return 0;
        double weight = userWeightKg > 0 ? userWeightKg : 70.0; // fallback standard 70kg
        double met;

        switch (workoutType != null ? workoutType.toUpperCase() : "OTHER") {
            case "STRENGTH":
                met = 5.0;
                break;
            case "CARDIO":
                met = 8.0;
                break;
            case "HIIT":
                met = 9.5;
                break;
            case "YOGA":
                met = 3.0;
                break;
            case "FLEXIBILITY":
                met = 2.5;
                break;
            case "ENDURANCE":
                met = 7.5;
                break;
            default:
                met = 5.0;
                break;
        }

        // Calories = MET * weight(kg) * (duration_minutes / 60)
        double burned = met * weight * (durationMinutes / 60.0);
        return (int) Math.round(burned);
    }
}
