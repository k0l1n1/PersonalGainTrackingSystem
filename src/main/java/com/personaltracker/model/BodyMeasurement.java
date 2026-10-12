package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.personaltracker.util.HealthCalculator;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Model representing a physical body measurement check-in.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BodyMeasurement {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("recorded_date")
    private LocalDate recordedDate = LocalDate.now();

    @JsonProperty("weight_kg")
    private double weightKg;

    @JsonProperty("height_cm")
    private Double heightCm;

    private Double bmi;

    @JsonProperty("waist_cm")
    private Double waistCm;

    @JsonProperty("chest_cm")
    private Double chestCm;

    @JsonProperty("arm_cm")
    private Double armCm;

    @JsonProperty("hips_cm")
    private Double hipsCm;

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    public BodyMeasurement() {}

    public BodyMeasurement(LocalDate recordedDate, double weightKg, Double heightCm) {
        this.recordedDate = recordedDate;
        this.weightKg = weightKg;
        this.heightCm = heightCm;
        recalculateBmi();
    }

    public void recalculateBmi() {
        if (weightKg > 0 && heightCm != null && heightCm > 0) {
            this.bmi = HealthCalculator.calculateBmi(weightKg, heightCm);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDate getRecordedDate() { return recordedDate; }
    public void setRecordedDate(LocalDate recordedDate) { this.recordedDate = recordedDate; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) {
        this.weightKg = weightKg;
        recalculateBmi();
    }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) {
        this.heightCm = heightCm;
        recalculateBmi();
    }

    public Double getBmi() {
        if (bmi == null && weightKg > 0 && heightCm != null && heightCm > 0) {
            recalculateBmi();
        }
        return bmi;
    }
    public void setBmi(Double bmi) { this.bmi = bmi; }

    public Double getWaistCm() { return waistCm; }
    public void setWaistCm(Double waistCm) { this.waistCm = waistCm; }

    public Double getChestCm() { return chestCm; }
    public void setChestCm(Double chestCm) { this.chestCm = chestCm; }

    public Double getArmCm() { return armCm; }
    public void setArmCm(Double armCm) { this.armCm = armCm; }

    public Double getHipsCm() { return hipsCm; }
    public void setHipsCm(Double hipsCm) { this.hipsCm = hipsCm; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
