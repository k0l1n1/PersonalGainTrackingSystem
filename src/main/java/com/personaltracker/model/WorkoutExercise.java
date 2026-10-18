package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Represents an individual exercise within a workout session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkoutExercise {
    private String id;

    @JsonProperty("workout_id")
    private String workoutId;

    @JsonProperty("exercise_name")
    private String exerciseName;

    private int sets = 1;
    private int reps = 1;

    @JsonProperty("weight_kg")
    private double weightKg = 0.0;

    private String notes;

    @JsonProperty("order_index")
    private int orderIndex = 0;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public WorkoutExercise() {}

    public WorkoutExercise(String exerciseName, int sets, int reps, double weightKg) {
        this.exerciseName = exerciseName;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkoutId() { return workoutId; }
    public void setWorkoutId(String workoutId) { this.workoutId = workoutId; }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public int getSets() { return sets; }
    public void setSets(int sets) { this.sets = sets; }

    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    @JsonIgnore
    public double getVolumeLoad() {
        return sets * reps * weightKg;
    }
}
