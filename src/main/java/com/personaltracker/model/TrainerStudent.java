package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Represents a relationship linking a personal trainer to an enrolled student.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainerStudent {
    private String id;

    @JsonProperty("trainer_id")
    private String trainerId;

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("enrolled_date")
    private LocalDate enrolledDate = LocalDate.now();

    private String notes;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    // Transient attributes populated for rich UI cards & progress tracking
    private UserProfile studentProfile;
    private int totalWorkouts = 0;
    private int activeGoals = 0;
    private Double latestWeightKg;
    private LocalDate lastActiveDate;

    public TrainerStudent() {}

    public TrainerStudent(String trainerId, String studentId) {
        this.trainerId = trainerId;
        this.studentId = studentId;
        this.enrolledDate = LocalDate.now();
    }

    public TrainerStudent(String trainerId, String studentId, String notes) {
        this.trainerId = trainerId;
        this.studentId = studentId;
        this.notes = notes;
        this.enrolledDate = LocalDate.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrainerId() { return trainerId; }
    public void setTrainerId(String trainerId) { this.trainerId = trainerId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public LocalDate getEnrolledDate() { return enrolledDate; }
    public void setEnrolledDate(LocalDate enrolledDate) { this.enrolledDate = enrolledDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public UserProfile getStudentProfile() { return studentProfile; }
    public void setStudentProfile(UserProfile studentProfile) { this.studentProfile = studentProfile; }

    public int getTotalWorkouts() { return totalWorkouts; }
    public void setTotalWorkouts(int totalWorkouts) { this.totalWorkouts = totalWorkouts; }

    public int getActiveGoals() { return activeGoals; }
    public void setActiveGoals(int activeGoals) { this.activeGoals = activeGoals; }

    public Double getLatestWeightKg() { return latestWeightKg; }
    public void setLatestWeightKg(Double latestWeightKg) { this.latestWeightKg = latestWeightKg; }

    public LocalDate getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDate lastActiveDate) { this.lastActiveDate = lastActiveDate; }
}
