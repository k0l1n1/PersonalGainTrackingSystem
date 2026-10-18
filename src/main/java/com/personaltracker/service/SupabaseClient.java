package com.personaltracker.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.personaltracker.config.AppConfig;
import com.personaltracker.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supabase HTTP Client and REST API integration.
 * Communicates with Supabase Auth (/auth/v1) and PostgREST (/rest/v1) with JWT Bearer tokens
 * so PostgreSQL Row Level Security (RLS) is strictly enforced.
 * Includes a thread-safe local fallback store for offline evaluation or demo testing.
 */
public class SupabaseClient {
    private static final Logger LOGGER = Logger.getLogger(SupabaseClient.class.getName());
    private static SupabaseClient instance;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppConfig config;

    // Current session
    private UserSession currentSession;

    // In-memory fallback data stores (used if offline or credentials not configured)
    private final Map<String, UserProfile> mockProfiles = new ConcurrentHashMap<>();
    private final Map<String, List<Workout>> mockWorkouts = new ConcurrentHashMap<>();
    private final Map<String, List<Goal>> mockGoals = new ConcurrentHashMap<>();
    private final Map<String, List<BodyMeasurement>> mockMeasurements = new ConcurrentHashMap<>();
    private final Map<String, List<NutritionEntry>> mockNutrition = new ConcurrentHashMap<>();
    private final Map<String, List<WaterIntake>> mockWater = new ConcurrentHashMap<>();
    private final Map<String, List<SleepRecord>> mockSleep = new ConcurrentHashMap<>();
    private final Map<String, List<Reminder>> mockReminders = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> mockUserAchievements = new ConcurrentHashMap<>();
    private final Map<String, List<TrainerStudent>> mockTrainerStudents = new ConcurrentHashMap<>();

    private SupabaseClient() {
        this.config = AppConfig.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        initMockDemoData();
        if (!config.isMockMode()) {
            this.currentSession = null;
        }
    }

