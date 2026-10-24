package com.personaltracker;

import com.personaltracker.config.AppConfig;
import com.personaltracker.dao.TrainerStudentDAO;
import com.personaltracker.model.Goal;
import com.personaltracker.model.StudentProgressDetail;
import com.personaltracker.model.TrainerStudent;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.model.Workout;
import com.personaltracker.model.WorkoutExercise;
import com.personaltracker.service.SupabaseClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Trainer-Student enrollment, role assignment,
 * unique code generation, validation constraints, and progress tracking data access.
 */
public class TrainerStudentTest {

    private TrainerStudentDAO dao;
    private SupabaseClient client;

    @BeforeEach
    public void setup() {
        AppConfig.getInstance().setMockMode(true);
        client = SupabaseClient.getInstance();
        client.enableDemoMode();
        dao = new TrainerStudentDAO();
        dao.clearRosterForTrainer("test-trainer-99");
        dao.clearRosterForTrainer("trainer-dup-test");
        dao.clearRosterForTrainer("trainer-remove-test");
        dao.clearRosterForTrainer("test-persistent-coach-1");
    }

    @Test
    @DisplayName("User code generation produces expected format PG-XXXXXX")
    public void testUserCodeGeneration() {
        String code1 = UserProfile.generateUniqueUserCode();
        String code2 = UserProfile.generateUniqueUserCode();

        assertNotNull(code1);
        assertNotNull(code2);
        assertTrue(code1.startsWith("PG-"), "Code should start with PG- prefix: " + code1);
        assertEquals(9, code1.length(), "Code format should be PG-XXXXXX (9 chars)");
        assertNotEquals(code1, code2, "Subsequent codes should be distinct");
    }

    @Test
    @DisplayName("Role defaults to MEMBER and isTrainer accurately reflects TRAINER role")
    public void testRoleDefaultAndTrainerDetection() {
        UserProfile member = new UserProfile("u-1", "member@test.com", "Member Test");
        assertEquals(UserProfile.ROLE_MEMBER, member.getRole());
        assertFalse(member.isTrainer());

        UserProfile trainer = new UserProfile("u-2", "coach@test.com", "Coach Test", UserProfile.ROLE_TRAINER);
        assertEquals(UserProfile.ROLE_TRAINER, trainer.getRole());
        assertTrue(trainer.isTrainer());
        assertEquals("Coach Test", trainer.getDisplayName());

        UserProfile namelessTrainer = new UserProfile("u-3", null, null, UserProfile.ROLE_TRAINER);
        assertEquals("Coach", namelessTrainer.getDisplayName());
    }

    @Test
    @DisplayName("Trainer successfully enrolls student via valid unique student code")
    public void testEnrollStudentByCodeSuccess() throws Exception {
        String trainerId = "test-trainer-99";
        // Pre-seeded demo student has code PG-MARCUS
        TrainerStudent enrolled = dao.enrollStudentByCode(trainerId, "PG-MARCUS");

        assertNotNull(enrolled);
        assertEquals(trainerId, enrolled.getTrainerId());
        assertNotNull(enrolled.getStudentProfile());
        assertEquals("Marcus Vance", enrolled.getStudentProfile().getFullName());
        assertEquals("PG-MARCUS", enrolled.getStudentProfile().getUserCode());

        // Verify roster contains student
        List<TrainerStudent> roster = dao.getEnrolledStudents(trainerId);
        assertTrue(roster.stream().anyMatch(ts -> "PG-MARCUS".equalsIgnoreCase(ts.getStudentProfile().getUserCode())));
    }

