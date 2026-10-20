package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.personaltracker.util.HealthCalculator;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model representing a logged sleep session.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SleepRecord {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("sleep_date")
    private LocalDate sleepDate = LocalDate.now();

    private OffsetDateTime bedtime;

    @JsonProperty("wake_time")
    private OffsetDateTime wakeTime;

    @JsonProperty("duration_minutes")
    private int durationMinutes = 0;

    @JsonProperty("quality_score")
    private Integer qualityScore = 3; // 1 (Poor) to 5 (Excellent)

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public SleepRecord() {}

    public SleepRecord(LocalDate sleepDate, OffsetDateTime bedtime, OffsetDateTime wakeTime, Integer qualityScore, String notes) {
        this.sleepDate = sleepDate != null ? sleepDate : LocalDate.now();
        this.bedtime = bedtime;
        this.wakeTime = wakeTime;
        this.qualityScore = qualityScore;
        this.notes = notes;
        this.durationMinutes = HealthCalculator.calculateSleepDurationMinutes(bedtime, wakeTime);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getSleepDate() { return sleepDate; }
    public void setSleepDate(LocalDate sleepDate) { this.sleepDate = sleepDate; }

    public OffsetDateTime getBedtime() { return bedtime; }
    public void setBedtime(OffsetDateTime bedtime) {
        this.bedtime = bedtime;
        recalculateDuration();
    }

    public OffsetDateTime getWakeTime() { return wakeTime; }
    public void setWakeTime(OffsetDateTime wakeTime) {
        this.wakeTime = wakeTime;
        recalculateDuration();
    }

    public int getDurationMinutes() {
        if (durationMinutes >= 24 * 60 && bedtime != null && wakeTime != null) {
            this.durationMinutes = HealthCalculator.calculateSleepDurationMinutes(bedtime, wakeTime);
        }
        return durationMinutes;
    }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    private void recalculateDuration() {
        if (bedtime != null && wakeTime != null) {
            this.durationMinutes = HealthCalculator.calculateSleepDurationMinutes(bedtime, wakeTime);
        }
    }

    @JsonIgnore
    public double getDurationHours() {
        return Math.round((durationMinutes / 60.0) * 10.0) / 10.0;
    }
}
