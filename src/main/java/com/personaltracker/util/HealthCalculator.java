package com.personaltracker.util;

import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;

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
        // Normalize against inadvertent multi-day calendar offsets (single sleep session is < 24h)
        if (minutes >= 24 * 60) {
            minutes = minutes % (24 * 60);
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
     * Uses standard MET (Metabolic Equivalent of Task) values with moderate intensity.
     */
    public static int estimateCaloriesBurned(String workoutType, int durationMinutes, double userWeightKg) {
        return estimateCaloriesBurned(workoutType, "MODERATE", durationMinutes, userWeightKg, null);
    }

    /**
     * Calculates estimated calories burned based on exercise type, intensity, duration,
     * athlete body weight, and logged resistance training volume (sets, reps, weight).
     * Incorporates Compendium of Physical Activities METs and mechanical resistance work + EPOC.
     */
    public static int estimateCaloriesBurned(String workoutType, String intensity, int durationMinutes, double userWeightKg, List<WorkoutExercise> exercises) {
        if (durationMinutes <= 0) return 0;
        double weight = userWeightKg > 0 ? userWeightKg : 70.0;

        String cleanType = workoutType != null ? workoutType.trim().toUpperCase() : "OTHER";
        String cleanIntensity = intensity != null ? intensity.trim().toUpperCase() : "MODERATE";

        double intensityMultiplier = 1.0;
        if (cleanIntensity.startsWith("LIGHT") || cleanIntensity.contains("LOW") || cleanIntensity.contains("0.8")) {
            intensityMultiplier = 0.80;
        } else if (cleanIntensity.startsWith("VIGOROUS") || cleanIntensity.contains("HIGH") || cleanIntensity.contains("1.25")) {
            intensityMultiplier = 1.25;
        }

        double baseMet;
        switch (cleanType) {
            case "STRENGTH":
                baseMet = 5.0;
                break;
            case "CARDIO":
                baseMet = 8.0;
                break;
            case "HIIT":
                baseMet = 10.0;
                break;
            case "YOGA":
                baseMet = 3.2;
                break;
            case "FLEXIBILITY":
            case "RECOVERY":
                baseMet = 2.5;
                break;
            case "ENDURANCE":
                baseMet = 8.5;
                break;
            case "WALKING":
                baseMet = 3.8;
                break;
            case "SWIMMING":
                baseMet = 7.0;
                break;
            case "BOXING":
            case "COMBAT":
                baseMet = 9.0;
                break;
            default:
                baseMet = 5.0;
                break;
        }

        double effectiveMet = baseMet * intensityMultiplier;

        // Base metabolic expenditure: Calories = MET * weight(kg) * (duration_minutes / 60)
        double durationHours = durationMinutes / 60.0;
        double baseBurned = effectiveMet * weight * durationHours;

        // For workouts with logged exercises: factor in mechanical work / volume load (sets * reps * weight)
        if (exercises != null && !exercises.isEmpty()) {
            double totalVolumeKg = 0;
            for (WorkoutExercise ex : exercises) {
                if (ex != null && ex.getSets() > 0 && ex.getReps() > 0) {
                    int reps = ex.getSets() * ex.getReps();
                    if (ex.getWeightKg() > 0) {
                        totalVolumeKg += reps * ex.getWeightKg();
                    } else {
                        // Bodyweight exercise (e.g. pull-ups, push-ups): estimate ~60% body weight
                        totalVolumeKg += reps * (weight * 0.60);
                    }
                }
            }

            if (cleanType.equals("STRENGTH")) {
                // In strength training: mechanical work + EPOC afterburn contributes ~0.015 kcal per kg lifted
                double volumeBonus = totalVolumeKg * 0.015 * intensityMultiplier;
                // Base metabolic burn adjusted for rest intervals between sets + actual tonnage lifted
                baseBurned = (baseMet * 0.80 * intensityMultiplier) * weight * durationHours + volumeBonus;
            } else {
                double volumeBonus = totalVolumeKg * 0.008;
                baseBurned += volumeBonus;
            }
        }

        return (int) Math.max(1, Math.round(baseBurned));
    }
}
