package com.personaltracker.dao;

import com.personaltracker.model.Achievement;
import com.personaltracker.model.UserAchievement;
import com.personaltracker.service.SupabaseClient;

import java.util.*;

/**
 * Data Access Object for Achievements and Gamification Badges.
 */
public class AchievementDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    private static final List<Achievement> DEFAULT_CATALOG = Arrays.asList(
            new Achievement("FIRST_WORKOUT", "First Step", "Completed your first recorded workout", "dumbbell", "WORKOUT"),
            new Achievement("WORKOUT_STREAK_3", "On Fire (3-Day Streak)", "Logged workouts on 3 consecutive days", "fire", "STREAK"),
            new Achievement("WORKOUT_STREAK_7", "Unstoppable (7-Day Streak)", "Logged workouts on 7 consecutive days", "flame", "STREAK"),
            new Achievement("WORKOUT_10", "Fitness Enthusiast", "Completed 10 total workout sessions", "medal", "WORKOUT"),
            new Achievement("WORKOUT_50", "Iron Will", "Completed 50 total workout sessions", "trophy", "WORKOUT"),
            new Achievement("FIRST_GOAL", "Goal Getter", "Successfully achieved your first fitness goal", "flag", "GOAL"),
            new Achievement("GOAL_MASTER", "Master of Goals", "Completed 5 fitness goals", "crown", "GOAL"),
            new Achievement("HYDRATION_HERO", "Hydration Hero", "Met your daily water intake target for the first time", "droplet", "WATER"),
            new Achievement("NUTRITION_TRACKER", "Clean Fuel", "Logged nutrition entries for an entire day", "apple", "NUTRITION"),
            new Achievement("BODY_TRACKER", "Self Awareness", "Recorded 5 body measurement check-ins", "ruler", "WEIGHT")
    );

    public List<Achievement> getAllAchievementsWithUserStatus() throws Exception {
        String uid = client.getCurrentUserId();
        List<Achievement> result = new ArrayList<>();

        Set<String> unlockedIds = new HashSet<>();
        if (uid != null) {
            if (com.personaltracker.config.AppConfig.getInstance().isMockMode() || (client.getCurrentSession() != null && "demo-user-123".equals(uid))) {
                unlockedIds.addAll(client.getMockUserAchievements(uid));
            } else {
                try {
                    List<UserAchievement> userBadges = client.executeGetList("user_achievements",
                            "user_id=eq." + uid, UserAchievement.class);
                    if (userBadges != null) {
                        for (UserAchievement ua : userBadges) {
                            unlockedIds.add(ua.getAchievementId());
                        }
                    }
                } catch (Exception ignored) {
                    unlockedIds.addAll(client.getMockUserAchievements(uid));
                }
            }
        }

        for (Achievement a : DEFAULT_CATALOG) {
            Achievement clone = new Achievement(a.getId(), a.getName(), a.getDescription(), a.getBadgeIcon(), a.getCategory());
            clone.setUnlocked(unlockedIds.contains(a.getId()));
            result.add(clone);
        }

        return result;
    }

    public boolean unlockAchievement(String achievementId) {
        String uid = client.getCurrentUserId();
        if (uid == null || achievementId == null) return false;

        if (com.personaltracker.config.AppConfig.getInstance().isMockMode() || (client.getCurrentSession() != null && "demo-user-123".equals(uid))) {
            Set<String> unlocked = client.getMockUserAchievements(uid);
            if (!unlocked.contains(achievementId)) {
                client.unlockMockAchievement(uid, achievementId);
                return true;
            }
            return false;
        }

        try {
            UserAchievement ua = new UserAchievement(uid, achievementId);
            client.executePost("user_achievements", ua, UserAchievement.class);
            return true;
        } catch (Exception e) {
            // Already unlocked (unique constraint violation)
            return false;
        }
    }
}
