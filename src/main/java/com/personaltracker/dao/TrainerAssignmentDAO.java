package com.personaltracker.dao;

import com.personaltracker.config.AppConfig;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Data Access Object for Trainer-to-Student Assignments (Goals, Scheduled Workouts, and Tasks)
 * and student notifications.
 */
public class TrainerAssignmentDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    /**
     * Retrieves all assignments assigned to the specified student.
     */
    public List<TrainerAssignment> getAssignmentsForStudent(String studentId) {
        if (studentId == null || studentId.isBlank()) return new ArrayList<>();

        List<TrainerAssignment> list = new ArrayList<>();
        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            list = new ArrayList<>(client.getTrainerAssignments(studentId));
        } else {
            try {
                list = new ArrayList<>(client.executeGetList("trainer_assignments",
                        "student_id=eq." + studentId + "&order=created_at.desc",
                        TrainerAssignment.class));
            } catch (Exception e) {
                list = new ArrayList<>();
            }
            // Merge in local fallback mock assignments seamlessly
            List<TrainerAssignment> local = client.getTrainerAssignments(studentId);
            for (TrainerAssignment l : local) {
                if (list.stream().noneMatch(a -> Objects.equals(a.getId(), l.getId()))) {
                    list.add(l);
                }
            }
        }

        // Sort descending by created_at or target_date
        list.sort((a, b) -> {
            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return 0;
        });

        return list;
    }

    /**
     * Returns assignments that are either unread or still pending student review.
     */
    public List<TrainerAssignment> getPendingOrUnreadAssignments(String studentId) {
        List<TrainerAssignment> all = getAssignmentsForStudent(studentId);
        List<TrainerAssignment> unread = new ArrayList<>();
        for (TrainerAssignment a : all) {
            if (!a.isRead() || a.isPending()) {
                unread.add(a);
            }
        }
        return unread;
    }

    /**
     * Assigns a fitness goal to an enrolled student.
     */
    public TrainerAssignment assignGoalToStudent(String trainerId, String trainerName, String studentId,
                                                  Goal goal, String coachingNotes) throws Exception {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be null.");
        }
        if (goal == null) {
            throw new IllegalArgumentException("Goal cannot be null.");
        }

        goal.setUserId(studentId);
        String prefix = "[Coach Assigned by " + (trainerName != null ? trainerName : "Coach") + "]";
        if (coachingNotes != null && !coachingNotes.isBlank()) {
            goal.setNotes(prefix + " " + coachingNotes.trim());
        } else if (goal.getNotes() == null || goal.getNotes().isBlank()) {
            goal.setNotes(prefix);
        }

        if (goal.getId() == null || goal.getId().isBlank()) {
            goal.setId(UUID.randomUUID().toString());
        }

        // Save Goal
        Goal savedGoal;
        try {
            savedGoal = client.executePost("goals", goal, Goal.class);
        } catch (Exception e) {
            savedGoal = client.saveMock("goals", goal, Goal.class);
        }

        // Create Assignment Notification Record
        TrainerAssignment assignment = new TrainerAssignment(
                trainerId,
                trainerName,
                studentId,
                "GOAL",
                goal.getTitle(),
                (coachingNotes != null && !coachingNotes.isBlank()) ? coachingNotes.trim() : ("Target: " + goal.getTargetValue() + " " + goal.getUnit()),
                goal.getTargetDate(),
                null
        );
        assignment.setId(UUID.randomUUID().toString());
        assignment.setAssociatedId(savedGoal != null ? savedGoal.getId() : goal.getId());
        assignment.setCreatedAt(OffsetDateTime.now());

        saveAssignment(assignment);
        return assignment;
    }

    /**
     * Schedules and assigns a workout routine to an enrolled student.
     */
    public TrainerAssignment assignWorkoutToStudent(String trainerId, String trainerName, String studentId,
                                                     Workout workout, String coachingNotes) throws Exception {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be null.");
        }
        if (workout == null) {
            throw new IllegalArgumentException("Workout cannot be null.");
        }

        workout.setUserId(studentId);
        workout.setStatus("PLANNED");
        String prefix = "[Coach Scheduled by " + (trainerName != null ? trainerName : "Coach") + "]";
        if (coachingNotes != null && !coachingNotes.isBlank()) {
            workout.setNotes(prefix + " " + coachingNotes.trim());
        } else if (workout.getNotes() == null || workout.getNotes().isBlank()) {
            workout.setNotes(prefix);
        }

        if (workout.getId() == null || workout.getId().isBlank()) {
            workout.setId(UUID.randomUUID().toString());
        }

        // Save workout
        Workout savedWorkout;
        try {
            savedWorkout = client.executePost("workouts", workout, Workout.class);
        } catch (Exception e) {
            savedWorkout = client.saveMock("workouts", workout, Workout.class);
        }
        String finalWorkoutId = (savedWorkout != null && savedWorkout.getId() != null) ? savedWorkout.getId() : workout.getId();

        // Save associated exercises
        if (workout.getExercises() != null && !workout.getExercises().isEmpty()) {
            int order = 0;
            for (WorkoutExercise ex : workout.getExercises()) {
                ex.setWorkoutId(finalWorkoutId);
                if (ex.getId() == null || ex.getId().isBlank()) {
                    ex.setId(UUID.randomUUID().toString());
                }
                ex.setOrderIndex(order++);
                try {
                    client.executePost("workout_exercises", ex, WorkoutExercise.class);
                } catch (Exception ignored) {}
            }
            if (savedWorkout != null) {
                savedWorkout.setExercises(workout.getExercises());
            }
        }

        // Create Assignment Notification Record
        StringBuilder details = new StringBuilder();
        if (coachingNotes != null && !coachingNotes.isBlank()) {
            details.append(coachingNotes.trim());
        } else {
            details.append(workout.getWorkoutType()).append(" routine (").append(workout.getDurationMinutes()).append(" mins)");
            if (workout.getExercises() != null && !workout.getExercises().isEmpty()) {
                details.append(" • ").append(workout.getExercises().size()).append(" exercises scheduled");
            }
        }

        TrainerAssignment assignment = new TrainerAssignment(
                trainerId,
                trainerName,
                studentId,
                "WORKOUT",
                workout.getTitle(),
                details.toString(),
                workout.getWorkoutDate(),
                null
        );
        assignment.setId(UUID.randomUUID().toString());
        assignment.setAssociatedId(finalWorkoutId);
        assignment.setCreatedAt(OffsetDateTime.now());

        saveAssignment(assignment);
        return assignment;
    }

    /**
     * Schedules a coaching task or check-in reminder for an enrolled student.
     */
    public TrainerAssignment assignScheduleToStudent(String trainerId, String trainerName, String studentId,
                                                      String title, String description, LocalDate date,
                                                      LocalTime time) throws Exception {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be null.");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Schedule task title cannot be empty.");
        }

        String prefix = "[Coach Task from " + (trainerName != null ? trainerName : "Coach") + "]";
        String fullDesc = prefix + " " + (description != null ? description.trim() : "");

        Reminder reminder = new Reminder(title.trim(), fullDesc, date != null ? date : LocalDate.now(), time != null ? time : LocalTime.of(9, 0));
        reminder.setUserId(studentId);
        reminder.setId(UUID.randomUUID().toString());

        Reminder saved;
        try {
            saved = client.executePost("reminders", reminder, Reminder.class);
        } catch (Exception e) {
            saved = client.saveMock("reminders", reminder, Reminder.class);
        }

        TrainerAssignment assignment = new TrainerAssignment(
                trainerId,
                trainerName,
                studentId,
                "SCHEDULE",
                title.trim(),
                description != null ? description.trim() : "Scheduled coaching task",
                date != null ? date : LocalDate.now(),
                time != null ? time : LocalTime.of(9, 0)
        );
        assignment.setId(UUID.randomUUID().toString());
        assignment.setAssociatedId(saved != null ? saved.getId() : reminder.getId());
        assignment.setCreatedAt(OffsetDateTime.now());

        saveAssignment(assignment);
        return assignment;
    }

    /**
     * Marks an assignment as acknowledged by the student.
     */
    public void acknowledgeAssignment(String studentId, String assignmentId) {
        if (assignmentId == null || studentId == null) return;
        List<TrainerAssignment> list = getAssignmentsForStudent(studentId);
        for (TrainerAssignment a : list) {
            if (assignmentId.equals(a.getId())) {
                a.setStatus("ACKNOWLEDGED");
                a.setRead(true);
                client.updateMockTrainerAssignment(a);
                try {
                    if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken())) {
                        client.executePatch("trainer_assignments", "id=eq." + assignmentId, a);
                    }
                } catch (Exception ignored) {}
                break;
            }
        }
    }

    /**
     * Marks all unread assignments for the student as read.
     */
    public void markAllAsRead(String studentId) {
        if (studentId == null) return;
        List<TrainerAssignment> list = getAssignmentsForStudent(studentId);
        for (TrainerAssignment a : list) {
            if (!a.isRead()) {
                a.setRead(true);
                client.updateMockTrainerAssignment(a);
                try {
                    if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken())) {
                        client.executePatch("trainer_assignments", "id=eq." + a.getId(), a);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void saveAssignment(TrainerAssignment assignment) throws Exception {
        if (assignment == null) return;
        client.addMockTrainerAssignment(assignment);
        if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            try {
                client.executePost("trainer_assignments", assignment, TrainerAssignment.class);
            } catch (Exception ignored) {
                // Table might not exist yet on remote, gracefully handled by in-memory local fallback store
            }
        }
    }
}
