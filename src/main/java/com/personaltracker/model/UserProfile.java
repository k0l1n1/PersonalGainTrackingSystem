package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Represents a user profile record in public.profiles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {
    private String id;
    private String email;

    @JsonProperty("full_name")
    private String fullName = "";

    private String gender;

    @JsonProperty("date_of_birth")
    private LocalDate dateOfBirth;

    @JsonProperty("height_cm")
    private Double heightCm;

    @JsonProperty("target_weight_kg")
    private Double targetWeightKg;

    @JsonProperty("daily_calorie_target")
    private int dailyCalorieTarget = 2000;

    @JsonProperty("daily_water_target_ml")
    private int dailyWaterTargetMl = 2500;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    public UserProfile() {}

    public UserProfile(String id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName != null ? fullName : "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Double getHeightCm() { return heightCm; }
    public void setHeightCm(Double heightCm) { this.heightCm = heightCm; }

    public Double getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(Double targetWeightKg) { this.targetWeightKg = targetWeightKg; }

    public int getDailyCalorieTarget() { return dailyCalorieTarget; }
    public void setDailyCalorieTarget(int dailyCalorieTarget) { this.dailyCalorieTarget = dailyCalorieTarget; }

    public int getDailyWaterTargetMl() { return dailyWaterTargetMl; }
    public void setDailyWaterTargetMl(int dailyWaterTargetMl) { this.dailyWaterTargetMl = dailyWaterTargetMl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return "Athlete";
    }
}
