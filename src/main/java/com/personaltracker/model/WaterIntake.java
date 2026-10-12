package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Model representing a logged water intake record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaterIntake {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("amount_ml")
    private int amountMl = 250;

    @JsonProperty("recorded_date")
    private LocalDate recordedDate = LocalDate.now();

    @JsonProperty("recorded_time")
    private LocalTime recordedTime = LocalTime.now();

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public WaterIntake() {}

    public WaterIntake(int amountMl, LocalDate recordedDate, LocalTime recordedTime) {
        this.amountMl = amountMl;
        this.recordedDate = recordedDate != null ? recordedDate : LocalDate.now();
        this.recordedTime = recordedTime != null ? recordedTime : LocalTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getAmountMl() { return amountMl; }
    public void setAmountMl(int amountMl) { this.amountMl = amountMl; }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) { this.recordedDate = recordedDate; }

    public LocalTime getRecordedTime() { return recordedTime; }
    public void setRecordedTime(LocalTime recordedTime) { this.recordedTime = recordedTime; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
