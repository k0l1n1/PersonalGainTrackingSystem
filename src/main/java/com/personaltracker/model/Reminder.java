package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Model representing a reminder event.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Reminder {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    private String title = "";

    private String description;

    @JsonProperty("reminder_date")
    private LocalDate reminderDate = LocalDate.now();

    @JsonProperty("reminder_time")
    private LocalTime reminderTime = LocalTime.of(8, 0);

    @JsonProperty("is_completed")
    private boolean isCompleted = false;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public Reminder() {}

    public Reminder(String title, String description, LocalDate reminderDate, LocalTime reminderTime) {
        this.title = title;
        this.description = description;
        this.reminderDate = reminderDate;
        this.reminderTime = reminderTime;
        this.isCompleted = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getReminderDate() { return reminderDate; }
    public void setReminderDate(LocalDate reminderDate) { this.reminderDate = reminderDate; }

    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isDueOrOverdue() {
        if (isCompleted) return false;
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        if (reminderDate.isBefore(today)) return true;
        if (reminderDate.isEqual(today)) {
            return reminderTime.isBefore(now) || reminderTime.equals(now);
        }
        return false;
    }
}
