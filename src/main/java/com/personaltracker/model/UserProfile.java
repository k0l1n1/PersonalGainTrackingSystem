package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Represents a user profile record in public.profiles.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {
    public static final String ROLE_MEMBER = "MEMBER";
    public static final String ROLE_TRAINER = "TRAINER";

    private String id;
    private String email;

    @JsonProperty("full_name")
    private String fullName = "";

    @JsonProperty("role")
    private String role = ROLE_MEMBER;

    @JsonProperty("user_code")
    private String userCode;

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

    public UserProfile() {
        if (this.userCode == null || this.userCode.isBlank()) {
            this.userCode = generateUniqueUserCode();
        }
    }

    public UserProfile(String id, String email, String fullName) {
        this.id = id;
        this.email = email;
        this.fullName = fullName != null ? fullName : "";
        this.userCode = generateUniqueUserCode();
    }

    public UserProfile(String id, String email, String fullName, String role) {
        this.id = id;
        this.email = email;
        this.fullName = fullName != null ? fullName : "";
        this.role = (role != null && !role.isBlank()) ? role : ROLE_MEMBER;
        this.userCode = generateUniqueUserCode();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @JsonProperty("full_name")
    public String getFullName() { return fullName; }
    @JsonProperty("full_name")
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

    @JsonProperty("role")
    public String getRole() { return role != null ? role : ROLE_MEMBER; }
    @JsonProperty("role")
    public void setRole(String role) { this.role = (role != null && !role.isBlank()) ? role : ROLE_MEMBER; }

    @JsonIgnore
    public boolean isTrainer() {
        return ROLE_TRAINER.equalsIgnoreCase(role);
    }

    @JsonProperty("user_code")
    public String getUserCode() {
        if (userCode == null || userCode.isBlank()) {
            userCode = generateUniqueUserCode();
        }
        return userCode;
    }

    @JsonProperty("user_code")
    public void setUserCode(String userCode) { this.userCode = userCode; }

    public static String generateUniqueUserCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("PG-");
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < 6; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }

    @JsonIgnore
    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        if (email != null && email.contains("@")) {
            return email.substring(0, email.indexOf('@'));
        }
        return isTrainer() ? "Coach" : "Athlete";
    }
}
