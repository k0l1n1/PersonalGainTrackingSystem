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

        return goals != null ? goals : Collections.emptyList();
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
