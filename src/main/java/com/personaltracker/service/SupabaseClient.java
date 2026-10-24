package com.personaltracker.service;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private final Map<String, List<TrainerAssignment>> mockTrainerAssignments = new ConcurrentHashMap<>();

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
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

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
            return getMockList(table, queryParams, clazz);
        }

        // If query targets a non-UUID student or user, route directly to local mock storage
        String uFilter = extractParamValue(queryParams, "user_id");
        if (uFilter != null && !isValidUuid(uFilter)) {
            return getMockList(table, queryParams, clazz);
        }
        String sFilter = extractParamValue(queryParams, "student_id");
        if (sFilter != null && !isValidUuid(sFilter)) {
            return getMockList(table, queryParams, clazz);
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
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken()) || containsNonUuidTarget(body)) {
            return saveMock(table, body, clazz);
        }

        ensureEntityId(body);

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

    private void ensureEntityId(Object body) {
        if (body == null) return;
        try {
            java.lang.reflect.Method getId = body.getClass().getMethod("getId");
            java.lang.reflect.Method setId = body.getClass().getMethod("setId", String.class);
            Object currentId = getId.invoke(body);
            if (currentId == null || currentId.toString().isBlank()) {
                setId.invoke(body, UUID.randomUUID().toString());
            }
        } catch (NoSuchMethodException ignored) {
            // Body doesn't have standard getId/setId(String)
        } catch (Exception ignored) {
        }
    }

    public void executePatch(String table, String filterQuery, Object body) throws Exception {
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken()) || containsNonUuidTarget(body)) {
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
        String uFilter = extractParamValue(filterQuery, "user_id");
        String sFilter = extractParamValue(filterQuery, "student_id");
        if (config.isMockMode() || currentSession == null || !isValidJwt(currentSession.getAccessToken())
                || (uFilter != null && !isValidUuid(uFilter)) || (sFilter != null && !isValidUuid(sFilter))) {
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

        Workout w4 = new Workout("Pull Day (Back & Biceps)", "Strength", today.minusDays(4), 50, 410);
        w4.setId(UUID.randomUUID().toString());
        w4.setUserId(demoUserId);
        w4.addExercise(new WorkoutExercise("Deadlift", 4, 6, 100.0));
        w4.addExercise(new WorkoutExercise("Lat Pulldown", 3, 10, 55.0));
        workouts.add(w4);

        Workout w5 = new Workout("Push Day (Chest & Triceps)", "Strength", today.minusDays(5), 45, 390);
        w5.setId(UUID.randomUUID().toString());
        w5.setUserId(demoUserId);
        w5.addExercise(new WorkoutExercise("Bench Press", 4, 10, 70.0));
        w5.addExercise(new WorkoutExercise("Tricep Pushdown", 3, 12, 25.0));
        workouts.add(w5);

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

        // Yesterday's demo nutrition (allows immediate interactive verification of Prev/Yesterday view)
        LocalDate yesterday = today.minusDays(1);
        NutritionEntry ny1 = new NutritionEntry("3 Scrambled Eggs & Sourdough Toast", "BREAKFAST", 390, 24.0, 32.0, 18.0, yesterday);
        ny1.setId(UUID.randomUUID().toString());
        ny1.setUserId(demoUserId);
        nutrition.add(ny1);

        NutritionEntry ny2 = new NutritionEntry("Turkey Breast & Avocado Wrap", "LUNCH", 580, 44.0, 46.0, 22.0, yesterday);
        ny2.setId(UUID.randomUUID().toString());
        ny2.setUserId(demoUserId);
        nutrition.add(ny2);

        NutritionEntry ny3 = new NutritionEntry("Grilled Salmon & Sweet Potato", "DINNER", 640, 48.0, 45.0, 20.0, yesterday);
        ny3.setId(UUID.randomUUID().toString());
        ny3.setUserId(demoUserId);
        nutrition.add(ny3);

        NutritionEntry ny4 = new NutritionEntry("Whey Protein Shake & Banana", "SNACK", 280, 30.0, 32.0, 3.0, yesterday);
        ny4.setId(UUID.randomUUID().toString());
        ny4.setUserId(demoUserId);
        nutrition.add(ny4);

        mockNutrition.put(demoUserId, nutrition);

        // Demo water
        List<WaterIntake> water = new ArrayList<>();
        water.add(new WaterIntake(500, today, LocalTime.of(8, 30)));
        water.add(new WaterIntake(500, today, LocalTime.of(11, 0)));
        water.add(new WaterIntake(750, today, LocalTime.of(14, 15)));
        water.add(new WaterIntake(500, yesterday, LocalTime.of(9, 0)));
        water.add(new WaterIntake(750, yesterday, LocalTime.of(13, 30)));
        water.add(new WaterIntake(500, yesterday, LocalTime.of(17, 45)));
        for (WaterIntake wi : water) {
            wi.setId(UUID.randomUUID().toString());
            wi.setUserId(demoUserId);
        }
        mockWater.put(demoUserId, water);

        // Demo sleep - full 7-day recovery history for rich chart and stat visualization
        List<SleepRecord> sleep = new ArrayList<>();
        OffsetDateTime bed0 = OffsetDateTime.now().minusDays(1).withHour(23).withMinute(15);
        OffsetDateTime wake0 = OffsetDateTime.now().withHour(7).withMinute(0);
        SleepRecord s0 = new SleepRecord(today, bed0, wake0, 4, "Woke up feeling energized");
        s0.setId(UUID.randomUUID().toString());
        s0.setUserId(demoUserId);
        sleep.add(s0);

        OffsetDateTime bed1 = OffsetDateTime.now().minusDays(2).withHour(23).withMinute(30);
        OffsetDateTime wake1 = OffsetDateTime.now().minusDays(1).withHour(7).withMinute(0);
        SleepRecord s1 = new SleepRecord(today.minusDays(1), bed1, wake1, 4, "Solid restful sleep");
        s1.setId(UUID.randomUUID().toString());
        s1.setUserId(demoUserId);
        sleep.add(s1);

        OffsetDateTime bed2 = OffsetDateTime.now().minusDays(3).withHour(22).withMinute(45);
        OffsetDateTime wake2 = OffsetDateTime.now().minusDays(2).withHour(7).withMinute(0);
        SleepRecord s2 = new SleepRecord(today.minusDays(2), bed2, wake2, 5, "Deep restorative rest");
        s2.setId(UUID.randomUUID().toString());
        s2.setUserId(demoUserId);
        sleep.add(s2);

        OffsetDateTime bed3 = OffsetDateTime.now().minusDays(3).withHour(0).withMinute(15);
        OffsetDateTime wake3 = OffsetDateTime.now().minusDays(3).withHour(7).withMinute(0);
        SleepRecord s3 = new SleepRecord(today.minusDays(3), bed3, wake3, 3, "Slightly interrupted sleep");
        s3.setId(UUID.randomUUID().toString());
        s3.setUserId(demoUserId);
        sleep.add(s3);

        OffsetDateTime bed4 = OffsetDateTime.now().minusDays(5).withHour(23).withMinute(0);
        OffsetDateTime wake4 = OffsetDateTime.now().minusDays(4).withHour(7).withMinute(0);
        SleepRecord s4 = new SleepRecord(today.minusDays(4), bed4, wake4, 5, "Hit 8-hour target perfectly");
        s4.setId(UUID.randomUUID().toString());
        s4.setUserId(demoUserId);
        sleep.add(s4);

        OffsetDateTime bed5 = OffsetDateTime.now().minusDays(6).withHour(23).withMinute(45);
        OffsetDateTime wake5 = OffsetDateTime.now().minusDays(5).withHour(7).withMinute(0);
        SleepRecord s5 = new SleepRecord(today.minusDays(5), bed5, wake5, 4, "Felt refreshed after morning run");
        s5.setId(UUID.randomUUID().toString());
        s5.setUserId(demoUserId);
        sleep.add(s5);

        OffsetDateTime bed6 = OffsetDateTime.now().minusDays(7).withHour(23).withMinute(20);
        OffsetDateTime wake6 = OffsetDateTime.now().minusDays(6).withHour(7).withMinute(0);
        SleepRecord s6 = new SleepRecord(today.minusDays(6), bed6, wake6, 4, "Consistent pre-workout sleep");
        s6.setId(UUID.randomUUID().toString());
        s6.setUserId(demoUserId);
        sleep.add(s6);

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
        // SEED DEMO ATHLETES & STUDENTS (For Trainer Portal & Presentation)
        // =========================================================================

        // -------------------------------------------------------------------------
        // DEMO ATHLETE 1: Sarah Connor (PG-SARAH7) - Tone & Endurance Focus
        // -------------------------------------------------------------------------
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
        mockProfiles.put("sarah@demo.com", pSarah);

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

        Workout sw3 = new Workout("Upper Body Definition & Arms", "Strength", today.minusDays(4), 45, 340);
        sw3.setId(UUID.randomUUID().toString());
        sw3.setUserId(s1Id);
        sw3.addExercise(new WorkoutExercise("Dumbbell Shoulder Press", 4, 10, 14.0));
        sw3.addExercise(new WorkoutExercise("Lat Pulldown", 3, 12, 42.0));
        sw3.addExercise(new WorkoutExercise("Cable Lateral Raise", 3, 15, 6.0));
        s1Workouts.add(sw3);

        Workout sw4 = new Workout("Core & HIIT Burner", "HIIT", today.minusDays(6), 30, 290);
        sw4.setId(UUID.randomUUID().toString());
        sw4.setUserId(s1Id);
        s1Workouts.add(sw4);
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

        Goal sg3 = new Goal("Run 5K in Under 25 Minutes", "Cardio", 25.0, 27.2, "minutes", today.plusMonths(1));
        sg3.setId(UUID.randomUUID().toString());
        sg3.setUserId(s1Id);
        s1Goals.add(sg3);
        mockGoals.put(s1Id, s1Goals);

        List<BodyMeasurement> s1Meas = new ArrayList<>();
        BodyMeasurement sm1 = new BodyMeasurement(today, 63.5, 168.0);
        sm1.setId(UUID.randomUUID().toString());
        sm1.setUserId(s1Id);
        sm1.setWaistCm(69.0);
        sm1.setChestCm(88.0);
        sm1.setHipsCm(96.0);
        s1Meas.add(sm1);

        BodyMeasurement sm2 = new BodyMeasurement(today.minusDays(7), 64.2, 168.0);
        sm2.setId(UUID.randomUUID().toString());
        sm2.setUserId(s1Id);
        sm2.setWaistCm(70.0);
        s1Meas.add(sm2);

        BodyMeasurement sm3 = new BodyMeasurement(today.minusDays(14), 65.0, 168.0);
        sm3.setId(UUID.randomUUID().toString());
        sm3.setUserId(s1Id);
        sm3.setWaistCm(71.5);
        s1Meas.add(sm3);

        BodyMeasurement sm4 = new BodyMeasurement(today.minusDays(28), 66.5, 168.0);
        sm4.setId(UUID.randomUUID().toString());
        sm4.setUserId(s1Id);
        sm4.setWaistCm(73.0);
        s1Meas.add(sm4);
        mockMeasurements.put(s1Id, s1Meas);

        List<NutritionEntry> s1Nut = new ArrayList<>();
        NutritionEntry sn1 = new NutritionEntry("Avocado Toast & Scrambled Egg Whites", "BREAKFAST", 410, 24.0, 38.0, 15.0, today);
        sn1.setId(UUID.randomUUID().toString());
        sn1.setUserId(s1Id);
        s1Nut.add(sn1);

        NutritionEntry sn2 = new NutritionEntry("Grilled Salmon Quinoa Bowl", "LUNCH", 560, 42.0, 40.0, 22.0, today);
        sn2.setId(UUID.randomUUID().toString());
        sn2.setUserId(s1Id);
        s1Nut.add(sn2);

        NutritionEntry sn3 = new NutritionEntry("Greek Yogurt with Mixed Berries", "SNACK", 220, 20.0, 22.0, 4.0, today);
        sn3.setId(UUID.randomUUID().toString());
        sn3.setUserId(s1Id);
        s1Nut.add(sn3);

        NutritionEntry sn4 = new NutritionEntry("Lemon Herb Chicken Breast & Broccoli", "DINNER", 480, 46.0, 28.0, 12.0, today);
        sn4.setId(UUID.randomUUID().toString());
        sn4.setUserId(s1Id);
        s1Nut.add(sn4);
        mockNutrition.put(s1Id, s1Nut);

        List<WaterIntake> s1Water = new ArrayList<>();
        s1Water.add(new WaterIntake(500, today, LocalTime.of(8, 0)));
        s1Water.add(new WaterIntake(1000, today, LocalTime.of(12, 30)));
        s1Water.add(new WaterIntake(750, today, LocalTime.of(16, 45)));
        for (WaterIntake wi : s1Water) {
            wi.setId(UUID.randomUUID().toString());
            wi.setUserId(s1Id);
        }
        mockWater.put(s1Id, s1Water);

        List<SleepRecord> s1Sleep = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            OffsetDateTime bed = OffsetDateTime.now().minusDays(i + 1).withHour(23).withMinute(0);
            OffsetDateTime wake = OffsetDateTime.now().minusDays(i).withHour(7).withMinute(15);
            SleepRecord sr = new SleepRecord(today.minusDays(i), bed, wake, 5, "Optimal rest & recovery");
            sr.setId(UUID.randomUUID().toString());
            sr.setUserId(s1Id);
            s1Sleep.add(sr);
        }
        mockSleep.put(s1Id, s1Sleep);

        // -------------------------------------------------------------------------
        // DEMO ATHLETE 2: Marcus Vance (PG-MARCUS) - Strength & Cutting Focus
        // -------------------------------------------------------------------------
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
        mockProfiles.put("marcus@demo.com", pMarcus);

        List<Workout> s2Workouts = new ArrayList<>();
        Workout mw1 = new Workout("Heavy Back & Deadlift Power", "Strength", today.minusDays(1), 65, 520);
        mw1.setId(UUID.randomUUID().toString());
        mw1.setUserId(s2Id);
        mw1.addExercise(new WorkoutExercise("Conventional Deadlift", 4, 6, 140.0));
        mw1.addExercise(new WorkoutExercise("Weighted Pull-Ups", 4, 8, 15.0));
        mw1.addExercise(new WorkoutExercise("Barbell Bent-Over Row", 4, 8, 85.0));
        mw1.addExercise(new WorkoutExercise("Barbell Bicep Curl", 3, 10, 35.0));
        s2Workouts.add(mw1);

        Workout mw2 = new Workout("Chest & Triceps Hypertrophy", "Strength", today.minusDays(2), 60, 480);
        mw2.setId(UUID.randomUUID().toString());
        mw2.setUserId(s2Id);
        mw2.addExercise(new WorkoutExercise("Barbell Bench Press", 4, 8, 100.0));
        mw2.addExercise(new WorkoutExercise("Incline Dumbbell Press", 3, 10, 36.0));
        mw2.addExercise(new WorkoutExercise("Parallel Bar Dips", 3, 12, 0.0));
        s2Workouts.add(mw2);

        Workout mw3 = new Workout("Leg Power & Squats", "Strength", today.minusDays(4), 65, 540);
        mw3.setId(UUID.randomUUID().toString());
        mw3.setUserId(s2Id);
        mw3.addExercise(new WorkoutExercise("Barbell Back Squat", 4, 6, 130.0));
        mw3.addExercise(new WorkoutExercise("Romanian Deadlift", 3, 8, 110.0));
        mw3.addExercise(new WorkoutExercise("Leg Press", 4, 10, 260.0));
        s2Workouts.add(mw3);

        Workout mw4 = new Workout("Boulder Shoulders & Core", "Strength", today.minusDays(5), 50, 420);
        mw4.setId(UUID.randomUUID().toString());
        mw4.setUserId(s2Id);
        mw4.addExercise(new WorkoutExercise("Overhead Barbell Press", 4, 6, 65.0));
        mw4.addExercise(new WorkoutExercise("Dumbbell Lateral Raise", 4, 15, 14.0));
        s2Workouts.add(mw4);
        mockWorkouts.put(s2Id, s2Workouts);

        List<Goal> s2Goals = new ArrayList<>();
        Goal mg1 = new Goal("Drop to 82kg Cutting Target", "Weight", 82.0, 87.2, "kg", today.plusMonths(3));
        mg1.setId(UUID.randomUUID().toString());
        mg1.setUserId(s2Id);
        s2Goals.add(mg1);

        Goal mg2 = new Goal("Deadlift 160kg for 5 Reps", "Strength", 160.0, 140.0, "kg", today.plusMonths(2));
        mg2.setId(UUID.randomUUID().toString());
        mg2.setUserId(s2Id);
        s2Goals.add(mg2);

        Goal mg3 = new Goal("Maintain 200g+ Daily Protein", "Nutrition", 200.0, 198.0, "grams", today.plusDays(30));
        mg3.setId(UUID.randomUUID().toString());
        mg3.setUserId(s2Id);
        s2Goals.add(mg3);
        mockGoals.put(s2Id, s2Goals);

        List<BodyMeasurement> s2Meas = new ArrayList<>();
        BodyMeasurement mm1 = new BodyMeasurement(today, 87.2, 183.0);
        mm1.setId(UUID.randomUUID().toString());
        mm1.setUserId(s2Id);
        mm1.setWaistCm(87.0);
        mm1.setChestCm(112.0);
        mm1.setArmCm(41.5);
        s2Meas.add(mm1);

        BodyMeasurement mm2 = new BodyMeasurement(today.minusDays(7), 88.0, 183.0);
        mm2.setId(UUID.randomUUID().toString());
        mm2.setUserId(s2Id);
        mm2.setWaistCm(88.5);
        s2Meas.add(mm2);

        BodyMeasurement mm3 = new BodyMeasurement(today.minusDays(14), 89.2, 183.0);
        mm3.setId(UUID.randomUUID().toString());
        mm3.setUserId(s2Id);
        mm3.setWaistCm(90.0);
        s2Meas.add(mm3);

        BodyMeasurement mm4 = new BodyMeasurement(today.minusDays(21), 90.5, 183.0);
        mm4.setId(UUID.randomUUID().toString());
        mm4.setUserId(s2Id);
        mm4.setWaistCm(91.5);
        s2Meas.add(mm4);

        BodyMeasurement mm5 = new BodyMeasurement(today.minusDays(28), 92.0, 183.0);
        mm5.setId(UUID.randomUUID().toString());
        mm5.setUserId(s2Id);
        mm5.setWaistCm(93.0);
        s2Meas.add(mm5);
        mockMeasurements.put(s2Id, s2Meas);

        List<NutritionEntry> s2Nut = new ArrayList<>();
        NutritionEntry mn1 = new NutritionEntry("6 Egg Whites + 2 Whole Eggs & Oats", "BREAKFAST", 580, 46.0, 52.0, 16.0, today);
        mn1.setId(UUID.randomUUID().toString());
        mn1.setUserId(s2Id);
        s2Nut.add(mn1);

        NutritionEntry mn2 = new NutritionEntry("Top Sirloin Steak & Jasmine Rice", "LUNCH", 780, 58.0, 68.0, 22.0, today);
        mn2.setId(UUID.randomUUID().toString());
        mn2.setUserId(s2Id);
        s2Nut.add(mn2);

        NutritionEntry mn3 = new NutritionEntry("Cream of Rice & Whey Isolate", "SNACK", 440, 38.0, 62.0, 4.0, today);
        mn3.setId(UUID.randomUUID().toString());
        mn3.setUserId(s2Id);
        s2Nut.add(mn3);

        NutritionEntry mn4 = new NutritionEntry("Grilled Chicken Breast & Sweet Potato", "DINNER", 680, 56.0, 54.0, 18.0, today);
        mn4.setId(UUID.randomUUID().toString());
        mn4.setUserId(s2Id);
        s2Nut.add(mn4);
        mockNutrition.put(s2Id, s2Nut);

        List<WaterIntake> s2Water = new ArrayList<>();
        s2Water.add(new WaterIntake(750, today, LocalTime.of(7, 30)));
        s2Water.add(new WaterIntake(1000, today, LocalTime.of(12, 0)));
        s2Water.add(new WaterIntake(750, today, LocalTime.of(15, 30)));
        s2Water.add(new WaterIntake(750, today, LocalTime.of(19, 0)));
        for (WaterIntake wi : s2Water) {
            wi.setId(UUID.randomUUID().toString());
            wi.setUserId(s2Id);
        }
        mockWater.put(s2Id, s2Water);

        List<SleepRecord> s2Sleep = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            OffsetDateTime bed = OffsetDateTime.now().minusDays(i + 1).withHour(23).withMinute(15);
            OffsetDateTime wake = OffsetDateTime.now().minusDays(i).withHour(7).withMinute(30);
            SleepRecord sr = new SleepRecord(today.minusDays(i), bed, wake, 5, "Deep restorative recovery");
            sr.setId(UUID.randomUUID().toString());
            sr.setUserId(s2Id);
            s2Sleep.add(sr);
        }
        mockSleep.put(s2Id, s2Sleep);

        // -------------------------------------------------------------------------
        // DEMO TRAINER ACCOUNT: Coach David Miller (PG-COACH9)
        // -------------------------------------------------------------------------
        String coachId = "trainer-coach-99";
        UserProfile pCoach = new UserProfile(coachId, "coach@demo.com", "Coach David Miller", UserProfile.ROLE_TRAINER);
        pCoach.setUserCode("PG-COACH9");
        pCoach.setGender("Male");
        pCoach.setDateOfBirth(LocalDate.of(1989, 4, 12));
        pCoach.setHeightCm(180.0);
        pCoach.setTargetWeightKg(80.0);
        pCoach.setRole(UserProfile.ROLE_TRAINER);
        mockProfiles.put(coachId, pCoach);

        // Clean initial roster for coach & demoUser so students can be enrolled live during presentation
        mockTrainerStudents.put(demoUserId, new ArrayList<>());
        mockTrainerStudents.put(coachId, new ArrayList<>());

        // Pre-seed demo coaching assignments for presentation demo
        List<TrainerAssignment> s1Assigns = new ArrayList<>();
        TrainerAssignment ta1 = new TrainerAssignment(coachId, "Coach David Miller", s1Id, "WORKOUT",
                "5K Interval Conditioning Drill",
                "10 min warm-up jog, then 6x 400m intervals at 4:50/km pace. Cool down 10 mins with hip stretches.",
                today.plusDays(1), LocalTime.of(17, 30));
        ta1.setId("assign-sarah-01");
        s1Assigns.add(ta1);
        mockTrainerAssignments.put(s1Id, s1Assigns);

        List<TrainerAssignment> s2Assigns = new ArrayList<>();
        TrainerAssignment ta2 = new TrainerAssignment(coachId, "Coach David Miller", s2Id, "GOAL",
                "Deadlift 160kg Peak Milestone",
                "Focus on explosive hip drive from the floor. Aim for 5 clean reps at 160kg next Friday!",
                today.plusWeeks(2), null);
        ta2.setId("assign-marcus-01");
        s2Assigns.add(ta2);
        mockTrainerAssignments.put(s2Id, s2Assigns);

        // Alex Hunter has no pending assignments initially (demonstrates "All caught up" clean state)
        mockTrainerAssignments.put(demoUserId, new ArrayList<>());

        // Set default session to demo user
        this.currentSession = new UserSession(demoUserId, demoProfile.getEmail(), "demo-token", "demo-refresh", 3600);
        this.currentSession.setProfile(demoProfile);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getMockList(String table, String queryParams, Class<T> clazz) {
        String uid = extractParamValue(queryParams, "user_id");
        if (uid == null) {
            uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        }
        switch (table) {
            case "workouts": {
                List<Workout> list = new ArrayList<>(mockWorkouts.computeIfAbsent(uid, k -> new ArrayList<>()));
                String date = extractParamValue(queryParams, "workout_date");
                if (date != null) {
                    list.removeIf(w -> w.getWorkoutDate() == null || !w.getWorkoutDate().toString().equals(date));
                }
                return (List<T>) list;
            }
            case "goals":
                return (List<T>) new ArrayList<>(mockGoals.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "body_measurements": {
                List<BodyMeasurement> list = new ArrayList<>(mockMeasurements.computeIfAbsent(uid, k -> new ArrayList<>()));
                String date = extractParamValue(queryParams, "recorded_date");
                if (date != null) {
                    list.removeIf(m -> m.getRecordedDate() == null || !m.getRecordedDate().toString().equals(date));
                }
                return (List<T>) list;
            }
            case "nutrition_entries": {
                List<NutritionEntry> list = new ArrayList<>(mockNutrition.computeIfAbsent(uid, k -> new ArrayList<>()));
                String date = extractParamValue(queryParams, "entry_date");
                if (date != null) {
                    list.removeIf(n -> n.getEntryDate() == null || !n.getEntryDate().toString().equals(date));
                }
                return (List<T>) list;
            }
            case "water_intake": {
                List<WaterIntake> list = new ArrayList<>(mockWater.computeIfAbsent(uid, k -> new ArrayList<>()));
                String date = extractParamValue(queryParams, "recorded_date");
                if (date != null) {
                    list.removeIf(w -> w.getRecordedDate() == null || !w.getRecordedDate().toString().equals(date));
                }
                return (List<T>) list;
            }
            case "sleep_records": {
                List<SleepRecord> list = new ArrayList<>(mockSleep.computeIfAbsent(uid, k -> new ArrayList<>()));
                String date = extractParamValue(queryParams, "sleep_date");
                if (date != null) {
                    list.removeIf(s -> s.getSleepDate() == null || !s.getSleepDate().toString().equals(date));
                }
                return (List<T>) list;
            }
            case "reminders":
                return (List<T>) new ArrayList<>(mockReminders.computeIfAbsent(uid, k -> new ArrayList<>()));
            case "profiles": {
                String code = extractParamValue(queryParams, "user_code");
                if (code != null) {
                    UserProfile p = findProfileByCode(code);
                    return p != null ? (List<T>) Collections.singletonList(p) : new ArrayList<>();
                }
                String profId = extractParamValue(queryParams, "id");
                if (profId != null) {
                    UserProfile p = mockProfiles.get(profId);
                    return p != null ? (List<T>) Collections.singletonList(p) : new ArrayList<>();
                }
                UserProfile p = mockProfiles.get(uid);
                return p != null ? (List<T>) Collections.singletonList(p) : new ArrayList<>();
            }
            case "trainer_students": {
                String trainerId = extractParamValue(queryParams, "trainer_id");
                String tid = trainerId != null ? trainerId : uid;
                return (List<T>) new ArrayList<>(mockTrainerStudents.computeIfAbsent(tid, k -> new ArrayList<>()));
            }
            case "trainer_assignments": {
                String studentId = extractParamValue(queryParams, "student_id");
                String sid = studentId != null ? studentId : uid;
                return (List<T>) new ArrayList<>(mockTrainerAssignments.computeIfAbsent(sid, k -> new ArrayList<>()));
            }
            case "user_achievements": {
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
            }
            case "workout_exercises": {
                String wId = extractParamValue(queryParams, "workout_id");
                List<Workout> userWks = mockWorkouts.get(uid);
                List<WorkoutExercise> exList = new ArrayList<>();
                if (userWks != null) {
                    for (Workout w : userWks) {
                        if (wId == null || Objects.equals(w.getId(), wId)) {
                            if (w.getExercises() != null) {
                                for (WorkoutExercise ex : w.getExercises()) {
                                    if (ex.getWorkoutId() == null) ex.setWorkoutId(w.getId());
                                    exList.add(ex);
                                }
                            }
                        }
                    }
                }
                return (List<T>) exList;
            }
            default:
                return new ArrayList<>();
        }
    }

    private String extractParamValue(String query, String param) {
        if (query == null || query.isBlank() || param == null) return null;
        for (String part : query.split("&")) {
            if (part.startsWith(param + "=eq.")) {
                return part.substring((param + "=eq.").length());
            } else if (part.startsWith(param + "=ilike.")) {
                return part.substring((param + "=ilike.").length());
            } else if (part.startsWith(param + "=")) {
                return part.substring((param + "=").length());
            }
        }
        return null;
    }

    public static boolean isValidUuid(String val) {
        if (val == null || val.isBlank()) return false;
        try {
            UUID.fromString(val);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean containsNonUuidTarget(Object body) {
        if (body == null) return false;
        if (body instanceof Goal) {
            String uid = ((Goal) body).getUserId();
            return !isValidUuid(uid);
        }
        if (body instanceof Workout) {
            String uid = ((Workout) body).getUserId();
            return !isValidUuid(uid);
        }
        if (body instanceof Reminder) {
            String uid = ((Reminder) body).getUserId();
            return !isValidUuid(uid);
        }
        if (body instanceof TrainerStudent) {
            String sid = ((TrainerStudent) body).getStudentId();
            return !isValidUuid(sid);
        }
        if (body instanceof TrainerAssignment) {
            String sid = ((TrainerAssignment) body).getStudentId();
            return !isValidUuid(sid);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public <T> T saveMock(String table, Object body, Class<T> clazz) {
        String uid = getCurrentUserId() != null ? getCurrentUserId() : "demo-user-123";
        String newId = UUID.randomUUID().toString();

        if (body instanceof Workout) {
            Workout w = (Workout) body;
            if (w.getId() == null || w.getId().isEmpty()) w.setId(newId);
            String targetUid = (w.getUserId() != null && !w.getUserId().isBlank()) ? w.getUserId() : uid;
            w.setUserId(targetUid);
            mockWorkouts.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, w);
            return (T) w;
        } else if (body instanceof Goal) {
            Goal g = (Goal) body;
            if (g.getId() == null || g.getId().isEmpty()) g.setId(newId);
            String targetUid = (g.getUserId() != null && !g.getUserId().isBlank()) ? g.getUserId() : uid;
            g.setUserId(targetUid);
            mockGoals.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, g);
            return (T) g;
        } else if (body instanceof BodyMeasurement) {
            BodyMeasurement m = (BodyMeasurement) body;
            if (m.getId() == null || m.getId().isEmpty()) m.setId(newId);
            String targetUid = (m.getUserId() != null && !m.getUserId().isBlank()) ? m.getUserId() : uid;
            m.setUserId(targetUid);
            mockMeasurements.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, m);
            return (T) m;
        } else if (body instanceof NutritionEntry) {
            NutritionEntry n = (NutritionEntry) body;
            if (n.getId() == null || n.getId().isEmpty()) n.setId(newId);
            String targetUid = (n.getUserId() != null && !n.getUserId().isBlank()) ? n.getUserId() : uid;
            n.setUserId(targetUid);
            mockNutrition.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, n);
            return (T) n;
        } else if (body instanceof WaterIntake) {
            WaterIntake wi = (WaterIntake) body;
            if (wi.getId() == null || wi.getId().isEmpty()) wi.setId(newId);
            String targetUid = (wi.getUserId() != null && !wi.getUserId().isBlank()) ? wi.getUserId() : uid;
            wi.setUserId(targetUid);
            mockWater.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, wi);
            return (T) wi;
        } else if (body instanceof SleepRecord) {
            SleepRecord sr = (SleepRecord) body;
            if (sr.getId() == null || sr.getId().isEmpty()) sr.setId(newId);
            String targetUid = (sr.getUserId() != null && !sr.getUserId().isBlank()) ? sr.getUserId() : uid;
            sr.setUserId(targetUid);
            mockSleep.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, sr);
            return (T) sr;
        } else if (body instanceof Reminder) {
            Reminder r = (Reminder) body;
            if (r.getId() == null || r.getId().isEmpty()) r.setId(newId);
            String targetUid = (r.getUserId() != null && !r.getUserId().isBlank()) ? r.getUserId() : uid;
            r.setUserId(targetUid);
            mockReminders.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, r);
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
        } else if (body instanceof TrainerAssignment) {
            TrainerAssignment ta = (TrainerAssignment) body;
            if (ta.getId() == null || ta.getId().isEmpty()) ta.setId(newId);
            String targetUid = (ta.getStudentId() != null && !ta.getStudentId().isBlank()) ? ta.getStudentId() : uid;
            mockTrainerAssignments.computeIfAbsent(targetUid, k -> new ArrayList<>()).add(0, ta);
            return (T) ta;
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
        } else if (body instanceof WaterIntake) {
            WaterIntake w = (WaterIntake) body;
            List<WaterIntake> list = mockWater.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), w.getId())) {
                        list.set(i, w);
                        return;
                    }
                }
            }
        } else if (body instanceof SleepRecord) {
            SleepRecord s = (SleepRecord) body;
            List<SleepRecord> list = mockSleep.get(uid);
            if (list != null) {
                for (int i = 0; i < list.size(); i++) {
                    if (Objects.equals(list.get(i).getId(), s.getId())) {
                        list.set(i, s);
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
        } else if (body instanceof TrainerAssignment) {
            TrainerAssignment ta = (TrainerAssignment) body;
            updateMockTrainerAssignment(ta);
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

    public List<TrainerAssignment> getTrainerAssignments(String studentId) {
        if (studentId == null) return new ArrayList<>();
        return new ArrayList<>(mockTrainerAssignments.computeIfAbsent(studentId, k -> new ArrayList<>()));
    }

    public void addMockTrainerAssignment(TrainerAssignment assignment) {
        if (assignment != null && assignment.getStudentId() != null) {
            if (assignment.getId() == null || assignment.getId().isBlank()) {
                assignment.setId(UUID.randomUUID().toString());
            }
            mockTrainerAssignments.computeIfAbsent(assignment.getStudentId(), k -> new ArrayList<>()).add(0, assignment);
        }
    }

    public void updateMockTrainerAssignment(TrainerAssignment assignment) {
        if (assignment == null || assignment.getStudentId() == null || assignment.getId() == null) return;
        List<TrainerAssignment> list = mockTrainerAssignments.get(assignment.getStudentId());
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                if (assignment.getId().equals(list.get(i).getId())) {
                    list.set(i, assignment);
                    return;
                }
            }
        }
    }
}
