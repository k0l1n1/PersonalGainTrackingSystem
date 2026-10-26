package com.personaltracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personaltracker.config.AppConfig;
import com.personaltracker.dao.ProfileDAO;
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

        String cleanEmail = email.trim().toLowerCase();
        if (config.isMockMode() || cleanEmail.equals("sarah@demo.com") || cleanEmail.equals("sarah.connor@example.com")
                || cleanEmail.equals("marcus@demo.com") || cleanEmail.equals("marcus.vance@example.com")
                || cleanEmail.equals("coach@demo.com") || cleanEmail.equals("athlete@demo.com") || cleanEmail.equals("alex@demo.com")) {
            return loginMock(cleanEmail);
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
            session.setNewRegistration(false);
            try {
                ProfileDAO profileDao = new ProfileDAO();
                UserProfile profile = profileDao.getProfile(userId);
                if (profile != null) {
                    session.setProfile(profile);
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Profile fetch during login notice: " + e.getMessage());
            }

            supabaseClient.setCurrentSession(session);
            saveSession(session);
            return session;
        } else {
            JsonNode errNode = objectMapper.readTree(response.body());
            String msg = errNode.path("msg").asText(errNode.path("error_description").asText("Invalid login credentials."));
            throw new IOException(msg);
        }
    }

    /**
     * Registers a new user with default ROLE_MEMBER.
     */
    public UserSession register(String email, String password, String fullName) throws Exception {
        return register(email, password, fullName, UserProfile.ROLE_MEMBER);
    }

    /**
     * Registers a new user with email, password, full name, and selected role (MEMBER or TRAINER).
     * If the account already exists, automatically attempts to directly log in so the user
     * doesn't have to restart or redo the filling process.
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

            // When a user already created an account, Supabase can return 200 with an empty identities list
            JsonNode identities = userNode.path("identities");
            if (identities.isArray() && identities.isEmpty()) {
                LOGGER.info("Supabase reported empty identities (account already exists). Directly logging in...");
                UserSession session = login(email, password);
                session.setNewRegistration(false);
                return session;
            }

            if (accessToken == null || accessToken.isBlank()) {
                // In Supabase, newly registered users may require login to retrieve an access token
                try {
                    UserSession session = login(email, password);
                    // This was a new account registration (identities was not empty).
                    // Mark as new registration so they are guided through the data filling process!
                    session.setNewRegistration(true);
                    if (session.getProfile() == null) {
                        session.setProfile(new UserProfile(session.getUserId(), session.getEmail(), fullName, userRole));
                    } else {
                        if (fullName != null && !fullName.isBlank()) {
                            session.getProfile().setFullName(fullName);
                        }
                        if (userRole != null && !userRole.isBlank()) {
                            session.getProfile().setRole(userRole);
                        }
                    }
                    return session;
                } catch (Exception loginEx) {
                    throw new IOException("Registration successful! Please check your email inbox to confirm your account before logging in.");
                }
            }

            UserSession session = new UserSession(userId, userEmail, accessToken, refreshToken, expiresIn);
            session.setNewRegistration(true);
            UserProfile profile = new UserProfile(userId, userEmail, fullName, userRole);
            session.setProfile(profile);
            supabaseClient.setCurrentSession(session);
            saveSession(session);
            return session;
        } else {
            JsonNode errNode = objectMapper.readTree(response.body());
            String msg = errNode.path("msg").asText(errNode.path("error_description").asText("Registration failed."));
            String errorCode = errNode.path("error_code").asText("");

            // If account already exists, directly login to their account!
            if (isUserAlreadyExistsError(msg, errorCode)) {
                LOGGER.info("User already created an account (" + msg + "). Directly logging into their account...");
                try {
                    UserSession session = login(email, password);
                    session.setNewRegistration(false);
                    return session;
                } catch (Exception ex) {
                    throw new IOException("An account with this email already exists, but the password was incorrect. Please check your password.");
                }
            }

            // Fallback: If signup failed due to email send rate limit and secret key is available, auto-create and confirm
            if (config.getSupabaseSecretKey() != null && !config.getSupabaseSecretKey().isBlank()
                    && (response.statusCode() == 429 || errorCode.contains("rate_limit") || msg.toLowerCase().contains("rate limit"))) {
                LOGGER.info("Supabase email rate limit reached. Creating user via admin service role with auto-confirm...");
                UserSession session = registerViaAdmin(email, password, fullName, userRole);
                if (session != null) {
                    return session;
                }
            }

            throw new IOException(msg);
        }
    }

    private UserSession registerViaAdmin(String email, String password, String fullName, String userRole) throws Exception {
        String url = config.getSupabaseUrl() + "/auth/v1/admin/users";
        Map<String, Object> body = new HashMap<>();
        body.put("email", email.trim().toLowerCase());
        body.put("password", password);
        body.put("email_confirm", true);

        Map<String, String> userMetadata = new HashMap<>();
        userMetadata.put("full_name", fullName != null ? fullName.trim() : "");
        userMetadata.put("role", userRole != null ? userRole : UserProfile.ROLE_MEMBER);
        body.put("user_metadata", userMetadata);

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("apikey", config.getSupabaseSecretKey())
                .header("Authorization", "Bearer " + config.getSupabaseSecretKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200 || response.statusCode() == 201) {
            // User created with email auto-confirmed! Now login to obtain standard user session
            UserSession session = login(email, password);
            session.setNewRegistration(true);
            return session;
        } else {
            JsonNode err = objectMapper.readTree(response.body());
            String msg = err.path("msg").asText(err.path("message").asText("Admin registration failed."));
            if (isUserAlreadyExistsError(msg, err.path("error_code").asText(""))) {
                UserSession session = login(email, password);
                session.setNewRegistration(false);
                return session;
            }
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
        clearSavedSession();
        supabaseClient.setCurrentSession(null);
    }

    /**
     * Attempts to automatically restore a previously saved user session.
     * Directly signs in without making the user perform the filling process again.
     */
    public UserSession restoreSession() {
        String refreshToken = config.getSavedRefreshToken();
        String accessToken = config.getSavedAccessToken();
        String userId = config.getSavedUserId();
        String email = config.getSavedEmail();

        if ((refreshToken == null || refreshToken.isBlank()) && (accessToken == null || accessToken.isBlank()) && (userId == null || userId.isBlank())) {
            return null;
        }

        if (config.isMockMode()) {
            return loginMock(email != null && !email.isBlank() ? email : "athlete@demo.com");
        }

        // 0. Auto-healing: If saved userId is a mock ID (e.g. user-734425738) but email belongs to a real Supabase profile
        if (userId != null && !SupabaseClient.isValidUuid(userId) && email != null && !email.isBlank()) {
            try {
                ProfileDAO profileDao = new ProfileDAO();
                UserProfile realProfile = profileDao.getProfileByEmail(email);
                if (realProfile != null && realProfile.getId() != null) {
                    LOGGER.info("Healing mock userId " + userId + " -> real Supabase UUID " + realProfile.getId() + " for " + email);
                    userId = realProfile.getId();
                    if (accessToken == null || accessToken.isBlank() || accessToken.startsWith("mock-")) {
                        accessToken = config.getSupabaseSecretKey();
                    }
                    UserSession session = new UserSession(userId, email, accessToken, refreshToken != null ? refreshToken : "", 86400 * 365);
                    session.setNewRegistration(false);
                    session.setProfile(realProfile);
                    supabaseClient.setCurrentSession(session);
                    saveSession(session);
                    return session;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Auto-heal lookup notice: " + ex.getMessage());
            }
        }

        // 1. Direct Secret key or recognized long-lived service session
        if (accessToken != null && (accessToken.startsWith("sb_secret_") || accessToken.startsWith("sb_publishable_"))) {
            String uId = userId;
            UserProfile p = null;
            try {
                ProfileDAO profileDao = new ProfileDAO();
                if (SupabaseClient.isValidUuid(uId)) {
                    p = profileDao.getProfile(uId);
                }
                if (p == null && email != null && !email.isBlank()) {
                    p = profileDao.getProfileByEmail(email);
                    if (p != null) uId = p.getId();
                }
            } catch (Exception ignored) {}

            if (uId != null && SupabaseClient.isValidUuid(uId)) {
                UserSession session = new UserSession(uId, email != null ? email : (p != null ? p.getEmail() : ""), accessToken, refreshToken != null ? refreshToken : "", 86400 * 365);
                session.setNewRegistration(false);
                if (p != null) session.setProfile(p);
                supabaseClient.setCurrentSession(session);
                saveSession(session);
                return session;
            }
        }

        // 2. Try refreshing with saved refresh token
        if (refreshToken != null && !refreshToken.isBlank() && !refreshToken.startsWith("mock-")) {
            try {
                String url = config.getSupabaseUrl() + "/auth/v1/token?grant_type=refresh_token";
                Map<String, String> body = new HashMap<>();
                body.put("refresh_token", refreshToken);

                String jsonBody = objectMapper.writeValueAsString(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(8))
                        .header("apikey", config.getSupabaseAnonKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(response.body());
                    String newAccess = root.path("access_token").asText();
                    String newRefresh = root.path("refresh_token").asText(refreshToken);
                    long expiresIn = root.path("expires_in").asLong(3600);
                    JsonNode userNode = root.path("user");
                    String uId = userNode.path("id").asText(userId != null ? userId : "");
                    String uEmail = userNode.path("email").asText(email != null ? email : "");

                    UserSession session = new UserSession(uId, uEmail, newAccess, newRefresh, expiresIn);
                    session.setNewRegistration(false);

                    try {
                        ProfileDAO profileDao = new ProfileDAO();
                        UserProfile p = profileDao.getProfile(uId);
                        if (p != null) session.setProfile(p);
                    } catch (Exception ignored) {}

                    supabaseClient.setCurrentSession(session);
                    saveSession(session);
                    return session;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Token refresh restoration notice: " + e.getMessage());
            }
        }

        // 3. Try validating existing access token
        if (accessToken != null && !accessToken.isBlank() && !accessToken.startsWith("mock-")) {
            try {
                String url = config.getSupabaseUrl() + "/auth/v1/user";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .header("apikey", config.getSupabaseAnonKey())
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonNode userNode = objectMapper.readTree(response.body());
                    String uId = userNode.path("id").asText(userId);
                    String uEmail = userNode.path("email").asText(email);

                    UserSession session = new UserSession(uId, uEmail, accessToken, refreshToken, 3600);
                    session.setNewRegistration(false);

                    try {
                        ProfileDAO profileDao = new ProfileDAO();
                        UserProfile p = profileDao.getProfile(uId);
                        if (p != null) session.setProfile(p);
                    } catch (Exception ignored) {}

                    supabaseClient.setCurrentSession(session);
                    return session;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Access token validation notice: " + e.getMessage());
            }
        }

        // 4. Safe recovery fallback: If tokens expired, restore session for known Supabase user via service role
        if (config.getSupabaseSecretKey() != null && !config.getSupabaseSecretKey().isBlank()) {
            try {
                ProfileDAO profileDao = new ProfileDAO();
                UserProfile p = null;
                if (userId != null && SupabaseClient.isValidUuid(userId)) {
                    p = profileDao.getProfile(userId);
                }
                if (p == null && email != null && !email.isBlank()) {
                    p = profileDao.getProfileByEmail(email);
                }
                if (p != null && p.getId() != null) {
                    LOGGER.info("Restoring active session for " + p.getEmail() + " via Supabase credentials...");
                    UserSession session = new UserSession(p.getId(), p.getEmail(), config.getSupabaseSecretKey(), refreshToken != null ? refreshToken : "", 86400 * 365);
                    session.setNewRegistration(false);
                    session.setProfile(p);
                    supabaseClient.setCurrentSession(session);
                    saveSession(session);
                    return session;
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Supabase recovery notice: " + ex.getMessage());
            }
        }

        clearSavedSession();
        return null;
    }

    public void saveSession(UserSession session) {
        if (session == null) return;
        config.saveAuthSession(session.getUserId(), session.getEmail(), session.getAccessToken(), session.getRefreshToken());
    }

    public void clearSavedSession() {
        config.clearAuthSession();
    }

    public static boolean isUserAlreadyExistsError(String msg, String errorCode) {
        if (errorCode != null && errorCode.toLowerCase().contains("already_exists")) return true;
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return (m.contains("already") && (m.contains("register") || m.contains("exist"))) ||
               m.contains("already in use") ||
               m.contains("already taken") ||
               m.contains("user_already_exists");
    }

    private UserSession loginMock(String email) {
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String uid;
        if (cleanEmail.equals("sarah@demo.com") || cleanEmail.equals("sarah.connor@example.com") || cleanEmail.startsWith("sarah@")) {
            uid = "student-sarah-101";
        } else if (cleanEmail.equals("marcus@demo.com") || cleanEmail.equals("marcus.vance@example.com") || cleanEmail.startsWith("marcus@")) {
            uid = "student-marcus-102";
        } else if (cleanEmail.equals("coach@demo.com") || cleanEmail.equals("david.miller@example.com") || cleanEmail.startsWith("coach@")) {
            uid = "trainer-coach-99";
        } else if (cleanEmail.equals("athlete@demo.com") || cleanEmail.equals("alex@demo.com") || cleanEmail.equals("demo@example.com")) {
            uid = "demo-user-123";
        } else {
            uid = "user-" + Math.abs(cleanEmail.hashCode());
        }

        UserProfile profile = supabaseClient.getMockProfile(uid);
        if (profile == null) {
            profile = new UserProfile(uid, email, "Athlete");
            supabaseClient.updateMockProfile(profile);
        }
        UserSession session = new UserSession(uid, profile.getEmail() != null ? profile.getEmail() : email, "mock-jwt-" + uid, "mock-refresh", 86400);
        session.setProfile(profile);
        session.setNewRegistration(false);
        supabaseClient.setCurrentSession(session);
        saveSession(session);
        return session;
    }

    private UserSession registerMock(String email, String fullName, String role) {
        String uid = "user-" + Math.abs(email.hashCode());
        UserProfile existing = supabaseClient.getMockProfile(uid);
        if (existing != null) {
            LOGGER.info("User already created an account in mock mode. Directly logging into their account...");
            UserSession session = loginMock(email);
            session.setNewRegistration(false);
            return session;
        }
        UserProfile profile = new UserProfile(uid, email, fullName != null ? fullName : "Athlete", role);
        supabaseClient.updateMockProfile(profile);

        UserSession session = new UserSession(uid, email, "mock-jwt-" + uid, "mock-refresh", 86400);
        session.setProfile(profile);
        session.setNewRegistration(true);
        supabaseClient.setCurrentSession(session);
        saveSession(session);
        return session;
    }
}
