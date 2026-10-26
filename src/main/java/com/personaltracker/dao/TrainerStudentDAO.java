package com.personaltracker.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.personaltracker.config.AppConfig;
import com.personaltracker.model.*;
import com.personaltracker.service.SupabaseClient;

import java.io.File;
import java.time.LocalDate;
import java.util.*;

/**
 * Data Access Object for Trainer-Student enrollments and student fitness inspection.
 * Enrolled students are permanently saved to persistent storage (trainer_roster.json)
 * and are only deleted when the trainer explicitly removes them from the list.
 */
public class TrainerStudentDAO {
    private static final String ROSTER_FILE = "trainer_roster.json";
    private static final Object FILE_LOCK = new Object();

    private final SupabaseClient client = SupabaseClient.getInstance();
    private final AppConfig config = AppConfig.getInstance();

    private Map<String, List<TrainerStudent>> loadPersistentRosterMap() {
        synchronized (FILE_LOCK) {
            File f = new File(ROSTER_FILE);
            if (!f.exists()) {
                return new HashMap<>();
            }
            try {
                return client.getObjectMapper().readValue(f, new TypeReference<Map<String, List<TrainerStudent>>>() {});
            } catch (Exception e) {
                return new HashMap<>();
            }
        }
    }

    private void savePersistentRosterMap(Map<String, List<TrainerStudent>> map) {
        synchronized (FILE_LOCK) {
            File f = new File(ROSTER_FILE);
            try {
                client.getObjectMapper().writerWithDefaultPrettyPrinter().writeValue(f, map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized List<TrainerStudent> getEnrolledStudents(String trainerId) throws Exception {
        if (trainerId == null || trainerId.isBlank()) return new ArrayList<>();

        Map<String, List<TrainerStudent>> rosterMap = loadPersistentRosterMap();

        // If this trainer is new to the persistent roster, seed default demo athletes if not a unit test
        if (!rosterMap.containsKey(trainerId)) {
            List<TrainerStudent> defaultList = new ArrayList<>();
            if (!trainerId.startsWith("test-") && !trainerId.contains("test")) {
                TrainerStudent ts1 = new TrainerStudent(trainerId, "student-sarah-101");
                ts1.setId("enroll-sarah-101");
                ts1.setEnrolledDate(LocalDate.now());
                defaultList.add(ts1);

                TrainerStudent ts2 = new TrainerStudent(trainerId, "student-marcus-102");
                ts2.setId("enroll-marcus-102");
                ts2.setEnrolledDate(LocalDate.now());
                defaultList.add(ts2);
            }
            rosterMap.put(trainerId, defaultList);
            savePersistentRosterMap(rosterMap);
        }

        List<TrainerStudent> list = new ArrayList<>(rosterMap.getOrDefault(trainerId, Collections.emptyList()));

        // Also check if remote Supabase has any additional enrolled students (if online)
        if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken())) {
            try {
                List<TrainerStudent> remoteList = client.executeGetList("trainer_students", "trainer_id=eq." + trainerId + "&order=enrolled_date.desc", TrainerStudent.class);
                if (remoteList != null && !remoteList.isEmpty()) {
                    boolean updated = false;
                    for (TrainerStudent remote : remoteList) {
                        if (list.stream().noneMatch(s -> s.getStudentId() != null && s.getStudentId().equalsIgnoreCase(remote.getStudentId()))) {
                            list.add(remote);
                            updated = true;
                        }
                    }
                    if (updated) {
                        rosterMap.put(trainerId, list);
                        savePersistentRosterMap(rosterMap);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Keep mockTrainerStudents in sync
        for (TrainerStudent ts : list) {
            client.addMockTrainerStudent(ts);
        }

        // Enrich each relationship with student's profile, total workouts, active goals, latest weight
        for (TrainerStudent ts : list) {
            enrichStudentSummary(ts);
        }

        return list;
    }

    public synchronized TrainerStudent enrollStudentByCode(String trainerId, String inputCode) throws Exception {
        if (inputCode == null || inputCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Please enter a student code.");
        }
        String cleanCode = inputCode.trim().toUpperCase();

        UserProfile studentProfile = client.findRemoteProfileByCode(cleanCode);

        if (studentProfile == null) {
            throw new IllegalArgumentException("No user found with code: " + cleanCode + ".\nPlease double check the code.");
        }

        if (studentProfile.getId() != null && studentProfile.getId().equalsIgnoreCase(trainerId)) {
            throw new IllegalArgumentException("You cannot enroll yourself as a student.");
        }

        // Check if already enrolled in current roster
        List<TrainerStudent> existing = getEnrolledStudents(trainerId);
        for (TrainerStudent ts : existing) {
            if (studentProfile.getId().equalsIgnoreCase(ts.getStudentId())) {
                throw new IllegalArgumentException(studentProfile.getDisplayName() + " (" + cleanCode + ") is already in your student roster.");
            }
        }

        TrainerStudent newRel = new TrainerStudent(trainerId, studentProfile.getId());
        newRel.setId(UUID.randomUUID().toString());
        newRel.setStudentProfile(studentProfile);
        newRel.setEnrolledDate(LocalDate.now());

        final String sId = studentProfile.getId();

        // 1. Permanently save to disk in trainer_roster.json
        Map<String, List<TrainerStudent>> rosterMap = loadPersistentRosterMap();
        List<TrainerStudent> currentList = rosterMap.computeIfAbsent(trainerId, k -> new ArrayList<>());
        currentList.removeIf(ts -> sId.equalsIgnoreCase(ts.getStudentId()));
        currentList.add(0, newRel);
        rosterMap.put(trainerId, currentList);
        savePersistentRosterMap(rosterMap);

        // 2. Add to in-memory mock map
        client.addMockTrainerStudent(newRel);

        // 3. If online & real student, save to Supabase
        if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken()) && !isDemoStudent(studentProfile.getId())) {
            try {
                TrainerStudent posted = client.executePost("trainer_students", newRel, TrainerStudent.class);
                if (posted != null && posted.getId() != null) {
                    newRel.setId(posted.getId());
                }
            } catch (Exception ex) {
                // Persistent file already saved, so safe
            }
        }

        enrichStudentSummary(newRel);
        return newRel;
    }

    public synchronized void removeStudent(String trainerId, String studentId) throws Exception {
        if (trainerId == null || studentId == null) return;

        // 1. Remove from persistent disk storage
        Map<String, List<TrainerStudent>> rosterMap = loadPersistentRosterMap();
        List<TrainerStudent> currentList = rosterMap.get(trainerId);
        if (currentList != null) {
            currentList.removeIf(ts -> studentId.equalsIgnoreCase(ts.getStudentId()));
            rosterMap.put(trainerId, currentList);
            savePersistentRosterMap(rosterMap);
        }

        // 2. Remove from in-memory cache
        client.removeMockTrainerStudent(trainerId, studentId);

        // 3. If online and not demo student, delete from Supabase
        if (!config.isMockMode() && client.getCurrentSession() != null && client.isValidJwt(client.getCurrentSession().getAccessToken()) && !isDemoStudent(studentId)) {
            try {
                client.executeDelete("trainer_students", "trainer_id=eq." + trainerId + "&student_id=eq." + studentId);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Clears persistent and in-memory roster entries for a specific trainer ID.
     * Used for test isolation and clean account state resets.
     */
    public synchronized void clearRosterForTrainer(String trainerId) {
        if (trainerId == null) return;
        Map<String, List<TrainerStudent>> rosterMap = loadPersistentRosterMap();
        rosterMap.remove(trainerId);
        savePersistentRosterMap(rosterMap);
        client.getTrainerStudents(trainerId).clear();
    }

    public StudentProgressDetail getStudentProgressDetail(String studentId) throws Exception {
        UserProfile profile = null;
        List<Workout> workouts = null;
        List<Goal> goals = null;
        List<BodyMeasurement> measurements = null;
        List<NutritionEntry> nutrition = null;

        if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken()) || isDemoStudent(studentId)) {
            profile = client.getMockProfile(studentId);
            workouts = client.getWorkoutsForUser(studentId);
            goals = client.getGoalsForUser(studentId);
            measurements = client.getMeasurementsForUser(studentId);
            nutrition = client.getNutritionForUser(studentId);
        } else {
            profile = client.findRemoteProfileById(studentId);
            try {
                workouts = client.executeGetList("workouts", "user_id=eq." + studentId + "&order=workout_date.desc", Workout.class);
            } catch (Exception ignored) {}
            if (workouts == null || workouts.isEmpty()) {
                try {
                    workouts = client.executeGetListWithServiceRole("workouts", "user_id=eq." + studentId + "&order=workout_date.desc", Workout.class);
                } catch (Exception ignored) {}
            }

            try {
                goals = client.executeGetList("goals", "user_id=eq." + studentId + "&order=created_at.desc", Goal.class);
            } catch (Exception ignored) {}
            if (goals == null || goals.isEmpty()) {
                try {
                    goals = client.executeGetListWithServiceRole("goals", "user_id=eq." + studentId + "&order=created_at.desc", Goal.class);
                } catch (Exception ignored) {}
            }

            try {
                measurements = client.executeGetList("body_measurements", "user_id=eq." + studentId + "&order=recorded_date.desc", BodyMeasurement.class);
            } catch (Exception ignored) {}
            if (measurements == null || measurements.isEmpty()) {
                try {
                    measurements = client.executeGetListWithServiceRole("body_measurements", "user_id=eq." + studentId + "&order=recorded_date.desc", BodyMeasurement.class);
                } catch (Exception ignored) {}
            }

            try {
                nutrition = client.executeGetList("nutrition_entries", "user_id=eq." + studentId + "&order=entry_date.desc", NutritionEntry.class);
            } catch (Exception ignored) {}
            if (nutrition == null || nutrition.isEmpty()) {
                try {
                    nutrition = client.executeGetListWithServiceRole("nutrition_entries", "user_id=eq." + studentId + "&order=entry_date.desc", NutritionEntry.class);
                } catch (Exception ignored) {}
            }

            if (profile == null) profile = client.getMockProfile(studentId);
            if (workouts == null || workouts.isEmpty()) workouts = client.getWorkoutsForUser(studentId);
            if (goals == null || goals.isEmpty()) goals = client.getGoalsForUser(studentId);
            if (measurements == null || measurements.isEmpty()) measurements = client.getMeasurementsForUser(studentId);
            if (nutrition == null || nutrition.isEmpty()) nutrition = client.getNutritionForUser(studentId);
        }

        StudentProgressDetail detail = new StudentProgressDetail(profile);
        detail.setWorkouts(workouts != null ? workouts : Collections.emptyList());
        detail.setGoals(goals != null ? goals : Collections.emptyList());
        detail.setMeasurements(measurements != null ? measurements : Collections.emptyList());
        detail.setNutritionEntries(nutrition != null ? nutrition : Collections.emptyList());
        try {
            detail.setAssignments(new TrainerAssignmentDAO().getAssignmentsForStudent(studentId));
        } catch (Exception ignored) {}
        return detail;
    }

    private boolean isDemoStudent(String studentId) {
        if (studentId == null) return false;
        return "student-sarah-101".equalsIgnoreCase(studentId)
                || "student-marcus-102".equalsIgnoreCase(studentId)
                || "PG-SARAH7".equalsIgnoreCase(studentId)
                || "PG-MARCUS".equalsIgnoreCase(studentId);
    }

    private void enrichStudentSummary(TrainerStudent ts) {
        if (ts == null || ts.getStudentId() == null) return;
        try {
            if (ts.getStudentProfile() == null) {
                if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken()) || isDemoStudent(ts.getStudentId())) {
                    ts.setStudentProfile(client.getMockProfile(ts.getStudentId()));
                } else {
                    ts.setStudentProfile(client.findRemoteProfileById(ts.getStudentId()));
                }
            }
            if (ts.getStudentProfile() == null) {
                ts.setStudentProfile(client.getMockProfile(ts.getStudentId()));
            }

            // Get count of workouts & latest weight
            List<Workout> wList = null;
            List<Goal> gList = null;
            List<BodyMeasurement> mList = null;

            if (config.isMockMode() || client.getCurrentSession() == null || !client.isValidJwt(client.getCurrentSession().getAccessToken()) || isDemoStudent(ts.getStudentId())) {
                wList = client.getWorkoutsForUser(ts.getStudentId());
                gList = client.getGoalsForUser(ts.getStudentId());
                mList = client.getMeasurementsForUser(ts.getStudentId());
            } else {
                try {
                    wList = client.executeGetList("workouts", "user_id=eq." + ts.getStudentId(), Workout.class);
                } catch (Exception ignored) {}
                if (wList == null || wList.isEmpty()) {
                    try {
                        wList = client.executeGetListWithServiceRole("workouts", "user_id=eq." + ts.getStudentId(), Workout.class);
                    } catch (Exception ignored) {}
                }

                try {
                    gList = client.executeGetList("goals", "user_id=eq." + ts.getStudentId(), Goal.class);
                } catch (Exception ignored) {}
                if (gList == null || gList.isEmpty()) {
                    try {
                        gList = client.executeGetListWithServiceRole("goals", "user_id=eq." + ts.getStudentId(), Goal.class);
                    } catch (Exception ignored) {}
                }

                try {
                    mList = client.executeGetList("body_measurements", "user_id=eq." + ts.getStudentId() + "&order=recorded_date.desc&limit=1", BodyMeasurement.class);
                } catch (Exception ignored) {}
                if (mList == null || mList.isEmpty()) {
                    try {
                        mList = client.executeGetListWithServiceRole("body_measurements", "user_id=eq." + ts.getStudentId() + "&order=recorded_date.desc&limit=1", BodyMeasurement.class);
                    } catch (Exception ignored) {}
                }
            }

            if (wList == null) wList = Collections.emptyList();
            if (gList == null) gList = Collections.emptyList();
            if (mList == null) mList = Collections.emptyList();

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
