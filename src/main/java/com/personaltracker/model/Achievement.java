package com.personaltracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Model representing an achievement badge in the system.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Achievement {
    private String id;
    private String name;
    private String description;

    @JsonProperty("badge_icon")
    private String badgeIcon;

    private String category; // WORKOUT, GOAL, STREAK, NUTRITION, WATER, WEIGHT

    @JsonIgnore
    private boolean unlocked = false;

    @JsonIgnore
    private OffsetDateTime unlockedAt;

    public Achievement() {}

    public Achievement(String id, String name, String description, String badgeIcon, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.badgeIcon = badgeIcon;
        this.category = category;
        this.unlocked = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBadgeIcon() { return badgeIcon; }
    public void setBadgeIcon(String badgeIcon) { this.badgeIcon = badgeIcon; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @JsonIgnore
    public boolean isUnlocked() { return unlocked; }
    @JsonIgnore
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    @JsonIgnore
    public OffsetDateTime getUnlockedAt() { return unlockedAt; }
    @JsonIgnore
    public void setUnlockedAt(OffsetDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