    @Test
    @DisplayName("Trainer cannot enroll themselves as a student")
    public void testCannotEnrollSelfAsStudent() {
        String trainerId = "demo-user-123";
        // demo-user-123 has code PG-ALEX99
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            dao.enrollStudentByCode(trainerId, "PG-ALEX99");
        });
        assertTrue(ex.getMessage().contains("cannot enroll yourself"));
    }

    @Test
    @DisplayName("Enrolling duplicate student throws friendly validation error")
    public void testCannotEnrollDuplicateStudent() throws Exception {
        String trainerId = "trainer-dup-test";
        dao.enrollStudentByCode(trainerId, "PG-MARCUS");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            dao.enrollStudentByCode(trainerId, "pg-marcus");
        });
        assertTrue(ex.getMessage().contains("already in your student roster"));
    }

    @Test
    @DisplayName("Enrolling with invalid non-existent code throws clear error")
    public void testInvalidStudentCodeThrowsError() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            dao.enrollStudentByCode("trainer-1", "PG-INVALID99");
        });
        assertTrue(ex.getMessage().contains("No user found with code"));
    }

    @Test
    @DisplayName("Student progress detail accurately retrieves workouts, goals, and measurements")
    public void testStudentProgressDetailRetrieval() throws Exception {
        // Sarah Connor has pre-seeded workouts, goals, and measurements
        String sarahId = "student-sarah-101";
        StudentProgressDetail detail = dao.getStudentProgressDetail(sarahId);

        assertNotNull(detail);
        assertNotNull(detail.getProfile());
        assertEquals("Sarah Connor", detail.getProfile().getFullName());
        assertFalse(detail.getWorkouts().isEmpty(), "Should have workouts logged");
        assertFalse(detail.getGoals().isEmpty(), "Should have goals logged");
        assertFalse(detail.getMeasurements().isEmpty(), "Should have measurements logged");
        assertFalse(detail.getNutritionEntries().isEmpty(), "Should have nutrition logged");
        assertTrue(detail.getCurrentWeight() > 0, "Current weight should be positive");
    }

    @Test
    @DisplayName("Removing student unlinks them from trainer roster")
    public void testRemoveStudentFromRoster() throws Exception {
        String trainerId = "trainer-remove-test";
        dao.enrollStudentByCode(trainerId, "PG-MARCUS");

        List<TrainerStudent> before = dao.getEnrolledStudents(trainerId);
        assertEquals(1, before.size());

        dao.removeStudent(trainerId, "student-marcus-102");

        List<TrainerStudent> after = dao.getEnrolledStudents(trainerId);
        assertTrue(after.isEmpty(), "Roster should be empty after removal");
    }

    @Test
    @DisplayName("Jackson serialization excludes computed helper methods and non-database columns")
    public void testJacksonSerializationExcludesNonColumns() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // 1. UserProfile serialization
        UserProfile p = new UserProfile("u-123", "athlete@gym.com", "Test User", UserProfile.ROLE_TRAINER);
        String profileJson = mapper.writeValueAsString(p);
        assertFalse(profileJson.contains("displayName"), "Should not serialize displayName");
        assertFalse(profileJson.contains("\"trainer\":"), "Should not serialize trainer boolean");
        assertTrue(profileJson.contains("full_name"), "Should serialize full_name in snake_case");
        assertTrue(profileJson.contains("user_code"), "Should serialize user_code in snake_case");
        assertTrue(profileJson.contains("role"), "Should serialize role");

        // 2. Goal serialization
        Goal g = new Goal("Bench Press 100kg", "Strength", 100, 80, "kg", java.time.LocalDate.now().plusMonths(1));
        String goalJson = mapper.writeValueAsString(g);
        assertFalse(goalJson.contains("progressPercentage"), "Should not serialize progressPercentage");
        assertFalse(goalJson.contains("\"completed\":"), "Should not serialize completed boolean");
        assertFalse(goalJson.contains("\"overdue\":"), "Should not serialize overdue boolean");

        // 3. Workout serialization
        Workout w = new Workout("Morning Cardio", "Cardio", java.time.LocalDate.now(), 30, 250);
        w.addExercise(new WorkoutExercise("Running", 1, 1, 0.0));
        String workoutJson = mapper.writeValueAsString(w);
        assertFalse(workoutJson.contains("exercises"), "Should not serialize exercises inside workout body");
        assertFalse(workoutJson.contains("totalWeightLiftedKg"), "Should not serialize totalWeightLiftedKg");

        // 4. WorkoutExercise serialization
        WorkoutExercise ex = new WorkoutExercise("Squat", 3, 10, 80.0);
        String exJson = mapper.writeValueAsString(ex);
        assertFalse(exJson.contains("volumeLoad"), "Should not serialize volumeLoad");
    }

    @Test
    @DisplayName("Enrolled students persist across DAO instances and only deleted on explicit removal")
    public void testEnrolledStudentsPersistUntilExplicitlyRemoved() throws Exception {
        String testTrainer = "test-persistent-coach-1";

        // 1. Enroll Marcus
        dao.enrollStudentByCode(testTrainer, "PG-MARCUS");

        // 2. Query with a new DAO instance to simulate app restart
        TrainerStudentDAO freshDao = new TrainerStudentDAO();
        List<TrainerStudent> roster = freshDao.getEnrolledStudents(testTrainer);
        assertEquals(1, roster.size(), "Student should be saved across DAO instances");
        assertEquals("student-marcus-102", roster.get(0).getStudentId());
        assertEquals(4, roster.get(0).getTotalWorkouts(), "Marcus should show 4 logged workouts");

        // 3. Enroll Sarah
        freshDao.enrollStudentByCode(testTrainer, "PG-SARAH7");
        roster = freshDao.getEnrolledStudents(testTrainer);
        assertEquals(2, roster.size(), "Both enrolled students should be saved");

        // 4. Student is ONLY deleted when explicitly removed by trainer
        freshDao.removeStudent(testTrainer, "student-marcus-102");

        TrainerStudentDAO afterDeleteDao = new TrainerStudentDAO();
        List<TrainerStudent> remaining = afterDeleteDao.getEnrolledStudents(testTrainer);
        assertEquals(1, remaining.size(), "Marcus was removed; only Sarah should remain");
        assertEquals("student-sarah-101", remaining.get(0).getStudentId());
        assertEquals(4, remaining.get(0).getTotalWorkouts(), "Sarah should show 4 logged workouts");

        // Clean up
        afterDeleteDao.removeStudent(testTrainer, "student-sarah-101");
        assertTrue(afterDeleteDao.getEnrolledStudents(testTrainer).isEmpty());
    }
}