    public static synchronized SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
        }
        return instance;
    }

    public void enableDemoMode() {
        config.setMockMode(true);
        initMockDemoData();
    }

    public boolean isValidJwt(String token) {
        if (token == null || token.isBlank()) return false;
        return token.split("\\.").length == 3;
    }

    private void applyAuthHeader(HttpRequest.Builder builder) {
        if (currentSession != null && isValidJwt(currentSession.getAccessToken())) {
            builder.header("Authorization", "Bearer " + currentSession.getAccessToken());
        }
    }

    public UserSession getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(UserSession session) {
        this.currentSession = session;
    }

    public boolean isAuthenticated() {
        return currentSession != null && currentSession.isAuthenticated();
    }

    public String getCurrentUserId() {
        return currentSession != null ? currentSession.getUserId() : null;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    // =========================================================================
    // REST API HTTP HELPERS
    // =========================================================================

    public <T> List<T> executeGetList(String table, String queryParams, Class<T> clazz) throws Exception {
        if (config.isMockMode() || (!"achievements".equals(table) && (currentSession == null || !isValidJwt(currentSession.getAccessToken())))) {
            return getMockList(table, clazz);
        }

        String url = config.getSupabaseUrl() + "/rest/v1/" + table;
        if (queryParams != null && !queryParams.isEmpty()) {
            url += "?" + queryParams;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Accept", "application/json")
                .GET();

        applyAuthHeader(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } else {
            throw new IOException("Supabase query error (" + response.statusCode() + "): " + response.body());
        }
    }

    public <T> T executePost(String table, Object body, Class<T> clazz) throws Exception {
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken())) {
            return saveMock(table, body, clazz);
        }

        String url = config.getSupabaseUrl() + "/rest/v1/" + table;
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        applyAuthHeader(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            List<T> list = objectMapper.readValue(response.body(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
            return list.isEmpty() ? null : list.get(0);
        } else {
            throw new IOException("Supabase insert error (" + response.statusCode() + "): " + response.body());
        }
    }

    public void executePatch(String table, String filterQuery, Object body) throws Exception {
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken())) {
            updateMock(table, body);
            return;
        }

        String url = config.getSupabaseUrl() + "/rest/v1/" + table + "?" + filterQuery;
        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

        applyAuthHeader(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase update error (" + response.statusCode() + "): " + response.body());
        }
    }

    public void executeDelete(String table, String filterQuery) throws Exception {
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken())) {
            deleteMock(table, filterQuery);
            return;
        }

        String url = config.getSupabaseUrl() + "/rest/v1/" + table + "?" + filterQuery;

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .DELETE();

        applyAuthHeader(builder);

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Supabase delete error (" + response.statusCode() + "): " + response.body());
        }
    }

    // =========================================================================
    // IN-MEMORY LOCAL DATA STORE IMPLEMENTATION (FOR OFFLINE / TEST / DEMO)
    // =========================================================================

    private void initMockDemoData() {
        String demoUserId = "demo-user-123";
        UserProfile demoProfile = new UserProfile(demoUserId, "athlete@demo.com", "Alex Hunter");
        demoProfile.setGender("Male");
        demoProfile.setDateOfBirth(LocalDate.of(1998, 5, 14));
        demoProfile.setHeightCm(178.0);
        demoProfile.setTargetWeightKg(75.0);
        demoProfile.setDailyCalorieTarget(2200);
        demoProfile.setDailyWaterTargetMl(3000);
        demoProfile.setUserCode("PG-ALEX99");
        demoProfile.setRole(UserProfile.ROLE_MEMBER);
        mockProfiles.put(demoUserId, demoProfile);

        // Demo workouts
        List<Workout> workouts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        Workout w1 = new Workout("Upper Body Power", "Strength", today, 55, 420);
        w1.setId(UUID.randomUUID().toString());
        w1.setUserId(demoUserId);
        w1.addExercise(new WorkoutExercise("Bench Press", 4, 8, 80.0));
        w1.addExercise(new WorkoutExercise("Overhead Press", 3, 10, 50.0));
        w1.addExercise(new WorkoutExercise("Incline Dumbbell Press", 3, 12, 28.0));
        workouts.add(w1);

        Workout w2 = new Workout("High-Intensity Interval Training", "HIIT", today.minusDays(1), 35, 380);
        w2.setId(UUID.randomUUID().toString());
        w2.setUserId(demoUserId);
        workouts.add(w2);

        Workout w3 = new Workout("Leg Hypertrophy", "Strength", today.minusDays(2), 60, 510);
        w3.setId(UUID.randomUUID().toString());
        w3.setUserId(demoUserId);
        w3.addExercise(new WorkoutExercise("Barbell Back Squat", 4, 8, 105.0));
        w3.addExercise(new WorkoutExercise("Romanian Deadlift", 3, 10, 90.0));
        workouts.add(w3);

        mockWorkouts.put(demoUserId, workouts);

        // Demo goals
        List<Goal> goals = new ArrayList<>();
        Goal g1 = new Goal("Reach Target Weight", "Weight", 75.0, 78.5, "kg", today.plusMonths(2));
        g1.setId(UUID.randomUUID().toString());
        g1.setUserId(demoUserId);
        goals.add(g1);

        Goal g2 = new Goal("Complete 20 Workouts this Month", "Workout Frequency", 20.0, 14.0, "workouts", today.plusDays(15));
        g2.setId(UUID.randomUUID().toString());
        g2.setUserId(demoUserId);
        goals.add(g2);

        Goal g3 = new Goal("Bench Press 100kg", "Strength", 100.0, 85.0, "kg", today.plusMonths(3));
        g3.setId(UUID.randomUUID().toString());
        g3.setUserId(demoUserId);
        goals.add(g3);

        mockGoals.put(demoUserId, goals);

        // Demo measurements
        List<BodyMeasurement> measurements = new ArrayList<>();
        BodyMeasurement m1 = new BodyMeasurement(today, 78.5, 178.0);
        m1.setId(UUID.randomUUID().toString());
        m1.setUserId(demoUserId);
        m1.setWaistCm(82.0);
        m1.setChestCm(102.0);
        m1.setArmCm(36.5);
        measurements.add(m1);

        BodyMeasurement m2 = new BodyMeasurement(today.minusDays(7), 79.2, 178.0);
        m2.setId(UUID.randomUUID().toString());
        m2.setUserId(demoUserId);
        measurements.add(m2);

        BodyMeasurement m3 = new BodyMeasurement(today.minusDays(14), 80.1, 178.0);
        m3.setId(UUID.randomUUID().toString());
        m3.setUserId(demoUserId);
        measurements.add(m3);

        mockMeasurements.put(demoUserId, measurements);

        // Demo nutrition
        List<NutritionEntry> nutrition = new ArrayList<>();
        NutritionEntry n1 = new NutritionEntry("Oatmeal with Blueberries & Whey", "BREAKFAST", 480, 32.0, 62.0, 9.0, today);
        n1.setId(UUID.randomUUID().toString());
        n1.setUserId(demoUserId);
        nutrition.add(n1);

        NutritionEntry n2 = new NutritionEntry("Grilled Chicken Breast & Brown Rice", "LUNCH", 650, 48.0, 70.0, 14.0, today);
        n2.setId(UUID.randomUUID().toString());
        n2.setUserId(demoUserId);
        nutrition.add(n2);

        NutritionEntry n3 = new NutritionEntry("Greek Yogurt with Almonds", "SNACK", 240, 18.0, 14.0, 11.0, today);
        n3.setId(UUID.randomUUID().toString());
        n3.setUserId(demoUserId);
        nutrition.add(n3);

        mockNutrition.put(demoUserId, nutrition);

        // Demo water
        List<WaterIntake> water = new ArrayList<>();
        water.add(new WaterIntake(500, today, LocalTime.of(8, 30)));
        water.add(new WaterIntake(500, today, LocalTime.of(11, 0)));
        water.add(new WaterIntake(750, today, LocalTime.of(14, 15)));
        for (WaterIntake wi : water) {
            wi.setId(UUID.randomUUID().toString());
            wi.setUserId(demoUserId);
        }
        mockWater.put(demoUserId, water);

        // Demo sleep
        List<SleepRecord> sleep = new ArrayList<>();
        OffsetDateTime bed = OffsetDateTime.now().minusDays(1).withHour(23).withMinute(15);
        OffsetDateTime wake = OffsetDateTime.now().withHour(7).withMinute(0);
        SleepRecord s1 = new SleepRecord(today, bed, wake, 4, "Woke up feeling energized");
        s1.setId(UUID.randomUUID().toString());
        s1.setUserId(demoUserId);
        sleep.add(s1);
        mockSleep.put(demoUserId, sleep);

        // Demo reminders
        List<Reminder> reminders = new ArrayList<>();
        Reminder r1 = new Reminder("Evening Leg Session", "Squats & Lunges at 6 PM", today, LocalTime.of(18, 0));
        r1.setId(UUID.randomUUID().toString());
        r1.setUserId(demoUserId);
        reminders.add(r1);

        Reminder r2 = new Reminder("Hydration Check", "Drink at least 500ml water", today, LocalTime.of(15, 0));
        r2.setId(UUID.randomUUID().toString());
        r2.setUserId(demoUserId);
        reminders.add(r2);
        mockReminders.put(demoUserId, reminders);

        // Demo achievements
        Set<String> unlocked = new HashSet<>();
        unlocked.add("FIRST_WORKOUT");
        unlocked.add("WORKOUT_STREAK_3");
        unlocked.add("HYDRATION_HERO");
        mockUserAchievements.put(demoUserId, unlocked);

        // =========================================================================
        // SEED DEMO STUDENTS (For Trainer Portal)
        // =========================================================================
        String s1Id = "student-sarah-101";
        UserProfile pSarah = new UserProfile(s1Id, "sarah.connor@example.com", "Sarah Connor", UserProfile.ROLE_MEMBER);
        pSarah.setUserCode("PG-SARAH7");
        pSarah.setGender("Female");
        pSarah.setDateOfBirth(LocalDate.of(2001, 3, 22));
        pSarah.setHeightCm(168.0);
        pSarah.setTargetWeightKg(60.0);
        pSarah.setDailyCalorieTarget(1850);
        pSarah.setDailyWaterTargetMl(2500);
        mockProfiles.put(s1Id, pSarah);

        List<Workout> s1Workouts = new ArrayList<>();
        Workout sw1 = new Workout("Lower Body Tone & Glutes", "Strength", today.minusDays(1), 50, 390);
        sw1.setId(UUID.randomUUID().toString());
        sw1.setUserId(s1Id);
        sw1.addExercise(new WorkoutExercise("Barbell Hip Thrust", 4, 12, 65.0));
        sw1.addExercise(new WorkoutExercise("Goblet Squat", 3, 12, 20.0));
        sw1.addExercise(new WorkoutExercise("Bulgarian Split Squat", 3, 10, 14.0));
        s1Workouts.add(sw1);

        Workout sw2 = new Workout("5K Interval Conditioning", "Cardio", today.minusDays(3), 32, 310);
        sw2.setId(UUID.randomUUID().toString());
        sw2.setUserId(s1Id);
        s1Workouts.add(sw2);
        mockWorkouts.put(s1Id, s1Workouts);

        List<Goal> s1Goals = new ArrayList<>();
        Goal sg1 = new Goal("Reach Target Weight", "Weight", 60.0, 63.5, "kg", today.plusMonths(2));
        sg1.setId(UUID.randomUUID().toString());
        sg1.setUserId(s1Id);
        s1Goals.add(sg1);

        Goal sg2 = new Goal("Log 16 Workouts this Month", "Workout Frequency", 16.0, 12.0, "workouts", today.plusDays(10));
        sg2.setId(UUID.randomUUID().toString());
        sg2.setUserId(s1Id);
        s1Goals.add(sg2);
        mockGoals.put(s1Id, s1Goals);

        List<BodyMeasurement> s1Meas = new ArrayList<>();
        BodyMeasurement sm1 = new BodyMeasurement(today, 63.5, 168.0);
        sm1.setId(UUID.randomUUID().toString());
        sm1.setUserId(s1Id);
        sm1.setWaistCm(69.0);
        s1Meas.add(sm1);
        BodyMeasurement sm2 = new BodyMeasurement(today.minusDays(14), 65.0, 168.0);
        sm2.setId(UUID.randomUUID().toString());
        sm2.setUserId(s1Id);
        sm2.setWaistCm(71.0);
        s1Meas.add(sm2);
        mockMeasurements.put(s1Id, s1Meas);

        List<NutritionEntry> s1Nut = new ArrayList<>();
        NutritionEntry sn1 = new NutritionEntry("Avocado Toast & Egg Whites", "BREAKFAST", 410, 24.0, 38.0, 15.0, today);
        sn1.setId(UUID.randomUUID().toString());
        sn1.setUserId(s1Id);
        s1Nut.add(sn1);
        NutritionEntry sn2 = new NutritionEntry("Grilled Salmon Salad", "LUNCH", 560, 42.0, 22.0, 28.0, today);
        sn2.setId(UUID.randomUUID().toString());
        sn2.setUserId(s1Id);
        s1Nut.add(sn2);
        mockNutrition.put(s1Id, s1Nut);

        // Student 2: Marcus Vance
        String s2Id = "student-marcus-102";
        UserProfile pMarcus = new UserProfile(s2Id, "marcus.vance@example.com", "Marcus Vance", UserProfile.ROLE_MEMBER);
        pMarcus.setUserCode("PG-MARCUS");
        pMarcus.setGender("Male");
        pMarcus.setDateOfBirth(LocalDate.of(1996, 8, 10));
        pMarcus.setHeightCm(183.0);
        pMarcus.setTargetWeightKg(82.0);
        pMarcus.setDailyCalorieTarget(2700);
        pMarcus.setDailyWaterTargetMl(3500);
        mockProfiles.put(s2Id, pMarcus);

        List<Workout> s2Workouts = new ArrayList<>();
        Workout mw1 = new Workout("Heavy Back & Biceps", "Strength", today.minusDays(2), 65, 520);
        mw1.setId(UUID.randomUUID().toString());
        mw1.setUserId(s2Id);
        mw1.addExercise(new WorkoutExercise("Conventional Deadlift", 4, 6, 140.0));
        mw1.addExercise(new WorkoutExercise("Pull-Ups", 4, 10, 0.0));
        mw1.addExercise(new WorkoutExercise("Barbell Bicep Curl", 3, 12, 35.0));
        s2Workouts.add(mw1);
        mockWorkouts.put(s2Id, s2Workouts);

        List<Goal> s2Goals = new ArrayList<>();
        Goal mg1 = new Goal("Drop to 82kg Cutting Target", "Weight", 82.0, 87.2, "kg", today.plusMonths(3));
        mg1.setId(UUID.randomUUID().toString());
        mg1.setUserId(s2Id);
        s2Goals.add(mg1);
        mockGoals.put(s2Id, s2Goals);

        List<BodyMeasurement> s2Meas = new ArrayList<>();
        BodyMeasurement mm1 = new BodyMeasurement(today, 87.2, 183.0);
        mm1.setId(UUID.randomUUID().toString());
        mm1.setUserId(s2Id);
        s2Meas.add(mm1);
        mockMeasurements.put(s2Id, s2Meas);

        // Pre-enroll Sarah Connor for demo user so trainer roster is immediately interactive
        List<TrainerStudent> demoTrainerRoster = new ArrayList<>();
        TrainerStudent tsSarah = new TrainerStudent(demoUserId, s1Id, "Goal: Lean muscle definition & 5k speed");
        tsSarah.setId(UUID.randomUUID().toString());
        tsSarah.setStudentProfile(pSarah);
        demoTrainerRoster.add(tsSarah);
        mockTrainerStudents.put(demoUserId, demoTrainerRoster);

        // Set default session to demo user
        this.currentSession = new UserSession(demoUserId, demoProfile.getEmail(), "demo-token", "demo-refresh", 3600);
        this.currentSession.setProfile(demoProfile);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getMockList(String table, Class<T> clazz) {
        String uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        switch (table) {
            case "workouts":
                return (List<T>) new ArrayList<>(mockWorkouts.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "goals":
                return (List<T>) new ArrayList<>(mockGoals.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "body_measurements":
                return (List<T>) new ArrayList<>(mockMeasurements.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "nutrition_entries":
                return (List<T>) new ArrayList<>(mockNutrition.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "water_intake":
                return (List<T>) new ArrayList<>(mockWater.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "sleep_records":
                return (List<T>) new ArrayList<>(mockSleep.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "reminders":
                return (List<T>) new ArrayList<>(mockReminders.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "profiles":
                UserProfile p = mockProfiles.get(uid);
                return p != null ? (List<T>) Collections.singletonList(p) : new ArrayList<>();
            case "trainer_students":
                return (List<T>) new ArrayList<>(mockTrainerStudents.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "user_achievements":
                Set<String> unlocked = mockUserAchievements.get(uid);
                List<UserAchievement> uasList = new ArrayList<>();
                if (unlocked != null) {
                    for (String achId : unlocked) {
                        UserAchievement ua = new UserAchievement(uid, achId);
                        ua.setId(UUID.randomUUID().toString());
                        uasList.add(ua);
                    }
                }
                return (List<T>) uasList;
            case "workout_exercises":
                List<Workout> userWks = mockWorkouts.get(uid);
                List<WorkoutExercise> exList = new ArrayList<>();
                if (userWks != null) {
                    for (Workout w : userWks) {
                        if (w.getExercises() != null) {
                            for (WorkoutExercise ex : w.getExercises()) {
                                if (ex.getWorkoutId() == null) ex.setWorkoutId(w.getId());
                                exList.add(ex);
                            }
                        }
                    }
                }
                return (List<T>) exList;
            default:
                return new ArrayList<>();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T saveMock(String table, Object body, Class<T> clazz) {
        String uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        String newId = UUID.randomUUID().toString();

        if (body instanceof Workout) {
            Workout w = (Workout) body;
            if (w.getId() == null || w.getId().isEmpty()) w.setId(newId);
            w.setUserId(uid);
            mockWorkouts.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, w);
            return (T) w;
        } else if (body instanceof Goal) {
            Goal g = (Goal) body;
            if (g.getId() == null || g.getId().isEmpty()) g.setId(newId);
            g.setUserId(uid);
            mockGoals.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, g);
            return (T) g;
        } else if (body instanceof BodyMeasurement) {
            BodyMeasurement m = (BodyMeasurement) body;
            if (m.getId() == null || m.getId().isEmpty()) m.setId(newId);
            m.setUserId(uid);
            mockMeasurements.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, m);
            return (T) m;
        } else if (body instanceof NutritionEntry) {
            NutritionEntry n = (NutritionEntry) body;
            if (n.getId() == null || n.getId().isEmpty()) n.setId(newId);
            n.setUserId(uid);
            mockNutrition.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, n);
            return (T) n;
        } else if (body instanceof WaterIntake) {
            WaterIntake wi = (WaterIntake) body;
            if (wi.getId() == null || wi.getId().isEmpty()) wi.setId(newId);
            wi.setUserId(uid);
            mockWater.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, wi);
            return (T) wi;
        } else if (body instanceof SleepRecord) {
            SleepRecord sr = (SleepRecord) body;
            if (sr.getId() == null || sr.getId().isEmpty()) sr.setId(newId);
            sr.setUserId(uid);
            mockSleep.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, sr);
            return (T) sr;
        } else if (body instanceof Reminder) {
            Reminder r = (Reminder) body;
            if (r.getId() == null || r.getId().isEmpty()) r.setId(newId);
            r.setUserId(uid);
            mockReminders.computeIfAbsent(uid, k -> new ArrayList<>()).add(0, r);
            return (T) r;
        } else if (body instanceof UserAchievement) {
            UserAchievement ua = (UserAchievement) body;
            if (ua.getId() == null || ua.getId().isEmpty()) ua.setId(newId);
            unlockMockAchievement(ua.getUserId() != null ? ua.getUserId() : uid, ua.getAchievementId());
            return (T) ua;
        } else if (body instanceof UserProfile) {
            UserProfile prof = (UserProfile) body;
            mockProfiles.put(prof.getId() != null ? prof.getId() : uid, prof);
            return (T) prof;
        } else if (body instanceof TrainerStudent) {
            TrainerStudent ts = (TrainerStudent) body;
            if (ts.getId() == null || ts.getId().isEmpty()) ts.setId(newId);
            if (ts.getTrainerId() == null) ts.setTrainerId(uid);
            mockTrainerStudents.computeIfAbsent(ts.getTrainerId(), k -> new ArrayList<>()).add(0, ts);
            return (T) ts;
        }
        return (T) body;
    }

    private void updateMock(String table, Object body) {
        String uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        if (body instanceof Workout) {
            Workout w = (Workout) body;
            List<Workout> list = mockWorkouts.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), w.getId())) {
                        list.set(i, w);
                        return;
                    }
                }
            }
        } else if (body instanceof Goal) {
            Goal g = (Goal) body;
            List<Goal> list = mockGoals.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), g.getId())) {
                        list.set(i, g);
                        return;
                    }
                }
            }
        } else if (body instanceof BodyMeasurement) {
            BodyMeasurement m = (BodyMeasurement) body;
            List<BodyMeasurement> list = mockMeasurements.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), m.getId())) {
                        list.set(i, m);
                        return;
                    }
                }
            }
        } else if (body instanceof NutritionEntry) {
            NutritionEntry n = (NutritionEntry) body;
            List<NutritionEntry> list = mockNutrition.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), n.getId())) {
                        list.set(i, n);
                        return;
                    }
                }
            }
        } else if (body instanceof Reminder) {
            Reminder r = (Reminder) body;
            List<Reminder> list = mockReminders.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), r.getId())) {
                        list.set(i, r);
                        return;
                    }
                }
            }
        } else if (body instanceof UserProfile) {
            UserProfile prof = (UserProfile) body;
            if (prof.getId() != null) {
                mockProfiles.put(prof.getId(), prof);
            } else {
                mockProfiles.put(uid, prof);
            }
        }
    }

    private void deleteMock(String table, String filterQuery) {
        String uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        String id = extractIdFromFilter(filterQuery);
        if (id == null) return;

        switch (table) {
            case "workouts":
                List<Workout> wList = mockWorkouts.get(uid);
                if (wList != null) wList.removeIf(w -> Objects.equals(w.getId(), id));
                break;
            case "goals":
                List<Goal> gList = mockGoals.get(uid);
                if (gList != null) gList.removeIf(g -> Objects.equals(g.getId(), id));
                break;
            case "body_measurements":
                List<BodyMeasurement> mList = mockMeasurements.get(uid);
                if (mList != null) mList.removeIf(m -> Objects.equals(m.getId(), id));
                break;
            case "nutrition_entries":
                List<NutritionEntry> nList = mockNutrition.get(uid);
                if (nList != null) nList.removeIf(n -> Objects.equals(n.getId(), id));
                break;
            case "water_intake":
                List<WaterIntake> wiList = mockWater.get(uid);
                if (wiList != null) wiList.removeIf(wi -> Objects.equals(wi.getId(), id));
                break;
            case "sleep_records":
                List<SleepRecord> sList = mockSleep.get(uid);
                if (sList != null) sList.removeIf(s -> Objects.equals(s.getId(), id));
                break;
            case "reminders":
                List<Reminder> rList = mockReminders.get(uid);
                if (rList != null) rList.removeIf(r -> Objects.equals(r.getId(), id));
                break;
            case "trainer_students":
                List<TrainerStudent> tsList = mockTrainerStudents.get(uid);
                if (tsList != null) {
                    if (id != null) {
                        tsList.removeIf(ts -> Objects.equals(ts.getId(), id));
                    }
                }
                break;
        }
    }

    private String extractIdFromFilter(String query) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            if (part.startsWith("id=eq.")) {
                return part.substring("id=eq.".length());
            }
        }
        return null;
    }

    public Set<String> getMockUserAchievements(String userId) {
        return mockUserAchievements.computeIfAbsent(userId, k -> new HashSet<>());
    }

    public void unlockMockAchievement(String userId, String achievementId) {
        mockUserAchievements.computeIfAbsent(userId, k -> new HashSet<>()).add(achievementId);
    }

    public UserProfile getMockProfile(String userId) {
        return mockProfiles.get(userId);
    }

    public void updateMockProfile(UserProfile profile) {
        if (profile != null && profile.getId() != null) {
            mockProfiles.put(profile.getId(), profile);
        }
    }

    public UserProfile findProfileByCode(String code) {
        if (code == null) return null;
        String clean = code.trim().toUpperCase();
        for (UserProfile p : mockProfiles.values()) {
            if (p.getUserCode() != null && clean.equalsIgnoreCase(p.getUserCode())) {
                return p;
            }
        }
        return null;
    }

    public List<Workout> getWorkoutsForUser(String userId) {
        return new ArrayList<>(mockWorkouts.computeIfAbsent(userId, k -> new ArrayList<>()));
    }

    public List<Goal> getGoalsForUser(String userId) {
        return new ArrayList<>(mockGoals.computeIfAbsent(userId, k -> new ArrayList<>()));
    }

    public List<BodyMeasurement> getMeasurementsForUser(String userId) {
        return new ArrayList<>(mockMeasurements.computeIfAbsent(userId, k -> new ArrayList<>()));
    }

    public List<NutritionEntry> getNutritionForUser(String userId) {
        return new ArrayList<>(mockNutrition.computeIfAbsent(userId, k -> new ArrayList<>()));
    }

    public List<TrainerStudent> getTrainerStudents(String trainerId) {
        return mockTrainerStudents.computeIfAbsent(trainerId, k -> new ArrayList<>());
    }

    public void addMockTrainerStudent(TrainerStudent ts) {
        if (ts != null && ts.getTrainerId() != null) {
            mockTrainerStudents.computeIfAbsent(ts.getTrainerId(), k -> new ArrayList<>()).add(ts);
        }
    }

    public void removeMockTrainerStudent(String trainerId, String studentId) {
        List<TrainerStudent> list = mockTrainerStudents.get(trainerId);
        if (list != null && studentId != null) {
            list.removeIf(ts -> studentId.equalsIgnoreCase(ts.getStudentId()));
        }
    }
}
