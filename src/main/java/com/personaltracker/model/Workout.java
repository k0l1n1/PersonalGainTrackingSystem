package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model representing a workout session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Workout {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    private String title = "";

    @JsonProperty("workout_type")
    private String workoutType = "Strength"; // Strength, Cardio, HIIT, Yoga, Flexibility, Endurance, Other

    @JsonProperty("workout_date")
    private LocalDate workoutDate = LocalDate.now();

    @JsonProperty("duration_minutes")
    private int durationMinutes = 0;

    @JsonProperty("calories_burned")
    private int caloriesBurned = 0;

    private String status = "COMPLETED"; // COMPLETED, PLANNED

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    // Associated exercises (can be populated via join or sub-query)
    @JsonIgnore
    private List<WorkoutExercise> exercises = new ArrayList<>();

    public Workout() {}

    public Workout(String title, String workoutType, LocalDate workoutDate, int durationMinutes, int caloriesBurned) {
        this.title = title;
        this.workoutType = workoutType;
        this.workoutDate = workoutDate;
        this.durationMinutes = durationMinutes;
        this.caloriesBurned = caloriesBurned;
        this.status = "COMPLETED";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public LocalDate getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDate workoutDate) { this.workoutDate = workoutDate; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public int getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(int caloriesBurned) { this.caloriesBurned = caloriesBurned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @JsonIgnore
    public List<WorkoutExercise> getExercises() { return exercises; }

    @JsonIgnore
    public void setExercises(List<WorkoutExercise> exercises) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }

    public void addExercise(WorkoutExercise exercise) {
        if (this.exercises == null) this.exercises = new ArrayList<>();
        this.exercises.add(exercise);
    }

    @JsonIgnore
    public double getTotalWeightLiftedKg() {
        if (exercises == null || exercises.isEmpty()) return 0.0;
        double total = 0.0;
        for (WorkoutExercise ex : exercises) {
            total += ex.getSets() * ex.getReps() * ex.getWeightKg();
        }
        return total;
    }
}
