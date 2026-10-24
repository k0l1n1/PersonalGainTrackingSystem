package com.personaltracker;

import com.personaltracker.config.AppConfig;
import com.personaltracker.dao.TrainerAssignmentDAO;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Trainer Assignment & Student Notification Test")
public class TrainerAssignmentTest {

    private TrainerAssignmentDAO dao;
    private SupabaseClient client;

    @BeforeEach
    void setUp() {
        AppConfig.getInstance().setMockMode(true);
        client = SupabaseClient.getInstance();
        client.enableDemoMode();
        dao = new TrainerAssignmentDAO();
    }

    @Test
    @DisplayName("Trainer can assign a fitness goal to an enrolled student")
    void testAssignGoalToStudent() throws Exception {
        String trainerId = "trainer-coach-99";
        String trainerName = "Coach David Miller";
        String studentId = "student-sarah-101";

        Goal goal = new Goal("Reach Target Weight 60kg", "Weight", 60.0, 63.5, "kg", LocalDate.now().plusMonths(2));
        String instructions = "Keep clean nutrition and hit daily 130g protein target.";

        TrainerAssignment assignment = dao.assignGoalToStudent(trainerId, trainerName, studentId, goal, instructions);

        assertNotNull(assignment);
        assertEquals("GOAL", assignment.getAssignmentType());
        assertEquals("Reach Target Weight 60kg", assignment.getTitle());
        assertEquals(studentId, assignment.getStudentId());
        assertEquals(trainerName, assignment.getTrainerName());
        assertTrue(assignment.isPending());

        // Verify goal was saved with studentId
        List<Goal> studentGoals = client.getGoalsForUser(studentId);
        assertNotNull(studentGoals);
        assertTrue(studentGoals.stream().anyMatch(g -> g.getTitle().contains("Target Weight") && studentId.equals(g.getUserId())));
    }

    @Test
    @DisplayName("Trainer can schedule and assign a tailored workout routine to a student")
    void testAssignWorkoutToStudent() throws Exception {
        String trainerId = "trainer-coach-99";
        String trainerName = "Coach David Miller";
        String studentId = "student-marcus-102";

        Workout workout = new Workout("Heavy Back & Deadlift Focus", "Strength", LocalDate.now().plusDays(1), 60, 500);
        workout.addExercise(new WorkoutExercise("Conventional Deadlift", 4, 5, 145.0));
        workout.addExercise(new WorkoutExercise("Barbell Row", 3, 8, 85.0));

        String notes = "Aim for clean form on top deadlift sets. Rest 2 mins between sets.";

        TrainerAssignment assignment = dao.assignWorkoutToStudent(trainerId, trainerName, studentId, workout, notes);

        assertNotNull(assignment);
        assertEquals("WORKOUT", assignment.getAssignmentType());
        assertEquals("Heavy Back & Deadlift Focus", assignment.getTitle());
        assertEquals(studentId, assignment.getStudentId());
        assertTrue(assignment.isPending());

        // Verify workout was saved with studentId and status PLANNED
        List<Workout> studentWorkouts = client.getWorkoutsForUser(studentId);
        assertNotNull(studentWorkouts);
        assertTrue(studentWorkouts.stream().anyMatch(w -> w.getTitle().contains("Heavy Back") && "PLANNED".equals(w.getStatus())));
    }

    @Test
    @DisplayName("Trainer can schedule a task / reminder for a student")
    void testAssignScheduleToStudent() throws Exception {
        String trainerId = "trainer-coach-99";
        String trainerName = "Coach David Miller";
        String studentId = "student-sarah-101";

        String title = "Sunday Body Composition Check-In";
        String instructions = "Step on the scale fasting and take waist measurement.";
        LocalDate date = LocalDate.now().plusDays(3);
        LocalTime time = LocalTime.of(8, 30);

        TrainerAssignment assignment = dao.assignScheduleToStudent(trainerId, trainerName, studentId, title, instructions, date, time);

        assertNotNull(assignment);
        assertEquals("SCHEDULE", assignment.getAssignmentType());
        assertEquals(title, assignment.getTitle());
        assertEquals(studentId, assignment.getStudentId());
        assertEquals(date, assignment.getTargetDate());
    }

    @Test
    @DisplayName("Student can view assignments and acknowledge them")
    void testStudentAcknowledgeAssignment() {
        String studentId = "student-sarah-101";

        List<TrainerAssignment> before = dao.getAssignmentsForStudent(studentId);
        assertNotNull(before);
        assertFalse(before.isEmpty(), "Demo student Sarah Connor has pre-seeded assignment");

        TrainerAssignment target = before.get(0);
        String targetId = target.getId();

        // Acknowledge assignment
        dao.acknowledgeAssignment(studentId, targetId);

        List<TrainerAssignment> after = dao.getAssignmentsForStudent(studentId);
        TrainerAssignment updated = after.stream().filter(a -> targetId.equals(a.getId())).findFirst().orElse(null);
        assertNotNull(updated);
        assertEquals("ACKNOWLEDGED", updated.getStatus());
        assertTrue(updated.isRead());
    }

    @Test
    @DisplayName("Student notification state shows clean state when no assignments exist")
    void testNoAssignmentsCleanNotificationState() {
        String emptyStudentId = "student-newbie-no-assigns-999";
        List<TrainerAssignment> assignments = dao.getAssignmentsForStudent(emptyStudentId);
        assertNotNull(assignments);
        assertTrue(assignments.isEmpty(), "New student with no assignments returns empty list");

        List<TrainerAssignment> pending = dao.getPendingOrUnreadAssignments(emptyStudentId);
        assertNotNull(pending);
        assertTrue(pending.isEmpty(), "No pending assignments to notify");
    }
}
