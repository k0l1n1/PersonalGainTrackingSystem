package com.personaltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaltracker.config.AppConfig;
import com.personaltracker.model.UserProfile;
import com.personaltracker.model.UserSession;
import com.personaltracker.util.ValidationUtil;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supabase GoTrue Authentication service.
 * Handles secure registration, password-based login, password recovery, and token management.
 * In offline/demo mode, manages user sessions with validation.
 */
public class SupabaseAuthService {
    private static final Logger LOGGER = Logger.getLogger(SupabaseAuthService.class.getName());
    private static SupabaseAuthService instance;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppConfig config;
    private final SupabaseClient supabaseClient;

    private SupabaseAuthService() {
        this.config = AppConfig.getInstance();
        this.supabaseClient = SupabaseClient.getInstance();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = supabaseClient.getObjectMapper();
    }

    public static synchronized SupabaseAuthService getInstance() {
        if (instance == null) {
            instance = new SupabaseAuthService();
        }
        return instance;
    }

    /**
     * Authenticates a user with email and password via Supabase Auth.
     */
    public UserSession login(String email, String password) throws Exception {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        if (config.isMockMode()) {
            return loginMock(email.trim().toLowerCase());
        }

        String url = config.getSupabaseUrl() + "/auth/v1/token?grant_type=password";
        Map<String, String> body = new HashMap<>();
        body.put("email", email.trim().toLowerCase());
        body.put("password", password);

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String accessToken = root.path("access_token").asText();
            String refreshToken = root.path("refresh_token").asText();
            long expiresIn = root.path("expires_in").asLong(3600);
            JsonNode userNode = root.path("user");
            String userId = userNode.path("id").asText();
            String userEmail = userNode.path("email").asText();

            UserSession session = new UserSession(userId, userEmail, accessToken, refreshToken, expiresIn);
            supabaseClient.setCurrentSession(session);
            return session;
        } else {
            JsonNode errNode = objectMapper.readTree(response.body());
            String msg = errNode.path("msg").asText(errNode.path("error_description").asText("Invalid login credentials."));
            throw new IOException(msg);
        }
    }

    /**
    public UserSession register(String email, String password, String fullName) throws Exception {
        return register(email, password, fullName, UserProfile.ROLE_MEMBER);
    }

    /**
     * Registers a new user with email, password, full name, and selected role (MEMBER or TRAINER).
     */
    public UserSession register(String email, String password, String fullName, String role) throws Exception {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }

        String userRole = (role != null && !role.isBlank()) ? role : UserProfile.ROLE_MEMBER;

        if (config.isMockMode()) {
            return registerMock(email.trim().toLowerCase(), fullName, userRole);
        }

        String url = config.getSupabaseUrl() + "/auth/v1/signup";
        Map<String, Object> body = new HashMap<>();
        body.put("email", email.trim().toLowerCase());
        body.put("password", password);

        Map<String, String> data = new HashMap<>();
        data.put("full_name", fullName != null ? fullName.trim() : "");
        data.put("role", userRole);
        body.put("data", data);

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 201) {
            JsonNode root = objectMapper.readTree(response.body());
            String accessToken = root.path("access_token").asText(null);
            String refreshToken = root.path("refresh_token").asText(null);
            long expiresIn = root.path("expires_in").asLong(3600);
            JsonNode userNode = root.has("user") ? root.path("user") : root;
            String userId = userNode.path("id").asText();
            String userEmail = userNode.path("email").asText();

            if (accessToken == null || accessToken.isBlank()) {
                // If email confirmation is enabled on Supabase, an access token won't be returned yet
                throw new IOException("Registration successful! Please check your email inbox to confirm your account before logging in.");
            }

            UserSession session = new UserSession(userId, userEmail, accessToken, refreshToken, expiresIn);
            UserProfile profile = new UserProfile(userId, userEmail, fullName, userRole);
            session.setProfile(profile);
            supabaseClient.setCurrentSession(session);
            return session;
        } else {
            JsonNode errNode = objectMapper.readTree(response.body());
            String msg = errNode.path("msg").asText(errNode.path("error_description").asText("Registration failed."));
            throw new IOException(msg);
        }
    }

    /**
     * Triggers Supabase password recovery email.
     */
    public void resetPassword(String email) throws Exception {
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Please enter a valid email address.");
        }

        if (config.isMockMode()) {
            LOGGER.info("Mock password reset link sent to: " + email);
            return;
        }

        String url = config.getSupabaseUrl() + "/auth/v1/recover";
        Map<String, String> body = new HashMap<>();
        body.put("email", email.trim().toLowerCase());

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseAnonKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            JsonNode errNode = objectMapper.readTree(response.body());
            String msg = errNode.path("msg").asText(errNode.path("error_description").asText("Password reset request failed."));
            throw new IOException(msg);
        }
    }

    public void logout() {
        if (!config.isMockMode() && supabaseClient.isAuthenticated()) {
            try {
                String url = config.getSupabaseUrl() + "/auth/v1/logout";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("apikey", config.getSupabaseAnonKey())
                        .header("Authorization", "Bearer " + supabaseClient.getCurrentSession().getAccessToken())
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error on remote logout: " + e.getMessage());
            }
        }
        supabaseClient.setCurrentSession(null);
    }

    private UserSession loginMock(String email) {
        String uid = "user-" + Math.abs(email.hashCode());
        UserProfile profile = supabaseClient.getMockProfile(uid);
        if (profile == null) {
            profile = new UserProfile(uid, email, "Athlete");
            supabaseClient.updateMockProfile(profile);
        }
        UserSession session = new UserSession(uid, email, "mock-jwt-" + uid, "mock-refresh", 86400);
        session.setProfile(profile);
        supabaseClient.setCurrentSession(session);
        return session;
    }

    private UserSession registerMock(String email, String fullName, String role) {
        String uid = "user-" + Math.abs(email.hashCode());
        UserProfile profile = new UserProfile(uid, email, fullName != null ? fullName : "Athlete", role);
        supabaseClient.updateMockProfile(profile);

        UserSession session = new UserSession(uid, email, "mock-jwt-" + uid, "mock-refresh", 86400);
        session.setProfile(profile);
        supabaseClient.setCurrentSession(session);
        return session;
    }
}
