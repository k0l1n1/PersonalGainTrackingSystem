package com.personaltracker.dao;

import com.personaltracker.model.Goal;
import com.personaltracker.service.SupabaseClient;

import java.util.Collections;
import java.util.List;

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

        return deduplicateWeightGoals(goals);
    }

    private List<Goal> deduplicateWeightGoals(List<Goal> goals) {
        if (goals == null || goals.isEmpty()) return Collections.emptyList();

        List<Goal> cleaned = new java.util.ArrayList<>();
        List<Goal> duplicatesToDelete = new java.util.ArrayList<>();
        Goal latestWeightGoal = null;

        for (Goal g : goals) {
            boolean isReachTargetWeight = "Weight".equalsIgnoreCase(g.getCategory()) &&
                    (g.getTitle() != null && g.getTitle().equalsIgnoreCase("Reach Target Weight"));

            if (isReachTargetWeight && !g.isCompleted()) {
                if (latestWeightGoal == null) {
                    latestWeightGoal = g;
                } else {
                    // Choose the more recent target weight goal
                    if (g.getTargetDate() != null &&
                            (latestWeightGoal.getTargetDate() == null || g.getTargetDate().isAfter(latestWeightGoal.getTargetDate()))) {
                        duplicatesToDelete.add(latestWeightGoal);
                        latestWeightGoal = g;
                    } else {
                        duplicatesToDelete.add(g);
                    }
                }
            } else {
                cleaned.add(g);
            }
        }

        if (latestWeightGoal != null) {
            cleaned.add(0, latestWeightGoal);
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

    public Goal saveGoal(Goal goal) throws Exception {
        goal.setUserId(client.getCurrentUserId());
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
