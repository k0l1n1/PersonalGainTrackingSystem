package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Model representing an achievement badge awarded to a specific user.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAchievement {
    private String id;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("achievement_id")
    private String achievementId;

    @JsonProperty("unlocked_at")
    private OffsetDateTime unlockedAt = OffsetDateTime.now();

    public UserAchievement() {}

    public UserAchievement(String userId, String achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.unlockedAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getAchievementId() { return achievementId; }
    public void setAchievementId(String achievementId) { this.achievementId = achievementId; }

    public OffsetDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(OffsetDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
