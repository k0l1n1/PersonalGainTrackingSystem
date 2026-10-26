package com.personaltracker.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the comprehensive fitness and health data of a student for trainer review.
 */
public class StudentProgressDetail {
    private UserProfile profile;
    private List<Workout> workouts = new ArrayList<>();
    private List<Goal> goals = new ArrayList<>();
    private List<BodyMeasurement> measurements = new ArrayList<>();
    private List<NutritionEntry> nutritionEntries = new ArrayList<>();
    private List<TrainerAssignment> assignments = new ArrayList<>();

    public StudentProgressDetail() {}

    public StudentProgressDetail(UserProfile profile) {
        this.profile = profile;
    }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }

    public List<Workout> getWorkouts() { return workouts; }
    public void setWorkouts(List<Workout> workouts) { this.workouts = workouts != null ? workouts : new ArrayList<>(); }

    public List<Goal> getGoals() { return goals; }
    public void setGoals(List<Goal> goals) { this.goals = goals != null ? goals : new ArrayList<>(); }

    public List<TrainerAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<TrainerAssignment> assignments) { this.assignments = assignments != null ? assignments : new ArrayList<>(); }

    public List<BodyMeasurement> getMeasurements() { return measurements; }
    public void setMeasurements(List<BodyMeasurement> measurements) { this.measurements = measurements != null ? measurements : new ArrayList<>(); }

    public List<NutritionEntry> getNutritionEntries() { return nutritionEntries; }
    public void setNutritionEntries(List<NutritionEntry> nutritionEntries) { this.nutritionEntries = nutritionEntries != null ? nutritionEntries : new ArrayList<>(); }

    public double getCurrentWeight() {
        if (!measurements.isEmpty()) {
            return measurements.get(0).getWeightKg();
        }
        if (profile != null && profile.getTargetWeightKg() != null) {
            return profile.getTargetWeightKg();
        }
        return 70.0;
    }

    public double getStartingWeight() {
        if (!measurements.isEmpty()) {
            return measurements.get(measurements.size() - 1).getWeightKg();
        }
        return getCurrentWeight();
    }

    public int getCompletedWorkoutsCount() {
        return (int) workouts.stream()
                .filter(w -> "COMPLETED".equalsIgnoreCase(w.getStatus()))
                .count();
    }

    public int getActiveGoalsCount() {
        return (int) goals.stream()
                .filter(g -> "IN_PROGRESS".equalsIgnoreCase(g.getStatus()))
                .count();
    }
}
