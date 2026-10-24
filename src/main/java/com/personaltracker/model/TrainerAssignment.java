package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * Represents a personalized fitness assignment (Goal, Workout Routine, or Task Schedule)
 * assigned by a Coach/Trainer to an enrolled student.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrainerAssignment {

    public enum Type {
        GOAL,
        WORKOUT,
        SCHEDULE
    }

    private String id;

    @JsonProperty("trainer_id")
    private String trainerId;

    @JsonProperty("trainer_name")
    private String trainerName = "Coach";

    @JsonProperty("student_id")
    private String studentId;

    @JsonProperty("assignment_type")
    private String assignmentType = "WORKOUT"; // "GOAL", "WORKOUT", "SCHEDULE"

    private String title = "";

    private String description = "";

    @JsonProperty("target_date")
    private LocalDate targetDate = LocalDate.now();

    @JsonProperty("target_time")
    private LocalTime targetTime;

    @JsonProperty("associated_id")
    private String associatedId;

    private String status = "PENDING"; // "PENDING", "ACKNOWLEDGED", "COMPLETED"

    @JsonProperty("is_read")
    private boolean isRead = false;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public TrainerAssignment() {}

    public TrainerAssignment(String trainerId, String trainerName, String studentId,
                             String assignmentType, String title, String description,
                             LocalDate targetDate, LocalTime targetTime) {
        this.trainerId = trainerId;
        this.trainerName = trainerName != null ? trainerName : "Coach";
        this.studentId = studentId;
        this.assignmentType = assignmentType != null ? assignmentType : "WORKOUT";
        this.title = title != null ? title : "";
        this.description = description != null ? description : "";
        this.targetDate = targetDate != null ? targetDate : LocalDate.now();
        this.targetTime = targetTime;
        this.status = "PENDING";
        this.isRead = false;
        this.createdAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTrainerId() { return trainerId; }
    public void setTrainerId(String trainerId) { this.trainerId = trainerId; }

    public String getTrainerName() { return trainerName; }
    public void setTrainerName(String trainerName) { this.trainerName = trainerName; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public LocalTime getTargetTime() { return targetTime; }
    public void setTargetTime(LocalTime targetTime) { this.targetTime = targetTime; }

    public String getAssociatedId() { return associatedId; }
    public void setAssociatedId(String associatedId) { this.associatedId = associatedId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(status);
    }

    public boolean isWorkout() {
        return "WORKOUT".equalsIgnoreCase(assignmentType);
    }

    public boolean isGoal() {
        return "GOAL".equalsIgnoreCase(assignmentType);
    }

    public boolean isSchedule() {
        return "SCHEDULE".equalsIgnoreCase(assignmentType);
    }
}
