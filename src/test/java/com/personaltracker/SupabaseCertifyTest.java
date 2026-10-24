package com.personaltracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaltracker.config.AppConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SupabaseCertifyTest {

    @Test
    public void runFullSupabaseCertification() throws Exception {
        AppConfig config = AppConfig.getInstance();
        String url = config.getSupabaseUrl();
        String anonKey = config.getSupabaseAnonKey();

        System.out.println("==================================================================");
        System.out.println(" SUPABASE LIVE SYSTEM CERTIFICATION REPORT");
        System.out.println("==================================================================");
        System.out.println("Target URL:      " + url);
        System.out.println("Anon Key Set:    " + (anonKey != null && !anonKey.isBlank()));
        System.out.println("Key Prefix:      " + (anonKey != null && anonKey.length() > 15 ? anonKey.substring(0, 15) + "..." : "N/A"));
        System.out.println("==================================================================");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        ObjectMapper mapper = new ObjectMapper();

        // 1. Auth Health Check
        System.out.println("\n[1/5] Checking Supabase GoTrue Auth Health...");
        try {
            HttpRequest authReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/auth/v1/health"))
                    .header("apikey", anonKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> authRes = client.send(authReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("   Status: " + authRes.statusCode());
            System.out.println("   Body:   " + authRes.body());
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getMessage());
        }

        // 2. Auth Settings Check
        System.out.println("\n[2/5] Checking Supabase Auth Settings (Signup / Providers)...");
        try {
            HttpRequest setReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/auth/v1/settings"))
                    .header("apikey", anonKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> setRes = client.send(setReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("   Status: " + setRes.statusCode());
            if (setRes.statusCode() == 200) {
                JsonNode sn = mapper.readTree(setRes.body());
                System.out.println("   Signup Disabled?      " + sn.path("disable_signup").asBoolean(false));
                System.out.println("   Mailer Autoconfirm?   " + sn.path("mailer_autoconfirm").asBoolean(false));
                System.out.println("   External Providers:   " + sn.path("external"));
            } else {
                System.out.println("   Body: " + setRes.body());
            }
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getMessage());
        }

        // 3. Database Tables Verification
        System.out.println("\n[3/5] Verifying Database Schema Tables (PostgREST)...");
        String[] tables = {
                "profiles",
                "workouts",
                "workout_exercises",
                "goals",
                "body_measurements",
                "nutrition_entries",
                "water_intake",
                "sleep_records",
                "reminders",
                "achievements",
                "user_achievements",
                "progress_photos",
                "trainer_students"
        };

        Map<String, List<String>> expectedColumns = new LinkedHashMap<>();
        expectedColumns.put("profiles", List.of("id", "email", "full_name", "role", "user_code", "gender", "date_of_birth", "height_cm", "target_weight_kg", "daily_calorie_target", "daily_water_target_ml"));
        expectedColumns.put("workouts", List.of("id", "user_id", "title", "workout_type", "workout_date", "duration_minutes", "calories_burned", "status", "notes"));
        expectedColumns.put("workout_exercises", List.of("id", "workout_id", "exercise_name", "sets", "reps", "weight_kg", "order_index"));
        expectedColumns.put("goals", List.of("id", "user_id", "title", "category", "target_value", "current_value", "unit", "start_date", "target_date", "status"));
        expectedColumns.put("body_measurements", List.of("id", "user_id", "recorded_date", "weight_kg", "height_cm", "bmi", "waist_cm", "chest_cm", "arm_cm", "hips_cm"));
        expectedColumns.put("nutrition_entries", List.of("id", "user_id", "food_name", "meal_type", "serving_size", "calories", "protein_g", "carbs_g", "fat_g", "entry_date"));
        expectedColumns.put("water_intake", List.of("id", "user_id", "amount_ml", "recorded_date", "recorded_time"));
        expectedColumns.put("sleep_records", List.of("id", "user_id", "sleep_date", "bedtime", "wake_time", "duration_minutes", "quality_score"));
        expectedColumns.put("reminders", List.of("id", "user_id", "title", "description", "reminder_date", "reminder_time", "is_completed"));
        expectedColumns.put("achievements", List.of("id", "name", "description", "badge_icon", "category"));
        expectedColumns.put("user_achievements", List.of("id", "user_id", "achievement_id", "unlocked_at"));
        expectedColumns.put("progress_photos", List.of("id", "user_id", "photo_url", "storage_path", "photo_date", "notes"));
        expectedColumns.put("trainer_students", List.of("id", "trainer_id", "student_id", "enrolled_date", "notes"));

        Map<String, String> tableStatus = new LinkedHashMap<>();
        List<String> missingColumnsReport = new ArrayList<>();

        for (String t : tables) {
            try {
                HttpRequest tReq = HttpRequest.newBuilder()
                        .uri(URI.create(url + "/rest/v1/" + t + "?limit=1"))
                        .header("apikey", anonKey)
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<String> tRes = client.send(tReq, HttpResponse.BodyHandlers.ofString());
                if (tRes.statusCode() == 200) {
                    tableStatus.put(t, "EXISTS (HTTP 200 OK)");

                    // Check columns
                    List<String> cols = expectedColumns.get(t);
                    if (cols != null) {
                        for (String col : cols) {
                            HttpRequest cReq = HttpRequest.newBuilder()
                                    .uri(URI.create(url + "/rest/v1/" + t + "?select=" + col + "&limit=1"))
                                    .header("apikey", anonKey)
                                    .timeout(Duration.ofSeconds(10))
                                    .GET()
                                    .build();
                            HttpResponse<String> cRes = client.send(cReq, HttpResponse.BodyHandlers.ofString());
                            if (cRes.statusCode() != 200) {
                                missingColumnsReport.add(t + "." + col + " (HTTP " + cRes.statusCode() + " - " + cRes.body().trim() + ")");
                            }
                        }
                    }
                } else if (tRes.statusCode() == 404) {
                    tableStatus.put(t, "MISSING (HTTP 404 - Not in schema cache)");
                } else {
                    tableStatus.put(t, "HTTP " + tRes.statusCode() + " (" + tRes.body().trim() + ")");
                }
            } catch (Exception e) {
                tableStatus.put(t, "ERROR: " + e.getMessage());
            }
        }

        for (Map.Entry<String, String> entry : tableStatus.entrySet()) {
            System.out.printf("   %-22s : %s\n", entry.getKey(), entry.getValue());
        }

        if (missingColumnsReport.isEmpty()) {
            System.out.println("   All expected columns verified in existing tables!");
        } else {
            System.out.println("\n   [!] MISSING / MISCONFIGURED COLUMNS DETECTED:");
            for (String colErr : missingColumnsReport) {
                System.out.println("       * " + colErr);
            }
        }

        // 4. Storage Bucket Check
        System.out.println("\n[4/5] Checking Supabase Storage Bucket ('progress_photos')...");
        try {
            HttpRequest sReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/storage/v1/bucket/progress_photos"))
                    .header("apikey", anonKey)
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> sRes = client.send(sReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("   Status: " + sRes.statusCode());
            System.out.println("   Body:   " + sRes.body());
        } catch (Exception e) {
            System.out.println("   FAILED: " + e.getMessage());
        }

        // 5. Test Auth Registration / Login capability
        System.out.println("\n[5/5] Testing Auth Signup Validation...");
        String testEmail = "test_connectivity_check_" + (System.currentTimeMillis() % 100000) + "@gmail.com";
        String testPass = "CertifyPass123!";
        try {
            Map<String, Object> signupBody = new HashMap<>();
            signupBody.put("email", testEmail);
            signupBody.put("password", testPass);
            Map<String, String> meta = new HashMap<>();
            meta.put("full_name", "Supabase Auditor");
            signupBody.put("data", meta);

            HttpRequest regReq = HttpRequest.newBuilder()
                    .uri(URI.create(url + "/auth/v1/signup"))
                    .header("apikey", anonKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(signupBody)))
                    .build();

            HttpResponse<String> regRes = client.send(regReq, HttpResponse.BodyHandlers.ofString());
            System.out.println("   Signup HTTP Status: " + regRes.statusCode());
            if (regRes.statusCode() == 200 || regRes.statusCode() == 201) {
                JsonNode rn = mapper.readTree(regRes.body());
                String uid = rn.path("user").path("id").asText(rn.path("id").asText(""));
                System.out.println("   SUCCESS! User created with ID: " + uid);
                System.out.println("   Session Token Received? " + (rn.hasNonNull("access_token") ? "YES" : "NO (Email confirmation required)"));
            } else {
                System.out.println("   Signup Response: " + regRes.body());
            }
        } catch (Exception e) {
            System.out.println("   Signup Error: " + e.getMessage());
        }

        System.out.println("\n==================================================================");
        System.out.println(" CERTIFICATION RUN COMPLETED");
        System.out.println("==================================================================");
    }
}
