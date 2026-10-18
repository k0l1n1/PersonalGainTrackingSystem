package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model representing a user fitness or personal development goal.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Goal {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    private String title = "";

    private String category = "Weight"; // Weight, Workout Frequency, Strength, Endurance, Nutrition, Habit

    @JsonProperty("target_value")
    private double targetValue = 0.0;

    @JsonProperty("current_value")
    private double currentValue = 0.0;

    private String unit = "kg";

    @JsonProperty("start_date")
    private LocalDate startDate = LocalDate.now();

    @JsonProperty("target_date")
    private LocalDate targetDate = LocalDate.now().plusMonths(1);

    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, OVERDUE, CANCELLED

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public Goal() {}

    public Goal(String title, String category, double targetValue, double currentValue, String unit, LocalDate targetDate) {
        this.title = title;
        this.category = category;
        this.targetValue = targetValue;
        this.currentValue = currentValue;
        this.unit = unit;
        this.startDate = LocalDate.now();
        this.targetDate = targetDate;
        this.status = "IN_PROGRESS";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getTargetValue() { return targetValue; }
    public void setTargetValue(double targetValue) {
        this.targetValue = targetValue;
        checkStatus();
    }

    public double getCurrentValue() { return currentValue; }
    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
        checkStatus();
    }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
        checkStatus();
    }

    public String getStatus() {
        checkStatus();
        return status;
    }

    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @JsonIgnore
    public int getProgressPercentage() {
        if (targetValue <= 0) return 0;
        int pct = (int) Math.round((currentValue / targetValue) * 100);
        return Math.min(100, Math.max(0, pct));
    }

    public void checkStatus() {
        if ("CANCELLED".equals(status)) return;
        if (targetValue > 0 && currentValue >= targetValue) {
            status = "COMPLETED";
        } else if (targetDate != null && LocalDate.now().isAfter(targetDate)) {
            status = "OVERDUE";
        } else {
            status = "IN_PROGRESS";
        }
    }

    @JsonIgnore
    public boolean isCompleted() {
        checkStatus();
        return "COMPLETED".equals(status);
    }

    @JsonIgnore
    public boolean isOverdue() {
        checkStatus();
        return "OVERDUE".equals(status);
    }
}
