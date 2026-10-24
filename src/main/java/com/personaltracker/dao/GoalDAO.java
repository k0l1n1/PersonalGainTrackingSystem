package com.personaltracker.dao;

import com.personaltracker.model.Goal;
import com.personaltracker.service.SupabaseClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for Goals.
 */
public class GoalDAO {
    private final SupabaseClient client = SupabaseClient.getInstance();

    public List<Goal> getAllGoals() throws Exception {
        String uid = client.getCurrentUserId();
        if (uid == null) return Collections.emptyList();

        List<Goal> goals = client.executeGetList("goals",
                "user_id=eq." + uid + "&order=target_date.asc,created_at.desc",
                Goal.class);

        if (goals == null || goals.isEmpty()) return Collections.emptyList();

        return deduplicateGoals(goals);
    }

    /**
     * Deduplicates goals:
     * If a new or active goal of the same thing (e.g. "Reach Target Weight", same title, or same weight goal)
     * is added or exists, any already completed goal for that same thing is removed.
     * Also keeps only the single most recent active goal if duplicates exist.
     */
    public List<Goal> deduplicateGoals(List<Goal> goals) {
        if (goals == null || goals.isEmpty()) return Collections.emptyList();

        List<Goal> cleaned = new ArrayList<>();
        List<Goal> duplicatesToDelete = new ArrayList<>();

        // Group goals by identity key (e.g. "__WEIGHT_GOAL__" or trimmed lowercase title)
        Map<String, List<Goal>> groups = new LinkedHashMap<>();
        for (Goal g : goals) {
            String key = getGoalKey(g);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(g);
        }

        for (List<Goal> group : groups.values()) {
            if (group.size() == 1) {
                cleaned.add(group.get(0));
                continue;
            }

            // Separate active vs completed in this group
            List<Goal> activeGoals = new ArrayList<>();
            List<Goal> completedGoals = new ArrayList<>();

            for (Goal g : group) {
                if (g.isCompleted()) {
                    completedGoals.add(g);
                } else {
                    activeGoals.add(g);
                }
            }

            if (!activeGoals.isEmpty()) {
                // If a new / active goal of the same thing is present:
                // 1. Keep the most recent active goal
                Goal bestActive = chooseMostRecentGoal(activeGoals);
                cleaned.add(bestActive);

                // Any other active duplicates in this group are deleted
                for (Goal act : activeGoals) {
                    if (act != bestActive) {
                        duplicatesToDelete.add(act);
                    }
                }

                // 2. Remove ANY already completed goal of the same thing!
                // ("if new of the same things is added remove the already completed thing from it")
                duplicatesToDelete.addAll(completedGoals);
            } else {
                // All goals in this group are completed. Keep the most recent completed goal, clean up older duplicates.
                Goal bestCompleted = chooseMostRecentGoal(completedGoals);
                cleaned.add(bestCompleted);
                for (Goal comp : completedGoals) {
                    if (comp != bestCompleted) {
                        duplicatesToDelete.add(comp);
                    }
                }
            }
        }

        // Clean up redundant duplicates from backend asynchronously
        if (!duplicatesToDelete.isEmpty()) {
            new Thread(() -> {
                for (Goal dup : duplicatesToDelete) {
                    if (dup.getId() != null) {
                        try {
                            deleteGoal(dup.getId());
                        } catch (Exception ignored) {}
                    }
                }
            }).start();
        }

        return cleaned;
    }

    public List<Goal> deduplicateWeightGoals(List<Goal> goals) {
        return deduplicateGoals(goals);
    }

    private Goal chooseMostRecentGoal(List<Goal> list) {
        Goal best = null;
        for (Goal g : list) {
            if (best == null) {
                best = g;
            } else {
                if (g.getTargetDate() != null &&
                        (best.getTargetDate() == null || g.getTargetDate().isAfter(best.getTargetDate()))) {
                    best = g;
                } else if (g.getCreatedAt() != null &&
                        (best.getCreatedAt() == null || g.getCreatedAt().isAfter(best.getCreatedAt()))) {
                    best = g;
                }
            }
        }
        return best;
    }

    public static String getGoalKey(Goal g) {
        if (g == null) return "unknown";
        if (isWeightGoal(g)) {
            return "__WEIGHT_GOAL__";
        }
        if (g.getTitle() != null && !g.getTitle().isBlank()) {
            return g.getTitle().trim().toLowerCase();
        }
        return (g.getCategory() != null ? g.getCategory().trim().toLowerCase() : "unknown");
    }

    public static boolean isWeightGoal(Goal g) {
        if (g == null) return false;
        if ("Weight".equalsIgnoreCase(g.getCategory())) return true;
        if (g.getTitle() != null) {
            String t = g.getTitle().trim().toLowerCase();
            return t.contains("target weight") || t.contains("reach weight") || t.equals("weight");
        }
        return false;
    }

    public Goal saveGoal(Goal goal) throws Exception {
        String uid = client.getCurrentUserId();
        goal.setUserId(uid);

        // If new of the same things is added, remove the already completed thing from it
        try {
            List<Goal> existing = client.executeGetList("goals",
                    "user_id=eq." + uid, Goal.class);
            if (existing != null) {
                String newKey = getGoalKey(goal);
                for (Goal ex : existing) {
                    if (newKey.equals(getGoalKey(ex))) {
                        // If already completed, remove it
                        if (ex.isCompleted()) {
                            if (ex.getId() != null) {
                                deleteGoal(ex.getId());
                            }
                        } else if (!goal.isCompleted()) {
                            // If both are active, remove older active one so new one cleanly replaces it
                            if (ex.getId() != null) {
                                deleteGoal(ex.getId());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to clean up duplicate goals prior to save: " + e.getMessage());
        }

        return client.executePost("goals", goal, Goal.class);
    }

    public void updateGoal(Goal goal) throws Exception {
        if (goal.getId() == null) return;
        client.executePatch("goals", "id=eq." + goal.getId(), goal);
    }

    public void deleteGoal(String goalId) throws Exception {
        if (goalId == null) return;
        client.executeDelete("goals", "id=eq." + goalId);
    }
}
