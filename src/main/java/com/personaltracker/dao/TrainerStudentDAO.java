package com.personaltracker.dao;

import com.personaltracker.config.AppConfig;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;

import java.time.LocalDate;
import java.util.*;

/**
 * Data Access Object for Trainer-Student enrollments and student fitness inspection.
 */
public class TrainerStudentDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    public List<TrainerStudent> getEnrolledStudents(String trainerId) throws Exception {
        if (trainerId == null) return new ArrayList<>();

        List<TrainerStudent> relationships;
        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            relationships = client.getTrainerStudents(trainerId);
        } else {
            relationships = client.executeGetList("trainer_students", "trainer_id=eq." + trainerId + "&order=enrolled_date.desc", TrainerStudent.class);
        }

        // Enrich each relationship with student's profile, total workouts, active goals, latest weight
        for (TrainerStudent ts : relationships) {
            enrichStudentSummary(ts);
        }

        return relationships;
    }

    public TrainerStudent enrollStudentByCode(String trainerId, String inputCode) throws Exception {
        if (inputCode == null || inputCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a student code.");
        }
        String cleanCode = inputCode.trim().toUpperCase();

        UserProfile studentProfile = null;
        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            studentProfile = client.findProfileByCode(cleanCode);
        } else {
            List<UserProfile> list = client.executeGetList("profiles", "user_code=ilike." + cleanCode, UserProfile.class);
            if (list != null && !list.isEmpty()) {
                studentProfile = list.get(0);
            }
        }

        if (studentProfile == null) {
            throw new IllegalArgumentException("No user found with code: " + cleanCode + ".\nPlease double check the code.");
        }

        if (studentProfile.getId() != null && studentProfile.getId().equalsIgnoreCase(trainerId)) {
            throw new IllegalArgumentException("You cannot enroll yourself as a student.");
        }

        // Check if already enrolled
        List<TrainerStudent> existing = getEnrolledStudents(trainerId);
        for (TrainerStudent ts : existing) {
            if (studentProfile.getId().equalsIgnoreCase(ts.getStudentId())) {
                throw new IllegalArgumentException(studentProfile.getDisplayName() + " (" + cleanCode + ") is already in your student roster.");
            }
        }

        TrainerStudent newRel = new TrainerStudent(trainerId, studentProfile.getId());
        newRel.setStudentProfile(studentProfile);
        newRel.setEnrolledDate(LocalDate.now());

        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            newRel.setId(UUID.randomUUID().toString());
            client.addMockTrainerStudent(newRel);
        } else {
            newRel = client.executePost("trainer_students", newRel, TrainerStudent.class);
            if (newRel != null) {
                newRel.setStudentProfile(studentProfile);
            }
        }

        enrichStudentSummary(newRel);
        return newRel;
    }

    public void removeStudent(String trainerId, String studentId) throws Exception {
        if (trainerId == null || studentId == null) return;
        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            client.removeMockTrainerStudent(trainerId, studentId);
        } else {
            client.executeDelete("trainer_students", "trainer_id=eq." + trainerId + "&student_id=eq." + studentId);
        }
    }

    public StudentProgressDetail getStudentProgressDetail(String studentId) throws Exception {
        UserProfile profile = null;
        List<Workout> workouts;
        List<Goal> goals;
        List<BodyMeasurement> measurements;
        List<NutritionEntry> nutrition;

        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            profile = client.getMockProfile(studentId);
            workouts = client.getWorkoutsForUser(studentId);
            goals = client.getGoalsForUser(studentId);
            measurements = client.getMeasurementsForUser(studentId);
            nutrition = client.getNutritionForUser(studentId);
        } else {
            List<UserProfile> plist = client.executeGetList("profiles", "id=eq." + studentId, UserProfile.class);
            if (plist != null && !plist.isEmpty()) profile = plist.get(0);
            workouts = client.executeGetList("workouts", "user_id=eq." + studentId + "&order=workout_date.desc", Workout.class);
            goals = client.executeGetList("goals", "user_id=eq." + studentId + "&order=created_at.desc", Goal.class);
            measurements = client.executeGetList("body_measurements", "user_id=eq." + studentId + "&order=recorded_date.desc", BodyMeasurement.class);
            nutrition = client.executeGetList("nutrition_entries", "user_id=eq." + studentId + "&order=entry_date.desc", NutritionEntry.class);
        }

        StudentProgressDetail detail = new StudentProgressDetail(profile);
        detail.setWorkouts(workouts);
        detail.setGoals(goals);
        detail.setMeasurements(measurements);
        detail.setNutritionEntries(nutrition);
        return detail;
    }

    private void enrichStudentSummary(TrainerStudent ts) {
        if (ts == null || ts.getStudentId() == null) return;
        try {
            if (ts.getStudentProfile() == null) {
                if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
                    ts.setStudentProfile(client.getMockProfile(ts.getStudentId()));
                } else {
                    List<UserProfile> list = client.executeGetList("profiles", "id=eq." + ts.getStudentId(), UserProfile.class);
                    if (list != null && !list.isEmpty()) ts.setStudentProfile(list.get(0));
                }
            }

            // Get count of workouts & latest weight
            List<Workout> wList;
            List<Goal> gList;
            List<BodyMeasurement> mList;

            if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken())) {
                wList = client.getWorkoutsForUser(ts.getStudentId());
                gList = client.getGoalsForUser(ts.getStudentId());
                mList = client.getMeasurementsForUser(ts.getStudentId());
            } else {
                wList = client.executeGetList("workouts", "user_id=eq." + ts.getStudentId(), Workout.class);
                gList = client.executeGetList("goals", "user_id=eq." + ts.getStudentId(), Goal.class);
                mList = client.executeGetList("body_measurements", "user_id=eq." + ts.getStudentId() + "&order=recorded_date.desc&limit=1", BodyMeasurement.class);
            }

            ts.setTotalWorkouts(wList.size());
            ts.setActiveGoals((int) gList.stream().filter(g -> "IN_PROGRESS".equalsIgnoreCase(g.getStatus())).count());
            if (!mList.isEmpty()) {
                ts.setLatestWeightKg(mList.get(0).getWeightKg());
            } else if (ts.getStudentProfile() != null && ts.getStudentProfile().getTargetWeightKg() != null) {
                ts.setLatestWeightKg(ts.getStudentProfile().getTargetWeightKg());
            }
            if (!wList.isEmpty()) {
                ts.setLastActiveDate(wList.get(0).getWorkoutDate());
            }
        } catch (Exception ignored) {}
    }
}
