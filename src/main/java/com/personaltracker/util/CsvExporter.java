package com.personaltracker.util;

import com.personaltracker.model.BodyMeasurement;
import com.personaltracker.model.NutritionEntry;
import com.personaltracker.model.Workout;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Utility for exporting application analytics and records to CSV files.
 */
public final class CsvExporter {

    private CsvExporter() {
        // Prevent instantiation of utility class
    }

    public static void exportWorkouts(List<Workout> workouts, File targetFile) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(bw)) {
            writer.println("Workout Date,Title,Type,Duration (Mins),Calories Burned,Status,Notes");
            if (workouts != null) {
                for (Workout w : workouts) {
                    writer.printf(Locale.US, "\"%s\",\"%s\",\"%s\",%d,%d,\"%s\",\"%s\"%n",
                            w.getWorkoutDate() != null ? w.getWorkoutDate().toString() : "",
                            escape(w.getTitle()),
                            escape(w.getWorkoutType()),
                            w.getDurationMinutes(),
                            w.getCaloriesBurned(),
                            escape(w.getStatus()),
                            escape(w.getNotes())
                    );
                }
            }
        }
    }

    public static void exportMeasurements(List<BodyMeasurement> measurements, File targetFile) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(bw)) {
            writer.println("Date,Weight (kg),Height (cm),BMI,Waist (cm),Chest (cm),Arm (cm),Notes");
            if (measurements != null) {
                for (BodyMeasurement m : measurements) {
                    double h = m.getHeightCm() != null ? m.getHeightCm() : 0.0;
                    double bmi = m.getBmi() != null ? m.getBmi() : 0.0;
                    double waist = m.getWaistCm() != null ? m.getWaistCm() : 0.0;
                    double chest = m.getChestCm() != null ? m.getChestCm() : 0.0;
                    double arm = m.getArmCm() != null ? m.getArmCm() : 0.0;

                    writer.printf(Locale.US, "\"%s\",%.2f,%.2f,%.1f,%.2f,%.2f,%.2f,\"%s\"%n",
                            m.getRecordedDate() != null ? m.getRecordedDate().toString() : "",
                            m.getWeightKg(),
                            h,
                            bmi,
                            waist,
                            chest,
                            arm,
                            escape(m.getNotes())
                    );
                }
            }
        }
    }

    public static void exportNutrition(List<NutritionEntry> entries, File targetFile) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(targetFile.toPath(), StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(bw)) {
            writer.println("Date,Meal Type,Food Name,Serving,Calories,Protein (g),Carbs (g),Fat (g),Notes");
            if (entries != null) {
                for (NutritionEntry n : entries) {
                    writer.printf(Locale.US, "\"%s\",\"%s\",\"%s\",\"%s\",%d,%.1f,%.1f,%.1f,\"%s\"%n",
                            n.getEntryDate() != null ? n.getEntryDate().toString() : "",
                            escape(n.getMealType()),
                            escape(n.getFoodName()),
                            escape(n.getServingSize()),
                            n.getCalories(),
                            n.getProteinG(),
                            n.getCarbsG(),
                            n.getFatG(),
                            escape(n.getNotes())
                    );
                }
            }
        }
    }

    private static String escape(String input) {
        if (input == null) return "";
        return input.replace("\"", "\"\"");
    }
}
