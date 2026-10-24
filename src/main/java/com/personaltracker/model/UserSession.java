package com.personaltracker.model;

import java.time.Instant;

/**
 * Holds authenticated user session state including JWT tokens and profile.
 */
public class UserSession {
    private String userId;
    private String email;
    private String accessToken;
    private String refreshToken;
    private Instant expiresAt;
    private UserProfile profile;
    private boolean newRegistration = false;

    public UserSession(String userId, String email, String accessToken, String refreshToken, long expiresInSeconds) {
        this.userId = userId;
        this.email = email;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = Instant.now().plusSeconds(expiresInSeconds);
        this.profile = new UserProfile(userId, email, "");
        this.newRegistration = false;
    }

    public boolean isNewRegistration() {
        return newRegistration;
    }

    public void setNewRegistration(boolean newRegistration) {
        this.newRegistration = newRegistration;
    }

    public boolean isAuthenticated() {
        return userId != null && !userId.isBlank() && accessToken != null && !accessToken.isBlank();
    }

    public boolean isExpired() {
        if (expiresAt == null) return false;
        return Instant.now().isAfter(expiresAt);
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public UserProfile getProfile() { return profile; }
    public void setProfile(UserProfile profile) { this.profile = profile; }
}
